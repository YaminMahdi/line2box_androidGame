package com.diu.yk_games.line2box.presentation.online

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
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
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.diu.yk_games.line2box.presentation.adapter.MsgListAdapter
import com.diu.yk_games.line2box.util.*
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject

class ChatFragmentGlobal : Fragment() {
    private lateinit var binding: FragmentChatGlobalBinding
    private val viewModel by activityViewModels<MainViewModel>()
    private lateinit var parentActivity: Activity
    private val msgListAdapter by lazy { MsgListAdapter(viewModel.playerId) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatGlobalBinding.inflate(inflater, container, false)
        parentActivity = requireActivity()
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.showMsgList.adapter = msgListAdapter
        binding.chatBoxGlobal.requestFocus()
        viewModel.globalChatList.collectWithLifecycle {
            msgListAdapter.submitList(it){
                binding.showMsgList.scrollToPosition(0)
            }
        }

        msgListAdapter.onClickListener = { msg ->
            //presentationEco str = (presentationEco)o; //As you are using Default String Adapter
            viewModel.player.playButtonClickSound()
            if (msg.playerId.isNotEmpty()) {
                val db = Firebase.firestore
                db.collection("gamerProfile").document(msg.playerId)
                    .get().addOnSuccessListener { documentSnapshot ->
                        val server2device = documentSnapshot.toObject<GameProfile>()
                        if (server2device != null) {
                            val builder = AlertDialog.Builder(parentActivity)
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
                                countryLTxt.gone()
                                buttonSaveInfo.gone()
                            }
                            val alertDialog = builder.create()
                            alertDialog.window?.setBackgroundDrawable(0.toDrawable())
                            binding.root.setOnClickListener {
                                runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
                            }
                            runCatching { alertDialog.show() }
                        }
                    }
            } else
                toast("Older messages don't have profile info.")
        }

        msgListAdapter.onLongClickListener = { msg ->
            parentActivity.setClipBoardData(msg.msgData, "Text copied")
            true
        }

        msgListAdapter.onJoinClickListener = { msg ->
            viewModel.getJoinRoute(msg).onSuccess {
                parentActivity.findViewById<DrawerLayout>(R.id.drawer_layout)?.closeDrawer(GravityCompat.START)
//                parentActivity.startActivity(Intent(parentActivity, GameActivity2::class.java).putExtras(it))
                navigateSafe(it)
            }.onFailure {
                toast(it.message.toString())
            }
        }

        binding.msgSendBtn.setBounceClickListener {
            viewModel.player.playPopSound()
            viewModel.sendMessage2GlobalChat(binding.chatBoxGlobal.text.toString())?.also{
                binding.chatBoxGlobal.setText("")
            } ?: toast("Write Something..")
        }

        binding.chatBoxGlobal.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                viewModel.sendMessage2GlobalChat(binding.chatBoxGlobal.text.toString())?.also {
                    binding.chatBoxGlobal.setText("")
                } ?: toast("Write Something..")
                true // Return true to indicate that you have consumed the event
            } else {
                false // Return false to allow the system to handle the event
            }
        }
    }
}