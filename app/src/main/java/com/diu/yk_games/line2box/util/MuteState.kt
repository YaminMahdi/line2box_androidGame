@file:Suppress("unused", "CONTEXT_RECEIVERS_DEPRECATED")

package com.diu.yk_games.line2box.util

import android.media.MediaPlayer
import android.widget.ImageButton
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.pref
import com.diu.yk_games.line2box.prefEditor
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

enum class MuteState(val backgroundRes: Int, val imageRes: Int) {
    MUTED(R.drawable.btn_gry_bg, R.drawable.icon_vol_mute),
    UNMUTED(R.drawable.btn_ylw_bg, R.drawable.icon_vol_unmute);
}

fun ImageButton.applyState(isMuted: Boolean) {
    val muteState = if (isMuted) MuteState.MUTED else MuteState.UNMUTED
    setBackgroundResource(muteState.backgroundRes)
    setImageResource(muteState.imageRes)
}


context(FragmentActivity)
fun ImageButton.performOnClick() {
    setBounceClickListener {
        lifecycleScope.launch {
            val isMuted = IO { pref.getBoolean("muted", false) }
            applyState(isMuted)
            if(!isMuted) {
                IO { prefEditor.putBoolean("muted", true).apply() }
            } else {
                runCatching {
                    val mediaPlayer = MediaPlayer.create(this@FragmentActivity, R.raw.btn_click_ef)
                    mediaPlayer.start()
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release)
                }
                IO { prefEditor.putBoolean("muted", false).apply() }
            }
        }
    }
}

inline fun FragmentActivity.isNotMuted(crossinline ifTrue: () -> Unit = {}, crossinline ifNotTrue: () -> Unit) {
    lifecycleScope.launch {
        runCatching {
            val isMuted = IO { pref.getBoolean("muted", false) }
            if(!isMuted) ifNotTrue() else ifTrue()
        }
    }
}

suspend fun isMuted(): Boolean {
    return coroutineContext.IO { pref.getBoolean("muted", false) }
}