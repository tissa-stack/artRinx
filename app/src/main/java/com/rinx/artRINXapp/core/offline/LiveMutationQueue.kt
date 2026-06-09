package com.rinx.artRINXapp.core.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rinx.artRINXapp.core.di.ApplicationScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.home.domain.repository.HomeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** A queued like/unlike that failed to reach the server while offline. */
data class LikeMutation(
    val type: String, // "artwork" | "curation"
    val id: Int,
    val like: Boolean,
)

/**
 * Offline like/unlike queue (handout §PublicArtDetailView → "enqueue in LiveMutationQueue, drain on
 * network resume"). When a like/unlike fails on a network error the UI stays optimistic and the
 * desired final state is persisted here; the queue replays it the moment connectivity returns.
 *
 * Net-of-action semantics: only the latest action per target is kept (like→unlike→like = one like).
 */
@Singleton
class LiveMutationQueue @Inject constructor(
    private val homeRepository: HomeRepository,
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val gson = Gson()
    private val mutex = Mutex()
    private val key = stringPreferencesKey("live_mutation_queue")

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    init {
        // Replay on connectivity restore, and once on startup for anything left from a prior run.
        runCatching {
            connectivityManager?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    scope.launch { drain() }
                }
            })
        }
        scope.launch { drain() }
    }

    /** Persist a failed like/unlike (latest action per target wins) and keep the optimistic UI. */
    suspend fun enqueue(type: String, id: Int, like: Boolean) = mutex.withLock {
        val items = load().filterNot { it.type == type && it.id == id }.toMutableList()
        items.add(LikeMutation(type, id, like))
        save(items)
    }

    /** Replay queued mutations; drop on success or a hard (non-network) rejection, keep on network failure. */
    suspend fun drain() = mutex.withLock {
        val items = load()
        if (items.isEmpty()) return@withLock
        val remaining = mutableListOf<LikeMutation>()
        for (m in items) {
            val result = when (m.type) {
                "artwork" -> if (m.like) homeRepository.likeArtwork(m.id) else homeRepository.unlikeArtwork(m.id)
                "curation" -> if (m.like) homeRepository.likeCuration(m.id) else homeRepository.unlikeCuration(m.id)
                else -> ApiResult.Success(Unit)
            }
            if (result is ApiResult.Error.Network) remaining.add(m) // still offline → retry later
        }
        save(remaining)
    }

    private suspend fun load(): List<LikeMutation> {
        val json = dataStore.data.first()[key] ?: return emptyList()
        return runCatching {
            gson.fromJson<List<LikeMutation>>(json, object : TypeToken<List<LikeMutation>>() {}.type)
        }.getOrNull() ?: emptyList()
    }

    private suspend fun save(items: List<LikeMutation>) {
        dataStore.edit { it[key] = gson.toJson(items) }
    }
}