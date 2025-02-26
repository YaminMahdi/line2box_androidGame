package com.diu.yk_games.line2box.presentation.online

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import com.diu.yk_games.line2box.BuildConfig
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.DialogFragmentShareBinding
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.diu.yk_games.line2box.util.setClipBoardData
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.ak1.BubbleTabBar

class ShareDialogFragment : BottomSheetDialogFragment() {

    private val viewModel: MainViewModel by activityViewModels()

    lateinit var binding: DialogFragmentShareBinding

    override fun onCreateDialog(
        savedInstanceState: Bundle?,
    ): Dialog {
        return BottomSheetDialog(requireContext(), R.style.TransparentBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogFragmentShareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.gameId?.let {
            binding.gameId.text = it
        }
        val gameLink = "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"

        val message = "Hey there! \uD83C\uDFAE\n\n" +
                "I’ve found an awesome game called *Line2Box*, and I’d love for you to join me!\n\n" +
                "\uD83D\uDC49 Game Link: $gameLink\n\n" +
                "Joining ID: *${viewModel.gameId}*\n\n" +
                "Let's have some fun! \uD83D\uDE80\uD83D\uDD25"

        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, message)
            setPackage("com.whatsapp")
            type = "text/plain"
        }
        binding.apply {
            btnCopy.setBounceClickListener {
                context.setClipBoardData(viewModel.gameId, "ID copied")
            }
            btnSend2Chat.setBounceClickListener {
                viewModel.sendInvitation2Chat(
                    gameId = viewModel.gameId,
//                    text = getString(R.string.join_my_match)
                    text = "${getString(R.string.join_my_match)}\n\nMatch ID: ${viewModel.gameId}"
                )
                dismiss()
                activity?.findViewById<DrawerLayout>(R.id.drawer_layout)?.openDrawer(GravityCompat.START)
                activity?.findViewById<BubbleTabBar>(R.id.bubbleTabBar)?.setSelected(1, true)
                activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.chatFragment, ChatFragmentGlobal.newInstance(viewModel.playerId))
                    ?.commit()
            }
            btnSend2WhatsApp.setBounceClickListener {
                try {
                    // Attempt to send via WhatsApp
                    startActivity(intent)
                } catch (_: Exception) {
                    // WhatsApp not installed, fallback to generic share intent
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, message)
                        type = "text/plain"
                    }
                    startActivity(Intent.createChooser(sendIntent, "Invite your friend"))
                }
            }
        }

    }
}