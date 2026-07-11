package com.vpnshop.smsconfirm

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * ذخیره‌سازی امن تنظیمات اپ (آدرس وبهوک، شناسه‌ی دستگاه، کلید مخفی).
 *
 * از EncryptedSharedPreferences استفاده می‌شود که کلید رمزنگاری‌اش در
 * Android Keystore (تراشه‌ی امنیتی گوشی) نگه داشته می‌شود، نه در یک فایل
 * ساده - یعنی حتی با روت گوشی هم نمی‌شود مستقیم secret را از فایل خواند.
 */
class SecureStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "sms_confirm_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var webhookUrl: String?
        get() = prefs.getString(KEY_WEBHOOK_URL, null)
        set(value) = prefs.edit().putString(KEY_WEBHOOK_URL, value).apply()

    var deviceId: String?
        get() = prefs.getString(KEY_DEVICE_ID, null)
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    var deviceSecret: String?
        get() = prefs.getString(KEY_DEVICE_SECRET, null)
        set(value) = prefs.edit().putString(KEY_DEVICE_SECRET, value).apply()

    /** فقط پیامک‌هایی که شماره/نامِ فرستنده‌شان حاوی این رشته باشد پردازش می‌شوند. */
    var senderFilter: String
        get() = prefs.getString(KEY_SENDER_FILTER, DEFAULT_SENDER_FILTER) ?: DEFAULT_SENDER_FILTER
        set(value) = prefs.edit().putString(KEY_SENDER_FILTER, value).apply()

    fun isConfigured(): Boolean =
        !webhookUrl.isNullOrBlank() && !deviceId.isNullOrBlank() && !deviceSecret.isNullOrBlank()

    companion object {
        private const val KEY_WEBHOOK_URL = "webhook_url"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_SECRET = "device_secret"
        private const val KEY_SENDER_FILTER = "sender_filter"
        const val DEFAULT_SENDER_FILTER = "Pasargad"
    }
}
