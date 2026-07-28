@file:Suppress("unused")

package com.diu.yk_games.line2box.util

import android.media.MediaPlayer
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MuteState(val backgroundRes: Int, val imageRes: Int) {
    MUTED(R.drawable.btn_gry_bg, R.drawable.icon_vol_mute),
    UNMUTED(R.drawable.btn_ylw_bg, R.drawable.icon_vol_unmute)
}

fun ImageButton.applyState(isMuted: Boolean) {
    val muteState = if (isMuted) MuteState.MUTED else MuteState.UNMUTED
    setBackgroundResource(muteState.backgroundRes)
    setImageResource(muteState.imageRes)
}


context(f: Fragment)
fun ImageButton.performOnClickF() {
    f.activity?.apply {
        this@performOnClickF.performOnClick()
    }
}

context(f: FragmentActivity)
fun ImageButton.performOnClick() {
    setBounceClickListener {
        f.lifecycleScope.launch {
            val isMuted = isMuted()
            applyState(!isMuted)
            if (isMuted) {
                runCatching {
                    val mediaPlayer = MediaPlayer.create(f, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
            }
            pref.save("muted", !isMuted)
        }
    }
}

inline fun LifecycleOwner.isNotMuted(
    crossinline ifMuted: () -> Unit = {},
    crossinline ifNotMuted: () -> Unit
) {
    lifecycleScope.launch {
        runCatching {
            if (isMuted()) ifMuted() else ifNotMuted()
        }
    }
}

suspend fun isMuted(): Boolean = withContext(Dispatchers.IO) {
    pref.read("muted", false)
}