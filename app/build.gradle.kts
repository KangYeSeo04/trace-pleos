import java.util.Properties

// PROBE용: STT·TTS 키는 local.properties(깃 제외)에서만 읽는다. 없으면 빈 문자열 → 화면에 "키 없음"
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.tracers.trace"
    compileSdk {
        version = release(37)
    }

    // 차량 속도·기어(CarPropertyManager)를 읽기 위한 AAOS 라이브러리
    useLibrary("android.car")

    defaultConfig {
        applicationId = "com.tracers.trace"
        minSdk = 28
        targetSdk = 34   // 37 → 34로 변경: 에뮬레이터·실차가 Android 14
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "PLEOS_CLIENT_ID", "\"${localProps.getProperty("PLEOS_CLIENT_ID", "")}\"")
        buildConfigField("String", "PLEOS_CLIENT_SECRET", "\"${localProps.getProperty("PLEOS_CLIENT_SECRET", "")}\"")
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Pleos SDK
    implementation("ai.pleos.playground:Vehicle:2.3.2")      // 브레이크·조향·방향지시등
    implementation("ai.pleos.playground:NaviHelper:3.0.5")   // 현재 위치
    implementation("ai.pleos.playground:SpeechToText:3.3.1")   // PROBE용: 음성 인식 연결 확인
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")   // PROBE용: 음성 출력 연결 확인
    implementation("ai.pleos.playground:FusedLocation:1.1.3")  // PROBE용: isSupported 확인

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}