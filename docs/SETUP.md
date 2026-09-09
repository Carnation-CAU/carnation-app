# 새 컴퓨터 환경설정

레포를 갓 클론한 상태에서 앱이 서버에 붙을 때까지. 순서대로 하면 된다.

> 이미 돌아가는 환경에서 **실행만** 하려는 거라면 [README](../README.md) 의 실행 절로 가면 된다.
> 이 문서는 아무것도 깔려 있지 않은 컴퓨터를 가정한다.

---

## 0. 제일 먼저: 클론에 없는 파일이 있다

`local.properties` 는 **`.gitignore` 에 있어서 클론에 딸려오지 않는다.** 기기마다 값이
달라서 일부러 뺀 파일이다. 이걸 안 만들면:

- Android SDK 경로를 못 찾아 빌드가 안 되고
- 서버 주소가 기본값 `10.0.2.2` (에뮬레이터 전용)로 잡혀서 **실기기가 서버에 안 붙는다**

만드는 법은 3장에 있다. 이 문서에서 제일 중요한 부분이다.

---

## 1. 설치할 것

| 항목 | 버전 | 확인 |
|---|---|---|
| **JDK** | **17** (필수, 21 아님) | `java -version` |
| **Android Studio** | Ladybug (2024.2.1) 이상 | AGP 8.7.3 이 요구 |
| **Android SDK Platform** | **API 35** | SDK Manager |
| **Android SDK Platform-Tools** | 최신 (`adb` 포함) | SDK Manager |
| Gradle | 8.9 — **설치 불필요** | 래퍼가 알아서 받는다 |

### JDK 17

Android Studio 에 번들된 JBR 을 그대로 써도 된다. 터미널에서 `.\gradlew.bat` 을 쓰려면
`JAVA_HOME` 이 17을 가리켜야 한다.

```bash
java -version   # "17.x" 가 나와야 한다
```

21이나 8이 나오면 Gradle 이 엉뚱한 곳에서 멈춘다. 이 프로젝트는 세 모듈 모두 17로 고정돼 있다
(`jvmToolchain(17)`, `sourceCompatibility = VERSION_17`).

### Android SDK

Android Studio → **More Actions → SDK Manager**

- **SDK Platforms** 탭 → `Android 15 (API 35)` 체크
- **SDK Tools** 탭 → `Android SDK Platform-Tools` 체크 (adb 가 여기 들어 있다)

`compileSdk = 35`, `targetSdk = 35`, `minSdk = 26` 이다. API 35가 없으면 sync 가 실패한다.

기본 설치 경로:

| OS | 경로 |
|---|---|
| Windows | `C:\Users\<이름>\AppData\Local\Android\Sdk` |
| macOS | `~/Library/Android/sdk` |
| Linux | `~/Android/Sdk` |

---

## 2. 클론

```bash
git clone https://github.com/Carnation-CAU/carnation-app.git
cd carnation-app
```

---

## 3. `local.properties` 만들기 ⭐

프로젝트 루트(= `settings.gradle.kts` 가 있는 곳)에 `local.properties` 파일을 만든다.

**Android Studio 로 프로젝트를 한 번 열면 `sdk.dir` 줄은 자동으로 생긴다.** 그래도
`server.host` 는 직접 넣어야 한다.

### Windows

```properties
sdk.dir=C\:\Users\<내이름>\AppData\Local\Android\Sdk
server.host=localhost
server.port=8080
```

> 백슬래시 두 개(`\`)와 콜론 앞 백슬래시(`C\:`)에 주의. `.properties` 파일 문법이다.

### macOS / Linux

```properties
sdk.dir=/Users/<내이름>/Library/Android/sdk
server.host=localhost
server.port=8080
```

### `server.host` 를 뭘로 둘지

| 값 | 언제 | 조건 |
|---|---|---|
| `localhost` | **실기기 USB (권장)** | `adb reverse` 터널 필요 (6장) |
| `10.0.2.2` | 에뮬레이터 | 에뮬레이터에서 호스트 PC 를 가리키는 특수 주소 |
| `192.168.x.x` | 실기기 무선 | 같은 Wi-Fi + 방화벽 8080 인바운드 허용 |

`localhost` + USB 터널을 권장하는 이유: **Wi-Fi 가 바뀌어도 IP 를 고치고 재빌드할 필요가 없다.**
카페·학교·집을 옮겨 다녀도 그대로 붙는다. 대신 케이블을 다시 꽂을 때마다 터널을 한 번 건다.

> ⚠️ `server.host` 는 `BuildConfig` 에 **컴파일 시점에 박힌다.** 값을 바꾸면 앱을 다시 빌드해야 한다.

---

## 4. 첫 빌드

프로젝트 루트에서:

```bash
.\gradlew.bat build
```

macOS/Linux 는 `./gradlew build`.

첫 실행은 Gradle 8.9 배포판과 의존성을 전부 받느라 **5~15분** 걸린다. 두 번째부터는 훨씬 빠르다.

> **PowerShell 은 `.\` 를 반드시 붙여야 한다.** 그냥 `gradlew.bat` 이라고 치면
> "용어가 cmdlet... 으로 인식되지 않습니다" 가 뜬다. 보안 정책상 현재 폴더를 PATH 로 안 본다.
> cmd 나 Git Bash 에서는 `gradlew.bat` / `./gradlew` 로 써도 된다.

### 테스트로 확인

```bash
.\gradlew.bat :contract:test :server:test :app:testDebugUnitTest
```

40건 전부 통과하면 환경설정은 끝난 것이다. 기기 없이 돌아간다.

---

## 5. 서버 띄우기

**앱보다 서버가 먼저다.** 앱은 로컬 캐시를 두지 않아서, 서버가 없으면 목록이 비어 있다.

### 권장: 독립 실행 파일로

한 번만 빌드해 두고,

```bash
.\gradlew.bat :server:installDist
```

그 다음부터는 이걸 실행한다:

```bash
server\build\install\server\bin\server.bat
```

`:server:run` 대신 이걸 권하는 이유: `:server:run` 은 Gradle 데몬과 서버 JVM을 **둘 다** 붙잡고
있어서, 데몬이 죽으면 서버도 같이 죽는다. 안드로이드 빌드를 동시에 돌리면 메모리가 모자라
`Failed to commit metaspace` 로 서버가 내려간다. 독립 실행 파일은 40MB 정도만 쓰고 빌드와 무관하게 산다.

### 확인

```bash
curl http://localhost:8080/health
```

`{"status":"ok",...}` 가 나오면 된다.

---

## 6. 실기기 연결

### 폰 준비 (한 번만)

1. 설정 → 휴대전화 정보 → **빌드번호 7번 탭**
2. 개발자 옵션 → **USB 디버깅** 켜기
3. USB 연결 → 폰에 뜨는 승인 팝업에서 "이 컴퓨터에서 항상 허용" 체크 후 허용

### 터널 열기 (꽂을 때마다)

```bash
.\connect-phone.bat
```

macOS/Linux 는 `./start-phone.sh` (서버 실행·빌드·설치까지 한 번에 한다).

스크립트가 adb 를 알아서 찾고(PATH → SDK 기본 경로 → `%ANDROID_HOME%`), 기기 연결을 확인하고,
폰에서 서버까지 실제로 닿는지 검사한다. `status:ok` 가 보이면 준비 완료다.

### 앱 설치

Android Studio 에서 `app` 실행하거나:

```bash
.\gradlew.bat :app:installDebug
```

---

## 7. 되는지 확인하는 순서

1. `curl http://localhost:8080/health` → `status:ok`
2. `.\connect-phone.bat` → `status:ok` (폰에서 본 결과)
3. 앱 실행 → 시드 이벤트 4건이 목록에 뜬다
4. 서버에 모의 이벤트 던지기:
   ```bash
   curl -X POST http://localhost:8080/debug/simulate
   ```
5. 앱 목록 맨 위에 **즉시** 새 알림이 뜨고 헤드업 알림이 온다 → **WebSocket 까지 정상**

4번에서 목록은 그대로인데 새로고침하면 보인다면, REST 는 되는데 WebSocket 이 안 붙은 것이다.

---

## 8. 자주 막히는 곳

| 증상 | 원인 | 해결 |
|---|---|---|
| `gradlew.bat 용어가 ... 인식되지 않습니다` | PowerShell 이 현재 폴더를 PATH 로 안 봄 | `.\gradlew.bat` |
| `SDK location not found` | `local.properties` 없음 | 3장 |
| 앱은 뜨는데 목록이 계속 비어 있음 | 서버 안 켜짐 / 주소 틀림 | 7장 1~2번 순서로 확인 |
| 앱에 "서버에 연결되지 않았어요" | 터널이 끊김 (케이블 재연결·adb 재시작) | `.\connect-phone.bat` 다시 |
| `adb 용어가 ... 인식되지 않습니다` | platform-tools 가 PATH 에 없음 | 스크립트를 쓰거나 PATH 에 추가 |
| `Failed to commit metaspace` | Gradle 데몬 여러 개 + 서버가 메모리 경쟁 | `.\gradlew.bat --stop` 후 5장 방식으로 |
| `Unsupported class file major version` | JDK 17이 아님 | `java -version` 확인 |
| API 35 관련 sync 실패 | SDK Platform 35 미설치 | SDK Manager |
| `-Pserver.host=localhost` 가 이상하게 잘림 | PowerShell 이 점에서 끊음 | 따옴표로: `"-Pserver.host=localhost"` |

### 메모리가 빠듯할 때

`gradle.properties` 는 `-Xmx2048m` 이다. 빌드가 자주 죽으면 Android Studio 와 CLI 빌드를
동시에 돌리지 않는 것만으로도 대부분 해결된다. 남은 데몬 확인·정리:

```bash
.\gradlew.bat --status
.\gradlew.bat --stop
```

---

## 9. 안 해도 되는 것

- **`app/google-services.json`** — FCM 은 아직 안 붙었다. 없어도 빌드된다
- **에뮬레이터** — 실기기로 검증해 왔다. 화면만 볼 거면 Android Studio 의 `@Preview` 로 충분하다
  (모든 화면에 `@Preview` 가 있다)
- **Gradle 설치** — 래퍼가 8.9를 자동으로 받는다

---

## 다음에 읽을 것

| 문서 | 내용 |
|---|---|
| [README](../README.md) | 프로젝트 구조, 설계 메모 |
| [WORKFLOW.md](WORKFLOW.md) | 데이터가 어디서 어디로 흐르고 무엇이 저장되는지 |
| [MODEL_TEAM_INTEGRATION.md](MODEL_TEAM_INTEGRATION.md) | 모델·CSI 팀 연동 가이드 |
| [server/README.md](../server/README.md) | 서버 API 명세 |
