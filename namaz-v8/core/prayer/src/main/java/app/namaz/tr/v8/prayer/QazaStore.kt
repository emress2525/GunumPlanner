package app.namaz.tr.v8.prayer

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.qazaDataStore by preferencesDataStore(name = "qaza_tracking")

interface QazaStore {
    val total: Flow<Int>
    suspend fun setTotal(value: Int)
}

class DataStoreQazaStore(private val context: Context) : QazaStore {
    private val totalKey = intPreferencesKey("user_entered_qaza_total")
    override val total: Flow<Int> = context.qazaDataStore.data.map { (it[totalKey] ?: 0).coerceAtLeast(0) }

    override suspend fun setTotal(value: Int) {
        require(value >= 0) { "Qaza total is user-entered and cannot be negative" }
        context.qazaDataStore.edit { it[totalKey] = value }
    }
}
