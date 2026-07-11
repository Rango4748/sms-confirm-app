package com.vpnshop.smsconfirm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.vpnshop.smsconfirm.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var store: SecureStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        store = SecureStore(this)

        // مقادیر ذخیره‌شده‌ی قبلی رو نشون بده
        binding.editWebhookUrl.setText(store.webhookUrl ?: "")
        binding.editDeviceId.setText(store.deviceId ?: "")
        binding.editDeviceSecret.setText(store.deviceSecret ?: "")
        binding.editSenderFilter.setText(store.senderFilter)

        updatePermissionStatus()

        binding.btnRequestPermission.setOnClickListener {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), REQ_SMS_PERMISSION)
        }

        binding.btnIgnoreBattery.setOnClickListener {
            requestIgnoreBatteryOptimizations()
        }

        binding.btnSave.setOnClickListener {
            store.webhookUrl = binding.editWebhookUrl.text.toString().trim()
            store.deviceId = binding.editDeviceId.text.toString().trim()
            store.deviceSecret = binding.editDeviceSecret.text.toString().trim()
            store.senderFilter = binding.editSenderFilter.text.toString().trim().ifBlank { SecureStore.DEFAULT_SENDER_FILTER }
            Toast.makeText(this, "ذخیره شد ✅", Toast.LENGTH_SHORT).show()
        }

        binding.btnTest.setOnClickListener { sendTestPing() }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        binding.textPermissionStatus.text = if (granted) "✅ دسترسی پیامک فعال است" else "❌ دسترسی پیامک فعال نیست"
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "از قبل فعال است ✅", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** یک پیامک واریزی نمونه می‌فرستد تا مطمئن بشیم اتصال به سرور و امضا درسته
     * (این پیامک واقعی نیست، فقط برای تست اتصاله - در پنل به‌عنوان no_match ثبت می‌شود). */
    private fun sendTestPing() {
        val webhookUrl = binding.editWebhookUrl.text.toString().trim()
        val deviceId = binding.editDeviceId.text.toString().trim()
        val secret = binding.editDeviceSecret.text.toString().trim()

        if (webhookUrl.isBlank() || deviceId.isBlank() || secret.isBlank()) {
            Toast.makeText(this, "اول فیلدها رو پر و ذخیره کن", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnTest.isEnabled = false
        binding.textTestResult.text = "در حال ارسال..."

        lifecycleScope.launch {
            val testBody = """{"sms_body":"777.**\n+1,234,567\n01/01_00:00\nمانده: 1,000,000","sender":"TEST-PING","sms_timestamp":"test"}"""
            val result = withContext(Dispatchers.IO) {
                WebhookSender.sendSms(webhookUrl, deviceId, secret, testBody)
            }
            binding.btnTest.isEnabled = true
            binding.textTestResult.text = if (result.success) {
                "✅ اتصال موفق (HTTP ${result.httpCode})\n${result.body}"
            } else {
                "❌ ناموفق (HTTP ${result.httpCode})\n${result.body}"
            }
        }
    }

    companion object {
        private const val REQ_SMS_PERMISSION = 100
    }
}
