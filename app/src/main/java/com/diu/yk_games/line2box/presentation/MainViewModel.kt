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
import com.diu.yk_games.line2box.presentation.component.DynamicIslandController
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.util.*
import com.google.android.gms.games.PlayGames
import com.google.firebase.Firebase
import com.google.firebase.auth.PlayGamesAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.database.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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

    val uiEvents = MutableStateFlow<MainUiEvent?>(null)
    var currentRoute: Routes = Routes.Home

    var lastMotionState: Int? = null
    val firebaseAuth by lazy { Firebase.auth }
    val database by lazy { Firebase.database }
    val firestore by lazy { Firebase.firestore }
    val globalChatRef by lazy { database.getReference("globalChat") }
    val multiPlayerRef by lazy { database.getReference("MultiPlayer") }
    val gamerProfileRef by lazy { firestore.collection("gamerProfile") }

    val matchRef
        get() = multiPlayerRef.child(matchRouteInfo.gameKey)
            .takeIf { matchRouteInfo.gameKey.isNotEmpty() }
    val friendlyChatRef
        get() = matchRef?.child("friendlyChat")
    val matchLiveRef
        get() = matchRef?.child("matchInfo")

    @OptIn(ExperimentalUuidApi::class)
    val uuidV7  // uuid as fallback
        get() = Uuid.generateV7().toString()

    var onlineStatus by savedStateHandle.saved { OnlineStatus.Offline }
    val isConnected
        get() = onlineStatus == OnlineStatus.Online || ConnectivityObserver.isConnected

    val isLoading = savedStateHandle.getStateFlow("isLoading", true)
    val globalChatList = savedStateHandle.getStateFlow("globalChatList", emptyList<MsgStore>())
    val friendlyChatList = savedStateHandle.getStateFlow("friendlyChatList", emptyList<MsgStore>())

    val matches = savedStateHandle.getStateFlow("matches", emptyList<GameRoom>())

    var ignoreDrawerClosesSound = false
    var localPlayerCount = 2

    var playerId by savedStateHandle.saved { "" }

    var matchRouteInfo by savedStateHandle.saved { Routes.GameOnline() }

    var tempKeys by savedStateHandle.saved { emptyList<String>() }

    var isStickySwitchRight by savedStateHandle.saved { false }
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
        removeProfileFromServerListener()
        removeGlobalChatListener()
        removeFriendlyChatListener()
        removeActiveMatchesListener()
        removeServerLineClickListener()
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
        gamerProfileRef.document(matchRouteInfo.currentPlayerId)
            .update(updates)
            .addOnFailureListener {
                it.logError("doOnMatchEnd")
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
                                    uiEvents.value = MainUiEvent.ShowHadith

                                val user = firebaseAuth.currentUser
                                if (isAuthenticated && user != null) {
                                    playersClient.currentPlayer.addOnSuccessListener { player ->
                                        val profileNeeded = playerId != player.playerId
                                        playerId = player.playerId
                                        player.playerId.log("playerId")
                                        if (profileNeeded) {
                                            firestore.collection("gamerProfile")
                                                .document(player.playerId)
                                                .get().addOnSuccessListener { document ->
                                                    if (document.exists()) {
                                                        loadProfileFromServer()
                                                        Log.d(TAG, "Profile exists!")
                                                        uiEvents.value =
                                                            MainUiEvent.ShowToast("Profile Exists and Loaded!")
                                                    } else {
                                                        Log.d(TAG, "Profile does not exist!")
                                                        setupNewUserProfile()
                                                    }
                                                }.addOnFailureListener {
                                                    authenticationFailed(
                                                        ErrorType.ServerResponseFailure,
                                                        it
                                                    )
                                                }
                                        } else {
                                            loadProfileFromServer()
                                        }
                                    }
                                } else {
                                    Log.d(TAG, "gamesSignInClient. isAuthenticated false")
                                    authenticationFailed(ErrorType.AuthenticationFailure)
                                }
                            }
                            .addOnFailureListener {
                                Log.d(TAG, "firebaseAuth signInWithCredential: failure: $it")
                                authenticationFailed(ErrorType.AuthenticationFailure, it)
                            }
                    }.addOnFailureListener {
                        Log.d(TAG, "requestServerSideAccess:failure authentication code $it")
                        authenticationFailed(ErrorType.PlayServiceNeeded, it)
                    }
                } else {
                    Log.d(TAG, "No Internet")
                    authenticationFailed(ErrorType.NoInternet)
                }
            }.addOnFailureListener {
                Log.d(TAG, "gamesSignInClient. isAuthenticated failure: $it")
                authenticationFailed(ErrorType.PlayServiceNeeded, it)
            }
    }

    private fun authenticationFailed(errorType: ErrorType, exception: Exception? = null) {
        exception?.logError("authenticationFailed")
        hasInitializedPlayGameUser = false
        uiEvents.value = MainUiEvent.UpdateUi(errorType)
        onlineStatus = OnlineStatus.Offline
        setLoading(false)
    }

    private var profileFromServerListener: ListenerRegistration? = null

    fun removeProfileFromServerListener() {
        profileFromServerListener?.remove()
        profileFromServerListener = null
    }

    private fun loadProfileFromServer() {
        removeProfileFromServerListener()
        val playerId = playerId.takeIf { it.isNotEmpty() } ?: return
        var firstError = false
        profileFromServerListener = firestore.collection("gamerProfile")
            .document(playerId)
            .addSnapshotListener { snapshot, exception ->
                setLoading(false)
                val profile = snapshot?.toObject<GameProfile>()
                if (profile != null && exception == null) {
                    onlineStatus = OnlineStatus.Online
                    updateProfile(profile)
                } else {
                    Log.d(TAG, "loadProfileFromServer: failure", exception)
                    if (!firstError) {
                        firstError = true
                        authenticationFailed(ErrorType.ServerResponseFailure, exception)
                    }
                }
            }
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
                    onlineStatus = OnlineStatus.Online
                    setLoading(false)
                    Log.d(TAG, "Profile Created")
                }
                .addOnFailureListener {
                    Log.d(TAG, "Profile Creation Failed")
                    authenticationFailed(ErrorType.ProfileCreationFailure, it)
                }
        }
    }

    fun setNewMsgBoltVisible(value: Boolean) {
        savedStateHandle["isNewMsgBoltVisible"] = value
    }

    fun setLoading(value: Boolean) {
//        savedStateHandle["isLoading"] = value
        if (value)
            DynamicIslandController.loading()
        else
            DynamicIslandController.idle()
    }

    fun initGameProfile() {
        gameProfileState.value = pref.read(PrefKeys.PROFILE, GameProfile())
        playerId = gameProfile.playerId
    }

    fun initMultiplayer() {
        matchRouteInfo = Routes.GameOnline(
            plr2Id = playerId,
            nm2 = gameProfile.nm,
            lvl2 = gameProfile.lvlByCal(),
            isPlyr1 = false
        )
        fetchGlobalChat()
        fetchActiveMatches()
    }

    fun addTempKey(key: String?) {
        if (key.isNullOrEmpty()) return
        tempKeys = (tempKeys + key).distinct().filter(String::isNotEmpty)
    }

    fun clearFriendlyChat() {
        viewModelScope.launch(Dispatchers.IO) {
            savedStateHandle["friendlyChatList"] = emptyList<MsgStore>()
            friendlyValueListener?.also { _friendlyChatRef?.removeEventListener(it) }
        }
    }

    fun clearTempMatches() {
        savedStateHandle["friendlyChatList"] = emptyList<MsgStore>()
        tempKeys.forEach {
            if (it.isEmpty()) return@forEach
            if (it == matchRouteInfo.gameKey)
                matchRouteInfo = Routes.GameOnline()
            multiPlayerRef.child(it).removeValue()
            tempKeys = tempKeys - it
        }
    }

    suspend fun removeOlderMatches() {
        runCatching {
            // 1. Calculate the cutoff timestamp
            val cutoffTime = System.currentTimeMillis() - Constants.DAY1_MILLIS

            // 2. Query ONLY the rooms older than the cutoff
            val olderMatches = multiPlayerRef
                .orderByChild("seenAt")
                .endAt(cutoffTime.toDouble())
                .get()
                .await()
                .children

            // 3. Perform an atomic multi-path update to delete all at once
            val deleteUpdates = mutableMapOf<String, Any?>()
            olderMatches.forEach { childSnapshot ->
                childSnapshot.key?.let { key ->
                    deleteUpdates[key] = null // Setting a key to null deletes it
                }
            }

            if (deleteUpdates.isNotEmpty())
                multiPlayerRef.updateChildren(deleteUpdates).await()
        }.onFailure {
            it.logError("removeOlderMatches")
        }
    }

    val lineIdsFromServer: StateFlow<Set<GameRoom.Line>>
        field = MutableStateFlow(setOf())

    var fetchServerLineClickListeners = mapOf<PlayerColor, ChildEventListener>()
    var fetchServerLineClickListener: ChildEventListener? = null

    fun removeServerLineClickListener() {
        fetchServerLineClickListeners.forEach {
            matchLiveRef?.child(it.key.ref)
                ?.removeEventListener(it.value)
        }
        fetchServerLineClickListeners = mapOf()
        fetchServerLineClickListener?.also {
            matchLiveRef?.child("clicks")
                ?.removeEventListener(it)
        }
        fetchServerLineClickListener = null
    }

    fun fetchServerLineClick(gameRoom: GameRoom? = getMatch(matchRouteInfo.gameKey)) {
        matchRouteInfo.log("fetchServerLineClick")
        viewModelScope.launch {
            lineIdsFromServer.value = setOf()
            removeServerLineClickListener()
            val matchLiveRef = matchLiveRef ?: return@launch
            when (gameRoom?.ver) {
                V1 -> fetchServerLineClickListeners = PlayerColor.entries.associateWith {
                    fetch4Player(matchLiveRef, it)
                }
                V2 -> fetchServerLineClickListener = fetchV2Clicks(matchLiveRef)
                else -> Log.d(TAG, "fetchServerLineClick: Unknown version")
            }

        }
    }

    private fun fetchV2Clicks(
        matchLiveRef: DatabaseReference
    ): ChildEventListener = matchLiveRef.child("clicks")
        .addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                val idFromServer = dataSnapshot.getValue<GameRoom.Line>() ?: return
                idFromServer.log("fetchServerLineClick idFromServer")
                lineIdsFromServer.update {
                    it.plus(idFromServer)
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                val idFromServer = dataSnapshot.getValue<GameRoom.Line>() ?: return
                lineIdsFromServer.update {
                    it.minus(idFromServer)
                }
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("TAG", "Failed to read value.", databaseError.toException())
            }
        })

    private fun fetch4Player(
        matchLiveRef: DatabaseReference,
        color: PlayerColor
    ): ChildEventListener = matchLiveRef.child(color.ref)
        .addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                val idFromServer = dataSnapshot.getValue<String>() ?: return
                idFromServer.log("fetchServerLineClick idFromServer")
                lineIdsFromServer.update {
                    it.plus(GameRoom.Line(idFromServer, color))
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                val idFromServer = dataSnapshot.getValue<String>() ?: return
                lineIdsFromServer.update {
                    it.minus(GameRoom.Line(idFromServer, color))
                }
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("TAG", "Failed to read value.", databaseError.toException())
            }
        })

    private var fetchGlobalChatListener: ValueEventListener? = null

    fun removeGlobalChatListener() {
        fetchGlobalChatListener?.let {
            globalChatRef.removeEventListener(it)
            fetchGlobalChatListener = null
        }
    }

    fun fetchGlobalChat() {
        if (fetchGlobalChatListener != null) return
        fetchGlobalChatListener =
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

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w("TAG", "Failed to read value.", databaseError.toException())
                    fetchGlobalChatListener = null
                }
            })
    }

    private var friendlyValueListener: ValueEventListener? = null
    private var _friendlyChatRef: DatabaseReference? = null

    fun removeFriendlyChatListener() {
        friendlyValueListener?.let {
            _friendlyChatRef?.removeEventListener(it)
            friendlyValueListener = null
            _friendlyChatRef = null
        }
    }

    fun fetchFriendlyChat() {
        viewModelScope.launch {
            removeFriendlyChatListener()
            if (matchRouteInfo.gameKey.isEmpty()) return@launch
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
                            savedStateHandle["friendlyChatList"] = chatList
                        }

                        override fun onCancelled(databaseError: DatabaseError) {}
                    })
        }
    }

    private data class ParsedCommand(
        val command: ChatCommand?,
        val flagWithCount: Map<ChatFlag, Int?>,
    ) {
        val isCommand: Boolean get() = command != null || flagWithCount.isNotEmpty()
    }

    private fun parseCommandAndFlags(text: String): ParsedCommand {
        val command = ChatCommand.find(text)
        val flags = ChatFlag.findAll(text)
        val flagWithCount = flags.associateWith { chatFlag ->
            // Matches the flag followed by whitespace and captures digits
            val regex = Regex("""${Regex.escape(chatFlag.flag)}\s+(\d+)""")
            regex.find(text)?.groupValues?.get(1)?.toIntOrNull()
        }
        return ParsedCommand(command, flagWithCount)
    }

    fun sendMessage(
        text: String,
        chatMode: ChatMode,
        type: MessageType = MessageType.Normal,
        chatRef: DatabaseReference? = if (chatMode.isGlobal) globalChatRef else friendlyChatRef
    ): Unit? {
        if (text.isEmpty() || chatRef == null) return null
        val parsed = parseCommandAndFlags(text)
        val currentChats = (if (chatMode.isGlobal) globalChatList else friendlyChatList).value

        if (!parsed.flagWithCount.contains(ChatFlag.Silent)) {
            chatRef.push().setValue(
                gameProfile.toMessage(
                    playerId = playerId,
                    msg = text,
                    type = if (parsed.isCommand) MessageType.Command else type
                )
            )
        }

        parsed.command?.let {
            sendCommand(
                command = it,
                flags = parsed.flagWithCount,
                currentChats = currentChats,
                chatRef = chatRef
            )
        }
        return Unit
    }

    fun sendCommand(
        command: ChatCommand,
        flags: Map<ChatFlag, Int?> = emptyMap(),
        currentChats: List<MsgStore> = emptyList(),
        chatRef: DatabaseReference
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (command) {
                    ChatCommand.DeleteLast -> deleteLastMessage(currentChats, chatRef, flags)
                    ChatCommand.ClearAll -> clearChat(chatRef)
                    ChatCommand.LastUser -> showLastUser(currentChats, chatRef)
                }
            } catch (e: Exception) {
                e.logError("sendCommand")
            }
        }
    }

    fun sendInvitation2Chat(gameId: String = getKey4(), text: String): Job? {
        if (gameId.isEmpty()) return null
        return viewModelScope.launch(Dispatchers.IO) {
            globalChatRef.push().setValue(
                gameProfile.toMessage(playerId = playerId, msg = text).copy(
                    gameId = gameId,
                    type = MessageType.Invitation.name
                )
            )
        }
    }

    private suspend fun deleteLastMessage(
        currentChats: List<MsgStore>,
        chatRef: DatabaseReference,
        flags: Map<ChatFlag, Int?>
    ) {
        if (currentChats.firstOrNull()?.msgData?.contains(ChatCommand.DeleteLast.command) == true)
            return
        val count = flags[ChatFlag.Count]?.coerceIn(1, 10) ?: 1

        globalChatList.value.take(count).forEach {
            chatRef.child(it.key)
                .removeValue()
                .await()
        }
    }

    private suspend fun clearChat(chatRef: DatabaseReference) {
        chatRef.push().setValue(
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

    private suspend fun showLastUser(
        currentChats: List<MsgStore>,
        chatRef: DatabaseReference
    ) {
        currentChats
            .find { it.type == MessageType.Normal.name && it.playerId != playerId }
            ?.playerId
            ?.let { playerId ->
                val playerInfo =
                    gamerProfileRef.document(playerId).get().await().toObject<GameProfile>()
                        ?: error("No player found")
                chatRef.push().setValue(
                    ChatCommand.bot.copy(
                        playerId = playerInfo.playerId,
                        user = playerInfo
                    )
                ).await()
            } ?: error("No message to show")
    }

    fun clearMultiPlayerDB() {
        viewModelScope.launch(Dispatchers.IO) {
            multiPlayerRef.removeValue()
        }

    }

    fun fuckIL() {
        Firebase.firestore.collection("gamerProfile").whereEqualTo("countryEmoji", "🇮🇱").get()
            .addOnSuccessListener { qs ->
                qs?.documents?.mapNotNull { it?.toObject<GameProfile>() }?.forEach {
                    it.countryEmoji = "🇵🇸"
                    it.countryNm = "Palestina"
                    Firebase.firestore.collection("gamerProfile").document(it.playerId).set(it)
                }
            }
    }

    private var fetchActiveMatchesListener: ChildEventListener? = null

    fun removeActiveMatchesListener() {
        fetchActiveMatchesListener?.let {
            multiPlayerRef.removeEventListener(it)
            fetchActiveMatchesListener = null
        }
    }

    fun fetchActiveMatches() {
        if (fetchActiveMatchesListener != null) return
        viewModelScope.launch(Dispatchers.IO) {
            removeOlderMatches()
            fetchActiveMatchesListener =
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
                        fetchActiveMatchesListener = null
                    }
                })
        }
    }

    fun getMatch(fullKey: String): GameRoom? =
        matches.value.find { it.key == fullKey && it.key.isNotBlank() }

    fun getValidMatch(shortKey: String): GameRoom? =
        matches.value.find { getKey4(it.key) == shortKey && it.key.isNotBlank() }

    fun getKey4(key: String = matchRouteInfo.gameKey): String {
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
        fun defError(): Result<Routes.GameOnline> {
            if (matches.value.isNotEmpty())
                globalChatRef.child(msg.key).removeValue()
            return Result.failure(Exception("Match expired."))
        }
        if (msg.gameId.length != 4) return defError()
        val gameRoom = getValidMatch(msg.gameId) ?: return defError()
        gameRoom.log("getJoinRoute")
        pref.save("tmpKey", gameRoom.key)

        if (currentRoute is Routes.GameOnline)
            return Result.failure(Exception("You are already in a match."))
        val isPlayer1 = gameRoom.player1.id == playerId
        val isPlayer2 = gameRoom.player2.id == playerId
        val isParticipant = isPlayer1 || isPlayer2

        val isRoomFull = gameRoom.player1.id.isNotEmpty() &&
                gameRoom.player2.id.isNotEmpty()

        // Reject if the player isn't in the room AND the room cannot accept new players
        if (!isParticipant && isRoomFull)
            return Result.failure(Exception("Match already started."))

        val isPlyr1 = gameRoom.player1.id.isEmpty() || isPlayer1
        matchRouteInfo = Routes.GameOnline(
            gameKey = gameRoom.key,
            isPlyr1 = isPlyr1,
            plr1Id = if (isPlyr1) playerId else gameRoom.player1.id,
            nm1 = if (isPlyr1) gameProfile.nm else gameRoom.player1.nm,
            lvl1 = if (isPlyr1) gameProfile.lvlByCal() else gameRoom.player1.lvl,
            plr2Id = if (isPlyr1) gameRoom.player2.id else playerId,
            nm2 = if (isPlyr1) gameRoom.player2.nm else gameProfile.nm,
            lvl2 = if (isPlyr1) gameRoom.player2.lvl else gameProfile.lvlByCal()
        )
        matchRouteInfo.log("getJoinRoute")
        sendInitialMessage(gameRoom, false)
        fetchServerLineClick(gameRoom)
        fetchFriendlyChat()
        return Result.success(matchRouteInfo)
    }

    fun sendInitialMessage(gameRoom: GameRoom, isNewRoom: Boolean) {
        val chatKey = getFriendlyChatKey()
        val initialMsg = gameProfile.toMessage(
            playerId = playerId,
            msg = if (isNewRoom) "Created the match." else "Joined the match.",
            type = MessageType.EnterText
        )

        if (isNewRoom) {
            // Player 1: Initialize new room
            val newRoomData = gameRoom.copy(
                ver = GameRoom.RoomType.V2,
                player1 = gameProfile.toPlayerInfo(),
                friendlyChat = mapOf(chatKey to initialMsg)
            )
            multiPlayerRef.child(gameRoom.key).setValue(newRoomData)
            return
        }

        val updates = mutableMapOf(
            "friendlyChat/$chatKey" to initialMsg,
            "playerCount" to "2"
        )

        when {
            gameRoom.player2.id.isEmpty() -> {
                // Player 2 fills empty slot
                updates["player2"] = gameProfile.toPlayerInfo(
                    score = gameRoom.player2.score,
                    cup = gameRoom.player2.cup
                )
            }

            gameRoom.player1.id.isEmpty() -> {
                // Fallback: Player 1 slot was vacant
                updates["player1"] = gameProfile.toPlayerInfo(
                    score = gameRoom.player1.score,
                    cup = gameRoom.player1.cup
                )
            }

            else -> Unit
        }

        multiPlayerRef.child(gameRoom.key).updateChildren(updates)
    }

    fun sendClick2Server(line: GameRoom.Line) {
        val isPlyr1 = matchRouteInfo.isPlyr1
        val isMe = isPlyr1 == line.color.isRed
        if (!isMe) return
        if (lineIdsFromServer.value.any { it.id == line.id }) return
        pushLineClick(line)
    }

    private fun pushLineClick(line: GameRoom.Line) {
        val matchRef = matchRef ?: return
        val matchLiveRef = matchLiveRef ?: return
        val isPlyr1 = matchRouteInfo.isPlyr1

        val playerMatchKey = if (isPlyr1) "plyr1" else "plyr2"
        val playerInfoKey = if (isPlyr1) "player1" else "player2"

        // Generate a unique push key under matchLiveRef
        val clickKey = matchLiveRef.child(playerMatchKey).push().key ?: uuidV7
        val clickKeyV2 = matchLiveRef.child("clicks").push().key ?: uuidV7

        val serverTimestamp = ServerValue.TIMESTAMP

        // Perform an atomic multi-location update across both nodes
        val updates = mapOf(
            "matchInfo/$playerMatchKey/$clickKey" to line.id,
            "matchInfo/clicks/$clickKeyV2" to line,
            "pingAt" to serverTimestamp,
            "$playerInfoKey/seenAt" to serverTimestamp
        )
        matchRef.updateChildren(updates)
    }

    fun pingCurrentMatch() {
        matchRef?.child("pingAt")
            ?.setValue(ServerValue.TIMESTAMP)
    }

    fun pingActiveStatus() {
        val playerInfoKey =
            if (matchRouteInfo.isPlyr1) "player1" else "player2"
        matchRef?.child(playerInfoKey)
            ?.child("seenAt")
            ?.setValue(ServerValue.TIMESTAMP)
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
