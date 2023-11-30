package com.diu.yk_games.line2box.presentation.online

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.FragmentChatGlobalBinding
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.model.GameProfile.Companion.setPreferences
import com.diu.yk_games.line2box.model.MsgStore
import com.diu.yk_games.line2box.presentation.MsgListAdapter
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class ChatFragmentGlobal : Fragment() {
    private lateinit var binding : FragmentChatGlobalBinding
    lateinit var msList: MutableList<MsgStore>
    private var database = Firebase.database
    private var myRef = database.getReference("globalChat")
    private lateinit var playerId: String
    private lateinit var activity: Activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            playerId = requireArguments().getString("playerId")!!
        }
        activity = requireActivity()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatGlobalBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        binding.chatBoxGlobal.isFocusableInTouchMode
        binding.chatBoxGlobal.requestFocus()
        val sharedPref = activity.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE
        )
        setPreferences(sharedPref)
        msList = mutableListOf()
        myRef.limitToLast(100).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                if (dataSnapshot.exists()) {
                    val ms = dataSnapshot.getValue(MsgStore::class.java)!!
                    msList.add(ms)
                    if(this@ChatFragmentGlobal.isAdded) {
                        val adapter = MsgListAdapter(activity, msList) //
                        binding.showMsgList.adapter = adapter
                    }
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {}
        })

        binding.showMsgList.onItemClickListener =
            OnItemClickListener { parent, view, position, id ->
                val (playerId1) = binding.showMsgList.getItemAtPosition(position) as MsgStore
                //presentationEco str = (presentationEco)o; //As you are using Default String Adapter
                if (!sharedPref.getBoolean("muted", false)) {
                    val mediaPlayer =
                        MediaPlayer.create(activity, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener (MediaPlayer::release)
                }
                if (playerId1 != "") {
                    Toast.makeText(activity, "Long Press To Copy Text/ID", Toast.LENGTH_SHORT).show()
                    val db = FirebaseFirestore.getInstance()
                    db.collection("gamerProfile").document(playerId1)
                        .get().addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {
                                val server2device = documentSnapshot.toObject(GameProfile::class.java)
                                val builder = AlertDialog.Builder(activity)
                                val v = LayoutInflater.from(activity).inflate(
                                    R.layout.dialog_layout_profile,
                                    parent.findViewById(R.id.profileLayoutDialog)
                                )
                                builder.setView(v)
                                val params = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                params.setMargins(60, 150, 60, 0)
                                v.findViewById<View>(R.id.linearLayoutFrame).layoutParams = params
                                assert(server2device != null)
                                (v.findViewById<View>(R.id.countryTxt) as TextView).text = "${server2device!!.countryNm} ${server2device.countryEmoji}"
                                (v.findViewById<View>(R.id.lvlTxt) as TextView).text = server2device.lvl.toString()
                                (v.findViewById<View>(R.id.coinHave) as TextView).text = server2device.coin.toString()
                                (v.findViewById<View>(R.id.matchPlayedTxt) as TextView).text = server2device.matchPlayed.toString()
                                (v.findViewById<View>(R.id.matchWonTxt) as TextView).text = server2device.matchWinMulti.toString()
                                val nmEditText = v.findViewById<EditText>(R.id.nmTxt)
                                nmEditText.isEnabled = false
                                nmEditText.setText(server2device.nm)
                                //nmEditText.setVisibility(View.GONE);
                                (v.findViewById<View>(R.id.profileTitle) as TextView).textSize = 28f
                                v.findViewById<View>(R.id.profileShapeLayout).visibility = View.GONE
                                v.findViewById<View>(R.id.nmEditBtn).visibility = View.GONE
                                v.findViewById<View>(R.id.nmLTxt).visibility = View.GONE
                                v.findViewById<View>(R.id.themeBox).visibility = View.GONE
                                v.findViewById<View>(R.id.countryLTxt).visibility = View.GONE
                                v.findViewById<View>(R.id.buttonSaveInfo).visibility = View.GONE
                                val alertDialog = builder.create()
                                alertDialog.window?.setBackgroundDrawable(ColorDrawable(0))
                                try {
                                    alertDialog.show()
                                } catch (npe: NullPointerException) {
                                    npe.printStackTrace()
                                }
                            }
                        }
                } else
                    Toast.makeText(activity, "Older messages don't have profile info.", Toast.LENGTH_SHORT).show()
            }
        binding.showMsgList.onItemLongClickListener =
            OnItemLongClickListener { parent, view, position, id ->
                Toast.makeText(activity, "Text/ID copied", Toast.LENGTH_SHORT).show()
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("msgData", msList[position].msgData)
                clipboard.setPrimaryClip(clip)
                true
            }

        binding.msgSendBtn.setBounceClickListener {
            val mp = MediaPlayer.create(activity, R.raw.pop)
            mp.start()
            mp.setOnCompletionListener (MediaPlayer::release)
            val gp = GameProfile()
            val ms = MsgStore()
            ms.playerId = playerId
            ms.nmData = gp.nm
            ms.lvlData = gp.lvlByCal.toString()
            ms.time = System.currentTimeMillis()
            ms.msgData = binding.chatBoxGlobal.text.toString()
            if (ms.msgData.isNotEmpty()) {
                val key = myRef.push().key!!
                myRef.child(key).setValue(ms)
                binding.chatBoxGlobal.setText("")
            } else {
                Toast.makeText(activity, "Write Something", Toast.LENGTH_SHORT).show()
            }
        }
        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance(playerId: String?): ChatFragmentGlobal {
            val fragment = ChatFragmentGlobal()
            val args = Bundle()
            args.putString("playerId", playerId)
            fragment.arguments = args
            return fragment
        }
    }
}