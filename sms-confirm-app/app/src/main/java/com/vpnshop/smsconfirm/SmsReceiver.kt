package com.vpnshop.smsconfirm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * فقط پیامک‌هایی که فرستنده‌شان شامل فیلتر تنظیم‌شده (پیش‌فرض: "Pasargad") باشد
 * را می‌گیرد؛ همه‌ی پیامک‌های دیگر بلافاصله و بدون هیچ پردازش/ذخیره‌ای نادیده
 * گرفته می‌شوند - این اپ به هیچ پیامک دیگری دست نمی‌زند.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val store = SecureStore(context)
        if (!store.isConfigured()) {
            Log.w(TAG, "اپ هنوز تنظیم نشده (webhook/device id/secret خالیه) — پیامک نادیده گرفته شد")
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // پیامک‌های چندبخشی بانک معمولاً یک پیام واحدند؛ همه‌ی بخش‌ها را به هم می‌چسبانیم
        val sender = messages[0].originatingAddress ?: messages[0].displayOriginatingAddress ?: ""
        val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }

        val filter = store.senderFilter
        if (filter.isNotBlank() && !sender.contains(filter, ignoreCase = true)) {
            // نه از طرف بانک پاسارگاد - رد می‌شود، جایی ذخیره نمی‌شود
            return
        }

        val json = JSONObject().apply {
            put("sms_body", fullBody)
            put("sender", sender)
            put("sms_timestamp", messages[0].timestampMillis.toString())
        }.toString()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val work = OneTimeWorkRequestBuilder<SendSmsWorker>()
            .setInputData(workDataOf(SendSmsWorker.KEY_JSON_BODY to json))
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        // اگه به هر دلیل همین پیامک دوبار broadcast بشه (بعضی گوشی‌ها این کارو می‌کنن)،
        // با همین تگ یکتا از ارسال تکراری جلوگیری می‌کنیم
        val uniqueTag = "sms-send-${messages[0].timestampMillis}"
        WorkManager.getInstance(context).enqueueUniqueWork(uniqueTag, ExistingWorkPolicy.KEEP, work)

        Log.i(TAG, "پیامک بانکی شناسایی و برای ارسال به وبهوک صف شد")
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
