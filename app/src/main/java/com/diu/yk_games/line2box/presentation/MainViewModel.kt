package com.diu.yk_games.line2box.presentation

import android.os.Bundle
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.model.GameRoom
import com.diu.yk_games.line2box.model.MsgStore
import com.diu.yk_games.line2box.model.MsgStore.Type
import com.diu.yk_games.line2box.model.toMessage
import com.diu.yk_games.line2box.model.toPlayerInfo
import com.diu.yk_games.line2box.model.typeEnum
import com.diu.yk_games.line2box.pref
import com.diu.yk_games.line2box.util.log
import com.diu.yk_games.line2box.util.tryGet
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val database by lazy { Firebase.database }
    val globalChatRef by lazy { database.getReference("globalChat") }
    val multiPlayerRef by lazy { database.getReference("MultiPlayer") }
    val scoreBoardKey  //fake key
        get() = database.getReference("ScoreBoard").child("allScore").key.orEmpty()

    lateinit var gameProfile: GameProfile

    val globalChatList = savedStateHandle.getStateFlow("globalChatList", emptyList<MsgStore>())
    val friendsChatList = savedStateHandle.getStateFlow("friendsChatList", emptyList<MsgStore>())

    var matchKeys = mutableListOf<String>()

    var ignoreDrawerClosesSound = false
    var localPlayerCount = 2

    var gameId
        get() = savedStateHandle["gameId"] ?: ""
        set(value) { savedStateHandle["gameId"] = value }

    var playerId
        get() = savedStateHandle["playerId"] ?: ""
        set(value) { savedStateHandle["playerId"] = value }

    val tempKeys
        get() = savedStateHandle.get<List<String>>("tempKeys") ?: emptyList()

    fun initGameProfile(playerId: String = this@MainViewModel.playerId, loadGlobalChat: Boolean = true) {
        gameProfile = GameProfile()
        if(playerId.isNotEmpty()) {
            this@MainViewModel.playerId = playerId
            gameProfile.playerId = playerId
            gameProfile.apply()
        }else
            this@MainViewModel.playerId = gameProfile.playerId
        if(loadGlobalChat)
            fetchGlobalChat()
    }

    fun addTempKey(key: String?) {
        if (key.isNullOrEmpty()) return
        savedStateHandle["tempKeys"] = tempKeys + key
        pref.edit { putString("tmpKey", key) }
        tempKeys.log("tempKeys")
    }

    fun clearTempMatches() {
        viewModelScope.launch(Dispatchers.IO) {
            tempKeys.log("tempKeys")
            tempKeys.forEach{
                tryGet { multiPlayerRef.child(it).removeValue().await() }
                savedStateHandle["tempKeys"] = tempKeys - it
                pref.getString("tmpKey", null)?.let { tmp ->
                    if(tmp == it) pref.edit { remove("tmpKey") }
                }
            }
        }
    }

    fun removeTempMatch() {
        viewModelScope.launch(Dispatchers.IO){
            pref.getString("tmpKey", null)?.let {
                multiPlayerRef.child(it).removeValue()
                pref.edit { remove("tmpKey") }
            }
        }
    }



    fun fetchGlobalChat(){
        viewModelScope.launch(Dispatchers.IO){
            globalChatRef.limitToLast(100).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val chatList = snapshot.children.mapNotNull {
                        val key = it.key
                        val ms = it.getValue<MsgStore>()
                        if(key == null || ms == null) return@mapNotNull null
                        ms.copy(key = key)
                    }.reversed()
                    savedStateHandle["globalChatList"] = chatList
                }
                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }
    }

    var friendlyValueListener  : ValueEventListener? = null
    var friendlyChatRef : DatabaseReference? = null
    fun fetchFriendlyChat(matchKey: String){
        viewModelScope.launch(Dispatchers.IO){
            val friendsChatRef = multiPlayerRef.child(matchKey).child("friendlyChat")
            friendlyValueListener?.also { friendlyChatRef?.removeEventListener(it) }
            friendlyChatRef = friendsChatRef
            friendlyValueListener = friendsChatRef.limitToLast(100).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val chatList = snapshot.children.mapNotNull {
                        val key = it.key
                        val ms = it.getValue<MsgStore>()
                        if(key == null || ms == null) return@mapNotNull null
                        if(ms.type.typeEnum == Type.ExitText)
                            localPlayerCount--
                        ms.copy(key = key)
                    }.reversed()
                    savedStateHandle["friendsChatList"] = chatList
                }
                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }
    }

    fun sendMessage2FriendlyChat(matchKey: String, text: String): Unit?{
        if(text.isEmpty()) return null
        val friendlyChatRef = multiPlayerRef.child(matchKey).child("friendlyChat")
        viewModelScope.launch(Dispatchers.IO) {
            friendlyChatRef.push().key?.let {
                friendlyChatRef.child(it).setValue(
                    gameProfile.toMessage(playerId = playerId, msg = text)
                )
            }
        }
        return Unit
    }

    fun sendMessage2GlobalChat(text: String): Unit?{
        if(text.isEmpty()) return null
        viewModelScope.launch(Dispatchers.IO) {
            globalChatRef.push().key?.let {
                globalChatRef.child(it).setValue(
                    gameProfile.toMessage(playerId = playerId, msg = text)
                )
            }
        }
        return Unit
    }

    fun sendInvitation2Chat(gameId: String?, text: String): Job? {
        if (gameId.isNullOrEmpty()) return null
        return viewModelScope.launch(Dispatchers.IO) {
            globalChatRef.push().key?.let {
                globalChatRef.child(it).setValue(
                    gameProfile.toMessage(playerId = playerId, msg = text).copy(
                        gameId = gameId,
                        type = Type.Invitation.name
                    )
                )
            }
        }
    }

    fun clearMultiPlayerDB() {
        viewModelScope.launch(Dispatchers.IO) {
            database.getReference("MultiPlayer").removeValue()
        }

    }

    fun fuckIL() {
        Firebase.firestore.collection("gamerProfile").whereEqualTo("countryEmoji", "🇮🇱")
            .addSnapshotListener { qs, ex ->
                qs?.documents?.mapNotNull { it?.toObject<GameProfile>() }?.forEach {
                    it.countryEmoji = "🇵🇸"
                    Firebase.firestore.collection("gamerProfile").document(it.playerId).set(it)
                }
            }
    }

    fun getValidKey(shortKey: String): String? = matchKeys.find { getKey4(it) == shortKey }

    fun getKey4(key: String?): String {
        if (key.isNullOrEmpty()) return ""
        return buildString {
            for (i in 4..key.length) {
                if(length == 4) break
                when (key[i]) {
                    '0', 'O', 'o' -> append('M')
                    '-', '_' -> continue
                    else -> append(key[i])
                }
            }
        }.uppercase()
    }

    suspend fun getJoinBundle(msg: MsgStore): Result<Bundle> = withContext(Dispatchers.IO) {
        fun defError(): Result<Bundle> {
            globalChatRef.child(msg.key).removeValue()
            return Result.failure<Bundle>(Exception("Match expired."))
        }
        if (msg.gameId.length != 4) return@withContext defError()
        val fullKey = getValidKey(msg.gameId) ?: return@withContext defError()
        pref.edit { putString("tmpKey", fullKey) }

        val gameRoom = suspendCoroutine<GameRoom?> { cont ->
            multiPlayerRef.child(fullKey)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        cont.resume(snapshot.getValue<GameRoom>())
                    }
                    override fun onCancelled(error: DatabaseError) {
                        cont.resume(null)
                    }
                })
        } ?: return@withContext defError()

        if (gameRoom.playerCount != "1") return@withContext Result.failure(Exception("Match already started."))
        if (gameRoom.player1.id == playerId) return@withContext Result.failure(Exception("You are already in the match."))

        // Update player2 and player count
        multiPlayerRef.child(fullKey).apply {
            child("player2").setValue(gameProfile.toPlayerInfo())
            child("playerCount").setValue("2")
            //remove
            child("playerInfo").child("nm2").setValue(gameProfile.nm)
            child("playerInfo").child("lvl2").setValue(gameProfile.lvlByCal)
            //remove
        }
        val friendsChatRef = multiPlayerRef.child(fullKey).child("friendlyChat")

        // Send join message
        friendsChatRef.push().key?.let { chatKey ->
            friendsChatRef.child(chatKey).push().setValue(
                gameProfile.toMessage(
                    playerId = playerId,
                    msg = "Joined the match.",
                    type = Type.EnterText
                )
            )
        }
        // Return the bundle
        Result.success(
            bundleOf(
                "gameKey" to fullKey,
                "plyr1" to false,

                "plr1Id" to gameRoom.player1.id,
                "nm1" to gameRoom.player1.nm,
                "lvl1" to gameRoom.player1.lvl,

                "plr2Id" to playerId,
                "nm2" to gameProfile.nm,
                "lvl2" to gameProfile.lvlByCal
            )
        )

    }

}