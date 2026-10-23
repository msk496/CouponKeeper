plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "kr.co.couponkeeper"
    compileSdk = 35
    defaultConfig {
        applicationId = "kr.co.couponkeeper"
        minSdk = 26
        targetSdk = 28
        versionCode = 1
        versionName = "1.0"
    }
}
