package com.diu.yk_games.line2box.presentation.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.DialogLayoutProfileBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutScrGlobeBinding
import com.diu.yk_games.line2box.databinding.FragmentDisplayBinding
import com.diu.yk_games.line2box.model.DataStore
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.presentation.adapter.ScoreListAdapter
import com.diu.yk_games.line2box.presentation.base.BaseFragment
import com.diu.yk_games.line2box.util.*
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject

class ScoreBoardFragment : BaseFragment<FragmentDisplayBinding>(FragmentDisplayBinding::inflate) {
    private var bestScore = "\n\n\nNetwork Error"
    private lateinit var p1Pro: GameProfile
    private lateinit var p2Pro: GameProfile

    private val scoreListAdapter by lazy { ScoreListAdapter() }

    companion object{
        private const val TAG = "ScoreBoardFragment"
    }

    private fun setupUI(){
        binding.fragLabel.text = getString(R.string.global_score_board)
        binding.statusLabel.text = getString(R.string.last_best_score)
        binding.recyclerView.adapter = scoreListAdapter
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        viewModel.firestore.collection("LastBestPlayer").document("LastBestPlayer")
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    Log.d(TAG, "Cached document data: " + document.data)
                    bestScore = document.data?.get("info")?.toString().orEmpty()
                    binding.status.text = "\uD83D\uDC51 $bestScore"
                } else {
                    Log.d(TAG, "Cached get failed: ", task.exception)
                }
            }
        viewModel.firestore.collection("ScoreBoard")
            .orderBy("time", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .addOnSuccessListener { task ->
                val dsList = task.documents.mapNotNull {
                    it.toObject<DataStore>()
                }
                Log.d(TAG, "isSuccessful: ${dsList.size}")
                scoreListAdapter.submitList(dsList)
            }

        var itemClicked = false
        binding.btnBack.setBounceClickListener(::onBackPressed)

        scoreListAdapter.onClickListener = run@{ gamerPro ->
            if(itemClicked && gamerPro.starData == "friendly") return@run
            itemClicked = true
            gamerPro.log("scoreListAdapter")
            if (gamerPro.starData == "friendly") {
                toast("Offline matches don't have match details.")
                return@run
            }
            viewModel.player.playButtonClickSound()
            val dialogBinding = DialogLayoutScrGlobeBinding.inflate(layoutInflater)
            val alertDialog = AlertDialog.Builder(parentActivity)
                .setView(dialogBinding.root).create()
            alertDialog.setOnDismissListener {
                itemClicked = false
            }
            Log.d(TAG, "onItemClick: 1id " + gamerPro.plr1Id)
            Log.d(TAG, "onItemClick: 2id " + gamerPro.plr2Id)
            viewModel.firestore.collection("gamerProfile").document(gamerPro.plr1Id)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val gp = documentSnapshot.toObject<GameProfile>() ?: return@addOnSuccessListener
                    val scr = gamerPro.redData.split(" ").dropLastWhile { it.isEmpty() }
                    Log.d(TAG, "onSuccess: scr " + scr[scr.size - 1])
                    dialogBinding.plr1Score.text = scr[scr.size - 1]
                    dialogBinding.plr1Cup.text = gamerPro.plr1Cup
                    Log.d(TAG, "onSuccess: cup " + gamerPro.plr1Cup)
                    p1Pro = gp
                    if (p1Pro.countryEmoji != "")
                        dialogBinding.plr1Flag.text = p1Pro.countryEmoji
                    dialogBinding.plr1Nm.text = p1Pro.nm
                    Log.d(TAG, "onSuccess: nm " + p1Pro.nm)
                    dialogBinding.plr1Lvl.text = "" + p1Pro.lvl
                }
            viewModel.firestore.collection("gamerProfile").document(gamerPro.plr2Id)
                .get().addOnSuccessListener { documentSnapshot ->
                    val gp = documentSnapshot.toObject<GameProfile>() ?: return@addOnSuccessListener
                    val scr = gamerPro.blueData.split(" ").dropLastWhile { it.isEmpty() }
                    dialogBinding.plr2Score.text = scr[scr.size - 1]
                    dialogBinding.plr2Cup.text = gamerPro.plr2Cup
                    p2Pro = gp
                    if (p2Pro.countryEmoji != "")
                        dialogBinding.plr2Flag.text = p2Pro.countryEmoji
                    dialogBinding.plr2Nm.text = p2Pro.nm
                    dialogBinding.plr2Lvl.text = "" + p2Pro.lvl
                    alertDialog.window?.setBackgroundDrawable(0.toDrawable())
                    runCatching { alertDialog.show() }

                }
            var itemClicked2 =false
            var itemClicked3 =false
            dialogBinding.linLayoutPlr1
                .setBounceClickListener {
                    if(itemClicked2) return@setBounceClickListener
                    itemClicked2 = true
                    onPlayerProfileClick(p1Pro, 60){
                        itemClicked2 = false
                    }
                }
            dialogBinding.linLayoutPlr2
                .setBounceClickListener {
                    if(itemClicked3) return@setBounceClickListener
                    itemClicked3 = true
                    onPlayerProfileClick(p2Pro, 420){
                        itemClicked3 = false
                    }
                }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun onPlayerProfileClick(profile: GameProfile, marginLeft: Int, onDismissed: () -> Unit) {
        viewModel.player.playButtonClickSound()
        val dBinding = DialogLayoutProfileBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(parentActivity)
            .setView(dBinding.root)
            .create()
        alertDialog.setOnDismissListener{
            onDismissed()
        }
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(marginLeft, 0, 60, 0)
        dBinding.linearLayoutFrame.apply {
            layoutParams = params
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cocX))
            backgroundTintMode = PorterDuff.Mode.ADD
        }
        dBinding.apply {
            if (profile.countryNm != "")
                countryTxt.text = profile.countryNm + " " + profile.countryEmoji
            else
                countryLayout.gone()
            lvlTxt.text = "" + profile.lvl
            coinHave.text = "" + profile.coin
            matchPlayedTxt.text = "" + profile.matchPlayed
            matchWonTxt.text = "" + profile.matchWinMulti
            nmTxt.isEnabled = false
            nmTxt.setText(profile.nm)
            profileTitle.textSize = 28f
            profileShapeLayout.gone()
            nmEditBtn.gone()
            nmLTxt.gone()
            countryLTxt.gone()
            buttonSaveInfo.gone()
        }
        alertDialog.window?.setBackgroundDrawable(0.toDrawable())
        dBinding.root.setOnClickListener {
            runCatching { if (alertDialog.isShowing) alertDialog.dismiss() }
        }
        runCatching { alertDialog.show() }
    }
}