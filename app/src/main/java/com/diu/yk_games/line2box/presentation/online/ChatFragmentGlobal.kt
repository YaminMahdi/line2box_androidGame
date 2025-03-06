package com.diu.yk_games.line2box.presentation.online

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.DialogLayoutProfileBinding
import com.diu.yk_games.line2box.databinding.FragmentChatGlobalBinding
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.pref
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.diu.yk_games.line2box.presentation.MsgListAdapter
import com.diu.yk_games.line2box.util.collectWithLifecycle
import com.diu.yk_games.line2box.util.gone
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.diu.yk_games.line2box.util.setClipBoardData
import com.diu.yk_games.line2box.util.toast
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject

class ChatFragmentGlobal : Fragment() {
    private lateinit var binding: FragmentChatGlobalBinding
    private val viewModel by activityViewModels<MainViewModel>()
    private lateinit var activity: Activity
//    private lateinit var playerId: String
    private val msgListAdapter by lazy { MsgListAdapter(viewModel.playerId) }


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
//        arguments?.let{
//            playerId = it.getString("playerId").orEmpty()
//        }
        binding.showMsgList.adapter = msgListAdapter
        binding.chatBoxGlobal.requestFocus()
        viewModel.globalChatList.collectWithLifecycle {
            msgListAdapter.submitList(it){
                binding.showMsgList.scrollToPosition(0)
            }
        }

        msgListAdapter.onClickListener = { msg ->
            //presentationEco str = (presentationEco)o; //As you are using Default String Adapter
            if (!pref.getBoolean("muted", false)) {
                val mediaPlayer =
                    MediaPlayer.create(activity, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            if (msg.playerId.isNotEmpty()) {
                val db = Firebase.firestore
                db.collection("gamerProfile").document(msg.playerId)
                    .get().addOnSuccessListener { documentSnapshot ->
                        val server2device = documentSnapshot.toObject<GameProfile>()
                        if (server2device != null) {
                            val builder = AlertDialog.Builder(activity)
                            val binding =
                                DialogLayoutProfileBinding.inflate(layoutInflater)
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
                            alertDialog.window?.setBackgroundDrawable(0.toDrawable())
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
            activity.setClipBoardData(msg.msgData, "Text copied")
            true
        }

        msgListAdapter.onJoinClickListener = { msg ->
            viewModel.getJoinBundle(msg).onSuccess {
                activity.findViewById<DrawerLayout>(R.id.drawer_layout)?.closeDrawer(GravityCompat.START)
                activity.startActivity(Intent(activity, GameActivity2::class.java).putExtras(it))
            }.onFailure {
                toast(it.message.toString())
            }
        }

        binding.msgSendBtn.setBounceClickListener {
            val mp = MediaPlayer.create(activity, R.raw.pop)
            mp.start()
            mp.setOnCompletionListener(MediaPlayer::release)
            viewModel.sendMessage2GlobalChat(binding.chatBoxGlobal.text.toString())?.also{
                binding.chatBoxGlobal.setText("")
            } ?: toast("Write Something..")
        }
    }

//    companion object {
//        fun newInstance(playerId: String?): ChatFragmentGlobal {
//            val fragment = ChatFragmentGlobal()
//            fragment.arguments = bundleOf("playerId" to playerId)
//            return fragment
//        }
//    }
}