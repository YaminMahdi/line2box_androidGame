@file:Suppress("unused", "CONTEXT_RECEIVERS_DEPRECATED")

package com.diu.yk_games.line2box.util

import android.media.MediaPlayer
import android.widget.ImageButton
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.pref
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch

enum class MuteState(val backgroundRes: Int, val imageRes: Int) {
    MUTED(R.drawable.btn_gry_bg, R.drawable.icon_vol_mute),
    UNMUTED(R.drawable.btn_ylw_bg, R.drawable.icon_vol_unmute)
}

fun ImageButton.applyState(isMuted: Boolean) {
    val muteState = if (isMuted) MuteState.MUTED else MuteState.UNMUTED
    setBackgroundResource(muteState.backgroundRes)
    setImageResource(muteState.imageRes)
}


context(f: FragmentActivity)
fun ImageButton.performOnClick() {
    setBounceClickListener {
        f.lifecycleScope.launch {
            val isMuted = isMuted()
            applyState(!isMuted)
            if(isMuted) {
                runCatching {
                    val mediaPlayer = MediaPlayer.create(f, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
            }
            IO { pref.edit { putBoolean("muted", !isMuted) } }
        }
    }
}

inline fun FragmentActivity.isNotMuted(crossinline ifMuted: () -> Unit = {}, crossinline ifNotMuted: () -> Unit) {
    lifecycleScope.launch {
        runCatching {
            if(isMuted()) ifMuted() else ifNotMuted()
        }
    }
}

suspend fun isMuted(): Boolean {
    return currentCoroutineContext().IO { pref.getBoolean("muted", false) }
}