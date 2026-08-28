package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {
    private val COMPANY_ID = stringPreferencesKey("company_id")
    private val USER_ID = stringPreferencesKey("user_id")
    private val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    
    // Google Sign-In & Google Drive fields
    private val GOOGLE_EMAIL = stringPreferencesKey("google_email")
    private val GOOGLE_DISPLAY_NAME = stringPreferencesKey("google_display_name")
    private val GOOGLE_DRIVE_TOKEN = stringPreferencesKey("google_drive_token")
    private val GOOGLE_DRIVE_FOLDER_ID = stringPreferencesKey("google_drive_folder_id")
    private val IS_DRIVE_AUTO_SYNC = booleanPreferencesKey("is_drive_auto_sync")
    private val HAS_ASKED_DRIVE_PERMISSION = booleanPreferencesKey("has_asked_drive_permission")
    private val LAST_DRIVE_SYNC_TIME = longPreferencesKey("last_drive_sync_time")

    val loggedInCompanyId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[COMPANY_ID]
    }
    
    val loggedInUserId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID]
    }

    val isDarkMode: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[IS_DARK_MODE]
    }

    val googleEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[GOOGLE_EMAIL]
    }

    val googleDisplayName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[GOOGLE_DISPLAY_NAME]
    }

    val googleDriveToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[GOOGLE_DRIVE_TOKEN]
    }

    val googleDriveFolderId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[GOOGLE_DRIVE_FOLDER_ID]
    }

    val isDriveAutoSync: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_DRIVE_AUTO_SYNC] ?: true
    }

    val hasAskedDrivePermission: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HAS_ASKED_DRIVE_PERMISSION] ?: false
    }

    val lastDriveSyncTime: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LAST_DRIVE_SYNC_TIME] ?: 0L
    }

    suspend fun setLoggedInSession(companyId: String, userId: String) {
        context.dataStore.edit { preferences ->
            preferences[COMPANY_ID] = companyId
            preferences[USER_ID] = userId
        }
    }

    suspend fun setGoogleUser(email: String, displayName: String?, token: String? = null) {
        context.dataStore.edit { preferences ->
            preferences[GOOGLE_EMAIL] = email
            if (displayName != null) preferences[GOOGLE_DISPLAY_NAME] = displayName
            if (token != null) preferences[GOOGLE_DRIVE_TOKEN] = token
        }
    }

    suspend fun setGoogleDriveToken(token: String?) {
        context.dataStore.edit { preferences ->
            if (token != null) {
                preferences[GOOGLE_DRIVE_TOKEN] = token
            } else {
                preferences.remove(GOOGLE_DRIVE_TOKEN)
            }
        }
    }

    suspend fun setGoogleDriveFolderId(folderId: String) {
        context.dataStore.edit { preferences ->
            preferences[GOOGLE_DRIVE_FOLDER_ID] = folderId
        }
    }

    suspend fun setDriveAutoSync(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DRIVE_AUTO_SYNC] = enabled
        }
    }

    suspend fun setHasAskedDrivePermission(asked: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_ASKED_DRIVE_PERMISSION] = asked
        }
    }

    suspend fun setLastDriveSyncTime(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_DRIVE_SYNC_TIME] = timestamp
        }
    }

    suspend fun setDarkMode(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = isDark
        }
    }
    
    suspend fun clearCompanySession() {
        context.dataStore.edit { preferences ->
            preferences.remove(COMPANY_ID)
            preferences.remove(USER_ID)
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(COMPANY_ID)
            preferences.remove(USER_ID)
            preferences.remove(GOOGLE_EMAIL)
            preferences.remove(GOOGLE_DISPLAY_NAME)
            preferences.remove(GOOGLE_DRIVE_TOKEN)
        }
    }
}
