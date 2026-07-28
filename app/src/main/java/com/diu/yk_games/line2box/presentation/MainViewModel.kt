package com.diu.yk_games.line2box.presentation

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.diu.yk_games.line2box.model.*
import com.diu.yk_games.line2box.model.MsgStore.Type
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.util.log
import com.diu.yk_games.line2box.util.pref
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.*
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    var lastMotionState: Int? = null
    val firebaseAuth by lazy { Firebase.auth }
    val database by lazy { Firebase.database }
    val firestore by lazy { Firebase.firestore }
    val globalChatRef by lazy { database.getReference("globalChat") }
    val multiPlayerRef by lazy { database.getReference("MultiPlayer") }
    val scoreBoardKey  //fake key
        get() = UUID.randomUUID().toString()

    var gameProfile
        get() = _gameProfile ?: GameProfile()
        set(value) {
            _gameProfile = value
            value.apply()
        }
    private var _gameProfile: GameProfile? = null
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


    fun setNewMsgBoltVisible(value: Boolean) {
        savedStateHandle["isNewMsgBoltVisible"] = value
    }

    fun setLoading(value: Boolean) {
        savedStateHandle["isLoading"] = value
    }

    fun initGameProfile(playerId: String = this@MainViewModel.playerId) {
        _gameProfile = GameProfile()
        if (playerId.isNotEmpty()) {
            this@MainViewModel.playerId = playerId
            gameProfile.playerId = playerId
            gameProfile.apply()
        } else this@MainViewModel.playerId = gameProfile.playerId
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
            friendlyValueListener?.also { friendlyChatRef?.removeEventListener(it) }
        }
    }

    fun clearTempMatches() {
        viewModelScope.launch(Dispatchers.IO) {
            savedStateHandle["friendsChatList"] = emptyList<MsgStore>()
            tempKeys.forEach {
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
            globalChatRef.limitToLast(100).addValueEventListener(object : ValueEventListener {
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

    var friendlyValueListener: ValueEventListener? = null
    var friendlyChatRef: DatabaseReference? = null

    fun fetchFriendlyChat(gameRoomKey: String = matchKey) {
        viewModelScope.launch {
            if (gameRoomKey.isEmpty()) return@launch
            val friendsChatRef = multiPlayerRef.child(gameRoomKey).child("friendlyChat")
            friendlyValueListener?.also { friendlyChatRef?.removeEventListener(it) }
            friendlyChatRef = friendsChatRef
            friendlyValueListener =
                friendsChatRef.limitToLast(100).addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val chatList = snapshot.children.mapNotNull {
                            val key = it.key
                            val ms = it.getValue<MsgStore>()
                            if (key == null || ms == null) return@mapNotNull null
                            if (ms.type.typeEnum == Type.ExitText)
                                localPlayerCount--
                            ms.copy(key = key)
                        }.sortedByDescending { it.time }
                        savedStateHandle["friendsChatList"] = chatList
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })
        }
    }

    fun sendMessage2FriendlyChat(text: String, type: Type = Type.Normal): Unit? {
        if (text.isEmpty()) return null
        val friendlyChatRef = multiPlayerRef.child(matchKey).child("friendlyChat")
        viewModelScope.launch(Dispatchers.IO) {
            friendlyChatRef.push().setValue(
                gameProfile.toMessage(playerId = playerId, msg = text, type = type)
            )
        }
        return Unit
    }

    fun sendMessage2GlobalChat(text: String): Unit? {
        if (text.isEmpty()) return null
        viewModelScope.launch(Dispatchers.IO) {
            globalChatRef.push().setValue(
                gameProfile.toMessage(playerId = playerId, msg = text)
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
                    type = Type.Invitation.name
                )
            )
        }
    }

    fun clearMultiPlayerDB() {
        viewModelScope.launch(Dispatchers.IO) {
            database.getReference("MultiPlayer").removeValue()
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
        fun defError(): Result<Routes.GameOnline> {
            if (matches.value.isNotEmpty())
                globalChatRef.child(msg.key).removeValue()
            return Result.failure(Exception("Match expired."))
        }
        if (msg.gameId.length != 4) return defError()
        val gameRoom = getValidMatch(msg.gameId) ?: return defError()
        val fullKey = gameRoom.key
        pref.save("tmpKey", fullKey)

        if (gameRoom.playerCount == "-1")
            return defError()
        if (gameRoom.player1.id == playerId || gameRoom.player2.id == playerId)
            return Result.failure(Exception("You are already in the match."))
        if (gameRoom.playerCount == "2")
            return Result.failure(Exception("Match already started."))

        // Update player2 and player count
        multiPlayerRef.child(fullKey).apply {
            child("player2").setValue(gameProfile.toPlayerInfo())
            child("playerCount").setValue("2")
            //remove
            child("playerInfo").child("nm2").setValue(gameProfile.nm)
            child("playerInfo").child("lvl2").setValue(gameProfile.lvlByCal())
            //remove
        }
        val friendsChatRef = multiPlayerRef.child(fullKey).child("friendlyChat")

        // Send join message
        friendsChatRef.push().setValue(
            gameProfile.toMessage(
                playerId = playerId,
                msg = "Joined the match.",
                type = Type.EnterText
            )
        )
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
}