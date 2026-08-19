package com.diu.yk_games.line2box.util

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.CustomKeysAndValues
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.database.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

inline fun <reified T> DatabaseReference.asValueFlow(): Flow<T> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            snapshot.getValue<T>()?.let {
                trySend(it)
            } ?: cancel()
        }

        override fun onCancelled(error: DatabaseError) {
            close(error.toException())
        }
    }

    addValueEventListener(listener)

    awaitClose {
        removeEventListener(listener)
    }
}

val crashlytics by lazy { Firebase.crashlytics }

context(cls: Any)
fun Throwable.logError(funName: String = "common", tag: String = cls.getTag()) {
    if (BuildConfig.DEBUG)
        Log.e("log> '$tag'", "$tag - $message", this)
    crashlytics.recordException(
        this,
        CustomKeysAndValues
            .Builder()
            .putString("tag", tag)
            .putString("funName", funName)
            .build()
    )
}
