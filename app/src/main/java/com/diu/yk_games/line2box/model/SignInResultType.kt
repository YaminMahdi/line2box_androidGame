package com.diu.yk_games.line2box.model

enum class ErrorType{
    PlayServiceNeeded,
    AuthenticationFailure,
    ServerNotResponding,
    NoInternet,
    NoError
}

val ErrorType.msg: String
    get() = this.name.replace(Regex("([a-z])([A-Z])"), "$1 $2")