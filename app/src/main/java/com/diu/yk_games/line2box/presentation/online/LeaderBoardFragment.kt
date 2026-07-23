package com.diu.yk_games.line2box.presentation.online

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.DialogLayoutProfileBinding
import com.diu.yk_games.line2box.databinding.FragmentDisplayBinding
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.diu.yk_games.line2box.presentation.RankListAdapter
import com.diu.yk_games.line2box.util.gone
import com.diu.yk_games.line2box.util.onBackPressed
import com.diu.yk_games.line2box.util.pref
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LeaderBoardFragment : Fragment() {
    private lateinit var binding: FragmentDisplayBinding
    private val viewModel by activityViewModels<MainViewModel>()
    private lateinit var parentActivity: Activity

    private var rankList= mutableListOf<GameProfile>()
    private val rankListAdapter by lazy { RankListAdapter(viewModel.playerId) }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDisplayBinding.inflate(inflater, container, false)
        parentActivity = requireActivity()
        return binding.root
    }

    private fun setupUI(){
        binding.fragLabel.text = getString(R.string.global_rank_list)
        binding.statusLabel.text = getString(R.string.total_player)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        binding.recyclerView.adapter = rankListAdapter
        val db = viewModel.firestore.collection("gamerProfile")
        db.whereNotEqualTo("coin", 100)
            .orderBy("coin", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result) {
                        Log.d(TAG, document.id)
                        val xx = document.toObject<GameProfile>()
                        rankList.add(xx)
                    }
                    rankListAdapter.submitList(rankList)
                    //rankList.sort(Comparator.comparing(a -> a.coin))
                    //Collections.reverse(rankList)
                    val pos = findIndex(rankList, viewModel.playerId)
                    try {
                        // rankList.indexOf(user)
                        Log.d(TAG, "onComplete(pos): $pos ser- ${rankList.getOrNull(pos)?.playerId} ${viewModel.playerId}")
                        if (pos > 5) binding.recyclerView.scrollToPosition(pos - 1)
                        //list.post(() -> list.smoothScrollToPosition(pos))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    Log.d(TAG, "Error getting documents: ", task.exception)
                }
            }
        db.count().get(AggregateSource.SERVER)
            .addOnSuccessListener {
                lifecycleScope.launch {
                    for(i in 0 .. it.count step  512) {
                        delay(45)
                        binding.status.text = "%,d".format(i)
                    }
                    binding.status.text = "%,d".format(it.count)
                }
            }
        var itemClicked = false
        binding.btnBack.setBounceClickListener(::onBackPressed)

        rankListAdapter.onClickListener = run@{ gamerPro ->
            if(itemClicked) return@run
            itemClicked = true
            if (!pref.read("muted", false)) {
                val mediaPlayer =
                    MediaPlayer.create(parentActivity, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            viewModel.firestore.collection("gamerProfile").document(gamerPro.playerId)
                .get().addOnSuccessListener { documentSnapshot ->
                    val server2device = documentSnapshot.toObject<GameProfile>()
                    if (server2device != null) {
                        val dialogBinding = DialogLayoutProfileBinding.inflate(layoutInflater)
                        val alertDialog = AlertDialog.Builder(parentActivity)
                            .setView(dialogBinding.root)
                            .create()
                        alertDialog.setOnDismissListener {
                            itemClicked = false
                        }
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.setMargins(60, 150, 60, 0)
                        dialogBinding.apply {
                            linearLayoutFrame.layoutParams = params
                            //v.findViewById(R.id.linearLayoutFrame).setPadding(20,0,20,0)
                            if (server2device.countryNm != "")
                                countryTxt.text = "${server2device.countryNm} ${server2device.countryEmoji}"
                            else
                                countryLayout.gone()
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
                        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
                        dialogBinding.root.setOnClickListener {
                            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
                        }
                        try {
                            alertDialog.show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
        }
    }

    private fun findIndex(list: List<GameProfile>, id: String?): Int {
        list.forEachIndexed { index, gameProfile ->
            if (gameProfile.playerId == id) return index
        }
        return -1
    }

    companion object {
        private const val TAG = "LeadBoardFrag"
        fun newInstance(playerId: String?): LeaderBoardFragment {
            val fragment = LeaderBoardFragment()
            fragment.arguments =  bundleOf("playerId" to playerId)
            return fragment
        }
    }
}