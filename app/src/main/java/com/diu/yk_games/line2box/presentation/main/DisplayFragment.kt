package com.diu.yk_games.line2box.presentation.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.DialogLayoutProfileBinding
import com.diu.yk_games.line2box.databinding.DialogLayoutScrGlobeBinding
import com.diu.yk_games.line2box.databinding.FragmentDisplayBinding
import com.diu.yk_games.line2box.model.DataStore
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.pref
import com.diu.yk_games.line2box.presentation.ScoreListAdapter
import com.diu.yk_games.line2box.util.gone
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.diu.yk_games.line2box.util.toast
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject

class DisplayFragment : Fragment() {
    lateinit var binding: FragmentDisplayBinding
    private var dsList= mutableListOf<DataStore>()
    private var bestScore = "\n\n\nNetwork Error"
    private lateinit var p1Pro: GameProfile
    private lateinit var p2Pro: GameProfile
    private lateinit var context: Context

    private val scoreListAdapter by lazy { ScoreListAdapter() }

    companion object{
        private const val TAG = "DisplayFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize dataset, this data would usually come from a local content provider or
        // remote server.
        context = requireContext()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDisplayBinding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.showScoreList.adapter = scoreListAdapter
        val db = Firebase.firestore
        //Source source = Source.CACHE;
        db.collection("LastBestPlayer").document("LastBestPlayer")
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    Log.d(TAG, "Cached document data: " + document.data)
                    bestScore = document.data?.get("info")?.toString().orEmpty()
                    binding.lastBestScore.text = "\uD83D\uDC51 $bestScore"
                } else {
                    Log.d(TAG, "Cached get failed: ", task.exception)
                }
            }
        db.collection("ScoreBoard")
            .orderBy("time")
            .limitToLast(100)
            .get()
            .addOnSuccessListener { task ->
                task.documents.forEach {
                    it.toObject<DataStore>()?.let {ds->
                        dsList.add(0, ds)
                    }
                }
                Log.d(TAG, "isSuccessful: ${dsList.size}")
                scoreListAdapter.submitList(dsList)
            }
        scoreListAdapter.onClickListener = { gamerPro ->
            if ((gamerPro.plr1Id == "offline"))
                toast("Offline match doesn't have Profile Info.")
            else if ((gamerPro.plr1Id == ""))
                toast("Old match doesn't have Profile Info.")
            else {
                if (!pref.getBoolean("muted", false)) {
                    val mediaPlayer =
                        MediaPlayer.create(context, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
                val builder = AlertDialog.Builder(context)
                val dialogBinding = DialogLayoutScrGlobeBinding.inflate(layoutInflater, null, false)
                builder.setView(dialogBinding.root)
                Log.d(TAG, "onItemClick: 1id " + gamerPro.plr1Id)
                Log.d(TAG, "onItemClick: 2id " + gamerPro.plr2Id)
                db.collection("gamerProfile").document(gamerPro.plr1Id)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        val gp = documentSnapshot.toObject<GameProfile>()
                        if (gp != null) {
                            val scr = gamerPro.redData.split(" ".toRegex())
                                .dropLastWhile { it.isEmpty() }
                                .toTypedArray()
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
                    }
                db.collection("gamerProfile").document(gamerPro.plr2Id)
                    .get().addOnSuccessListener { documentSnapshot ->
                        val gp = documentSnapshot.toObject<GameProfile>()
                        if (gp != null) {
                            val scr = gamerPro.blueData.split(" ".toRegex())
                                .dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                            dialogBinding.plr2Score.text = scr[scr.size - 1]
                            dialogBinding.plr2Cup.text = gamerPro.plr2Cup
                            p2Pro = gp
                            if (p2Pro.countryEmoji != "")
                                dialogBinding.plr2Flag.text = p2Pro.countryEmoji
                            dialogBinding.plr2Nm.text = p2Pro.nm
                            dialogBinding.plr2Lvl.text = "" + p2Pro.lvl
                        }
                        val alertDialog = builder.create()
                        alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
                        try { alertDialog.show() }
                        catch (e: Exception) { e.printStackTrace() }
                    }
                dialogBinding.linLayoutPlr1
                    .setBounceClickListener {
                        onPlayerProfileClick(p1Pro)
                    }
                dialogBinding.linLayoutPlr2
                    .setBounceClickListener {
                        onPlayerProfileClick(p2Pro, 420)
                    }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun onPlayerProfileClick(profile: GameProfile, marginLeft: Int = 60) {
        if (!pref.getBoolean("muted", false)) {
            val mediaPlayer = MediaPlayer.create(context, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        val builder2 = AlertDialog.Builder(context)
        val dBinding = DialogLayoutProfileBinding.inflate(layoutInflater, null, false)

        builder2.setView(dBinding.root)
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
            themeBox.gone()
            countryLTxt.gone()
            buttonSaveInfo.gone()
        }
        val alertDialog = builder2.create()
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
        dBinding.root.setOnClickListener {
            alertDialog.dismiss()
        }
        try {
            alertDialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}