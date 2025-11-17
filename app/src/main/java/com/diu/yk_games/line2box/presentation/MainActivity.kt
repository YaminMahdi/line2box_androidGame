package com.diu.yk_games.line2box.presentation

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.createGraph
import androidx.navigation.findNavController
import androidx.navigation.fragment.fragment
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.ActivityMainDrawerBinding
import com.diu.yk_games.line2box.presentation.main.StartFragment
import com.diu.yk_games.line2box.presentation.navigation.Fragments
import com.diu.yk_games.line2box.util.closeKeyboard
import com.diu.yk_games.line2box.util.isNotMuted

@Suppress("DEPRECATION")
@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {
    private lateinit var bindingMain: ActivityMainDrawerBinding
    private val binding by lazy { bindingMain.main }
    private val viewModel: MainViewModel by viewModels()

    lateinit var playerId: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingMain = ActivityMainDrawerBinding.inflate(layoutInflater)
        setContentView(bindingMain.root)
    }

    fun setupNavigation(){
        val navController = findNavController(bindingMain.main.mainNavHost.id)
        navController.graph = navController.createGraph(
            startDestination = Fragments.Home
        ) {
            fragment<StartFragment, Fragments.Home> {
                label = resources.getString(R.string.name)
            }
        }
    }


    private fun closeNavBtn() {
        closeKeyboard()
        bindingMain.root.close()
        viewModel.setNewMsgBoltVisible(false)
    }

    private fun openNavBtn() {
        bindingMain.root.open()
        if(viewModel.friendsChatList.value.isEmpty())
            bindingMain.bubbleTabBar.setSelected(0, true)
        viewModel.setNewMsgBoltVisible(false)
    }

    private fun backBtn() {
        isNotMuted {
            val mediaPlayer = MediaPlayer.create(this, R.raw.btn_click_ef)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener(MediaPlayer::release)
        }
        onBackPressedDispatcher.onBackPressed()
    }


    companion object {
        private const val TAG = "MainActivity"
    }
}