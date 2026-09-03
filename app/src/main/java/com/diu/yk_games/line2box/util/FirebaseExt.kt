package com.diu.yk_games.line2box.util

import android.util.Log
import com.diu.yk_games.line2box.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.CustomKeysAndValues
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.database.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

inline fun <reified T> Query.asValueFlow(): Flow<T> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            snapshot.getValueOrNull<T>()?.let {
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

inline fun <reified T> DataSnapshot.getValueOrNull(): T? {
    return runCatching {
        getValue<T>()
    }.onFailure { 
        it.logError()
    }.getOrNull()
}

inline fun <reified T> DocumentSnapshot.toObjectOrNull(): T? {
    return runCatching {
        toObject<T>()
    }.onFailure {
        it.logError()
    }.getOrNull()
}

inline fun <reified T> Query.asValueFlowList(): Flow<List<T>> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val list = mutableListOf<T>()

            // 1. Iterate through children to preserve Firebase's ascending order
            for (childSnapshot in snapshot.children) {
                childSnapshot.getValue(T::class.java)?.let { item ->
                    list.add(item)
                }
            }

            // 2. Reverse the list locally to achieve descending order
            list.reverse()

            // 3. Emit the correctly sorted, parsed list to the flow
            trySend(list)
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
