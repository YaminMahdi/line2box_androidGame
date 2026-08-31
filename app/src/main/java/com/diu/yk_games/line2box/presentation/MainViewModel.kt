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
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import org.jsoup.Jsoup
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
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
    val firebaseAuth = Firebase.auth
    val database = Firebase.database
    val firestore = Firebase.firestore

    val globalChatRef = database.getReference("globalChat")
    val multiPlayerRef = database.getReference("MultiPlayer")
    val activePlayersRef = database.getReference("actives")
    val gamerProfileRef = firestore.collection("gamerProfile")

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

    val globalChatList = savedStateHandle.getStateFlow("globalChatList", listOf<MsgStore>())
    val friendlyChatList = savedStateHandle.getStateFlow("friendlyChatList", listOf<MsgStore>())

    val matches: StateFlow<List<GameRoom>>
        field = savedStateHandle.getMutableStateFlow("matches", listOf())

    val actives = activePlayersRef
        .limitToLast(100)
        .asValueFlowList<PlayerInfo>()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = listOf()
        )

    var ignoreDrawerClosesSound = false
    var localPlayerCount = 2

    var playerId by savedStateHandle.saved { "" }

    var matchRouteInfo by savedStateHandle.saved { Routes.GameOnline() }
    val joiningGame: StateFlow<Routes.GameOnline?>
        field = savedStateHandle.getMutableStateFlow("joiningGame", null)

    var tempKeys by savedStateHandle.saved { listOf<String>() }

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
        player.release()
        clearMultiPlayerData()
        runBlocking(Dispatchers.IO) { removeOlderMatches() }
    }

    fun clearMultiPlayerData() {
        clearTempMatches()
        clearFriendlyChat()
        clearJoiningJob()
        matches.value = listOf()
        matchRouteInfo = Routes.GameOnline()
        removeProfileFromServerListener()
        removeGlobalChatListener()
        removeFriendlyChatListener()
        removeActiveMatchesListener()
        removeServerLineClickListener()
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
        if (matchRouteInfo.watchOnly) return
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
                val profile = snapshot?.toObjectOrNull<GameProfile>()
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

    var pingJob: Job? = null

    fun initMultiplayer() {
        isStickySwitchRight = false
        matchRouteInfo = Routes.GameOnline()
        fetchGlobalChat()
        fetchActiveMatches()
        pingJob?.cancel()
        pingJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                while (isActive) {
                    pingActiveStatus()
                    delay(2.minutes)
                }
            }
        }
    }

    fun addTempKey(key: String?) {
        if (key.isNullOrEmpty()) return
        tempKeys = (tempKeys + key).distinct().filter(String::isNotEmpty)
    }

    fun clearFriendlyChat() {
        viewModelScope.launch(Dispatchers.IO) {
            savedStateHandle["friendlyChatList"] = listOf<MsgStore>()
            friendlyValueListener?.also { _friendlyChatRef?.removeEventListener(it) }
        }
    }

    fun clearTempMatches() {
        savedStateHandle["friendlyChatList"] = listOf<MsgStore>()

        val updates = matches.value
            .filter { it.key in tempKeys && it.key.isNotEmpty() }
            .onEach {
                if (it.key == matchRouteInfo.gameKey)
                    matchRouteInfo = Routes.GameOnline()
            }
            .filter { it.matchInfo.result.run { score1 == 0 && score2 == 0 } }
            .associate { it.key to null }

        if (updates.isNotEmpty())
            multiPlayerRef.updateChildren(updates)

        tempKeys = listOf()
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

    private fun removeUnplayedOlderMatches() {
        val deleteUpdates = matches.value
            .filter { match ->
                match.pingAt.isMoreThanAgo(1.hours) && match.matchInfo.result.run { score1 == 0 && score2 == 0 }
            }
            .associate { match -> match.key to null }

        if (deleteUpdates.isNotEmpty())
            multiPlayerRef.updateChildren(deleteUpdates)
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

    fun fetchServerLineClick(room: GameRoom? = getMatch(matchRouteInfo.gameKey)) {
        matchRouteInfo.log("fetchServerLineClick")
        viewModelScope.launch {
            lineIdsFromServer.value = setOf()
            removeServerLineClickListener()
            val matchLiveRef = matchLiveRef ?: return@launch
            when (room?.ver) {
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
                val idFromServer = dataSnapshot.getValueOrNull<GameRoom.Line>() ?: return
                idFromServer.log("fetchServerLineClick idFromServer")
                lineIdsFromServer.update {
                    it.plus(idFromServer)
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                val idFromServer = dataSnapshot.getValueOrNull<GameRoom.Line>() ?: return
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
                val idFromServer = dataSnapshot.getValueOrNull<String>() ?: return
                idFromServer.log("fetchServerLineClick idFromServer")
                lineIdsFromServer.update {
                    it.plus(GameRoom.Line(idFromServer, color))
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                val idFromServer = dataSnapshot.getValueOrNull<String>() ?: return
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
                        val ms = it.getValueOrNull<MsgStore>()
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
                                val ms = it.getValueOrNull<MsgStore>()
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

    var createAndFetchJob: Job? = null

    fun clearJoiningJob() {
        createAndFetchJob?.cancel()
        createAndFetchJob = null
        joiningGame.value = null
    }

    fun createAndFetchJoiningPlayerInfo(key: String) {
        if (key.isEmpty()) return
        clearJoiningJob()
        createAndFetchJob = viewModelScope.launch {
            clearTempMatches()
            addTempKey(key)
            val room = GameRoom(key = key)
            matchRouteInfo = room.toRoutes(gameProfile)
            sendInitialMessage(room, JoinType.Create)
            fetchFriendlyChat()
            matches.collect { matches ->
                val room = matches.find { it.key == key } ?: return@collect
                if (room.player2.run { id.isEmpty() && seenAt < 0 } || room.key.isEmpty()) return@collect
                fetchServerLineClick(room)
                joiningGame.value = room.toRoutes(playerId)
            }
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
        currentChats: List<MsgStore> = listOf(),
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

        val updates = globalChatList.value
            .take(count)
            .associate { it.key to null }

        if (updates.isNotEmpty())
            chatRef.updateChildren(updates).await()
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
                    gamerProfileRef.document(playerId).get().await().toObjectOrNull<GameProfile>()
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
        gamerProfileRef.whereEqualTo("countryEmoji", "🇮🇱").get()
            .addOnSuccessListener { qs ->
                qs?.documents?.mapNotNull { it?.toObjectOrNull<GameProfile>() }?.forEach {
                    it.countryEmoji = "🇵🇸"
                    it.countryNm = "Palestina"
                    gamerProfileRef.document(it.playerId).set(it)
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
                            matches.value = matches.value.addSorted(dataSnapshot)
                            removeUnplayedOlderMatches()
                        }.onFailure {
                            it.logError("fetchActiveMatches")
                        }
                    }

                    override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
                        removeByKey(dataSnapshot.key)
                        runCatching {
                            matches.value = matches.value.addSorted(dataSnapshot)
                        }.onFailure {
                            it.logError("fetchActiveMatches")
                        }
                    }

                    override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                        removeByKey(dataSnapshot.key)
                    }

                    private fun List<GameRoom>.addSorted(dataSnapshot: DataSnapshot) =
                        dataSnapshot.getValueOrNull<GameRoom>()?.let { game ->
                            plus(game.copy(key = dataSnapshot.key.orEmpty()))
                                .distinctBy { it.key }
                                .sortedByDescending { it.pingAt }
                                .partition { it.ver.isV1 }
                                .let { pair ->
                                    pair.second
                                        .plus(pair.first.sortedByDescending { room ->
                                            val time =
                                                room.friendlyChat.firstNotNullOfOrNull { it.value }?.time
                                                    ?: -1L
                                            room.pingAt = time
                                            time
                                        })
                                }
                        }.orEmpty()

                    private fun removeByKey(key: String?) {
                        key?.let { key ->
                            matches.value.toMutableList().apply {
                                if (removeIf { it.key == key })
                                    matches.value = toList()
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
            globalChatRef.child(msg.key).removeValue()
            return Result.failure(Exception("Match expired."))
        }
        if (msg.gameId.length != 4) return defError()
        val room = getValidMatch(msg.gameId) ?: return defError()
        room.log("getJoinRoute")
        pref.save("tmpKey", room.key)

        if (currentRoute is Routes.GameOnline)
            return Result.failure(Exception("You are already in a match."))
        val isPlayer1 = room.player1.id == playerId
        val isPlayer2 = room.player2.id == playerId
        val isParticipant = isPlayer1 || isPlayer2

        val isRoomFull =
            room.player1.seenAt > 0 && room.player2.seenAt > 0

        // Reject if the player isn't in the room AND the room cannot accept new players
        if (!isParticipant && isRoomFull)
            return Result.failure(Exception("Match already started."))

        matchRouteInfo = room.toRoutes(gameProfile)
        matchRouteInfo.log("getJoinRoute")
        sendInitialMessage(room)
        fetchServerLineClick(room)
        fetchFriendlyChat()
        return Result.success(matchRouteInfo)
    }

    fun getJoinRoute(room: GameRoom): Result<Routes.GameOnline> {
        room.log("getJoinRoute")

        val isPlayer1 = room.player1.id == playerId
        val isPlayer2 = room.player2.id == playerId
        val isParticipant = isPlayer1 || isPlayer2

        val isRoomFull = room.player1.seenAt > 0 && room.player2.seenAt > 0

        // Reject if the player isn't in the room AND the room cannot accept new players
        if (!isParticipant && isRoomFull)
            return Result.failure(Exception("Match already started."))

        matchRouteInfo = room.toRoutes(gameProfile)
        matchRouteInfo.log("getJoinRoute")
        sendInitialMessage(room)
        fetchServerLineClick(room)
        fetchFriendlyChat()
        return Result.success(matchRouteInfo)
    }

    fun getWatchRoute(room: GameRoom): Routes.GameOnline {
        matchRouteInfo = room.toRoutes(playerId = playerId, watchOnly = true)
        sendInitialMessage(room, JoinType.Watch)
        fetchServerLineClick(room)
        fetchFriendlyChat()
        return matchRouteInfo
    }

    fun sendInitialMessage(room: GameRoom, joinType: JoinType = JoinType.Join) {
        val chatKey = friendlyChatRef?.push()?.key ?: uuidV7
        val initialMsg = gameProfile.toMessage(
            playerId = playerId,
            msg = when (joinType) {
                Create -> "Created the match."
                Join -> "Joined the match."
                Watch -> "Watching the match."
            },
            type = MessageType.EnterText
        )

        if (joinType.isNew) {
            // Player 1: Initialize new room
            val newRoomData = room.copy(
                ver = GameRoom.Version.V2,
                player1 = gameProfile.toPlayerInfo(),
                friendlyChat = mapOf(chatKey to initialMsg)
            ).asMap().plus("pingAt" to ServerValue.TIMESTAMP)
            multiPlayerRef.child(room.key).setValue(newRoomData)
            return
        }

        val updates = mutableMapOf(
            "friendlyChat/$chatKey" to initialMsg,
            "pingAt" to ServerValue.TIMESTAMP
        )

        when {
            joinType == JoinType.Watch -> Unit

            room.player2.id.isEmpty() -> {
                // Player 2 fills empty slot
                updates["player2"] = gameProfile.toPlayerInfoDB()
                updates["playerCount"] = "2"
            }

            room.player1.id.isEmpty() -> {
                // Fallback: Player 1 slot was vacant
                updates["player1"] = gameProfile.toPlayerInfoDB()
                updates["playerCount"] = "2"
            }

            else -> Unit
        }

        multiPlayerRef.child(room.key).updateChildren(updates)
    }

    fun increaseServerScore(isRed: Boolean) {
        if (matchRouteInfo.gameKey.isEmpty()) return
        val isPlyr1 = matchRouteInfo.isPlyr1
        val isMe = isPlyr1 == isRed
        if (!isMe) return
        val playerScoreKey = if (isPlyr1) "score1" else "score2"

        matchLiveRef
            ?.child("result/$playerScoreKey")
            ?.setValue(ServerValue.increment(1))
    }

    fun sendCup2Server(plr1Cup: String, plr2Cup: String) {
        if (matchRouteInfo.gameKey.isEmpty()) return
        val isMe = matchRouteInfo.currentPlayerId == playerId
        val isPlyr1 = matchRouteInfo.isPlyr1
        if (!isMe) return
        val playerCupKey = if (isPlyr1) "cup1" else "cup2"
        val cup = if (isPlyr1) plr1Cup else plr2Cup
        val updates = buildMap {
            put("matchInfo/result/$playerCupKey", cup)
            if (!matchRouteInfo.isPlyr1)
                put("plr2Cup", plr2Cup)
        }
        matchRef?.updateChildren(updates)
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

    suspend fun pingActiveStatus() = withContext(Dispatchers.IO) {
        gameProfile.takeIf { it.playerId.isNotEmpty() && ConnectivityObserver.isConnected }?.let {
            activePlayersRef.child(it.playerId)
                .setValue(it.toPlayerInfoDB()).await()
        }
    }

    fun pingCurrentMatch() {
        if (matchRouteInfo.watchOnly) return
        val isPlyr1 = matchRouteInfo.isPlyr1
        val playerInfoKey = if (isPlyr1) "player1" else "player2"

        val serverTimestamp = ServerValue.TIMESTAMP

        val updates = mapOf(
            "pingAt" to serverTimestamp,
            "$playerInfoKey/seenAt" to serverTimestamp
        )
        matchRef?.updateChildren(updates)
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
