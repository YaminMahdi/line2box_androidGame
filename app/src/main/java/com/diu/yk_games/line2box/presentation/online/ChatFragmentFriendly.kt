package com.diu.yk_games.line2box.presentation.online

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.DialogLayoutProfileBinding
import com.diu.yk_games.line2box.databinding.FragmentChatFriendlyBinding
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.model.MsgStore
import com.diu.yk_games.line2box.pref
import com.diu.yk_games.line2box.presentation.MsgListAdapter
import com.diu.yk_games.line2box.util.gone
import com.diu.yk_games.line2box.util.loadDrawable
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.diu.yk_games.line2box.util.setClipBoardData
import com.diu.yk_games.line2box.util.show
import com.diu.yk_games.line2box.util.toast
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatFragmentFriendly : Fragment() {
    private lateinit var binding : FragmentChatFriendlyBinding
    var msList: MutableList<MsgStore> = mutableListOf()
    private var database = FirebaseDatabase.getInstance()
    private lateinit var myRef: DatabaseReference
    var tempMsg: String? = null
    var lastMsg: String? = null
    private lateinit var activity: Activity
    private lateinit var playerId: String
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

        binding.chatBoxFriendly.requestFocus()
        activity = requireActivity()
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                            activity.findViewById<View>(R.id.newMsgBoltu).show()
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
        binding.showMsgList.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val msgData = msList[position]
                //presentationEco str = (presentationEco)o; //As you are using Default String Adapter
                if (!pref.getBoolean("muted", false)) {
                    val mediaPlayer =
                        MediaPlayer.create(activity, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
                if (msgData.playerId != "") {
                    toast("Long Press To Copy Text/ID")

                    val db = FirebaseFirestore.getInstance()
                    db.collection("gamerProfile").document(msgData.playerId)
                        .get()
                        .addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {
                                val server2device = documentSnapshot.toObject<GameProfile>()
                                val builder = AlertDialog.Builder(activity)
                                val binding = DialogLayoutProfileBinding.inflate(layoutInflater, null, false)
                                builder.setView(binding.root)
                                val params = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                params.setMargins(60, 150, 60, 0)
                                binding.linearLayoutFrame.layoutParams = params
                                binding.countryTxt.text = "${server2device!!.countryNm} ${server2device.countryEmoji}"
                                binding.lvlTxt.text = server2device.lvl.toString()
                                binding.coinHave.text = server2device.coin.toString()
                                binding.matchPlayedTxt.text = server2device.matchPlayed.toString()
                                binding.matchWonTxt.text = server2device.matchWinMulti.toString()
                                binding.nmTxt.isEnabled = false
                                binding.nmTxt.setText(server2device.nm)
                                //nmEditText.setVisibility(View.GONE);
                                binding.profileTitle.textSize = 28f
                                binding.profileShapeLayout.gone()
                                binding.nmEditBtn.gone()
                                binding.nmLTxt.gone()
                                binding.themeBox.gone()
                                binding.countryLTxt.gone()
                                binding.buttonSaveInfo.gone()
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
                    toast("Older messages don't have profile info.")
            }
        binding.showMsgList.onItemLongClickListener =
            OnItemLongClickListener { _, _, position, _ ->
                activity.setClipBoardData(msList[position].msgData, "Text/ID copied")
                true
            }
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
    }

    fun emojiRunner(gif: Int, sound: Int) {
        binding.sendHaha.isEnabled = false
        binding.sendCry.isEnabled = false
        binding.sendKiss.isEnabled = false
        binding.sendScream.isEnabled = false
        binding.sendYawn.isEnabled = false
        if (!pref.getBoolean("muted", false)) {
            val mediaPlayer = MediaPlayer.create(activity, sound)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener (MediaPlayer::release)
        }
        (activity.findViewById<View>(R.id.emojiPlay) as ImageView).loadDrawable(gif)
        activity.findViewById<View>(R.id.emojiPlay).show()
        (activity.findViewById<View>(R.id.drawer_layout) as DrawerLayout).closeDrawer(
            GravityCompat.START
        )
        activity.findViewById<View>(R.id.newMsgBoltu).gone()
        lifecycleScope.launch {
            delay(2500)
            activity.findViewById<View>(R.id.emojiPlay).gone()
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
        fun newInstance(key: String?, playerId: String?): ChatFragmentFriendly {
            val fragment = ChatFragmentFriendly()
            fragment.arguments = bundleOf("key" to key, "playerId" to playerId)
            return fragment
        }
    }
}