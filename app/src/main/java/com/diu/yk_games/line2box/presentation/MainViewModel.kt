package com.diu.yk_games.line2box.presentation

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.base.getPersistentListMutableStateFlow
import com.diu.yk_games.line2box.model.*
import com.diu.yk_games.line2box.model.MsgStore.MessageType
import com.diu.yk_games.line2box.presentation.island.DynamicIslandController
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.util.*
import com.google.android.gms.games.PlayGames
import com.google.firebase.Firebase
import com.google.firebase.auth.PlayGamesAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.database.*
import com.google.firebase.firestore.*
import com.google.firebase.firestore.Query
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.google.gson.Gson
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import org.jsoup.Jsoup
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MainViewModel(
    context: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(context) {

    val uiEvents = MutableStateFlow<MainUiEvent?>(null)
    var currentRoute: Routes = Routes.Home

    var lastMotionState: Int? = null
    val firebaseAuth = Firebase.auth
    val database = Firebase.database
    val firestore = Firebase.firestore
    val remoteConfig = Firebase.remoteConfig

    val globalChatRef = database.getReference("globalChat")
    val multiPlayerRef = database.getReference("MultiPlayer")
    val activePlayersRef = database.getReference("actives")
    val gamerProfileRef = firestore.collection("gamerProfile")
    val scoreBoardRef = firestore.collection("ScoreBoard")

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
        get() = onlineStatus == OnlineStatus.Online && ConnectivityObserver.isConnected

    val globalChatList: StateFlow<List<MsgStore>>
        field = savedStateHandle.getMutableStateFlow("globalChatList", listOf())
    val friendlyChatList: StateFlow<List<MsgStore>>
        field = savedStateHandle.getMutableStateFlow("friendlyChatList", listOf())

    val scoreboard: StateFlow<ScoreBoardState>
        field = savedStateHandle.getMutableStateFlow(
            "scoreboard",
            ScoreBoardState(isLoading = true)
        )
    val leaderboard: StateFlow<LeaderBoardState?>
        field = savedStateHandle.getMutableStateFlow("leaderboard", null)

    val matches: StateFlow<PersistentList<GameRoom>>
        field = savedStateHandle.getPersistentListMutableStateFlow(
            "matches",
            persistentListOf(),
            viewModelScope
        )

    val actives = activePlayersRef
        .limitToLast(100)
        .asValueFlowList<PlayerInfo>()
        .map { lst -> lst.sortedByDescending { it.seenAt }.toPersistentList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = persistentListOf()
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

    val showBanner4Page = savedStateHandle.getMutableStateFlow("showBanner4Page", true)
    val showBannerServer = savedStateHandle.getMutableStateFlow("showBannerServer", false)

    val showBanner = combine(showBanner4Page, showBannerServer, settingsState) {
            banner4Page, bannerServer, settings ->
        banner4Page && bannerServer && settings.showBanner
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = false
    )

    val player = SoundEffectPlayer(context, isMuted = { settings.isMuted })

    init {
        viewModelScope.launch(Dispatchers.IO) {
            settingsState.value = pref.read(PrefKeys.SETTINGS, Settings())
            initGameProfile()
            initializeFirebaseConfig()
        }
    }

    override fun onCleared() {
        player.release()
        clearMultiPlayerData()
        runBlocking(Dispatchers.IO) {
            runCatching { removeOlderMatches() }
        }
    }

    fun clearMultiPlayerData() {
        clearTempMatches()
        clearFriendlyChat()
        clearJoiningJob()
        matches.value = persistentListOf()
        matchRouteInfo = Routes.GameOnline()
        removeProfileFromServerListener()
        removeGlobalChatListener()
        removeFriendlyChatListener()
        removeActiveMatchesListener()
        removeServerLineClickListener()
    }

    private fun initializeFirebaseConfig() {
        viewModelScope.launch {
            try {
                val configSettings = remoteConfigSettings {
                    // 15 days * 24 hours * 60 minutes * 60 seconds
                    minimumFetchIntervalInSeconds = 15L * 24 * 60 * 60
                }
                remoteConfig.setConfigSettingsAsync(configSettings).await()

                val updated = remoteConfig.fetchAndActivate().await()
                Log.d("RemoteConfig", "Fetched and activated: $updated")

                showBannerServer.value = remoteConfig.getBoolean("showBanner")
                Log.d("RemoteConfig", "showBannerServer: ${showBannerServer.value}")
            } catch (e: Exception) {
                Log.e("RemoteConfig", "Error during config setup", e)
            }
        }
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

    fun initializePlayGameUser(activity: Activity, onSuccess: () -> Unit = {}) {
        if (onlineStatus == OnlineStatus.Online) return
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
                                            gamerProfileRef
                                                .document(player.playerId)
                                                .get().addOnSuccessListener { document ->
                                                    if (document.exists()) {
                                                        loadProfileFromServer(onSuccess)
                                                        Log.d(TAG, "Profile exists!")
                                                        uiEvents.value =
                                                            MainUiEvent.ShowToast("Profile Exists and Loaded!")
                                                    } else {
                                                        Log.d(TAG, "Profile does not exist!")
                                                        setupNewUserProfile(onSuccess)
                                                    }
                                                }.addOnFailureListener {
                                                    authenticationFailed(
                                                        ErrorType.ServerResponseFailure,
                                                        it
                                                    )
                                                }
                                        } else {
                                            loadProfileFromServer(onSuccess)
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
        uiEvents.value = MainUiEvent.UpdateUi(errorType)
        onlineStatus = OnlineStatus.Offline.apply {
            error = errorType
        }
        setLoading(false)
    }

    private var profileFromServerListener: ListenerRegistration? = null

    fun removeProfileFromServerListener() {
        profileFromServerListener?.remove()
        profileFromServerListener = null
    }

    private fun loadProfileFromServer(onSuccess: () -> Unit) {
        removeProfileFromServerListener()
        val playerId = playerId.takeIf { it.isNotEmpty() } ?: return
        var firstError = false
        profileFromServerListener = gamerProfileRef
            .document(playerId)
            .addSnapshotListener { snapshot, exception ->
                setLoading(false)
                val profile = snapshot?.toObjectOrNull<GameProfile>()
                if (profile != null && exception == null) {
                    onlineStatus = OnlineStatus.Online
                    updateProfile(profile)
                    onSuccess()
                } else {
                    Log.d(TAG, "loadProfileFromServer: failure", exception)
                    if (!firstError) {
                        firstError = true
                        authenticationFailed(ErrorType.ServerResponseFailure, exception)
                    }
                }
            }
    }

    private fun setupNewUserProfile(onSuccess: () -> Unit) {
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

            gamerProfileRef.document(playerId)
                .set(profile)
                .addOnSuccessListener {
                    pref.save("needProfile", false)
                    onlineStatus = OnlineStatus.Online
                    setLoading(false)
                    onSuccess()
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
        if (value)
            DynamicIslandController.loading()
        else
            DynamicIslandController.idle()
    }


    private val fetchJobs = mutableMapOf<Score.Type, Job>()
    private val lastFetchTimes = mutableMapOf<Score.Type, Long>()
    private var lastBestFetchTime = 0L

    private val friendlyFilter = Filter.or(
        Filter.equalTo("starData", "friendly"),
        Filter.equalTo("type", "Friendly")
    )
    private val globeFilter = Filter.or(
        Filter.equalTo("starData", "globe"),
        Filter.equalTo("type", "Globe")
    )

    fun fetchScoreBoard(type: Score.Type, force: Boolean = false) {
        cat("fetchScoreBoard enter type: $type")
        if (fetchJobs[type]?.isActive == true) return
        if (!force && isFresh(type)) return

        scoreboard.update {
            it.copy(isLoading = true)
        }

        fetchJobs[type] = viewModelScope.launch {
            cat("fetchScoreBoard viewModelScope")
            try {
                val matches: PersistentList<Score>
                val freshLastBest: String?
                coroutineScope {
                    val matchesTask = async { loadMatches(type) }
                    val lastBestTask = async { loadLastBest() }
                    matches = matchesTask.await()
                    freshLastBest = lastBestTask.await()
                }

                scoreboard.update {
                    val best = freshLastBest ?: it.lastBest
                    when (type) {
                        Friendly -> it.copy(
                            friendlyMatches = matches,
                            lastBest = best,
                            isLoading = false
                        )

                        Globe -> it.copy(
                            globalMatches = matches,
                            lastBest = best,
                            isLoading = false
                        )
                    }
                }

                cat("fetchScoreBoard update")
                scoreboard.value.log("fetchScoreBoard")
                lastFetchTimes[type] = System.currentTimeMillis()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                scoreboard.update {
                    DynamicIslandController.message(e.message)
                    it.copy(isLoading = false)
                }
                e.logError("fetchScoreBoard")
            }
        }
    }

    private fun isFresh(type: Score.Type): Boolean {
        val state = scoreboard.value
        val cached = if (type.isFriendly) state.friendlyMatches else state.globalMatches
        return cached.isNotEmpty() &&
                lastFetchTimes[type]?.isLessThanAgo(CACHE_TTL) == true
    }

    private suspend fun loadMatches(type: Score.Type): PersistentList<Score> =
        scoreBoardRef.where(if (type.isFriendly) friendlyFilter else globeFilter)
            .orderBy("time", Query.Direction.DESCENDING)
            .limit(SCOREBOARD_LIMIT)
            .get()
            .await()
            .documents
            .mapNotNullTo(persistentListOf<Score>().builder()) { doc ->
                if (doc.contains("starData")) doc.toObjectOrNull<DataStore>()?.toScore()
                else doc.toObjectOrNull<Score>()
            }
            .build()

    // null = keep whatever is already in state
    private suspend fun loadLastBest(): String? {
        if (lastBestFetchTime.isLessThanAgo(LAST_BEST_TTL)) return null
        return tryGet {
            firestore.collection("LastBestPlayer")
                .document("LastBestPlayer")
                .get().await()
                .getString("info")
        }?.also { lastBestFetchTime = System.currentTimeMillis() }
    }

    private var leaderBoardJob: Job? = null
    private var lastFetchLeaderBoardTime = 0L
    private var lastCountFetchTime = 0L

    fun fetchLeaderBoard(force: Boolean = false) {
        if (leaderBoardJob?.isActive == true) return
        if (!force &&
            lastFetchLeaderBoardTime.isLessThanAgo(CACHE_TTL) &&
            !leaderboard.value?.list.isNullOrEmpty()
        ) return

        leaderBoardJob = viewModelScope.launch {
            try {
                val profiles: PersistentList<GameProfile>
                val freshCount: Long?
                coroutineScope {
                    val listTask = async { loadTopProfiles() }
                    val countTask = async { loadPlayerCount() }
                    profiles = listTask.await()
                    freshCount = countTask.await()
                }

                leaderboard.update { current ->
                    val state = current ?: LeaderBoardState()
                    state.copy(list = profiles, count = freshCount ?: state.count)
                }
                lastFetchLeaderBoardTime = System.currentTimeMillis()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                leaderboard.update {
                    val state = it ?: LeaderBoardState()
                    state.copy(error = e)
                }
                e.logError("fetchLeaderBoard")
            }
        }
    }

    private suspend fun loadTopProfiles(): PersistentList<GameProfile> =
        gamerProfileRef
            .whereGreaterThan("coin", MIN_COIN)
            .orderBy("coin", Query.Direction.DESCENDING)
            .limit(LEADERBOARD_LIMIT)
            .get()
            .await()
            .documents
            .mapNotNullTo(persistentListOf<GameProfile>().builder()) {
                it.toObjectOrNull<GameProfile>()
            }
            .build()

    // null = leave the existing count untouched
    private suspend fun loadPlayerCount(): Long? {
        if (lastCountFetchTime.isLessThanAgo(COUNT_TTL)) return null
        return tryGet {
            gamerProfileRef.count().get(AggregateSource.SERVER).await().count
        }?.also { lastCountFetchTime = System.currentTimeMillis() }
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

    suspend fun sendCup2LiveMatch(plr1Cup: String, plr2Cup: String) {
        if (matchRouteInfo.gameKey.isEmpty()) return
        if (matchRouteInfo.currentPlayerId != playerId) return

        val isPlyr1 = matchRouteInfo.isPlyr1
        val playerCupKey = if (isPlyr1) "cup1" else "cup2"
        val cup = if (isPlyr1) plr1Cup else plr2Cup

        val updates = mutableMapOf<String, Any>("matchInfo/result/$playerCupKey" to cup)
        if (!isPlyr1) updates["plr2Cup"] = plr2Cup // fix: remove some days

        matchRef?.updateChildren(updates)?.await()
    }

    fun saveMultiplayerScore2ScoreBoard(
        plr1Cup: String,
        plr2Cup: String,
        score1: Int,
        score2: Int
    ) {
        if (!isConnected) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val key = matchRouteInfo.gameKey
                if (key.isEmpty() || matchRouteInfo.watchOnly) return@launch

                sendCup2LiveMatch(plr1Cup, plr2Cup)
                if (!matchRouteInfo.isPlyr1) return@launch

                val redData = "${matchRouteInfo.nm1.trim().split("\n", " ").firstOrNull()}: $score1"
                val blueData =
                    "${matchRouteInfo.nm2.trim().split("\n", " ").firstOrNull()}: $score2"

                val room = withTimeoutOrNull(5.seconds) {
                    getMatchFLow().first { it.matchInfo.result.cup2.isNotEmpty() || it.plr2Cup.isNotEmpty() }
                } ?: getMatch() ?: return@launch

                val resolvedCup2 = room.matchInfo.result.cup2.takeIf { it.isNotEmpty() }
                    ?: room.plr2Cup.takeIf { it.isNotEmpty() }
                    ?: multiPlayerRef.child(key)
                        .child("plr2Cup")
                        .get().await()
                        .getValueOrNull<String>()
                    ?: plr2Cup

                val score = room.toScore(
                    cup1 = plr1Cup,
                    cup2 = resolvedCup2,
                    time = System.currentTimeMillis()
                )

                val maxScore = maxOf(score1, score2)
                val highestLocalData = if (score1 >= score2) redData else blueData
                val docRef = firestore
                    .collection("LastBestPlayer")
                    .document("LastBestPlayer")

                coroutineScope {
                    launch {
                        scoreBoardRef.document(room.key.ifEmpty { uuidV7 })
                            .set(score).await()
                    }
                    launch {
                        firestore.runTransaction { txn ->
                            val currentBestScore = txn.get(docRef).getString("info")
                                ?.substringAfterLast(": ", "0")
                                ?.toIntOrNull() ?: 0
                            if (maxScore > currentBestScore) txn.update(
                                docRef,
                                "info",
                                highestLocalData
                            )
                        }.await()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.logError("saveMultiplayerScore2ScoreBoard")
            }
        }
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
            friendlyChatList.value = listOf()
            friendlyValueListener?.also { _friendlyChatRef?.removeEventListener(it) }
        }
    }

    fun clearTempMatches() {
        friendlyChatList.value = listOf()

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

    private suspend fun removeOlderMatches2() {
        try {
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.logError("removeOlderMatches")
        }
    }

    private suspend fun removeOlderMatches() {
        val deleteUpdates = matches.value
            .filter { match ->
                match.pingAt.isMoreThanAgo(1.hours) && match.matchInfo.result.run { score1 == 0 && score2 == 0 }
                        || match.pingAt.isMoreThanAgo(1.days)
            }
            .associate { match -> match.key to null }

        if (deleteUpdates.isNotEmpty())
            multiPlayerRef.updateChildren(deleteUpdates).await()
    }

    private var syncDone by savedStateHandle.saved { false }

    private suspend fun syncCompletedMatches() = withContext(Dispatchers.IO) {
        if (syncDone) return@withContext
        syncDone = true
        var scoresToSync = matches.value
            .filter { it.matchInfo.result.run { score1 + score2 == 36 && cup1.isNotEmpty() && (cup2.isNotEmpty() || it.plr2Cup.isNotEmpty()) } }
            .associate { it.key.ifEmpty { uuidV7 } to it.toScore() }
        if (scoresToSync.isEmpty())
            return@withContext
        val existingKeys = getExistingKeys(scoreBoardRef, scoresToSync.keys.toList())
        scoresToSync = scoresToSync - existingKeys
        if (scoresToSync.isEmpty())
            return@withContext
        scoresToSync.toList().chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            for ((key, score) in chunk) {
                val docId = key.ifEmpty { uuidV7 }
                val docRef = scoreBoardRef.document(docId)
                batch.set(docRef, score)
            }
            batch.commit().await()
        }
    }

    suspend fun getExistingKeys(
        collection: CollectionReference,
        keysToCheck: List<String>
    ): Set<String> {
        if (keysToCheck.isEmpty()) return emptySet()
        return coroutineScope {
            keysToCheck
                .chunked(30)
                .map { chunk ->
                    async {
                        collection
                            .whereIn(FieldPath.documentId(), chunk)
                            .get()
                            .await()
                            .documents
                            .filter { it.exists() }
                            .map { it.id }
                    }
                }
                .awaitAll()
                .flatten()
                .toSet()
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

    fun fetchServerLineClick(room: GameRoom? = getMatch()) {
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
                    globalChatList.value = chatList
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
                            friendlyChatList.value = chatList
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
            matches.collect {
                val room = getMatch(key) ?: return@collect
                if (room.player2.id.isEmpty() || room.key.isEmpty()) return@collect
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
            } catch (e: CancellationException) {
                throw e
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
        val count = flags[ChatFlag.Count]?.coerceIn(1, 20) ?: 1

        val updates = currentChats
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
                qs?.documents?.size.log("fuckIL")
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
            launch {
                delay(5.seconds)
                syncCompletedMatches()
                removeOlderMatches()
            }
            fetchActiveMatchesListener =
                multiPlayerRef.limitToLast(100).addChildEventListener(object : ChildEventListener {
                    override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                        Log.d("addList", "onChildAdded: " + dataSnapshot.key)
                        try {
                            matches.value = matches.value
                                .addSorted(dataSnapshot.getRoom() ?: return)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            e.logError("fetchActiveMatches")
                        }
                    }

                    override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
                        try {
                            matches.value = matches.value
                                .removeByKey(dataSnapshot.key)
                                .addSorted(dataSnapshot.getRoom() ?: return)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            e.logError("fetchActiveMatches")
                        }
                    }

                    override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                        matches.value = matches.value.removeByKey(dataSnapshot.key)
                    }

                    private fun DataSnapshot.getRoom() =
                        getValueOrNull<GameRoom>()?.copy(key = key.orEmpty())

                    private fun PersistentList<GameRoom>.addSorted(room: GameRoom) =
                        plus(room)
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
                            }.toPersistentList()


                    private fun PersistentList<GameRoom>.removeByKey(key: String?): PersistentList<GameRoom> {
                        return key?.let { key ->
                            toMutableList().run {
                                removeIf { it.key == key }
                                toPersistentList()
                            }
                        } ?: this
                    }

                    override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.w("TAG", "Failed to read value.", databaseError.toException())
                        fetchActiveMatchesListener = null
                    }
                })
        }
    }

    fun getMatch(fullKey: String = matchRouteInfo.gameKey): GameRoom? =
        matches.value.find { it.key == fullKey && it.key.isNotBlank() }

    fun getMatchFLow(fullKey: String = matchRouteInfo.gameKey) =
        matches.mapNotNull { match -> match.find { it.key == fullKey && it.key.isNotBlank() } }

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
        val isPlayer1 = room.player1.id == playerId
        val isPlayer2 = room.player2.id == playerId

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

            isPlayer1 || (!isPlayer2 && room.player1.shouldEnter(room.ver))-> {
                // Fallback: Player 1 slot was vacant
                updates["player1"] = gameProfile.toPlayerInfoDB()
                updates["playerCount"] = "2"
            }

            isPlayer2 || (room.player2.shouldEnter(room.ver))-> {
                // Fallback: Player 2 slot was vacant
                updates["player2"] = gameProfile.toPlayerInfoDB()
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
        const val SCOREBOARD_LIMIT = 100L
        const val LEADERBOARD_LIMIT = 100L
        const val MIN_COIN = 100L
        val CACHE_TTL = 2.minutes
        val LAST_BEST_TTL = 10.minutes
        val COUNT_TTL = 15.minutes
    }
}
