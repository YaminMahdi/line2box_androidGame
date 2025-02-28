package com.diu.yk_games.line2box.presentation.online

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.DialogLayoutProfileBinding
import com.diu.yk_games.line2box.databinding.FragmentChatFriendlyBinding
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.pref
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.diu.yk_games.line2box.presentation.MsgListAdapter
import com.diu.yk_games.line2box.util.collectWithLifecycle
import com.diu.yk_games.line2box.util.gone
import com.diu.yk_games.line2box.util.loadDrawable
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
    private lateinit var activity: Activity
    private lateinit var playerId: String
    private val msgListAdapter by lazy { MsgListAdapter(playerId) }
    private val drawerLayout by lazy { activity.findViewById<DrawerLayout>(R.id.drawer_layout) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatFriendlyBinding.inflate(inflater, container, false)
        activity = requireActivity()
        arguments?.let{
            key = it.getString("key").orEmpty()
            playerId = it.getString("playerId").orEmpty()
        }
        binding.showMsgList.adapter = msgListAdapter
        viewModel.fetchFriendlyChat(key)
        val mp = MediaPlayer.create(activity, R.raw.pop)
        viewModel.friendsChatList.collectWithLifecycle {
            msgListAdapter.submitList(it)
            val lastMsg = it.firstOrNull()
            if(lastMsg?.key == lastMsgKey) return@collectWithLifecycle
            when(lastMsg?.msgData){
                "🤣" -> emojiRunner(R.drawable.emoji_haha, R.raw.haha)
                "😭" -> emojiRunner(R.drawable.emoji_cry, R.raw.cry)
                "😱" -> emojiRunner(R.drawable.emoji_scream, R.raw.scream)
                "😘" -> emojiRunner(R.drawable.emoji_kiss, R.raw.kiss)
                "🥱" -> emojiRunner(R.drawable.emoji_yawn, R.raw.yawn)
                else -> {
                    mp.start()
                    activity.findViewById<View>(R.id.newMsgBoltu).show()
                }
            }
            lastMsg?.key?.let {
                lastMsgKey = it
            }
        }
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.chatBoxFriendly.requestFocus()

        msgListAdapter.onClickListener = { msg ->
            //presentationEco str = (presentationEco)o; //As you are using Default String Adapter
            if (!pref.getBoolean("muted", false)) {
                val mediaPlayer =
                    MediaPlayer.create(activity, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            if (msg.playerId != "") {
                toast("Long Press To Copy Text/ID")

                val db = Firebase.firestore
                db.collection("gamerProfile").document(msg.playerId)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
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
                            } catch (npe: Exception) {
                                npe.printStackTrace()
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
        binding.msgSendBtn.setBounceClickListener {
            viewModel.sendMessage2FriendlyChat(matchKey = key, text = binding.chatBoxFriendly.text.toString())?.also{
                binding.chatBoxFriendly.setText("")
            } ?: toast("Write Something..")
        }
        binding.sendHaha.setBounceClickListener { sendEmoji("🤣") }
        binding.sendCry.setBounceClickListener { sendEmoji("😭") }
        binding.sendKiss.setBounceClickListener { sendEmoji("😘") }
        binding.sendScream.setBounceClickListener { sendEmoji("😱") }
        binding.sendYawn.setBounceClickListener { sendEmoji("🥱") }
    }

    private fun sendEmoji(emoji: String) {
        viewModel.sendMessage2FriendlyChat(matchKey = key, text = emoji)
        viewModel.ignoreDrawerClosesSound = true
        drawerLayout.closeDrawer(GravityCompat.START)
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
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        activity.findViewById<ImageView>(R.id.emojiPlay).apply {
            loadDrawable(gif)
            show()
        }
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


    companion object {
        lateinit var key: String
        var lastMsgKey = ""
        fun newInstance(key: String?, playerId: String?): ChatFragmentFriendly {
            val fragment = ChatFragmentFriendly()
            fragment.arguments = bundleOf("key" to key, "playerId" to playerId)
            return fragment
        }
    }
}