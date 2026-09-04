package com.diu.yk_games.line2box.base

import android.os.Parcel
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.model.Score
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.parcelableCreator

object ScoreListParceler : PersistentListParceler<Score>(parcelableCreator<Score>())
object GameProfileListParceler : PersistentListParceler<GameProfile>(parcelableCreator<GameProfile>())

open class PersistentListParceler<T : Parcelable>(
    private val creator: Parcelable.Creator<T>
) : Parceler<PersistentList<T>> {

    override fun create(parcel: Parcel): PersistentList<T> {
        val list = ArrayList<T>()
        parcel.readTypedList(list, creator)
        return list.toPersistentList()
    }

    override fun PersistentList<T>.write(parcel: Parcel, flags: Int) {
        parcel.writeTypedList(this)
    }
}

fun <T : Parcelable> SavedStateHandle.getPersistentList(key: String): PersistentList<T>? {
    val list = get<ArrayList<T>>(key) ?: return null
    return list.toPersistentList()
}

fun <T : Parcelable> SavedStateHandle.setPersistentList(key: String, value: PersistentList<T>) {
    set(key, ArrayList(value))
}

// 2. Extension to obtain a StateFlow of PersistentList
fun <T : Parcelable> SavedStateHandle.getPersistentListMutableStateFlow(
    key: String,
    initialValue: PersistentList<T>,
    scope: CoroutineScope
): MutableStateFlow<PersistentList<T>> {
    // 1. Restore saved ArrayList or initialize with initialValue
    val savedList = get<ArrayList<T>>(key)
    val restoredPersistentList = savedList?.toPersistentList() ?: initialValue

    // 2. Create the backing MutableStateFlow
    val stateFlow = MutableStateFlow(restoredPersistentList)

    // 3. Observe StateFlow changes and sync back to SavedStateHandle as ArrayList
    scope.launch {
        stateFlow.collect { newList ->
            set(key, ArrayList(newList))
        }
    }

    return stateFlow
}