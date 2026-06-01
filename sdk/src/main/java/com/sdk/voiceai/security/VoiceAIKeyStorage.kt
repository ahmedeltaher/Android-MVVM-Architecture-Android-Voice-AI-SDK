package com.sdk.voiceai.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber

class VoiceAIKeyStorage(context: Context) {

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "voice_ai_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun store(key: String, value: String) {
        Timber.d("Storing key: %s", key)
        encryptedPrefs.edit().putString(key, value).apply()
    }

    fun retrieve(key: String): String? {
        Timber.d("Retrieving key: %s", key)
        return encryptedPrefs.getString(key, null)
    }

    fun delete(key: String) {
        Timber.d("Deleting key: %s", key)
        encryptedPrefs.edit().remove(key).apply()
    }

    fun deleteAll() {
        Timber.d("Deleting all keys")
        encryptedPrefs.edit().clear().apply()
    }
}
