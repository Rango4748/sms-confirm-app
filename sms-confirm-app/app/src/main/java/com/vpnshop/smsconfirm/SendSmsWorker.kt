package com.vpnshop.smsconfirm

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

/**
 * ارسال یک پیامک به وبهوک، با retry خودکار (WorkManager) اگر اینترنت موقتاً
 * قطع باشد. تا وقتی ارسال با موفقیت انجام نشه (یا تعداد تلاش‌ها تمام بشه)
 * سیستم دوباره امتحان می‌کند، حتی اگر اپ در این فاصله کشته شده باشد.
 */
class SendSmsWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val store = SecureStore(applicationContext)
        val webhookUrl = store.webhookUrl
        val deviceId = store.deviceId
        val secret = store.deviceSecret
        val jsonBody = inputData.getString(KEY_JSON_BODY)

        if (webhookUrl.isNullOrBlank() || deviceId.isNullOrBlank() || secret.isNullOrBlank() || jsonBody.isNullOrBlank()) {
            // تنظیمات ناقصه - تلاش دوباره فایده‌ای نداره
            return Result.failure()
        }

        val res = WebhookSender.sendSms(webhookUrl, deviceId, secret, jsonBody)

        return when {
            res.success -> Result.success(workDataOf(KEY_RESPONSE to res.body))
            // خطاهای امنیتی/اعتبارسنجی (۴۰۰/۴۰۱/۴۰۹) با retry هم درست نمی‌شن
            res.httpCode in 400..499 -> Result.failure(workDataOf(KEY_RESPONSE to res.body, KEY_HTTP_CODE to res.httpCode))
            else -> Result.retry()
        }
    }

    companion object {
        const val KEY_JSON_BODY = "json_body"
        const val KEY_RESPONSE = "response"
        const val KEY_HTTP_CODE = "http_code"
    }
}
