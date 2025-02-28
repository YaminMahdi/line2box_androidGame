package com.diu.yk_games.line2box.presentation.online

import android.annotation.SuppressLint
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
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.DialogLayoutProfileBinding
import com.diu.yk_games.line2box.databinding.FragmentLeaderBoardBinding
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.pref
import com.diu.yk_games.line2box.presentation.RankListAdapter
import com.diu.yk_games.line2box.util.gone
import com.google.firebase.Firebase
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LeaderBoardFragment : Fragment() {
    private lateinit var binding: FragmentLeaderBoardBinding
    private var rankList= mutableListOf<GameProfile>()
    private lateinit var playerId: String
    private val rankListAdapter by lazy { RankListAdapter(playerId) }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLeaderBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playerId = arguments?.getString("playerId").orEmpty()
        binding.showRankList.adapter = rankListAdapter
        val db = Firebase.firestore.collection("gamerProfile")
        db.whereNotEqualTo("coin", 100)
            .orderBy("coin", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result) {
                        Log.d(TAG, document.getId() )
                        val xx = document.toObject<GameProfile>()
                        rankList.add(xx)
                        rankListAdapter.submitList(rankList)
                    }
                    //rankList.sort(Comparator.comparing(a -> a.coin))
                    //Collections.reverse(rankList)
                    val pos = findIndex(rankList, playerId)
                    try {
                        // rankList.indexOf(user)
                        Log.d(TAG, "onComplete(pos): "+pos+" ser- "+rankList.get(pos).playerId+" "+playerId)
                        if (pos > 5) binding.showRankList.scrollToPosition(pos - 1)
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
                        binding.playerCount.text = "%,d".format(i)
                    }
                    binding.playerCount.text = "%,d".format(it.count)
                }
            }
        rankListAdapter.onClickListener = { gamerPro ->
            if (!pref.getBoolean("muted", false)) {
                val mediaPlayer =
                    MediaPlayer.create(context, R.raw.btn_click_ef)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener(MediaPlayer::release)
            }
            Firebase.firestore.collection("gamerProfile").document(gamerPro.playerId)
                .get().addOnSuccessListener { documentSnapshot ->
                    val server2device = documentSnapshot.toObject<GameProfile>()
                    if (server2device != null) {
                        val dialogBinding = DialogLayoutProfileBinding.inflate(layoutInflater)
                        val alertDialog = AlertDialog.Builder(context)
                            .setView(dialogBinding.root)
                            .create()
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
                            alertDialog.dismiss()
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