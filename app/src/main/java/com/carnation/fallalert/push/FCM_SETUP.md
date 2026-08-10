# FCM 연동 뼈대 (명세 6장 7번 / 7장 미확정 항목)

지금은 **Firebase 의존성을 넣지 않았다.** `google-services.json` 없이 Firebase 플러그인을 붙이면
빌드가 깨지고, 서버 전송 방식(FCM / REST / WebSocket)도 팀 미확정이기 때문이다.

대신 연동 시 **바뀌지 않을 부분**만 미리 만들어 뒀다.

| 구성요소 | 파일 | 상태 |
|---|---|---|
| 페이로드 → FallEvent 파싱 | `PushPayloadParser.kt` | 완성 (Firebase 무관, 테스트 있음) |
| 알림 채널·표시 | `FallAlertNotifier.kt` | 완성 |
| 새 이벤트 주입 경로 | `FallEventRepository.upsert()` | 완성 |
| 가짜 푸시 생성 | `MockPushGenerator.kt` | 완성 (목록 화면 "테스트 알림" 버튼) |
| FCM 수신 서비스 | 아래 코드 | **미적용** |

## 팀 확정 후 할 일

1. Firebase 콘솔에서 앱 등록 → `app/google-services.json` 배치.
2. `gradle/libs.versions.toml` 에 추가:
   ```toml
   [versions]
   googleServices = "4.4.2"
   firebaseBom = "33.7.0"

   [libraries]
   firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
   firebase-messaging = { group = "com.google.firebase", name = "firebase-messaging" }

   [plugins]
   google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
   ```
3. `app/build.gradle.kts` 에 플러그인 `alias(libs.plugins.google.services)` 와
   `implementation(platform(libs.firebase.bom))`, `implementation(libs.firebase.messaging)` 추가.
4. 아래 서비스를 `FallAlertMessagingService.kt` 로 만들고, `AndroidManifest.xml` 의
   주석 처리된 `<service>` 블록을 살린다.

```kotlin
package com.carnation.fallalert.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FallAlertMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // TODO(팀 확정 후): 토큰을 서버에 등록. 엔드포인트 미확정.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val event = PushPayloadParser.parse(message.data) ?: return
        FallAlertNotifier.notify(this, event)
        // TODO: 앱이 죽어 있을 때를 위해 로컬 저장(Room/DataStore) 후
        //       FallEventRepository.refresh() 가 읽어가도록 한다.
    }
}
```

## 서버에 요청할 페이로드 형식

`data` 메시지로 보내되(알림 문구는 앱이 만든다), 키 하나에 계약 JSON 전체를 넣어 달라고 요청한다:

```json
{
  "to": "<device-token>",
  "data": { "payload": "{\"schema_version\":\"1.0\",\"event_type\":\"fall_suspected\", ...}" }
}
```

`notification` 필드를 함께 보내면 앱이 백그라운드일 때 시스템이 임의 문구로 알림을 띄워
"낙상 의심" 표기 원칙(명세 8장)이 깨진다. **`data`-only 로 요청할 것.**

## adb 로 수동 테스트

Firebase 붙이기 전이라도 목록 화면의 **"테스트 알림 받기"** 버튼으로 수신 경로 전체를 재현할 수 있다.
