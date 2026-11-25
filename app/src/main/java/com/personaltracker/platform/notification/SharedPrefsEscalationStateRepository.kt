package com.personaltracker.platform.notification

import android.content.Context
import android.content.SharedPreferences
import com.personaltracker.domain.model.EscalationPolicy
import com.personaltracker.domain.repository.EscalationStateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Synchronized SharedPreferences-backed implementation of [EscalationStateRepository] (AD-1, FR-5.4).
 * Uses synchronous `commit()` writes to avoid race conditions across BroadcastReceiver wakeups
 * and avoids unnecessary Room schema migrations for transient acknowledgement flags.
 */
@Singleton
class SharedPrefsEscalationStateRepository private constructor(
    private val prefs: SharedPreferences
) : EscalationStateRepository {

    @Inject
    constructor(@ApplicationContext context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    private val lock = Any()
    private val policyFlow = MutableStateFlow(readPolicyLocked())

    override fun observePolicy(): Flow<EscalationPolicy> = policyFlow.asStateFlow()

    override fun getPolicy(): EscalationPolicy = synchronized(lock) {
        readPolicyLocked()
    }

    override fun updatePolicy(policy: EscalationPolicy) {
        synchronized(lock) {
            prefs.edit()
                .putInt(KEY_RETRY_INTERVAL_MINUTES, policy.retryIntervalMinutes)
                .putInt(KEY_MAX_RE_FIRES, policy.maxReFires)
                .commit()
            policyFlow.value = policy
        }
    }

    override fun isAcknowledged(taskId: String): Boolean {
        if (taskId.isBlank()) return false
        return synchronized(lock) {
            prefs.getBoolean("$KEY_ACK_PREFIX$taskId", false)
        }
    }

    override fun markAcknowledged(taskId: String) {
        if (taskId.isBlank()) return
        synchronized(lock) {
            prefs.edit()
                .putBoolean("$KEY_ACK_PREFIX$taskId", true)
                .commit()
        }
    }

    override fun clearAcknowledgement(taskId: String) {
        if (taskId.isBlank()) return
        synchronized(lock) {
            prefs.edit()
                .remove("$KEY_ACK_PREFIX$taskId")
                .commit()
        }
    }

    private fun readPolicyLocked(): EscalationPolicy {
        val interval = prefs.getInt(KEY_RETRY_INTERVAL_MINUTES, DEFAULT_RETRY_INTERVAL_MINUTES)
        val maxReFires = prefs.getInt(KEY_MAX_RE_FIRES, DEFAULT_MAX_RE_FIRES)
        return runCatching {
            EscalationPolicy(retryIntervalMinutes = interval, maxReFires = maxReFires)
        }.getOrDefault(EscalationPolicy())
    }

    companion object {
        const val PREFS_NAME = "escalation_state_prefs"
        private const val KEY_RETRY_INTERVAL_MINUTES = "retry_interval_minutes"
        private const val KEY_MAX_RE_FIRES = "max_re_fires"
        private const val KEY_ACK_PREFIX = "ack_"
        private const val DEFAULT_RETRY_INTERVAL_MINUTES = 5
        private const val DEFAULT_MAX_RE_FIRES = 3

        internal fun createForTest(prefs: SharedPreferences): SharedPrefsEscalationStateRepository =
            SharedPrefsEscalationStateRepository(prefs)
    }
}
