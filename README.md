# 낙상 감지 보호자 앱 (Android / Jetpack Compose)

`앱개발_명세서_ClaudeCode용.md` 구현. **목업 데이터 기준 3화면**까지 완성.

## 실행

Android Studio (Ladybug 이상) 로 이 폴더를 열고 `app` 실행. Android SDK 35, JDK 17 필요.

```bash
./gradlew :app:testDebugUnitTest
```

> Gradle wrapper JAR 은 포함하지 않았다. Android Studio 가 첫 sync 때 생성하거나,
> Gradle 8.9 가 설치돼 있으면 `gradle wrapper` 로 만들면 된다.

## 구조

| 계층 | 파일 |
|---|---|
| 계약 모델 | [model/FallEvent.kt](app/src/main/java/com/carnation/fallalert/model/FallEvent.kt) |
| 파서·검증 | [data/FallEventJson.kt](app/src/main/java/com/carnation/fallalert/data/FallEventJson.kt) |
| 목업 소스 | [data/FallEventSource.kt](app/src/main/java/com/carnation/fallalert/data/FallEventSource.kt), [assets/mock_events.json](app/src/main/assets/mock_events.json) |
| 상태 보관 | [data/FallEventRepository.kt](app/src/main/java/com/carnation/fallalert/data/FallEventRepository.kt) |
| ViewModel | [ui/FallAlertViewModel.kt](app/src/main/java/com/carnation/fallalert/ui/FallAlertViewModel.kt) |
| 네비게이션 | [ui/FallAlertNavHost.kt](app/src/main/java/com/carnation/fallalert/ui/FallAlertNavHost.kt) |
| 화면 A/B/C | [ui/screen/](app/src/main/java/com/carnation/fallalert/ui/screen) |
| 표시 포맷 | [util/Formatting.kt](app/src/main/java/com/carnation/fallalert/util/Formatting.kt) |
| 푸시 뼈대 | [push/](app/src/main/java/com/carnation/fallalert/push) + [FCM_SETUP.md](app/src/main/java/com/carnation/fallalert/push/FCM_SETUP.md) |

화면은 모두 `@Preview` 가 있어 에뮬레이터 없이 Android Studio 미리보기로 확인할 수 있다.

## 명세 체크리스트 (6장)

| # | 항목 | 상태 |
|---|---|---|
| 1 | Compose 프로젝트 생성 | 완료 |
| 2 | FallEvent / Evidence / PresenceState | 완료 |
| 3 | mock_events.json + 파서 | 완료 (4건, 계약 위반 필터 포함) |
| 4 | A→B→C 네비게이션 | 완료 |
| 5 | 시각 포맷 / % 변환 | 완료 (단위 테스트 있음) |
| 6 | 확인 상태 처리 (로컬) | 완료, 서버 전송은 TODO |
| 7 | FCM 뼈대 + 가짜 알림 테스트 | 파싱·알림·주입 경로 완료, Firebase 의존성만 미적용 |
| 8 | 실제 서버 연동 | 팀 확정 대기 |

## 연동할 때 바꿀 곳 (명세 7장)

세 군데만 손대면 화면·모델은 그대로다.

1. `FallEventSource` 구현을 REST/WebSocket 버전으로 교체 — `AssetsFallEventSource` 삭제.
2. `FallEventRepository.confirm()` 안의 TODO 에 확인 결과 전송 추가.
3. `push/FCM_SETUP.md` 절차대로 Firebase 붙이고 `FallAlertMessagingService` 활성화.

## 설계 메모

- `no_recovery_sec` 는 선택 필드라 nullable 이고, 값이 없으면 상세 화면에서 **행 자체를 숨긴다.**
- `room_id` 는 계약상 임의 문자열이라, 아는 값만 한글로 바꾸고 모르는 값은 원문 그대로 노출한다.
- 표기는 항상 **"낙상 의심"** — 알림 문구도 앱이 만든다(그래서 FCM 은 `data`-only 를 요청해야 한다).
- `detected_at` 파싱 실패나 계약 위반 이벤트로 화면이 죽지 않도록 방어했다.
- 미확인 = 빨강 강조 + 테두리, 확인 완료 = 회색 톤 이력. 확인 버튼은 최소 높이 68dp.
