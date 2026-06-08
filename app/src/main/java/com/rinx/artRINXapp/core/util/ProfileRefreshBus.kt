package com.rinx.artRINXapp.core.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide signal that the current user's artworks/curations changed (edit or delete), so the
 * Profile screen should reload its grids + counts when it next observes. Emitted from the upload
 * repositories on successful update/delete; collected by [UserProfileViewModel].
 */
@Singleton
class ProfileRefreshBus @Inject constructor() {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun signal() {
        _events.tryEmit(Unit)
    }
}
