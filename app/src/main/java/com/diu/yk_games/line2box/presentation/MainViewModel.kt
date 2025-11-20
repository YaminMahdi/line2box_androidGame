package com.diu.yk_games.line2box.presentation

import android.os.Bundle
import android.util.Log
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
import com.diu.yk_games.line2box.util.log
import com.diu.yk_games.line2box.util.pref
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.ChildEventListener
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

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
    var onlineStatus =""

    val isLoading = savedStateHandle.getStateFlow("isLoading", true)
    val globalChatList = savedStateHandle.getStateFlow("globalChatList", emptyList<MsgStore>())
    val friendsChatList = savedStateHandle.getStateFlow("friendsChatList", emptyList<MsgStore>())

    val matches = savedStateHandle.getStateFlow("matches", emptyList<GameRoom>())

    var ignoreDrawerClosesSound = false
    var localPlayerCount = 2

    var gameId
        get() = savedStateHandle["gameId"] ?: ""
        set(value) { savedStateHandle["gameId"] = value }

    var playerId
        get() = savedStateHandle["playerId"] ?: ""
        set(value) { savedStateHandle["playerId"] = value }

    var matchKey
        get() = savedStateHandle["matchKey"] ?: ""
        set(value) {
            savedStateHandle["matchKey"] = value
            if (value.isNotEmpty()) {
                fetchFriendlyChat()
                addTempKey(value)
            }
        }

    var matchBundle = savedStateHandle.getStateFlow("matchBundle", Bundle())

    val tempKeys
        get() = savedStateHandle.get<List<String>>("tempKeys") ?: emptyList()

    var isStickySwitchRight
        get() = savedStateHandle.get<Boolean>("isStickySwitchRight") == true
        set(value) { savedStateHandle["isStickySwitchRight"] = value }

    val isNewMsgBoltVisible = savedStateHandle.getStateFlow("isNewMsgBoltVisible", false)


    fun setNewMsgBoltVisible(value: Boolean) {
        savedStateHandle["isNewMsgBoltVisible"] = value
    }

    fun setLoading(value: Boolean) {
        savedStateHandle["isLoading"] = value
    }

    fun initGameProfile(playerId: String = this@MainViewModel.playerId, loadGlobalChat: Boolean = true) {
        _gameProfile = GameProfile()
        if(playerId.isNotEmpty()) {
            this@MainViewModel.playerId = playerId
            gameProfile.playerId = playerId
            gameProfile.apply()
        }
        else this@MainViewModel.playerId = gameProfile.playerId
        if(loadGlobalChat) {
            fetchGlobalChat()
            fetchActiveMatches()
        }
    }

    fun addTempKey(key: String?) {
        if (key.isNullOrEmpty()) return
        viewModelScope.launch(Dispatchers.IO){
            savedStateHandle["tempKeys"] = (tempKeys + key).distinct()
            pref.save("tmpKey", key)
            tempKeys.log("tempKeys")
        }
    }

    fun clearFriendlyChat() {
        viewModelScope.launch(Dispatchers.IO){
            savedStateHandle["friendsChatList"] = emptyList<MsgStore>()
            friendlyValueListener?.also { friendlyChatRef?.removeEventListener(it) }
        }
    }

    fun clearTempMatches() {
        viewModelScope.launch(Dispatchers.IO){
            savedStateHandle["friendsChatList"] = emptyList<MsgStore>()
            tempKeys.forEach {
                if(it == matchKey)
                    matchKey = ""
                multiPlayerRef.child(it).removeValue()
                savedStateHandle["tempKeys"] = tempKeys - it
                pref.read<String?>("tmpKey", null)?.let { tmp ->
                    if(tmp == it) pref.remove("tmpKey")
                }
            }
        }
    }

    fun removeTempMatch() {
        viewModelScope.launch(Dispatchers.IO){
            pref.read<String?>("tmpKey", null)?.let {
                multiPlayerRef.child(it).removeValue()
                pref.remove("tmpKey")
            }
        }
    }

    val viewIdFromServer = MutableSharedFlow<String>()

    fun fetchServerLineClick(gameKey: String, isPlyr1: Boolean) {
        viewModelScope.launch {
            matchKey = gameKey
            val matchRef = multiPlayerRef.child(gameKey).child("matchInfo")
            matchRef.child(if(isPlyr1) "plyr2" else "plyr1").addChildEventListener(object : ChildEventListener {
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

    fun fetchGlobalChat(){
        viewModelScope.launch {
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
    fun fetchFriendlyChat(){
        viewModelScope.launch {
            if(matchKey.isEmpty()) return@launch
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

    fun sendMessage2FriendlyChat(text: String): Unit?{
        if(text.isEmpty()) return null
        val friendlyChatRef = multiPlayerRef.child(matchKey).child("friendlyChat")
        viewModelScope.launch(Dispatchers.IO) {
            friendlyChatRef.push().setValue(
                gameProfile.toMessage(playerId = playerId, msg = text)
            )
        }
        return Unit
    }

    fun sendMessage2GlobalChat(text: String): Unit?{
        if(text.isEmpty()) return null
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
            .addSnapshotListener { qs, ex ->
                qs?.documents?.mapNotNull { it?.toObject<GameProfile>() }?.forEach {
                    it.countryEmoji = "🇵🇸"
                    it.countryNm = "Palestina"
                    Firebase.firestore.collection("gamerProfile").document(it.playerId).set(it)
                }
            }
    }

    fun fetchActiveMatches(){
        viewModelScope.launch(Dispatchers.IO){
            multiPlayerRef.limitToLast(100).addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    Log.d("addList", "onChildAdded: " + dataSnapshot.key)
                    runCatching {
                        dataSnapshot.getValue<GameRoom>()?.let { game ->
                            savedStateHandle["matches"] = (matches.value + game.copy(key= dataSnapshot.key.orEmpty())).distinctBy { it.key }
                        }
                    }
                }
                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
                    removeByKey(dataSnapshot.key)
                    runCatching {
                        dataSnapshot.getValue<GameRoom>()?.let { game ->
                            savedStateHandle["matches"] = (matches.value + game.copy(key= dataSnapshot.key.orEmpty()))
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

    fun getValidMatch(shortKey: String): GameRoom? = matches.value.find { getKey4(it.key) == shortKey }

    fun getKey4(key: String = matchKey): String {
        if (key.isEmpty()) return ""
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

    fun getJoinBundle(msg: MsgStore): Result<Bundle> {
        fun defError(): Result<Bundle> {
            if(matches.value.isNotEmpty())
                globalChatRef.child(msg.key).removeValue()
            return Result.failure<Bundle>(Exception("Match expired."))
        }
        if (msg.gameId.length != 4) return defError()
        val gameRoom = getValidMatch(msg.gameId) ?: return defError()
        val fullKey = gameRoom.key
        pref.save("tmpKey", fullKey)

//        val gameRoom = suspendCoroutine<GameRoom?> { cont ->
//            multiPlayerRef.child(fullKey)
//                .addListenerForSingleValueEvent(object : ValueEventListener {
//                    override fun onDataChange(snapshot: DataSnapshot) {
//                        cont.resume(snapshot.getValue<GameRoom>())
//                    }
//                    override fun onCancelled(error: DatabaseError) {
//                        cont.resume(null)
//                    }
//                })
//        } ?: return@withContext defError()

        if (gameRoom.playerCount == "-1") return defError()
        if (gameRoom.player1.id == playerId || gameRoom.player2.id == playerId) return Result.failure(Exception("You are already in the match."))
        if (gameRoom.playerCount == "2") return Result.failure(Exception("Match already started."))

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
        friendsChatRef.push().setValue(
            gameProfile.toMessage(
                playerId = playerId,
                msg = "Joined the match.",
                type = Type.EnterText
            )
        )
        // Return the bundle
        return Result.success(
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



    val countryEmojis = ArrayList(
        listOf(
            "🇦🇫", "🇦🇱", "🇩🇿", "🇦🇩", "🇦🇴", "🇦🇬", "🇦🇷", "🇦🇲", "🇦🇺", "🇦🇹", "🇦🇿", "🇧🇸", "🇧🇭", "🇧🇩", "🇧🇧", "🇧🇾", "🇧🇪",
            "🇧🇿", "🇧🇯", "🇧🇹", "🇧🇴", "🇧🇦", "🇧🇼", "🇧🇷", "🇧🇳", "🇧🇬", "🇧🇫", "🇧🇮", "🇨🇻", "🇰🇭", "🇨🇲", "🇨🇦", "🇨🇫", "🇹🇩",
            "🇨🇱", "🇨🇳", "🇨🇴", "🇰🇲", "🇨🇩", "🇨🇷", "🇭🇷", "🇨🇺", "🇨🇾", "🇨🇿", "🇨🇮", "🇩🇰", "🇩🇯", "🇩🇲", "🇩🇴", "🇨🇩", "🇪🇨",
            "🇪🇬", "🇸🇻", "🏴󠁧󠁢󠁥󠁮󠁧󠁿", "🇬🇶", "🇪🇷", "🇪🇪", "🇸🇿", "🇪🇹", "🇫🇯", "🇫🇮", "🇫🇷", "🇬🇦", "🇬🇲", "🇬🇪", "🇩🇪", "🇬🇭", "🇬🇷",
            "🇬🇩", "🇬🇹", "🇬🇳", "🇬🇼", "🇬🇾", "🇭🇹", "🇭🇳", "🇭🇰", "🇭🇺", "🇮🇸", "🇮🇳", "🇮🇩", "🇮🇷", "🇮🇶", "🇮🇪", "🇮🇱", "🇮🇹",
            "🇯🇲", "🇯🇵", "🇯🇴", "🇰🇿", "🇰🇪", "🇰🇮", "🇰🇼", "🇰🇬", "🇱🇦", "🇱🇻", "🇱🇧", "🇱🇸", "🇱🇷", "🇱🇾", "🇱🇮", "🇱🇹", "🇱🇺",
            "🇲🇬", "🇲🇼", "🇲🇾", "🇲🇻", "🇲🇱", "🇲🇹", "🇲🇭", "🇲🇶", "🇲🇺", "🇲🇽", "🇫🇲", "🇲🇩", "🇲🇨", "🇲🇳", "🇲🇪", "🇲🇦", "🇲🇿",
            "🇲🇲", "🇳🇦", "🇳🇷", "🇳🇵", "🇳🇱", "🇳🇿", "🇳🇮", "🇳🇪", "🇳🇬", "🇰🇵", "🇲🇰", "🇳🇴", "🇴🇲", "🇵🇰", "🇵🇼", "🇵🇸", "🇵🇦",
            "🇵🇬", "🇵🇾", "🇵🇪", "🇵🇭", "🇵🇱", "🇵🇹", "🇶🇦", "🇷🇴", "🇷🇺", "🇷🇼", "🇰🇳", "🇱🇨", "🇻🇨", "🇼🇸", "🇸🇲", "🇸🇹", "🇸🇦",
            "🏴󠁧󠁢󠁳󠁣󠁴󠁿", "🇸🇳", "🇷🇸", "🇸🇨", "🇸🇱", "🇸🇬", "🇸🇰", "🇸🇮", "🇸🇧", "🇸🇴", "🇿🇦", "🇰🇷", "🇸🇸", "🇪🇸", "🇱🇰", "🇸🇩", "🇸🇷",
            "🇸🇪", "🇨🇭", "🇸🇾", "🇹🇼", "🇹🇯", "🇹🇿", "🇹🇭", "🇹🇱", "🇹🇬", "🇹🇴", "🇹🇹", "🇹🇳", "🇹🇷", "🇹🇲", "🇹🇻", "🇺🇬", "🇺🇦",
            "🇦🇪", "🇬🇧", "🇺🇸", "🇺🇾", "🇺🇿", "🇻🇺", "🇻🇪", "🇻🇳", "🇾🇪", "🇿🇲", "🇿🇼"
        )
    )
    val countryNm = ArrayList(
        listOf(
            "Afghanistan", "Albania", "Algeria", "Andorra", "Angola", "Antigua and Barbuda", "Argentina", "Armenia", "Australia", "Austria", "Azerbaijan", "Bahamas", "Bahrain", "Bangladesh", "Barbados", "Belarus", "Belgium", "Belize", "Benin", "Bhutan", "Bolivia", "Bosnia and Herzegovina", "Botswana", "Brazil", "Brunei",
            "Bulgaria", "Burkina Faso", "Burundi", "Cabo Verde", "Cambodia", "Cameroon", "Canada", "Central African Republic", "Chad", "Chile", "China", "Colombia", "Comoros", "Congo", "Costa Rica", "Croatia", "Cuba", "Cyprus", "Czechia", "Côte d'Ivoire", "Denmark", "Djibouti", "Dominica", "Dominican Republic", "DR Congo",
            "Ecuador", "Egypt", "El Salvador", "England", "Equatorial Guinea", "Eritrea", "Estonia", "Eswatini (Swaziland)", "Ethiopia", "Fiji", "Finland", "France", "Gabon", "Gambia", "Georgia", "Germany", "Ghana", "Greece", "Grenada", "Guatemala", "Guinea", "Guinea-Bissau", "Guyana", "Haiti", "Honduras", "Hong Kong", "Hungary",
            "Iceland", "India", "Indonesia", "Iran", "Iraq", "Ireland", "Israel", "Italy", "Jamaica", "Japan", "Jordan", "Kazakhstan", "Kenya", "Kiribati", "Kuwait", "Kyrgyzstan", "Laos", "Latvia", "Lebanon", "Lesotho", "Liberia", "Libya", "Liechtenstein", "Lithuania", "Luxembourg", "Madagascar", "Malawi", "Malaysia", "Maldives",
            "Mali", "Malta", "Marshall Islands", "Martinique", "Mauritius", "Mexico", "Micronesia", "Moldova", "Monaco", "Mongolia", "Montenegro", "Morocco", "Mozambique", "Myanmar", "Namibia", "Nauru", "Nepal", "Netherlands", "New Zealand", "Nicaragua", "Niger", "Nigeria", "North Korea", "North Macedonia", "Norway", "Oman",
            "Pakistan", "Palau", "Palestine", "Panama", "Papua New Guinea", "Paraguay", "Peru", "Philippines", "Poland", "Portugal", "Qatar", "Romania", "Russia", "Rwanda", "Saint Kitts and Nevis", "Saint Lucia", "Saint Vincent", "Samoa", "San Marino", "São Tomé and Príncipe", "Saudi Arabia", "Scotland", "Senegal", "Serbia",
            "Seychelles", "Sierra Leone", "Singapore", "Slovakia", "Slovenia", "Solomon Islands", "Somalia", "South Africa", "South Korea", "South Sudan", "Spain", "Sri Lanka", "Sudan", "Suriname", "Sweden", "Switzerland", "Syria", "Taiwan", "Tajikistan", "Tanzania", "Thailand", "Timor-Leste", "Togo", "Tonga",
            "Trinidad and Tobago", "Tunisia", "Turkey", "Turkmenistan", "Tuvalu", "Uganda", "Ukraine", "United Arab Emirates", "United Kingdom", "United States", "Uruguay", "Uzbekistan", "Vanuatu", "Venezuela", "Vietnam", "Yemen", "Zambia", "Zimbabwe"
        )
    )

    val countryList = listOf(
        "Afghanistan" to "🇦🇫", "Albania" to "🇦🇱", "Algeria" to "🇩🇿", "Andorra" to "🇦🇩", "Angola" to "🇦🇴", "Antigua and Barbuda" to "🇦🇬", "Argentina" to "🇦🇷", "Armenia" to "🇦🇲", "Australia" to "🇦🇺", "Austria" to "🇦🇹", "Azerbaijan" to "🇦🇿", "Bahamas" to "🇧🇸", "Bahrain" to "🇧🇭", "Bangladesh" to "🇧🇩", "Barbados" to "🇧🇧", "Belarus" to "🇧🇾",
        "Belgium" to "🇧🇪", "Belize" to "🇧🇿", "Benin" to "🇧🇯", "Bhutan" to "🇧🇹", "Bolivia" to "🇧🇴", "Bosnia and Herzegovina" to "🇧🇦", "Botswana" to "🇧🇼", "Brazil" to "🇧🇷", "Brunei" to "🇧🇳", "Bulgaria" to "🇧🇬", "Burkina Faso" to "🇧🇫", "Burundi" to "🇧🇮", "Cabo Verde" to "🇨🇻", "Cambodia" to "🇰🇭", "Cameroon" to "🇨🇲", "Canada" to "🇨🇦",
        "Central African Republic" to "🇨🇫", "Chad" to "🇹🇩", "Chile" to "🇨🇱", "China" to "🇨🇳", "Colombia" to "🇨🇴", "Comoros" to "🇰🇲", "Congo" to "🇨🇩", "Costa Rica" to "🇨🇷", "Croatia" to "🇭🇷", "Cuba" to "🇨🇺", "Cyprus" to "🇨🇾", "Czechia" to "🇨🇿", "Côte d'Ivoire" to "🇨🇮", "Denmark" to "🇩🇰", "Djibouti" to "🇩🇯", "Dominica" to "🇩🇲",
        "Dominican Republic" to "🇩🇴", "DR Congo" to "🇨🇩", "Ecuador" to "🇪🇨", "Egypt" to "🇪🇬", "El Salvador" to "🇸🇻", "England" to "🏴", "Equatorial Guinea" to "🇬🇶", "Eritrea" to "🇪🇷", "Estonia" to "🇪🇪", "Eswatini (Swaziland)" to "🇸🇿", "Ethiopia" to "🇪🇹", "Fiji" to "🇫🇯", "Finland" to "🇫🇮", "France" to "🇫🇷", "Gabon" to "🇬🇦", "Gambia" to "🇬🇲",
        "Georgia" to "🇬🇪", "Germany" to "🇩🇪", "Ghana" to "🇬🇭", "Greece" to "🇬🇷", "Grenada" to "🇬🇩", "Guatemala" to "🇬🇹", "Guinea" to "🇬🇳", "Guinea-Bissau" to "🇬🇼", "Guyana" to "🇬🇾", "Haiti" to "🇭🇹", "Honduras" to "🇭🇳", "Hong Kong" to "🇭🇰", "Hungary" to "🇭🇺", "Iceland" to "🇮🇸", "India" to "🇮🇳", "Indonesia" to "🇮🇩", "Iran" to "🇮🇷",
        "Iraq" to "🇮🇶", "Ireland" to "🇮🇪", "Italy" to "🇮🇹", "Jamaica" to "🇯🇲", "Japan" to "🇯🇵", "Jordan" to "🇯🇴", "Kazakhstan" to "🇰🇿", "Kenya" to "🇰🇪", "Kiribati" to "🇰🇮", "Kuwait" to "🇰🇼", "Kyrgyzstan" to "🇰🇬", "Laos" to "🇱🇦", "Latvia" to "🇱🇻", "Lebanon" to "🇱🇧", "Lesotho" to "🇱🇸", "Liberia" to "🇱🇷", "Libya" to "🇱🇾", "Liechtenstein" to "🇱🇮",
        "Lithuania" to "🇱🇹", "Luxembourg" to "🇱🇺", "Madagascar" to "🇲🇬", "Malawi" to "🇲🇼", "Malaysia" to "🇲🇾", "Maldives" to "🇲🇻", "Mali" to "🇲🇱", "Malta" to "🇲🇹", "Marshall Islands" to "🇲🇭", "Mexico" to "🇲🇽", "Moldova" to "🇲🇩", "Monaco" to "🇲🇨", "Mongolia" to "🇲🇳", "Montenegro" to "🇲🇪", "Morocco" to "🇲🇦", "Mozambique" to "🇲🇿",
        "Myanmar" to "🇲🇲", "Namibia" to "🇳🇦", "Nauru" to "🇳🇷", "Nepal" to "🇳🇵", "Netherlands" to "🇳🇱", "New Zealand" to "🇳🇿", "Nicaragua" to "🇳🇮", "Niger" to "🇳🇪", "Nigeria" to "🇳🇬", "North Korea" to "🇰🇵", "North Macedonia" to "🇲🇰", "Norway" to "🇳🇴", "Oman" to "🇴🇲", "Pakistan" to "🇵🇰", "Palestine" to "🇵🇸", "Panama" to "🇵🇦", "Papua New Guinea" to "🇵🇬",
        "Paraguay" to "🇵🇾", "Peru" to "🇵🇪", "Philippines" to "🇵🇭", "Poland" to "🇵🇱", "Portugal" to "🇵🇹", "Qatar" to "🇶🇦", "Romania" to "🇷🇴", "Russia" to "🇷🇺", "Rwanda" to "🇷🇼", "Saudi Arabia" to "🇸🇦", "Scotland" to "🏴", "Serbia" to "🇷🇸", "South Korea" to "🇰🇷", "Spain" to "🇪🇸", "Sri Lanka" to "🇱🇰", "Turkey" to "🇹🇷", "United Arab Emirates" to "🇦🇪",
        "United Kingdom" to "🇬🇧", "United States" to "🇺🇸", "Uruguay" to "🇺🇾", "Uzbekistan" to "🇺🇿", "Vanuatu" to "🇻🇺", "Venezuela" to "🇻🇪", "Vietnam" to "🇻🇳", "Yemen" to "🇾🇪", "Zambia" to "🇿🇲", "Zimbabwe" to "🇿🇼"
    )

}