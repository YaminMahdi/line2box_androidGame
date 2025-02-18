package com.diu.yk_games.line2box.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.model.toMessage
import com.google.firebase.Firebase
import com.google.firebase.database.database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val database by lazy { Firebase.database }
    private val globalChatRef by lazy { database.getReference("globalChat") }

    var gameId
        get() = savedStateHandle.get<String>("gameId")
        set(value) {
            savedStateHandle["gameId"] = value
        }

    fun sendMessage2GlobalChat(text: String): Job? {
        if (text.isEmpty()) return null
        return viewModelScope.launch(Dispatchers.IO) {
            globalChatRef.push().key?.let {
                globalChatRef.child(it).setValue(GameProfile().toMessage(text))
            }
        }
    }
}