package com.diu.yk_games.line2box.presentation

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.model.*
import com.diu.yk_games.line2box.model.MsgStore.MessageType
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.util.*
import com.google.android.gms.games.PlayGames
import com.google.firebase.Firebase
import com.google.firebase.auth.PlayGamesAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.database.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.jsoup.Jsoup
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MainViewModel(
    context: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(context) {
    private var hasInitializedPlayGameUser = false

    val uiEvents: SharedFlow<MainUiEvent>
        field = MutableSharedFlow<MainUiEvent>()

    var lastMotionState: Int? = null
    val firebaseAuth by lazy { Firebase.auth }
    val database by lazy { Firebase.database }
    val firestore by lazy { Firebase.firestore }
    val globalChatRef by lazy { database.getReference("globalChat") }
    val multiPlayerRef by lazy { database.getReference("MultiPlayer") }
    val friendlyChatRef
        get() = multiPlayerRef.child(matchKey)
            .child("friendlyChat")
            .takeIf { matchKey.isNotEmpty() }
    val gamerProfileRef by lazy { firestore.collection("gamerProfile") }

    @OptIn(ExperimentalUuidApi::class)
    val uuidV7  //fake key
        get() = Uuid.generateV7().toString()

    var onlineStatus = ""

    val isLoading = savedStateHandle.getStateFlow("isLoading", true)
    val globalChatList = savedStateHandle.getStateFlow("globalChatList", emptyList<MsgStore>())
    val friendsChatList = savedStateHandle.getStateFlow("friendsChatList", emptyList<MsgStore>())

    val matches = savedStateHandle.getStateFlow("matches", emptyList<GameRoom>())

    var ignoreDrawerClosesSound = false
    var localPlayerCount = 2

    var gameId
        get() = savedStateHandle["gameId"] ?: ""
        set(value) {
            savedStateHandle["gameId"] = value
        }

    var playerId
        get() = savedStateHandle["playerId"] ?: ""
        set(value) {
            savedStateHandle["playerId"] = value
        }

    var matchInfo by savedStateHandle.saved { Routes.GameOnline() }

    var matchKey
        get() = savedStateHandle["matchKey"] ?: ""
        set(value) {
            savedStateHandle["matchKey"] = value
            if (value.isNotEmpty()) {
                fetchFriendlyChat()
                addTempKey(value)
            }
        }

    var gameOnline = Routes.GameOnline()

    val tempKeys
        get() = savedStateHandle.get<List<String>>("tempKeys") ?: emptyList()

    var isStickySwitchRight
        get() = savedStateHandle.get<Boolean>("isStickySwitchRight") == true
        set(value) {
            savedStateHandle["isStickySwitchRight"] = value
        }

    val isNewMsgBoltVisible = savedStateHandle.getStateFlow("isNewMsgBoltVisible", false)

    val gameProfileState: StateFlow<GameProfile>
        field = MutableStateFlow(GameProfile())

    val gameProfile
        get() = gameProfileState.value

    val settingsState: StateFlow<Settings>
        field = MutableStateFlow(Settings())

    val settings
        get() = settingsState.value

    val player = SoundEffectPlayer(context, isMuted = { settings.isMuted })

    init {
        viewModelScope.launch(Dispatchers.IO) {
            settingsState.value = pref.read(PrefKeys.SETTINGS, Settings())
            initGameProfile()
        }
    }

    override fun onCleared() {
        clearTempMatches()
        player.release()
    }

    fun updateSettings(settings: Settings) {
        updateSettings { settings }
        player.playButtonClickSound()
    }

    fun updateSettings(transform: Settings.() -> Settings) {
        val updated = settings.transform()
        settingsState.value = updated
        pref.save(PrefKeys.SETTINGS, updated)
    }

    fun updateProfile(profile: GameProfile) {
        updateProfile { profile }
        pref.save(PrefKeys.PROFILE, profile)
    }

    fun updateProfile(transform: GameProfile.() -> GameProfile) {
        val updated = gameProfile.transform()
        gameProfileState.value = updated
        pref.save(PrefKeys.PROFILE, updated)
    }

    fun doOnMatchEnd(rewardCoin: Int, isWin: Boolean = true) {
        val coinDelta = if (isWin) rewardCoin else -rewardCoin

        // 1. Update local UI state
        updateProfile {
            copy(
                matchPlayed = matchPlayed + 1,
                matchWinMulti = if (isWin) matchWinMulti + 1 else matchWinMulti,
                coin = coin + coinDelta
            )
        }

        // 2. Build Firestore atomic updates
        val updates = buildMap {
            put("matchPlayed", FieldValue.increment(1))
            put("coin", FieldValue.increment(coinDelta.toLong()))
            if (isWin) put("matchWinMulti", FieldValue.increment(1))
        }

        // 3. Execute Firestore update with error handling
        gamerProfileRef.document(matchInfo.currentPlayerId)
            .update(updates)
            .addOnFailureListener { exception ->
                Log.d(TAG, "doOnMatchEnd: failure", exception)
            }
    }

    fun initializePlayGameUser(activity: Activity) {
        if (hasInitializedPlayGameUser) return
        hasInitializedPlayGameUser = true
        setLoading(true)

        val gamesSignInClient = PlayGames.getGamesSignInClient(activity)
        val playersClient = PlayGames.getPlayersClient(activity)
        gamesSignInClient.isAuthenticated
            .addOnSuccessListener { authenticationResult ->
                val isAuthenticated = authenticationResult.isAuthenticated
                if (ConnectivityObserver.isConnected) {
                    gamesSignInClient.requestServerSideAccess(
                        getApplication<Application>().getString(R.string.default_web_client_id),
                        false
                    ).addOnSuccessListener { serverAuthToken ->
                        val credential = PlayGamesAuthProvider.getCredential(serverAuthToken)
                        firebaseAuth.signInWithCredential(credential)
                            .addOnSuccessListener {
                                Log.d(TAG, "signInWithCredential: success")
                                if (settings.showHadith)
                                    uiEvents.tryEmit(MainUiEvent.ShowHadith)

                                val user = firebaseAuth.currentUser
                                if (isAuthenticated && user != null) {
                                    playersClient.currentPlayer.addOnSuccessListener { player ->
                                        val profileNeeded = playerId != player.playerId
                                        playerId = player.playerId
                                        player.playerId.log("playerId")
                                        if (profileNeeded || pref.read("needProfile", true)) {
                                            firestore.collection("gamerProfile")
                                                .document(player.playerId)
                                                .get().addOnSuccessListener { document ->
                                                    if (document.exists()) {
                                                        pref.save("needProfile", false)
                                                        loadProfileFromServer()
                                                        Log.d(TAG, "Profile exists!")
                                                        uiEvents.tryEmit(
                                                            MainUiEvent.ShowToast("Profile Exists and Loaded!")
                                                        )
                                                    } else {
                                                        Log.d(TAG, "Profile does not exist!")
                                                        setupNewUserProfile()
                                                    }
                                                }.addOnFailureListener {
                                                    Log.d(TAG, "Failed with: ", it)
                                                    onlineStatus = "needReload"
                                                    setLoading(false)
                                                }
                                        } else {
                                            loadProfileFromServer()
                                        }
                                    }
                                    uiEvents.tryEmit(MainUiEvent.UpdateUi(ErrorType.NoError))
                                } else {
                                    Log.d(TAG, "gamesSignInClient. isAuthenticated false")
                                    authenticationFailed(ErrorType.AuthenticationFailure)
                                }
                            }
                            .addOnFailureListener {
                                Log.d(TAG, "firebaseAuth signInWithCredential: failure: $it")
                                authenticationFailed(ErrorType.AuthenticationFailure)
                            }
                    }.addOnFailureListener {
                        Log.d(TAG, "requestServerSideAccess:failure authentication code $it")
                        authenticationFailed(ErrorType.PlayServiceNeeded)
                    }
                } else {
                    Log.d(TAG, "No Internet")
                    authenticationFailed(ErrorType.NoInternet)
                }
            }.addOnFailureListener {
                Log.d(TAG, "gamesSignInClient. isAuthenticated failure: $it")
                authenticationFailed(ErrorType.PlayServiceNeeded)
            }
    }

    private fun authenticationFailed(errorType: ErrorType) {
        uiEvents.tryEmit(MainUiEvent.UpdateUi(errorType))
        onlineStatus = "needReload"
        setLoading(false)
    }

    private fun loadProfileFromServer() {
        if (playerId.isEmpty()) return
        firestore.collection("gamerProfile")
            .document(playerId)
            .addSnapshotListener { snapshot, exception ->
                exception?.let {
                    Log.d(TAG, "loadProfileFromServer: failure", exception)
                }
                snapshot?.toObject<GameProfile>()?.let(::updateProfile)
            }
        onlineStatus = "pass"
        setLoading(false)
    }

    private fun setupNewUserProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            val playerId = this@MainViewModel.playerId.takeIf { it.isNotEmpty() } ?: return@launch
            val profile = GameProfile(playerId = playerId)
            val countryPair = tryGet {
                val doc = Jsoup.connect(Constants.IP_INFO_URL).ignoreContentType(true).get()
                Log.d(TAG, "getLocation: Success")
                val countryInfo = Gson().fromJson(doc.body().text(), CountryInfo::class.java)
                Log.d(TAG, "getLocation: $countryInfo")
                profile.query = countryInfo.query
                profile.cityNm = countryInfo.city
                GameUtils.countryList.find { it.first == countryInfo.country }
                    ?: GameUtils.countryList.find {
                        it.first.contains(countryInfo.country, ignoreCase = true)
                    }
            } ?: ("Palestina" to "🇵🇸")

            Log.d(TAG, "country ${countryPair.first}, emoji ${countryPair.second}")
            profile.countryNm = countryPair.first
            profile.countryEmoji = countryPair.second
            updateProfile(profile)

            firestore.collection("gamerProfile").document(playerId)
                .set(profile)
                .addOnSuccessListener {
                    pref.save("needProfile", false)
                    onlineStatus = "pass"
                    setLoading(false)
                    Log.d(TAG, "Profile Created")
                }
                .addOnFailureListener {
                    Log.d(TAG, "Profile Creation Failed")
                    onlineStatus = "needReload"
                    setLoading(false)
                }
        }
    }

    fun setNewMsgBoltVisible(value: Boolean) {
        savedStateHandle["isNewMsgBoltVisible"] = value
    }

    fun setLoading(value: Boolean) {
        savedStateHandle["isLoading"] = value
    }

    fun initGameProfile() {
        gameProfileState.value = pref.read(PrefKeys.PROFILE, GameProfile())
    }

    fun initMultiplayer() {
        gameOnline = Routes.GameOnline()
        fetchGlobalChat()
        fetchActiveMatches()
    }

    fun addTempKey(key: String?) {
        if (key.isNullOrEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            savedStateHandle["tempKeys"] = (tempKeys + key).distinct().filter(String::isNotEmpty)
            pref.save("tmpKey", key)
            tempKeys.log("tempKeys")
        }
    }

    fun clearFriendlyChat() {
        viewModelScope.launch(Dispatchers.IO) {
            savedStateHandle["friendsChatList"] = emptyList<MsgStore>()
            friendlyValueListener?.also { _friendlyChatRef?.removeEventListener(it) }
        }
    }

    fun clearTempMatches() {
        viewModelScope.launch(Dispatchers.IO) {
            savedStateHandle["friendsChatList"] = emptyList<MsgStore>()
            tempKeys.forEach {
                if (it.isEmpty()) return@forEach
                if (it == matchKey)
                    matchKey = ""
                multiPlayerRef.child(it).removeValue()
                savedStateHandle["tempKeys"] = tempKeys - it
                pref.read<String?>("tmpKey", null)?.let { tmp ->
                    if (tmp == it) pref.remove("tmpKey")
                }
            }
        }
    }

    fun removeTempMatch() {
        viewModelScope.launch(Dispatchers.IO) {
            pref.read<String?>("tmpKey", null)?.let {
                multiPlayerRef.child(it).removeValue()
                pref.remove("tmpKey")
            }
        }
    }

    val viewIdFromServer = MutableSharedFlow<String>()

    fun fetchServerLineClick(gameKey: String = matchKey, isPlyr1: Boolean) {
        viewModelScope.launch {
            matchKey = gameKey
            val matchRef = multiPlayerRef.child(gameKey).child("matchInfo")
            matchRef.child(if (isPlyr1) "plyr2" else "plyr1")
                .addChildEventListener(object : ChildEventListener {
                    override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                        val idFromServer = dataSnapshot.getValue<String>() ?: return
                        viewModelScope.launch {
                            viewIdFromServer.emit(idFromServer)
                        }
                    }

                    override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
                    override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
                    override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.w("TAG", "Failed to read value.", databaseError.toException())
                    }
                })
        }
    }

    fun fetchGlobalChat() {
        viewModelScope.launch {
            globalChatRef.limitToLast(150).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val chatList = snapshot.children.mapNotNull {
                        val key = it.key
                        val ms = it.getValue<MsgStore>()
                        if (key == null || ms == null) return@mapNotNull null
                        ms.copy(key = key)
                    }.reversed()
                    savedStateHandle["globalChatList"] = chatList
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }
    }

    private var friendlyValueListener: ValueEventListener? = null
    private var _friendlyChatRef: DatabaseReference? = null

    fun fetchFriendlyChat() {
        viewModelScope.launch {
            val matchKey = matchKey
            if (matchKey.isEmpty()) return@launch
            friendlyValueListener?.also {
                _friendlyChatRef?.removeEventListener(it)
                friendlyValueListener = null
                _friendlyChatRef = null
            }
            _friendlyChatRef = friendlyChatRef
            friendlyValueListener =
                _friendlyChatRef?.limitToLast(100)
                    ?.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val chatList = snapshot.children.mapNotNull {
                                val key = it.key
                                val ms = it.getValue<MsgStore>()
                                if (key == null || ms == null) return@mapNotNull null
                                if (ms.type.typeEnum == MessageType.ExitText)
                                    localPlayerCount--
                                ms.copy(key = key)
                            }.reversed()
                            savedStateHandle["friendsChatList"] = chatList
                        }

                        override fun onCancelled(databaseError: DatabaseError) {}
                    })
        }
    }

    fun getMessageType(msg: String, default: MessageType = MessageType.Normal): MessageType {
        val command = ChatCommand.entries.find {
            msg.lowercase().startsWith(it.command.lowercase())
        }
        return if (command != null) MessageType.Command else default
    }

    fun sendMessage2FriendlyChat(
        text: String,
        type: MessageType = Normal,
        command: ChatCommand? = null,
        flag: ChatFlag? = null
    ): Unit? {
        // TODO: IMPROVE
        if (text.isEmpty() || matchKey.isEmpty()) return null
        viewModelScope.launch(Dispatchers.IO) {
            friendlyChatRef?.push()?.setValue(
                gameProfile.toMessage(
                    playerId = playerId,
                    msg = text,
                    type = getMessageType(text, type)
                )
            )
        }
        return Unit
    }

    fun sendMessage2GlobalChat(
        text: String,
        command: ChatCommand? = null,
        flag: ChatFlag? = null
    ): Unit? {
        if (text.isEmpty()) return null
        viewModelScope.launch(Dispatchers.IO) {
            globalChatRef.push().setValue(
                gameProfile.toMessage(
                    playerId = playerId,
                    msg = text,
                    type = getMessageType(text)
                )
            )
        }
        return Unit
    }

    fun sendInvitation2Chat(gameId: String?, text: String): Job? {
        if (gameId.isNullOrEmpty()) return null
        return viewModelScope.launch(Dispatchers.IO) {
            globalChatRef.push().setValue(
                gameProfile.toMessage(playerId = playerId, msg = text).copy(
                    gameId = gameId,
                    type = MessageType.Invitation.name
                )
            )
        }
    }

    fun sendCommand(command: ChatCommand, text: String, mode: ChatMode) {
        player.playButtonClickSound()
        val ref = if (mode == ChatMode.GLOBAL) globalChatRef else multiPlayerRef
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sendCommandMessage(ref, command)
                when (command) {
                    ChatCommand.DeleteLast -> deleteLastMessage(ref)
                    ChatCommand.ClearAll -> clearChat(ref)
                    ChatCommand.LastUser -> showLastUser(ref)
                }
            } catch (e: Exception) {
                Log.d(TAG, "sendCommand: ${e.message}")
            }
        }
    }

    private suspend fun deleteLastMessage(ref: DatabaseReference) {
        globalChatList.value.firstOrNull()?.let {
            ref.child(it.key)
                .removeValue()
                .await()
        } ?: error("No message to delete")
    }

    private suspend fun clearChat(ref: DatabaseReference) {
        ref.push().setValue(
            ChatCommand.bot.copy(
                playerId = playerId,
                msgData = "\uFE0E\n\n\n\n\n\n\n\n\n\n\n\n\n\n" +
                        "\uFE0Eㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤ\uFE0E" +
                        "\uFE0Eㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤ\uFE0E" +
                        "\uFE0Eㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤ\uFE0E" +
                        "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\uFE0E"
            )
        ).await()
    }

    private suspend fun showLastUser(ref: DatabaseReference) {
        globalChatList.value.find { it.type == MessageType.Normal.name && it.playerId != playerId }?.playerId?.let { playerId ->
            val playerInfo =
                gamerProfileRef.document(playerId).get().await().toObject<GameProfile>()
                    ?: error("No player found")
            globalChatRef.push().setValue(
                ChatCommand.bot.copy(
                    playerId = playerInfo.playerId,
                    type = MessageType.UserInfo.name,
                    user = playerInfo
                )
            )
        } ?: error("No message to show")
    }

    private suspend fun sendCommandMessage(ref: DatabaseReference, command: ChatCommand) {
        ref.push().setValue(
            gameProfile.toMessage(
                playerId = playerId,
                msg = command.name,
                type = MessageType.Command
            )
        ).await()
    }

    fun clearMultiPlayerDB() {
        viewModelScope.launch(Dispatchers.IO) {
            multiPlayerRef.removeValue()
        }

    }

    fun fuckIL() {
        Firebase.firestore.collection("gamerProfile").whereEqualTo("countryEmoji", "🇮🇱")
            .addSnapshotListener { qs, _ ->
                qs?.documents?.mapNotNull { it?.toObject<GameProfile>() }?.forEach {
                    it.countryEmoji = "🇵🇸"
                    it.countryNm = "Palestina"
                    Firebase.firestore.collection("gamerProfile").document(it.playerId).set(it)
                }
            }
    }

    fun fetchActiveMatches() {
        viewModelScope.launch(Dispatchers.IO) {
            multiPlayerRef.limitToLast(100).addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    Log.d("addList", "onChildAdded: " + dataSnapshot.key)
                    runCatching {
                        dataSnapshot.getValue<GameRoom>()?.let { game ->
                            savedStateHandle["matches"] =
                                (matches.value + game.copy(key = dataSnapshot.key.orEmpty())).distinctBy { it.key }
                        }
                    }
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
                    removeByKey(dataSnapshot.key)
                    runCatching {
                        dataSnapshot.getValue<GameRoom>()?.let { game ->
                            savedStateHandle["matches"] =
                                (matches.value + game.copy(key = dataSnapshot.key.orEmpty()))
                        }
                    }
                }

                override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                    removeByKey(dataSnapshot.key)
                }

                private fun removeByKey(key: String?) {
                    key?.let { key ->
                        matches.value.toMutableList().apply {
                            if (removeIf { it.key == key })
                                savedStateHandle["matches"] = toList()
                        }
                    }
                }

                override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w("TAG", "Failed to read value.", databaseError.toException())
                }
            })
        }
    }

    fun getValidMatch(shortKey: String): GameRoom? =
        matches.value.find { getKey4(it.key) == shortKey }

    fun getKey4(key: String = matchKey): String {
        if (key.isEmpty()) return ""
        return buildString {
            for (i in 4..key.length) {
                if (length == 4) break
                when (key[i]) {
                    '0', 'O', 'o' -> append('M')
                    '-', '_' -> continue
                    else -> append(key[i])
                }
            }
        }.uppercase()
    }

    fun getJoinRoute(msg: MsgStore): Result<Routes.GameOnline> {
        // TODO: IMPROVE
        fun defError(): Result<Routes.GameOnline> {
            if (matches.value.isNotEmpty())
                globalChatRef.child(msg.key).removeValue()
            return Result.failure(Exception("Match expired."))
        }
        if (msg.gameId.length != 4) return defError()
        val gameRoom = getValidMatch(msg.gameId) ?: return defError()
        val fullKey = gameRoom.key
        if (fullKey.isEmpty()) return Result.failure(Exception("Match expired."))
        pref.save("tmpKey", fullKey)

        if (gameRoom.playerCount == "-1")
            return defError()
        if (gameRoom.player1.id == playerId || gameRoom.player2.id == playerId)
            return Result.failure(Exception("You are already in the match."))
        if (gameRoom.playerCount == "2")
            return Result.failure(Exception("Match already started."))

        sendInitialMessage(gameRoom)
        /*        multiPlayerRef.child(fullKey).apply {
                    child("player2").setValue(gameProfile.toPlayerInfo())
                    child("playerCount").setValue("2")
                }
                // Send join message
                friendlyChatRef?.push()?.setValue(
                    gameProfile.toMessage(
                        playerId = playerId,
                        msg = "Joined the match.",
                        type = MessageType.EnterText
                    )
                )*/
        return Result.success(
            Routes.GameOnline(
                gameKey = fullKey,
                isPlyr1 = false,
                plr1Id = gameRoom.player1.id,
                nm1 = gameRoom.player1.nm,
                lvl1 = gameRoom.player1.lvl,
                plr2Id = playerId,
                nm2 = gameProfile.nm,
                lvl2 = gameProfile.lvlByCal()
            )
        )
    }

    fun sendInitialMessage(gameRoom: GameRoom, isPlyr1: Boolean = false) {
        matchKey = gameRoom.key
        val ms = gameProfile.toMessage(
            playerId = playerId,
            msg = if (isPlyr1) "Created the match." else "Joined the match.",
            type = MessageType.EnterText
        )
        if (isPlyr1) {
            // Player 1: Create room with initial message
            multiPlayerRef.child(gameRoom.key).setValue(
                gameRoom.copy(
                    player1 = gameProfile.toPlayerInfo(),
                    friendlyChat = mapOf(getFriendlyChatKey() to ms)
                )
            )
        } else {
            // Player 2: Join existing room
            multiPlayerRef.child(gameRoom.key).updateChildren(
                mapOf(
                    "playerCount" to "2",
                    "player2" to gameProfile.toPlayerInfo(),
                    "friendlyChat" to gameRoom.friendlyChat + mapOf(getFriendlyChatKey() to ms)
                )
            )
        }
    }

    fun getFriendlyChatKey(): String {
        return friendlyChatRef?.push()?.key ?: uuidV7
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}

sealed interface MainUiEvent {
    data object ShowHadith : MainUiEvent
    data class ShowToast(val message: String) : MainUiEvent
    data class UpdateUi(val errorType: ErrorType) : MainUiEvent
}
