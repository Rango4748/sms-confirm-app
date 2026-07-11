plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.vpnshop.smsconfirm"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vpnshop.smsconfirm"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06") // برای ذخیره‌ی امن secret روی گوشی
    implementation("androidx.work:work-runtime-ktx:2.9.1") // صف ارسال + retry وقتی نت قطعه
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4") // lifecycleScope
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
