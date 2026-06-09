package com.rinx.artRINXapp.core.util

import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory record of users *I* have blocked, updated whenever a block/unblock succeeds (via
 * ProfileRepository) and seeded from the blocked-users list. Lets screens — especially chat —
 * reliably know "I blocked this user" even when the other user's profile/thread endpoints fail
 * (e.g. a 500 when they've ALSO blocked me, a mutual block). Cleared on logout/account-delete.
 */
@Singleton
class BlockedUsersStore @Inject constructor() {
    private val ids = Collections.synchronizedSet(mutableSetOf<Int>())

    fun isBlocked(userId: Int): Boolean = ids.contains(userId)
    fun markBlocked(userId: Int) { ids.add(userId) }
    fun markUnblocked(userId: Int) { ids.remove(userId) }
    fun seed(userIds: Collection<Int>) { ids.addAll(userIds) }
    fun clear() { ids.clear() }
}
