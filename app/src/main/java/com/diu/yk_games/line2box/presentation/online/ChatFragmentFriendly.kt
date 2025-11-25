package com.diu.yk_games.line2box.presentation.online

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.DialogLayoutProfileBinding
import com.diu.yk_games.line2box.databinding.FragmentChatFriendlyBinding
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.diu.yk_games.line2box.presentation.MsgListAdapter
import com.diu.yk_games.line2box.util.collectWithLifecycle
import com.diu.yk_games.line2box.util.gone
import com.diu.yk_games.line2box.util.loadDrawable
import com.diu.yk_games.line2box.util.log
import com.diu.yk_games.line2box.util.pref
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.diu.yk_games.line2box.util.setClipBoardData
import com.diu.yk_games.line2box.util.show
import com.diu.yk_games.line2box.util.toast
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatFragmentFriendly : Fragment() {
    private lateinit var binding: FragmentChatFriendlyBinding
    private val viewModel by activityViewModels<MainViewModel>()
    private lateinit var parentActivity: Activity
    private val msgListAdapter by lazy { MsgListAdapter(viewModel.playerId) }
    private val drawerLayout by lazy { parentActivity.findViewById<DrawerLayout>(R.id.drawer_layout) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatFriendlyBinding.inflate(inflater, container, false)
        parentActivity = requireActivity()
        binding.showMsgList.adapter = msgListAdapter
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.chatBoxFriendly.requestFocus()
        val mp = MediaPlayer.create(parentActivity, R.raw.pop)

        viewModel.friendsChatList.collectWithLifecycle {
            it.size.log("friendsChatList")
            msgListAdapter.submitList(it) {
                binding.showMsgList.scrollToPosition(0)
            }
            val lastMsg = it.firstOrNull()
            if (lastMsg?.key == lastMsgKey) return@collectWithLifecycle
            when (lastMsg?.msgData) {
                "🤣" -> emojiRunner(R.drawable.emoji_haha, R.raw.haha)
                "😭" -> emojiRunner(R.drawable.emoji_cry, R.raw.cry)
                "😱" -> emojiRunner(R.drawable.emoji_scream, R.raw.scream)
                "😘" -> emojiRunner(R.drawable.emoji_kiss, R.raw.kiss)
                "🥱" -> emojiRunner(R.drawable.emoji_yawn, R.raw.yawn)
                else -> {
                    mp.start()
                    viewModel.setNewMsgBoltVisible(true)
                }
            }
            lastMsg?.key?.let {
                lastMsgKey = lastMsg.key
            }
        }

        msgListAdapter.onClickListener = { msg ->
            //presentationEco str = (presentationEco)o; //As you are using Default String Adapter
            if (!pref.read("muted", false)) {
                val mediaPlayer =
                    MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            if (msg.playerId.isNotEmpty()) {
                val db = Firebase.firestore
                db.collection("gamerProfile").document(msg.playerId)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
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
                                countryTxt.text =
                                    "${server2device.countryNm} ${server2device.countryEmoji}"
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
        binding.msgSendBtn.setBounceClickListener {
            viewModel.sendMessage2FriendlyChat(binding.chatBoxFriendly.text.toString())?.also {
                binding.chatBoxFriendly.setText("")
            } ?: toast("Write Something..")
        }
        binding.sendHaha.setBounceClickListener { sendEmoji("🤣") }
        binding.sendCry.setBounceClickListener { sendEmoji("😭") }
        binding.sendKiss.setBounceClickListener { sendEmoji("😘") }
        binding.sendScream.setBounceClickListener { sendEmoji("😱") }
        binding.sendYawn.setBounceClickListener { sendEmoji("🥱") }
        binding.chatBoxFriendly.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                viewModel.sendMessage2FriendlyChat(binding.chatBoxFriendly.text.toString())?.also {
                    binding.chatBoxFriendly.setText("")
                } ?: toast("Write Something..")
                true // Return true to indicate that you have consumed the event
            } else {
                false // Return false to allow the system to handle the event
            }
        }
    }

    private fun sendEmoji(emoji: String) {
        viewModel.sendMessage2FriendlyChat(text = emoji)
        viewModel.ignoreDrawerClosesSound = true
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    fun emojiRunner(gif: Int, sound: Int) {
        binding.sendHaha.isEnabled = false
        binding.sendCry.isEnabled = false
        binding.sendKiss.isEnabled = false
        binding.sendScream.isEnabled = false
        binding.sendYawn.isEnabled = false
        if (!pref.read("muted", false)) {
            val mediaPlayer = MediaPlayer.create(parentActivity, sound)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        parentActivity.findViewById<ImageView>(R.id.emojiPlay).apply {
            loadDrawable(gif)
            show()
        }
        viewModel.setNewMsgBoltVisible(false)
        lifecycleScope.launch {
            delay(2500)
            parentActivity.findViewById<View>(R.id.emojiPlay).gone()
            binding.sendHaha.isEnabled = true
            binding.sendCry.isEnabled = true
            binding.sendKiss.isEnabled = true
            binding.sendScream.isEnabled = true
            binding.sendYawn.isEnabled = true
        }
    }


    companion object {
        var lastMsgKey = ""
    }
}