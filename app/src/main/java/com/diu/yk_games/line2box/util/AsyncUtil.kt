package com.diu.yk_games.line2box.util

import kotlinx.coroutines.*

context(scope: CoroutineScope)
fun <T> Iterable<T>.forEachAsync(action: suspend CoroutineScope.(T) -> Unit) {
    this.forEach {
        scope.launch {
            action(it)
        }
    }
}

suspend fun <T, R> Iterable<T>.mapAsync(action: suspend CoroutineScope.(T) -> R): List<R> {
    return coroutineScope {
        this@mapAsync.map {
            async {
                action(it)
            }
        }.awaitAll()
    }
}