package com.diu.yk_games.line2box.presentation.online

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.R
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            playerId = requireArguments().getString("playerId")!!
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLeaderBoardBinding.inflate(inflater, container, false)
        val db = Firebase.firestore.collection("gamerProfile")
        db.whereNotEqualTo("coin", 100)
            .orderBy("coin", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result) {
                        //Log.d(TAG, document.getId() );
                        val xx = document.toObject<GameProfile>()
                        rankList.add(xx)
                    }
                    //rankList.sort(Comparator.comparing(a -> a.coin));
                    //Collections.reverse(rankList);
                    val pos = findIndex(rankList, playerId)
                    try {
                        if(isAdded){
                            val adapter = RankListAdapter(requireContext(), rankList, playerId)
                            binding.showRankList.adapter = adapter
                        }
                        // rankList.indexOf(user);
                        //Log.d(TAG, "onComplete(pos): "+pos+" ser- "+rankList.get(pos).playerId+" "+playerId);
                        if (pos > 5) binding.showRankList.setSelection(pos - 1)
                        //list.post(() -> list.smoothScrollToPosition(pos));
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
                    for(i in 0 .. it.count step  270) {
                        delay(45)
                        binding.playerCount.text = "%,d".format(i)
                    }
                    binding.playerCount.text = "%,d".format(it.count)
                }
            }
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pos = findIndex(rankList, playerId)
        binding.showRankList.setOnItemClickListener{ parent, _, position, _ ->
            val gamerPro = rankList[position]
            if (position != pos) {
                if (!pref.getBoolean("muted", false)) {
                    val mediaPlayer =
                        MediaPlayer.create(context, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
                }
                if (gamerPro.playerId != "") {
                    Firebase.firestore.collection("gamerProfile").document(gamerPro.playerId)
                        .get().addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {
                                val server2device = documentSnapshot.toObject<GameProfile>()!!

                                val builder = AlertDialog.Builder(context)
                                val v = LayoutInflater.from(context).inflate(
                                    R.layout.dialog_layout_profile,
                                    parent.findViewById(R.id.profileLayoutDialog)
                                )
                                builder.setView(v)
                                val params = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                params.setMargins(60, 150, 60, 0)
                                v.findViewById<View>(R.id.linearLayoutFrame).layoutParams =
                                    params
                                //v.findViewById(R.id.linearLayoutFrame).setPadding(20,0,20,0);
                                if (server2device.countryNm != "")
                                    (v.findViewById<View>(R.id.countryTxt) as TextView)
                                        .text = "${server2device.countryNm} ${server2device.countryEmoji}"
                                else
                                    v.findViewById<View>(R.id.countryLayout).gone()
                                (v.findViewById<View>(R.id.lvlTxt) as TextView).text = server2device.lvl.toString()
                                (v.findViewById<View>(R.id.coinHave) as TextView).text = server2device.coin.toString()
                                (v.findViewById<View>(R.id.matchPlayedTxt) as TextView).text = server2device.matchPlayed.toString()
                                (v.findViewById<View>(R.id.matchWonTxt) as TextView).text = server2device.matchWinMulti.toString()
                                val nmEditText =
                                    v.findViewById<EditText>(R.id.nmTxt)
                                nmEditText.isEnabled = false
                                nmEditText.setText(server2device.nm)
                                //nmEditText.setVisibility(View.GONE);
                                (v.findViewById<View>(R.id.profileTitle) as TextView).textSize = 28f
                                v.findViewById<View>(R.id.profileShapeLayout).gone()
                                v.findViewById<View>(R.id.nmEditBtn).gone()
                                v.findViewById<View>(R.id.nmLTxt).gone()
                                v.findViewById<View>(R.id.themeBox).gone()
                                v.findViewById<View>(R.id.countryLTxt).gone()
                                v.findViewById<View>(R.id.buttonSaveInfo).gone()
                                val alertDialog = builder.create()
                                alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
                                v.setOnClickListener {
                                    alertDialog.dismiss()
                                }
                                try {
                                    alertDialog.show()
                                } catch (npe: NullPointerException) {
                                    npe.printStackTrace()
                                }
                            }
                        }
                } else
                    Toast.makeText(context, "Older profile don't have profile info.", Toast.LENGTH_SHORT).show()
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