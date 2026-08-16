# FCM 연동 뼈대 (명세 6장 7번 / 7장 미확정 항목)

지금은 **Firebase 의존성을 넣지 않았다.** `google-services.json` 없이 Firebase 플러그인을 붙이면
빌드가 깨지고, 서버 전송 방식(FCM / REST / WebSocket)도 팀 미확정이기 때문이다.

대신 연동 시 **바뀌지 않을 부분**만 미리 만들어 뒀다.

**FCM 이 없어도 앱이 켜져 있는 동안은 WebSocket 으로 실시간 알림을 받는다.**
FCM 이 추가로 해결하는 건 **앱이 완전히 종료된 상태**뿐이다. 우선순위를 그렇게 보면 된다.

| 구성요소 | 파일 | 상태 |
|---|---|---|
| 실시간 수신 (앱 실행 중) | `data/remote/EventStreamClient.kt` | 완성 (WebSocket + 재연결) |
| 페이로드 → FallEvent 파싱 | `PushPayloadParser.kt` | 완성 (Firebase 무관, 테스트 있음) |
| 알림 채널·표시 | `FallAlertNotifier.kt` | 완성 |
| 새 이벤트 주입 경로 | `FallEventRepository` 의 스트림 처리 | 완성 |
| 가짜 이벤트 생성 | 서버 `POST /api/v1/debug/simulate` | 완성 (목록 화면 "테스트 이벤트 보내기" 버튼) |
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
        // 알림만 띄우면 된다. 사용자가 앱을 열면 REST 로 전체 목록을 다시 읽으므로
        // 여기서 따로 저장할 필요가 없다.
        FallAlertNotifier.notify(this, event)
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

## 지금 수신 경로를 테스트하는 법

로컬 서버를 띄우고 목록 화면의 **"테스트 이벤트 보내기"** 버튼을 누르면 된다.
서버가 이벤트를 만들어 WebSocket 으로 되돌려 주므로, 알림 표시까지 실제 경로를 그대로 탄다.

```bash
curl -X POST http://localhost:8080/api/v1/debug/simulate
```

curl 로 직접 쏴도 연결된 앱에 똑같이 도착한다.
