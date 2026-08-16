# 낙상 감지 보호자 앱

`앱개발_명세서_ClaudeCode용.md` 구현.

| 모듈 | 내용 | 상태 |
|---|---|---|
| `:contract` | 앱·서버가 공유하는 계약 모델 + API 타입 | 완료 (테스트 8건) |
| `:server` | Ktor 로컬 서버 (REST + WebSocket) | 완료 (테스트 11건, 실행 검증) |
| `:app` | Jetpack Compose 보호자 앱 3화면 + 서버 연결 | 완료 (테스트 21건) |

계약 모델이 `:contract` 한 곳에만 있어서, 앱과 서버 중 한쪽만 계약을 어기면 컴파일이 깨진다.

## 실행

**서버를 먼저 띄워야 한다.** 앱은 로컬 캐시를 두지 않으므로 서버가 없으면 목록이 비어 있다.

```bash
.\gradlew.bat :server:run
```

앱: Android Studio (Ladybug 이상) 로 이 폴더를 열고 `app` 실행. Android SDK 35, JDK 17 필요.

### macOS/Linux: 서버부터 실기기 앱 연결까지 한 번에

USB 디버깅을 켠 폰을 연결하고 프로젝트 루트에서 실행한다. 서버 실행, 앱 빌드·설치,
기존 `adb reverse` 터널 재생성, 알림 권한 설정, 앱 실행까지 모두 처리한다.

```bash
./start-phone.sh
```

앱 설치를 생략하고 연결만 빠르게 복구하려면:

```bash
./start-phone.sh --skip-install
```

연결 직후 앱을 백그라운드로 보내 테스트 알림까지 발송하려면:

```bash
./start-phone.sh --skip-install --test-alert
```

USB 케이블을 다시 꽂았거나 앱에 오프라인 배너가 나타나면 스크립트를 다시 실행한다.
스크립트는 목록에만 남은 불완전한 터널도 복구하도록 기존 터널을 제거한 뒤 새로 만든다.

### 실기기 연결 (기본: USB 터널)

폰: 설정 → 휴대전화 정보 → 빌드번호 7번 탭 → 개발자 옵션 → **USB 디버깅** 켜기 → USB 연결.
처음 꽂으면 폰에 승인 팝업이 뜬다. "이 컴퓨터에서 항상 허용" 체크하고 허용.

그 다음 **폰을 꽂을 때마다 한 번** 실행한다:

```bash
.\connect-phone.bat
```

폰의 `localhost:8080` 이 USB 를 통해 PC 의 8080 으로 연결된다 (`adb reverse` 터널).
**Wi-Fi 가 바뀌어도, 다른 네트워크에 있어도, 방화벽 규칙이 없어도 그대로 붙는다.**
이게 IP 를 매번 고치고 재빌드하지 않아도 되는 이유다.

스크립트는 adb 를 알아서 찾고(PATH → `%LOCALAPPDATA%\Android\Sdk` → `%ANDROID_HOME%`),
기기 연결 상태를 확인한 뒤, 폰에서 서버까지 실제로 닿는지까지 검사한다.
`{"status":"ok"...}` 가 보이면 준비 완료다.

연결이 끊기면 터널도 함께 사라진다. 앱이 오프라인 배너를 띄우면 이 스크립트부터 다시 실행해 볼 것.

### 서버 주소 설정

접속 주소는 **`local.properties`** 에서 읽는다 (커밋되지 않는 파일이라 IP 를 코드에 박지 않는다).
`BuildConfig` 에 박히는 값이라 **바꾼 뒤에는 앱을 다시 빌드**해야 한다.

```properties
server.host=localhost     # adb reverse 터널 (기본, 권장)
# server.host=10.0.2.2    # 에뮬레이터
# server.host=192.168.0.5 # 무선 실기기: PC 의 Wi-Fi IPv4 (ipconfig)
server.port=8080
```

무선(케이블 없이)으로 붙일 때만 사설 IP 를 쓴다. 그 경우 같은 Wi-Fi 여야 하고,
윈도우 방화벽에 8080 인바운드 허용이 필요하다 — **관리자 권한** PowerShell 에서:

```powershell
New-NetFirewallRule -DisplayName "Carnation dev server 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow -Profile Private,Public
```

평문(http) 통신은 **debug 빌드에서만** 허용된다 — release 는 전면 금지라 배포로 새지 않는다.

테스트:
```bash
.\gradlew.bat :contract:test :server:test :app:testDebugUnitTest
```

> PowerShell 에서는 `.\` 를 붙여야 한다. cmd 나 Git Bash 라면 `gradlew.bat` / `./gradlew` 로 쓴다.

서버 API 명세와 접속 주소는 [server/README.md](server/README.md) 참고.
**모델·CSI 팀에 넘길 연동 가이드**는 [docs/MODEL_TEAM_INTEGRATION.md](docs/MODEL_TEAM_INTEGRATION.md).

## 구조

| 계층 | 파일 |
|---|---|
| 계약 모델 (공유) | [contract/model/FallEvent.kt](contract/src/main/kotlin/com/carnation/fallalert/model/FallEvent.kt) |
| 파서·검증 (공유) | [contract/model/ContractJson.kt](contract/src/main/kotlin/com/carnation/fallalert/model/ContractJson.kt) |
| 앱↔서버 API (공유) | [contract/api/Api.kt](contract/src/main/kotlin/com/carnation/fallalert/api/Api.kt) |
| 서버 | [server/](server) — [README](server/README.md) |
| REST 클라이언트 | [data/remote/CarnationApi.kt](app/src/main/java/com/carnation/fallalert/data/remote/CarnationApi.kt) |
| WebSocket + 재연결 | [data/remote/EventStreamClient.kt](app/src/main/java/com/carnation/fallalert/data/remote/EventStreamClient.kt) |
| 확인 결과 재시도 큐 | [data/ConfirmationOutbox.kt](app/src/main/java/com/carnation/fallalert/data/ConfirmationOutbox.kt) |
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
| 6 | 확인 상태 처리 | 완료 — 서버 전송 + 실패 시 재시도 |
| 7 | 실시간 수신 + 알림 | 완료 (WebSocket). FCM 은 미적용 — 앱 종료 중 수신용 |
| 8 | 실제 서버 연동 | 완료 (로컬 서버 기준) |

## 남은 것

- **FCM** — 앱이 완전히 종료된 동안의 알림. 지금은 앱 실행 중에만 받는다. [FCM_SETUP.md](app/src/main/java/com/carnation/fallalert/push/FCM_SETUP.md)
- **인증** — 없음. 명세 7장 미확정.
- **서버 영속화** — 인메모리라 재시작하면 확인 이력이 사라진다.
- **에뮬레이터** — 이 PC 에 시스템 이미지가 없다. 실기기(SM-S948N)로 검증했다.

## 실기기 검증 기록 (2026-08-16, SM-S948N)

- 목록 조회 → 서버 시드 4건 표시
- `POST /debug/simulate` → **WebSocket 으로 즉시** 목록 맨 위에 추가 + 헤드업 알림
- 상세 → "도움 필요" → 화면 C 가 "서버에 전달했어요" 표시, 서버에 `help_needed` 기록 확인
- 오프라인 상태에서 확인 → 재연결 시 자동 전송 (아웃박스)
- 이 과정에서 버그 하나를 잡았다: `no_recovery_sec` 가 `10.20427782593417초` 로 그대로 노출됐다.
  목업(4.0, 1.5)에서는 안 보이던 문제라, 서버가 랜덤 double 을 보내기 시작하고서야 드러났다.
  → [util/Formatting.kt](app/src/main/java/com/carnation/fallalert/util/Formatting.kt) 의 `formatSeconds` 로 옮기고 소수점 한 자리로 자름.

## 연결 동작

- 앱 시작 → REST 로 목록 조회 + WebSocket 구독
- 끊기면 1→2→4→8→16→30초 백오프로 재연결, 그동안 상단에 오프라인 배너
- 다시 붙으면 **전체 목록을 다시 읽어** 끊긴 동안의 공백을 메운다
- 확인 버튼 → 화면은 즉시 반응(낙관적 갱신), 전송은 아웃박스가 보장
- 전송 실패 시 큐는 **디스크에 남는다.** 앱을 껐다 켜도 다시 보낸다
- `404` 는 재시도해도 같으므로 큐에서 버린다 (`ConfirmResult.Permanent`)

## 설계 메모

- `no_recovery_sec` 는 선택 필드라 nullable 이고, 값이 없으면 상세 화면에서 **행 자체를 숨긴다.**
- `room_id` 는 계약상 임의 문자열이라, 아는 값만 한글로 바꾸고 모르는 값은 원문 그대로 노출한다.
- 표기는 항상 **"낙상 의심"** — 알림 문구도 앱이 만든다(그래서 FCM 은 `data`-only 를 요청해야 한다).
- `detected_at` 파싱 실패나 계약 위반 이벤트로 화면이 죽지 않도록 방어했다.
- 미확인 = 빨강 강조 + 테두리, 확인 완료 = 회색 톤 이력. 확인 버튼은 최소 높이 68dp.
- 확인 결과 API 는 `window_id` 기준 **멱등**이다. 앱이 실패 시 재시도할 것이므로 필수 조건.
- `confirmed_at` 은 서버가 **첫 보고 시각을 유지**한다. 재시도로 늦게 도착해도 실제로 누른 시각이 남는다.
- 계약 위반 이벤트는 서버가 `422` 로 막고, **무엇이 틀렸는지 문자열로 돌려준다.** 이벤트를
  만드는 쪽(모델·CSI)이 추측하지 않고 고칠 수 있어야 연동이 빨리 끝난다.
- 타임존 없는 `detected_at` 도 거부한다. 통과시키면 앱이 epoch 로 떨어뜨려 방금 난 사고가
  목록 맨 아래로 밀린다 — 에러 없이 조용히 틀리는 쪽이 더 위험하다.
- **목록을 못 읽었을 때 "알림 없음"으로 보이지 않게** 했다. 사고가 없는 것과 확인을 못 한 것은 다르다.
- 확인 결과가 아직 안 갔으면 화면 C 가 그렇게 말한다. "접수했어요"라고 해놓고 큐에만 있는 게 제일 나쁜 거짓말이다.
- 평문 통신은 **debug 빌드에만** 허용한다. main 쪽 설정은 전면 금지이고, `src/debug` 가 그걸 덮어쓴다 — 개발 편의가 배포로 새지 않는다.
- 개발 서버 IP 는 `local.properties` 에서 읽는다. 기기마다 다른 값을 저장소에 넣지 않기 위해서다.
- 기본값을 `localhost` + `adb reverse` 로 잡은 건, Wi-Fi 가 바뀔 때마다 IP 를 고치고 재빌드하는 걸
  없애기 위해서다. 대신 케이블을 다시 꽂을 때마다 터널을 한 번 걸어야 한다.
