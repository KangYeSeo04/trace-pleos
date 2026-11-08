plugins {
    alias(libs.plugins.android.application)
}

// Pleos Connect PiP 지원 여부 확인용 앱 (공모전 앱과 무관). 표준 API만 쓰고 의존성 없음
android {
    namespace = "com.probe.pip"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.probe.pip"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
