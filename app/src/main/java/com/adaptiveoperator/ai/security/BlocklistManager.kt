package com.adaptiveoperator.ai.security

import com.adaptiveoperator.ai.memory.db.dao.PreferenceDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Section 37: apps the operator will never interact with unless the user explicitly
 * allow-lists them. Seeded with common sensitive categories; package names are
 * user/device specific so this ships empty and is populated from the Security
 * Center UI (Section 44), not hardcoded vendor lists.
 */
@Singleton
class BlocklistManager @Inject constructor(
    private val preferenceDao: PreferenceDao
) {
    private val _blockedPackages = MutableStateFlow<Set<String>>(emptySet())
    val blockedPackages: StateFlow<Set<String>> = _blockedPackages.asStateFlow()

    suspend fun load() {
        val stored = preferenceDao.get(PREF_KEY)?.value ?: return
        _blockedPackages.value = stored.split(",").filter { it.isNotBlank() }.toSet()
    }

    fun isBlocked(packageName: String): Boolean = packageName in _blockedPackages.value

    suspend fun block(packageName: String) = persist(_blockedPackages.value + packageName)
    suspend fun unblock(packageName: String) = persist(_blockedPackages.value - packageName)

    private suspend fun persist(next: Set<String>) {
        _blockedPackages.value = next
        preferenceDao.set(
            com.adaptiveoperator.ai.memory.db.entity.PreferenceEntity(
                key = PREF_KEY,
                value = next.joinToString(","),
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    companion object {
        private const val PREF_KEY = "security.blocked_packages"
    }
}
