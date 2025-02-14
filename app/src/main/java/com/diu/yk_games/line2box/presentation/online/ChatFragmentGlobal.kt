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
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.DialogLayoutProfileBinding
import com.diu.yk_games.line2box.databinding.FragmentChatGlobalBinding
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.model.MsgStore
import com.diu.yk_games.line2box.pref
import com.diu.yk_games.line2box.presentation.MsgListAdapter
import com.diu.yk_games.line2box.util.gone
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.diu.yk_games.line2box.util.setClipBoardData
import com.diu.yk_games.line2box.util.toast
import com.google.firebase.Firebase
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject

class ChatFragmentGlobal : Fragment() {
    private lateinit var binding: FragmentChatGlobalBinding
    var msList = mutableListOf<MsgStore>()
    private var database = Firebase.database
    private var myRef = database.getReference("globalChat")
    private lateinit var activity: Activity
    private val msgListAdapter by lazy { MsgListAdapter() }
    private lateinit var playerId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let{
            playerId = it.getString("playerId").orEmpty()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatGlobalBinding.inflate(inflater, container, false)
        activity = requireActivity()
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.showMsgList.adapter = msgListAdapter
        binding.chatBoxGlobal.requestFocus()
        myRef.limitToLast(100).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                val ms = dataSnapshot.getValue<MsgStore>()
                if (dataSnapshot.exists() && ms != null) {
                    msList.add(0,ms)
                    msgListAdapter.submitList(msList)
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {}
        })

        msgListAdapter.onClickListener = { msg ->
            val playerId1 = msg.playerId
            //presentationEco str = (presentationEco)o; //As you are using Default String Adapter
            if (!pref.getBoolean("muted", false)) {
                val mediaPlayer =
                    MediaPlayer.create(activity, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            if (playerId1 != "") {
                val db = Firebase.firestore
                db.collection("gamerProfile").document(playerId1)
                    .get().addOnSuccessListener { documentSnapshot ->
                        val server2device = documentSnapshot.toObject<GameProfile>()
                        if (server2device != null) {
                            val builder = AlertDialog.Builder(activity)
                            val binding =
                                DialogLayoutProfileBinding.inflate(layoutInflater, null, false)
                            builder.setView(binding.root)
                            val params = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            params.setMargins(60, 150, 60, 0)
                            binding.apply {
                                linearLayoutFrame.layoutParams = params
                                countryTxt.text = "${server2device.countryNm} ${server2device.countryEmoji}"
                                lvlTxt.text = server2device.lvl.toString()
                                coinHave.text = server2device.coin.toString()
                                matchPlayedTxt.text = server2device.matchPlayed.toString()
                                matchWonTxt.text = server2device.matchWinMulti.toString()
                                nmTxt.isEnabled = false
                                nmTxt.setText(server2device.nm)
                                profileTitle.textSize = 28f
                                profileShapeLayout.gone()
                                nmEditBtn.gone()
                                nmLTxt.gone()
                                themeBox.gone()
                                countryLTxt.gone()
                                buttonSaveInfo.gone()
                            }
                            val alertDialog = builder.create()
                            alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
                            binding.root.setOnClickListener {
                                alertDialog.dismiss()
                            }
                            try {
                                alertDialog.show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
            } else
                toast("Older messages don't have profile info.")
        }

        msgListAdapter.onLongClickListener = { msg ->
            activity.setClipBoardData(msg.msgData, "Text/ID copied")
            true
        }

        binding.msgSendBtn.setBounceClickListener {
            val mp = MediaPlayer.create(activity, R.raw.pop)
            mp.start()
            mp.setOnCompletionListener(MediaPlayer::release)
            val gp = GameProfile()
            val ms = MsgStore()
            ms.playerId = playerId
            ms.nmData = gp.nm
            ms.lvlData = gp.lvlByCal.toString()
            ms.time = System.currentTimeMillis()
            ms.msgData = binding.chatBoxGlobal.text.toString()
            if (ms.msgData.isNotEmpty()) {
                val key = myRef.push().key!!
                myRef.child(key).setValue(ms)
                binding.chatBoxGlobal.setText("")
            } else {
                Toast.makeText(activity, "Write Something", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        fun newInstance(playerId: String?): ChatFragmentGlobal {
            val fragment = ChatFragmentGlobal()
            fragment.arguments = bundleOf("playerId" to playerId)
            return fragment
        }
    }
}