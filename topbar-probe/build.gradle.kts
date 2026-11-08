plugins {
    alias(libs.plugins.android.application)
}

// Pleos Connect 상단 표시 가능성 테스트용 앱 (공모전 앱과 무관)
android {
    namespace = "com.probe.topbar"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.probe.topbar"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
