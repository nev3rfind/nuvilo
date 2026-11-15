package com.example.nuvilo.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.nuvilo.security.LockGracePeriod
import com.example.nuvilo.system.notification.NuviloNotificationCategory
import com.example.nuvilo.ui.haptics.HapticIntensity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isNotificationPrivacyMasked: Boolean = false,
    val isBiometricLockEnabled: Boolean = false,
    val lastReconciliationEpochMs: Long = 0L,
    val lowStockNotifiedMedIds: Set<String> = emptySet(),
    val lockGracePeriod: LockGracePeriod = LockGracePeriod.IMMEDIATELY,
    val isRecentsPreviewMasked: Boolean = false,
    val lastExportEpochMs: Long = 0L,
    val hapticsEnabled: Boolean = true,
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,
    val expiringLotNotifiedKeys: Set<String> = emptySet(),
    val discardLotNotifiedKeys: Set<String> = emptySet(),
    val categorySoundEnabled: Map<NuviloNotificationCategory, Boolean> = emptyMap(),
    val categoryVibrationEnabled: Map<NuviloNotificationCategory, Boolean> = emptyMap()
) {
    fun isCategorySoundEnabled(category: NuviloNotificationCategory): Boolean =
        categorySoundEnabled[category] ?: category.defaultSound

    fun isCategoryVibrationEnabled(category: NuviloNotificationCategory): Boolean =
        categoryVibrationEnabled[category] ?: category.defaultVibration
}

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATION_PRIVACY_MASKED = booleanPreferencesKey("notification_privacy_masked")
        val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
        val LAST_RECONCILIATION_EPOCH_MS = longPreferencesKey("last_reconciliation_epoch_ms")
        val LOW_STOCK_NOTIFIED_MED_IDS = stringSetPreferencesKey("low_stock_notified_med_ids")
        val LOCK_GRACE_PERIOD = stringPreferencesKey("lock_grace_period")
        val RECENTS_PREVIEW_MASKED = booleanPreferencesKey("recents_preview_masked")
        val LAST_EXPORT_EPOCH_MS = longPreferencesKey("last_export_epoch_ms")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val HAPTIC_INTENSITY = stringPreferencesKey("haptic_intensity")
        val EXPIRING_LOT_NOTIFIED_KEYS = stringSetPreferencesKey("expiring_lot_notified_keys")
        val DISCARD_LOT_NOTIFIED_KEYS = stringSetPreferencesKey("discard_lot_notified_keys")

        fun categorySoundKey(category: NuviloNotificationCategory) =
            booleanPreferencesKey("notif_sound_${category.name}")

        fun categoryVibrationKey(category: NuviloNotificationCategory) =
            booleanPreferencesKey("notif_vib_${category.name}")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeStr = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            val themeMode = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.SYSTEM)
            val privacyMasked = preferences[PreferencesKeys.NOTIFICATION_PRIVACY_MASKED] ?: false
            val biometricEnabled = preferences[PreferencesKeys.BIOMETRIC_LOCK_ENABLED] ?: false
            val lastRecon = preferences[PreferencesKeys.LAST_RECONCILIATION_EPOCH_MS] ?: 0L
            val lowStockIds = preferences[PreferencesKeys.LOW_STOCK_NOTIFIED_MED_IDS] ?: emptySet()
            val graceStr = preferences[PreferencesKeys.LOCK_GRACE_PERIOD] ?: LockGracePeriod.IMMEDIATELY.name
            val gracePeriod = runCatching { LockGracePeriod.valueOf(graceStr) }.getOrDefault(LockGracePeriod.IMMEDIATELY)
            val recentsMasked = preferences[PreferencesKeys.RECENTS_PREVIEW_MASKED] ?: false
            val lastExport = preferences[PreferencesKeys.LAST_EXPORT_EPOCH_MS] ?: 0L
            val hapticsEnabled = preferences[PreferencesKeys.HAPTICS_ENABLED] ?: true
            val intensityStr = preferences[PreferencesKeys.HAPTIC_INTENSITY] ?: HapticIntensity.MEDIUM.name
            val hapticIntensity = runCatching { HapticIntensity.valueOf(intensityStr) }.getOrDefault(HapticIntensity.MEDIUM)
            val expiryKeys = preferences[PreferencesKeys.EXPIRING_LOT_NOTIFIED_KEYS] ?: emptySet()
            val discardKeys = preferences[PreferencesKeys.DISCARD_LOT_NOTIFIED_KEYS] ?: emptySet()
            val soundMap = NuviloNotificationCategory.entries.associateWith { cat ->
                preferences[PreferencesKeys.categorySoundKey(cat)] ?: cat.defaultSound
            }
            val vibMap = NuviloNotificationCategory.entries.associateWith { cat ->
                preferences[PreferencesKeys.categoryVibrationKey(cat)] ?: cat.defaultVibration
            }

            UserPreferences(
                themeMode = themeMode,
                isNotificationPrivacyMasked = privacyMasked,
                isBiometricLockEnabled = biometricEnabled,
                lastReconciliationEpochMs = lastRecon,
                lowStockNotifiedMedIds = lowStockIds,
                lockGracePeriod = gracePeriod,
                isRecentsPreviewMasked = recentsMasked,
                lastExportEpochMs = lastExport,
                hapticsEnabled = hapticsEnabled,
                hapticIntensity = hapticIntensity,
                expiringLotNotifiedKeys = expiryKeys,
                discardLotNotifiedKeys = discardKeys,
                categorySoundEnabled = soundMap,
                categoryVibrationEnabled = vibMap
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setNotificationPrivacy(masked: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_PRIVACY_MASKED] = masked
        }
    }

    suspend fun setBiometricLock(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setLastReconciliationEpochMs(epochMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_RECONCILIATION_EPOCH_MS] = epochMs
        }
    }

    suspend fun markLowStockNotified(medicationId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.LOW_STOCK_NOTIFIED_MED_IDS] ?: emptySet()
            preferences[PreferencesKeys.LOW_STOCK_NOTIFIED_MED_IDS] = current + medicationId
        }
    }

    suspend fun clearLowStockNotified(medicationId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.LOW_STOCK_NOTIFIED_MED_IDS] ?: emptySet()
            preferences[PreferencesKeys.LOW_STOCK_NOTIFIED_MED_IDS] = current - medicationId
        }
    }

    suspend fun setLockGracePeriod(gracePeriod: LockGracePeriod) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOCK_GRACE_PERIOD] = gracePeriod.name
        }
    }

    suspend fun setRecentsPreviewMasked(masked: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RECENTS_PREVIEW_MASKED] = masked
        }
    }

    suspend fun setLastExportEpochMs(epochMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_EXPORT_EPOCH_MS] = epochMs
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun setHapticIntensity(intensity: HapticIntensity) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_INTENSITY] = intensity.name
        }
    }

    suspend fun markLotExpiryNotified(dedupKey: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.EXPIRING_LOT_NOTIFIED_KEYS] ?: emptySet()
            preferences[PreferencesKeys.EXPIRING_LOT_NOTIFIED_KEYS] = current + dedupKey
        }
    }

    suspend fun clearLotExpiryNotified(dedupKey: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.EXPIRING_LOT_NOTIFIED_KEYS] ?: emptySet()
            preferences[PreferencesKeys.EXPIRING_LOT_NOTIFIED_KEYS] = current - dedupKey
        }
    }

    suspend fun markLotDiscardNotified(dedupKey: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.DISCARD_LOT_NOTIFIED_KEYS] ?: emptySet()
            preferences[PreferencesKeys.DISCARD_LOT_NOTIFIED_KEYS] = current + dedupKey
        }
    }

    suspend fun clearLotDiscardNotified(dedupKey: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.DISCARD_LOT_NOTIFIED_KEYS] ?: emptySet()
            preferences[PreferencesKeys.DISCARD_LOT_NOTIFIED_KEYS] = current - dedupKey
        }
    }

    suspend fun setCategorySoundEnabled(category: NuviloNotificationCategory, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.categorySoundKey(category)] = enabled
        }
    }

    suspend fun setCategoryVibrationEnabled(category: NuviloNotificationCategory, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.categoryVibrationKey(category)] = enabled
        }
    }
}
