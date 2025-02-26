package com.diu.yk_games.line2box.presentation

import android.os.Bundle
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.model.GameRoom
import com.diu.yk_games.line2box.model.MsgStore.Type
import com.diu.yk_games.line2box.model.toMessage
import com.diu.yk_games.line2box.model.toPlayerInfo
import com.diu.yk_games.line2box.pref
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val database by lazy { Firebase.database }
    private val globalChatRef by lazy { database.getReference("globalChat") }
    private val friendsChatRef by lazy { database.getReference("friendsChat") }
    private val multiPlayerRef by lazy { database.getReference("MultiPlayer") }
    lateinit var gameProfile: GameProfile

    fun initGameProfile(playerId: String = this@MainViewModel.playerId) {
        this@MainViewModel.playerId = playerId
        gameProfile = GameProfile()
        gameProfile.playerId = playerId
    }

    var gameId
        get() = savedStateHandle.get<String>("gameId")
        set(value) {
            savedStateHandle["gameId"] = value
        }

    var playerId
        get() = savedStateHandle.get<String>("playerId") ?: ""
        set(value) {
            savedStateHandle["playerId"] = value
        }

    var matchKeys = mutableListOf<String>()

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

    suspend fun getJoinBundle(shortKey: String) = withContext(Dispatchers.IO) {
        val defError = Result.failure<Bundle>(Exception("Match expired."))
        val fullKey = getValidKey(shortKey) ?: return@withContext defError
        pref.edit { putString("tmpKey", fullKey) }
        if (shortKey.length != 4) return@withContext defError

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
        } ?: return@withContext defError

        if (gameRoom.playerCount.toIntOrNull() != 1) return@withContext Result.failure(Exception("Match already started."))
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
        // Send join message
        friendsChatRef.push().key?.let { chatKey ->
            friendsChatRef.child(chatKey).child("friendlyChat").push().setValue(
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