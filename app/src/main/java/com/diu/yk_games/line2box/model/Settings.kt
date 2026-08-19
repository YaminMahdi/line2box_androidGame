package com.diu.yk_games.line2box.model

import androidx.annotation.DrawableRes
import com.diu.yk_games.line2box.R

data class Settings(
    val isMuted: Boolean = false,
    val isFirstRun: Boolean = true,
    val showHadith: Boolean = true,
    val language: Language = Language.EN,
    val theme: Theme = Theme.BG1,
) {
    enum class Language {
        EN, BN;

        fun flip(): Language = if (this == EN) BN else EN
    }

    enum class Theme(@DrawableRes val background: Int) {
        BG1(R.drawable.bg1),
        BG2(R.drawable.bg2),
        BG3(R.drawable.bg3),
        BG4(R.drawable.bg4),
        BG5(R.drawable.bg5),
        BG6(R.drawable.bg6),
        BG7(R.drawable.bg7),
        BG8(R.drawable.bg8)
    }
}
