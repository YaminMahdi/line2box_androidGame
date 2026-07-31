package com.diu.yk_games.line2box.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes
import com.diu.yk_games.line2box.R

class SoundEffectPlayer(
    context: Context,
    private val isMuted: () -> Boolean
) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds: Map<Int, Int> = mapOf(
        R.raw.box_ef to soundPool.load(context, R.raw.box_ef, 1),
        R.raw.line_click_ef to soundPool.load(context, R.raw.line_click_ef, 1),
        R.raw.btn_click_ef to soundPool.load(context, R.raw.btn_click_ef, 1),
        R.raw.win_ef to soundPool.load(context, R.raw.win_ef, 1),
        R.raw.pop to soundPool.load(context, R.raw.pop, 1),
        R.raw.slide to soundPool.load(context, R.raw.slide, 1),
        R.raw.haha to soundPool.load(context, R.raw.haha, 1),
        R.raw.cry to soundPool.load(context, R.raw.cry, 1),
        R.raw.scream to soundPool.load(context, R.raw.scream, 1),
        R.raw.kiss to soundPool.load(context, R.raw.kiss, 1),
        R.raw.yawn to soundPool.load(context, R.raw.yawn, 1)
    )

    fun playSound(@RawRes rawRes: Int) {
        if (isMuted()) return
        soundIds[rawRes]?.let { soundPool.play(it, 1f, 1f, 1, 0, 1f) }
    }

    fun playBoxSound() = playSound(R.raw.box_ef)
    fun playLineClickSound() = playSound(R.raw.line_click_ef)
    fun playButtonClickSound() = playSound(R.raw.btn_click_ef)
    fun playWinSound() = playSound(R.raw.win_ef)
    fun playPopSound() = playSound(R.raw.pop)
    fun playSlideSound() = playSound(R.raw.slide)

    fun release() = soundPool.release()
}