plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // api: 이 모듈을 쓰는 쪽(앱·서버)이 직렬화 타입을 그대로 볼 수 있어야 한다.
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlin.test.junit)
}

tasks.test {
    useJUnit()
}
