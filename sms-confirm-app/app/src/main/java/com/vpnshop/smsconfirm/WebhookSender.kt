package com.vpnshop.smsconfirm

import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * ساخت امضای HMAC-SHA256 و ارسال پیامک به وبهوک پنل.
 *
 * فرمت امضا دقیقاً باید با سمت سرور (app/services/sms_payment.py -> build_signing_string)
 * یکی باشد: "{device_id}.{timestamp}.{nonce}." + بدنه‌ی خام JSON.
 */
object WebhookSender {

    data class Result(val success: Boolean, val httpCode: Int, val body: String)

    fun sendSms(webhookUrl: String, deviceId: String, secret: String, jsonBody: String): Result {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val nonce = randomNonce()

        val bodyBytes = jsonBody.toByteArray(StandardCharsets.UTF_8)
        val signingBase = "$deviceId.$timestamp.$nonce.".toByteArray(StandardCharsets.UTF_8) + bodyBytes
        val signature = hmacSha256Hex(secret, signingBase)

        var connection: HttpURLConnection? = null
        return try {
            val url = URL(webhookUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("X-Device-Id", deviceId)
                setRequestProperty("X-Timestamp", timestamp)
                setRequestProperty("X-Nonce", nonce)
                setRequestProperty("X-Signature", signature)
            }
            connection.outputStream.use { it.write(bodyBytes) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(StandardCharsets.UTF_8)?.readText() ?: ""
            Result(code in 200..299, code, responseBody)
        } catch (e: Exception) {
            Result(false, -1, e.message ?: "خطای نامشخص در اتصال")
        } finally {
            connection?.disconnect()
        }
    }

    private fun hmacSha256Hex(secret: String, message: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        val raw = mac.doFinal(message)
        return raw.joinToString("") { "%02x".format(it) }
    }

    private fun randomNonce(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
