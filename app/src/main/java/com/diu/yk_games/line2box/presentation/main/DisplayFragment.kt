package com.diu.yk_games.line2box.presentation.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.model.DataStore
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.presentation.MyListAdapter
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Objects

class DisplayFragment : Fragment() {
    private val TAG = "Dis_frag"
    private lateinit var dsList: ArrayList<DataStore>
    private var bestScore = "\n\n\nNetwork Error"
    private lateinit var p1Pro: GameProfile
    private lateinit var p2Pro: GameProfile
    private lateinit var sharedPref: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize dataset, this data would usually come from a local content provider or
        // remote server.
        sharedPref = requireContext().getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE
        )
    }

    @SuppressLint("SetTextI18n", "CutPasteId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_display, container, false)
        val lbs = v.findViewById<TextView>(R.id.lastBestScore)
        dsList = ArrayList()

        val db = FirebaseFirestore.getInstance()
        //Source source = Source.CACHE;
        db.collection("LastBestPlayer").document("LastBestPlayer")
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    Log.d(TAG, "Cached document data: " + document.data)
                    bestScore =
                        Objects.requireNonNull(Objects.requireNonNull(document.data)["info"])
                            .toString()
                    lbs.text = "\uD83D\uDC51 $bestScore"
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
                    val ds = it.toObject(DataStore::class.java)!!
                    dsList.add(0, ds)
                }
                Log.d(TAG, "isSuccessful: ${dsList.size}")
                try {
                    val adapter = MyListAdapter(requireContext(), dsList) //
                    val list = v.findViewById<ListView>(R.id.showScoreList)
                    list.adapter = adapter
                    list.onItemClickListener =
                        OnItemClickListener { parent: AdapterView<*>, view: View?, position: Int, id: Long ->
                            val gamerPro = list.getItemAtPosition(position) as DataStore
                            if ((gamerPro.plr1Id == "offline"))
                                Toast.makeText(context, "Offline match doesn't have Profile Info.", Toast.LENGTH_SHORT).show()
                            else if ((gamerPro.plr1Id == ""))
                                Toast.makeText(context, "Old match doesn't have Profile Info.", Toast.LENGTH_SHORT).show()
                            else {
                                if (!sharedPref.getBoolean("muted", false)) {
                                    val mediaPlayer =
                                        MediaPlayer.create(context, R.raw.btn_click_ef)
                                    mediaPlayer.start()
                                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                                }
                                val builder = AlertDialog.Builder(context)
                                val v1 = LayoutInflater.from(context).inflate(
                                    R.layout.dialog_layout_scr_globe,
                                    parent.findViewById(R.id.scoreDetailsLayoutDialog)
                                )
                                builder.setView(v1)
                                Log.d(
                                    TAG,
                                    position.toString() + "onItemClick: 1id " + gamerPro.plr1Id
                                )
                                Log.d(TAG, "onItemClick: 2id " + gamerPro.plr2Id)
                                db.collection("gamerProfile").document(gamerPro.plr1Id)
                                    .get()
                                    .addOnSuccessListener { documentSnapshot ->
                                        if (documentSnapshot.exists()) {
                                            val scr = gamerPro.redData.split(" ".toRegex())
                                                .dropLastWhile { it.isEmpty() }
                                                .toTypedArray()
                                            Log.d(TAG, "onSuccess: scr " + scr[scr.size - 1])
                                            v1.findViewById<TextView>(R.id.plr1Score).text = scr[scr.size - 1]
                                            v1.findViewById<TextView>(R.id.plr1Cup).text = gamerPro.plr1Cup
                                            Log.d(TAG, "onSuccess: cup " + gamerPro.plr1Cup)
                                            p1Pro = documentSnapshot.toObject(GameProfile::class.java)!!
                                            if (p1Pro.countryEmoji != "")
                                                v1.findViewById<TextView>(R.id.plr1Flag).text = p1Pro.countryEmoji
                                            v1.findViewById<TextView>(R.id.plr1Nm).text = p1Pro.nm
                                            Log.d(TAG, "onSuccess: nm " + p1Pro.nm)
                                            v1.findViewById<TextView>(R.id.plr1Lvl).text = "" + p1Pro.lvl
                                        }
                                    }
                                db.collection("gamerProfile").document(gamerPro.plr2Id)
                                    .get().addOnSuccessListener { documentSnapshot ->
                                        if (documentSnapshot.exists()) {
                                            val scr = gamerPro.blueData.split(" ".toRegex())
                                                .dropLastWhile { it.isEmpty() }
                                                .toTypedArray()
                                            v1.findViewById<TextView>(R.id.plr2Score).text = scr[scr.size - 1]
                                            v1.findViewById<TextView>(R.id.plr2Cup).text = gamerPro.plr2Cup
                                            p2Pro = documentSnapshot.toObject(GameProfile::class.java)!!
                                            if (p2Pro.countryEmoji != "")
                                                v1.findViewById<TextView>(R.id.plr2Flag).text = p2Pro.countryEmoji
                                            v1.findViewById<TextView>(R.id.plr2Nm).text = p2Pro.nm
                                            v1.findViewById<TextView>(R.id.plr2Lvl).text = "" + p2Pro.lvl
                                        }
                                        val alertDialog = builder.create()
                                        alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
                                        try { alertDialog.show() }
                                        catch (e: Exception) {
                                            e.printStackTrace() }
                                    }
                                v1.findViewById<View>(R.id.linLayoutPlr1)
                                    .setBounceClickListener {
                                        if (!sharedPref.getBoolean("muted", false)) {
                                            val mediaPlayer = MediaPlayer.create(context, R.raw.btn_click_ef)
                                            mediaPlayer.start()
                                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                                        }
                                        val builder2 = AlertDialog.Builder(context)
                                        val v2 = LayoutInflater.from(context).inflate(
                                            R.layout.dialog_layout_profile,
                                            parent.findViewById(R.id.profileLayoutDialog)
                                        )
                                        builder2.setView(v2)
                                        val params = LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                        )
                                        params.setMargins(60, 0, 60, 0)
                                        val linearLayoutFrame = v2.findViewById<View>(R.id.linearLayoutFrame)
                                        linearLayoutFrame.layoutParams = params
                                        linearLayoutFrame.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.cocX))
                                        linearLayoutFrame.backgroundTintMode = PorterDuff.Mode.ADD
                                        if (p1Pro.countryNm != "")
                                            (v2.findViewById<View>(R.id.countryTxt) as TextView).text = p1Pro.countryNm + " " + p1Pro.countryEmoji
                                        else
                                            v2.findViewById<View>(R.id.countryLayout).visibility = View.GONE
                                        (v2.findViewById<View>(R.id.lvlTxt) as TextView).text = "" + p1Pro.lvl
                                        (v2.findViewById<View>(R.id.coinHave) as TextView).text = "" + p1Pro.coin
                                        (v2.findViewById<View>(R.id.matchPlayedTxt) as TextView).text = "" + p1Pro.matchPlayed
                                        (v2.findViewById<View>(R.id.matchWonTxt) as TextView).text = "" + p1Pro.matchWinMulti
                                        val nmEditText =
                                            v2.findViewById<EditText>(R.id.nmTxt)
                                        nmEditText.isEnabled = false
                                        nmEditText.setText(p1Pro.nm)
                                        //nmEditText.setVisibility(View.GONE);
                                        v2.findViewById<TextView>(R.id.profileTitle).textSize = 28f
                                        v2.findViewById<View>(R.id.profileShapeLayout).visibility = View.GONE
                                        v2.findViewById<View>(R.id.nmEditBtn).visibility = View.GONE
                                        v2.findViewById<View>(R.id.nmLTxt).visibility = View.GONE
                                        v2.findViewById<View>(R.id.themeBox).visibility = View.GONE
                                        v2.findViewById<View>(R.id.countryLTxt).visibility = View.GONE
                                        v2.findViewById<View>(R.id.buttonSaveInfo).visibility = View.GONE
                                        val alertDialog = builder2.create()
                                        alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
                                        try {
                                            alertDialog.show()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                v1.findViewById<View>(R.id.linLayoutPlr2)
                                    .setBounceClickListener {
                                        if (!sharedPref.getBoolean("muted", false)) {
                                            val mediaPlayer =
                                                MediaPlayer.create(context, R.raw.btn_click_ef)
                                            mediaPlayer.start()
                                            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                                        }
                                        val builder2 = AlertDialog.Builder(context)
                                        val v2 = LayoutInflater.from(context).inflate(
                                            R.layout.dialog_layout_profile,
                                            parent.findViewById(R.id.profileLayoutDialog)
                                        )
                                        builder2.setView(v2)
                                        val params = LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                        )
                                        params.setMargins(420, 0, 60, 0)
                                        val linearLayoutFrame =
                                            v2.findViewById<View>(R.id.linearLayoutFrame)

                                        linearLayoutFrame.layoutParams =
                                            params
                                        linearLayoutFrame.backgroundTintList =
                                            ColorStateList.valueOf(
                                                ContextCompat.getColor(
                                                    requireContext(), R.color.cocX
                                                )
                                            )
                                        linearLayoutFrame.backgroundTintMode =
                                            PorterDuff.Mode.ADD
                                        if (p2Pro.countryNm != "")
                                            v2.findViewById<TextView>(R.id.countryTxt).text =
                                                p2Pro.countryNm + " " + p2Pro.countryEmoji
                                        else
                                            v2.findViewById<View>(R.id.countryLayout).visibility = View.GONE
                                        v2.findViewById<TextView>(R.id.lvlTxt).text = "" + p2Pro.lvl
                                        v2.findViewById<TextView>(R.id.coinHave).text = "" + p2Pro.coin
                                        v2.findViewById<TextView>(R.id.matchPlayedTxt).text = "" + p2Pro.matchPlayed
                                        v2.findViewById<TextView>(R.id.matchWonTxt).text = "" + p2Pro.matchWinMulti
                                        val nmEditText = v2.findViewById<EditText>(R.id.nmTxt)
                                        nmEditText.isEnabled = false
                                        nmEditText.setText(p2Pro.nm)
                                        //nmEditText.setVisibility(View.GONE);
                                        v2.findViewById<TextView>(R.id.profileTitle).textSize = 28f
                                        v2.findViewById<View>(R.id.profileShapeLayout).visibility = View.GONE
                                        v2.findViewById<View>(R.id.nmEditBtn).visibility = View.GONE
                                        v2.findViewById<View>(R.id.nmLTxt).visibility = View.GONE
                                        v2.findViewById<View>(R.id.themeBox).visibility = View.GONE
                                        v2.findViewById<View>(R.id.countryLTxt).visibility = View.GONE
                                        v2.findViewById<View>(R.id.buttonSaveInfo).visibility = View.GONE
                                        val alertDialog = builder2.create()
                                        alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
                                        try { alertDialog.show() }
                                        catch (e: Exception) { e.printStackTrace() }
                                    }
                            }
                        }
                } catch (npe: NullPointerException) {
                    npe.printStackTrace()
                }
            }
        return v
    }
}