package com.diu.yk_games.line2box.model

enum class JoinType {
    Create,
    Join,
    Watch;
    val isNew: Boolean get() = this == Create
}