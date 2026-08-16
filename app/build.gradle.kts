import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/**
 * 개발 서버 주소는 기기마다 다르다 (에뮬레이터는 10.0.2.2, 실기기는 PC 의 사설 IP).
 * 그래서 커밋되지 않는 local.properties 에서 읽는다.
 *
 *   server.host=192.168.0.5
 *   server.port=8080
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
// -Pserver.host=localhost 처럼 한 번만 다르게 빌드하고 싶을 때는 Gradle 속성이 이긴다.
// local.properties 를 고쳤다 되돌리지 않아도 된다.
val devServerHost: String = providers.gradleProperty("server.host").orNull
    ?: localProperties.getProperty("server.host")
    ?: "10.0.2.2"
val devServerPort: String = providers.gradleProperty("server.port").orNull
    ?: localProperties.getProperty("server.port")
    ?: "8080"

android {
    namespace = "com.carnation.fallalert"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.carnation.fallalert"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "SERVER_BASE_URL",
                "\"http://$devServerHost:$devServerPort\"",
            )
        }
        release {
            // TODO(팀 확정 후): 배포 서버 주소. 반드시 https 로 — release 는 평문을 막아 뒀다.
            buildConfigField("String", "SERVER_BASE_URL", "\"https://TODO-배포-서버\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
        compose = true
        buildConfig = true
    }

    testOptions {
        // 파서가 android.util.Log 를 쓰므로, JVM 테스트에서 스텁을 허용한다.
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // 계약 모델·API 타입은 서버와 공유한다. kotlinx-serialization 도 여기서 딸려 온다.
    implementation(project(":contract"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.serialization.kotlinx.json)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // 앱의 실제 HTTP/WebSocket 클라이언트를 진짜 서버에 붙여 검증한다.
    // 계약이 어긋나면 여기서 잡힌다 — 기기 없이 돌릴 수 있는 마지막 방어선.
    testImplementation(project(":server"))
    // :server 가 ktor 를 implementation 으로 쓰므로 테스트에서 직접 선언해야 한다.
    testImplementation(libs.ktor.server.core)
    testImplementation(libs.ktor.server.netty)
    testRuntimeOnly(libs.logback.classic)
    androidTestImplementation(libs.androidx.junit)
}
