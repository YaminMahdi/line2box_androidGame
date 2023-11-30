package com.diu.yk_games.line2box.presentation.online

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.FragmentChatFriendlyBinding
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.model.MsgStore
import com.diu.yk_games.line2box.presentation.MsgListAdapter
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pl.droidsonroids.gif.GifImageView

class ChatFragmentFriendly : Fragment() {
    private lateinit var binding : FragmentChatFriendlyBinding
    var msList: MutableList<MsgStore> = mutableListOf()
    private var database = FirebaseDatabase.getInstance()
    private lateinit var myRef: DatabaseReference
    var tempMsg: String? = null
    var lastMsg: String? = null
    private lateinit var activity: Activity
    private lateinit var playerId: String
    lateinit var sharedPref: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            key = requireArguments().getString("key")!!
            playerId = requireArguments().getString("playerId")!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatFriendlyBinding.inflate(inflater, container, false)
        myRef = database.getReference("MultiPlayer").child(key).child("friendlyChat")

//        val v = inflater.inflate(R.layout.fragment_chat_friendly, container, false)
        binding.chatBoxFriendly.isFocusableInTouchMode
        binding.chatBoxFriendly.requestFocus()
        activity = requireActivity()
        sharedPref = activity.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE
        )
        GameProfile.setPreferences(sharedPref)
        val mp = MediaPlayer.create(activity, R.raw.pop)
        val mDrawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout)
        myRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                if (dataSnapshot.exists()) {
                    val ms = dataSnapshot.getValue(MsgStore::class.java)!!
                    msList.add(ms)
                    lastMsg = ms.msgData
                    if(this@ChatFragmentFriendly.isAdded) {
                        try {
                            val adapter = MsgListAdapter(activity, msList) //
                            binding.showMsgList.adapter = adapter
                            binding.showMsgList.onItemClickListener =
                                OnItemClickListener { parent, view, position, id ->
                                    val msgData = binding.showMsgList.getItemAtPosition(position) as MsgStore
                                    //presentationEco str = (presentationEco)o; //As you are using Default String Adapter
                                    if (!sharedPref.getBoolean("muted", false)) {
                                        val mediaPlayer =
                                            MediaPlayer.create(activity, R.raw.btn_click_ef)
                                        mediaPlayer.start()
                                        mediaPlayer.setOnCompletionListener (MediaPlayer::release)
                                    }
                                    if (msgData.playerId != "") {
                                        Toast.makeText(
                                            activity,
                                            "Long Press To Copy Text/ID",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        val db = FirebaseFirestore.getInstance()
                                        db.collection("gamerProfile").document(msgData.playerId)
                                            .get()
                                            .addOnSuccessListener { documentSnapshot ->
                                                if (documentSnapshot.exists()) {
                                                    val server2device = documentSnapshot.toObject(
                                                        GameProfile::class.java
                                                    )
                                                    val builder = AlertDialog.Builder(activity)
                                                    val v = LayoutInflater.from(activity).inflate(
                                                        R.layout.dialog_layout_profile,
                                                        parent.findViewById(R.id.profileLayoutDialog)
                                                    )
                                                    builder.setView(v)
                                                    val params = LinearLayout.LayoutParams(
                                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                                    )
                                                    params.setMargins(60, 150, 60, 0)
                                                    v.findViewById<View>(R.id.linearLayoutFrame).layoutParams =
                                                        params
                                                    assert(server2device != null)
                                                    (v.findViewById<View>(R.id.countryTxt) as TextView).text = "${server2device!!.countryNm} ${server2device.countryEmoji}"
                                                    (v.findViewById<View>(R.id.lvlTxt) as TextView).text = server2device.lvl.toString()
                                                    (v.findViewById<View>(R.id.coinHave) as TextView).text = server2device.coin.toString()
                                                    (v.findViewById<View>(R.id.matchPlayedTxt) as TextView).text = server2device.matchPlayed.toString()
                                                    (v.findViewById<View>(R.id.matchWonTxt) as TextView).text = server2device.matchWinMulti.toString()
                                                    val nmEditText = v.findViewById<EditText>(R.id.nmTxt)
                                                    nmEditText.isEnabled = false
                                                    nmEditText.setText(server2device.nm)
                                                    //nmEditText.setVisibility(View.GONE);
                                                    (v.findViewById<View>(R.id.profileTitle) as TextView).textSize = 28f
                                                    v.findViewById<View>(R.id.profileShapeLayout).visibility = View.GONE
                                                    v.findViewById<View>(R.id.nmEditBtn).visibility = View.GONE
                                                    v.findViewById<View>(R.id.nmLTxt).visibility = View.GONE
                                                    v.findViewById<View>(R.id.themeBox).visibility = View.GONE
                                                    v.findViewById<View>(R.id.countryLTxt).visibility = View.GONE
                                                    v.findViewById<View>(R.id.buttonSaveInfo).visibility = View.GONE
                                                    val alertDialog = builder.create()
                                                    alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
                                                    try {
                                                        alertDialog.show()
                                                    } catch (npe: Exception) {
                                                        npe.printStackTrace()
                                                    }
                                                }
                                            }
                                    } else
                                        Toast.makeText(activity, "Older messages don't have profile info.", Toast.LENGTH_SHORT).show()
                                }
                            binding.showMsgList.onItemLongClickListener =
                                OnItemLongClickListener { parent, view, position, id ->
                                    Toast.makeText(activity, "Text/ID copied", Toast.LENGTH_SHORT).show()
                                    val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("msgData", ms.msgData)
                                    clipboard.setPrimaryClip(clip)
                                    true
                                }
                            requireActivity().findViewById<View>(R.id.newMsgBoltu).visibility = View.VISIBLE
                        } catch (npe: Exception) {
                            npe.printStackTrace()
                        }
                    }
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {}
        })
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    if (lastMsg == "🤣") emojiRunner(R.drawable.emoji_haha, R.raw.haha)
                    else if (lastMsg == "😭") emojiRunner(R.drawable.emoji_cry, R.raw.cry)
                    else if (lastMsg == "😱") emojiRunner(R.drawable.emoji_scream, R.raw.scream)
                    else if (lastMsg == "😘") emojiRunner(R.drawable.emoji_kiss, R.raw.kiss)
                    else if (lastMsg == "🥱") emojiRunner(R.drawable.emoji_yawn, R.raw.yawn)
                    else if (lastMsg == tempMsg) {
                        val mediaPlayer = MediaPlayer.create(activity, R.raw.pop)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener (MediaPlayer::release)
                    } else if (!mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                        mp.start()
                        //mp.setOnCompletionListener(MediaPlayer::release);
                    }
                    tempMsg = " # # 69"
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
        binding.msgSendBtn.setBounceClickListener {
            sendThisMsg(binding.chatBoxFriendly.text.toString())
            binding.chatBoxFriendly.setText("")
        }
        binding.sendHaha.setBounceClickListener { sendThisMsg("🤣") }
        binding.sendCry.setBounceClickListener { sendThisMsg("😭") }
        binding.sendKiss.setBounceClickListener { sendThisMsg("😘") }
        binding.sendScream.setBounceClickListener { sendThisMsg("😱") }
        binding.sendYawn.setBounceClickListener { sendThisMsg("🥱") }
        return binding.root
    }

    fun emojiRunner(gif: Int, sound: Int) {
        binding.sendHaha.isEnabled = false
        binding.sendCry.isEnabled = false
        binding.sendKiss.isEnabled = false
        binding.sendScream.isEnabled = false
        binding.sendYawn.isEnabled = false
        if (!sharedPref.getBoolean("muted", false)) {
            val mediaPlayer = MediaPlayer.create(activity, sound)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener (MediaPlayer::release)
        }
        (activity.findViewById<View>(R.id.emojiPlay) as GifImageView).setImageResource(gif)
        activity.findViewById<View>(R.id.emojiPlay).visibility = View.VISIBLE
        (activity.findViewById<View>(R.id.drawer_layout) as DrawerLayout).closeDrawer(
            GravityCompat.START
        )
        activity.findViewById<View>(R.id.newMsgBoltu).visibility = View.GONE
        lifecycleScope.launch {
            delay(2500)
            activity.findViewById<View>(R.id.emojiPlay).visibility = View.GONE
            binding.sendHaha.isEnabled = true
            binding.sendCry.isEnabled = true
            binding.sendKiss.isEnabled = true
            binding.sendScream.isEnabled = true
            binding.sendYawn.isEnabled = true
        }
    }

    private fun sendThisMsg(msg: String?) {
        val gp = GameProfile()
        val ms = MsgStore()
        ms.playerId = playerId
        ms.nmData = gp.nm
        ms.lvlData = gp.lvlByCal.toString()
        ms.time = System.currentTimeMillis()
        tempMsg = msg
        ms.msgData = msg!!
        val key = myRef.push().key!!
        myRef.child(key).setValue(ms)
    }

    companion object {
        lateinit var key: String
        @JvmStatic
        fun newInstance(key: String?, playerId: String?): ChatFragmentFriendly {
            val fragment = ChatFragmentFriendly()
            val args = Bundle()
            args.putString("key", key)
            args.putString("playerId", playerId)
            fragment.arguments = args
            return fragment
        }
    }
}