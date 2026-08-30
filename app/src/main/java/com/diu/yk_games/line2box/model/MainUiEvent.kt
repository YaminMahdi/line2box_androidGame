package com.diu.yk_games.line2box.model

sealed interface MainUiEvent {
    data object ShowHadith : MainUiEvent
    data class ShowToast(val message: String) : MainUiEvent
    data class UpdateUi(val errorType: ErrorType) : MainUiEvent
}