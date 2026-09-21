package com.diu.yk_games.line2box.model

enum class ErrorType(val description: String) {
    PlayServiceNeeded("Google Play Games Services is required."),
    AuthenticationFailure("Failed to authenticate user."),
    ServerResponseFailure("Server did not respond."),
    NoInternet("No active internet connection found."),
    ProfileCreationFailure("Failed to create profile.")
}

val ErrorType.msg: String
    get() = this.name.replace(Regex("([a-z])([A-Z])"), "$1 $2")