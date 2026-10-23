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
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
    }
}
dependencies {
    implementation("androidx.work:work-runtime-ktx:2.10.0")
}