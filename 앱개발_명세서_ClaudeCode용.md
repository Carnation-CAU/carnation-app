# 낙상 감지 보호자 앱 — 개발 명세서

> 용도: Claude Code에 넘겨 실제 Android 앱을 구현하기 위한 명세.
> 기준: 팀의 `fall_event.schema.json` 계약(v1.0). 모델·서버 없이 **목업 데이터로 먼저 개발** → 나중에 연동만 교체.
> 담당: 앱(Android). 대상 사용자: 독거노인의 **보호자**.

---

## 1. 앱이 받는 데이터 (팀 계약 = 유일한 의존성)

서버/모델이 최종적으로 이 형식(JSON)을 앱에 보낸다. 앱은 이 형식만 알면 된다.

```json
{
  "schema_version": "1.0",
  "event_type": "fall_suspected",
  "window_id": "trial-01:7",
  "detected_at": "2026-08-05T10:30:00+09:00",
  "room_id": "living-room",
  "risk_score": 0.9,
  "evidence": {
    "motion_label": "fall_like",
    "motion_confidence": 0.91,
    "presence_state": "present",
    "presence_probability": 0.95,
    "no_recovery_sec": 4.0
  }
}
```

필드 규칙(계약 기준):
- `schema_version`: 항상 `"1.0"`
- `event_type`: 항상 `"fall_suspected"`
- `window_id`, `room_id`: 비어있지 않은 문자열
- `detected_at`: ISO-8601 date-time (타임존 포함)
- `risk_score`: 0.0 ~ 1.0
- `evidence.presence_state`: `present | absent | unknown` 중 하나
- `evidence.no_recovery_sec`: **선택 필드** (없을 수 있음 → nullable 처리)

---

## 2. Kotlin 데이터 모델

`kotlinx.serialization` 기준. 계약과 1:1 매핑.

```kotlin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FallEvent(
    @SerialName("schema_version") val schemaVersion: String,   // "1.0"
    @SerialName("event_type")     val eventType: String,       // "fall_suspected"
    @SerialName("window_id")      val windowId: String,
    @SerialName("detected_at")    val detectedAt: String,      // ISO-8601, 이후 Instant 파싱
    @SerialName("room_id")        val roomId: String,
    @SerialName("risk_score")     val riskScore: Double,       // 0.0 ~ 1.0
    val evidence: Evidence,
)

@Serializable
data class Evidence(
    @SerialName("motion_label")         val motionLabel: String,
    @SerialName("motion_confidence")    val motionConfidence: Double,
    @SerialName("presence_state")       val presenceState: PresenceState,
    @SerialName("presence_probability") val presenceProbability: Double,
    @SerialName("no_recovery_sec")      val noRecoverySec: Double? = null,  // 선택 필드
)

@Serializable
enum class PresenceState {
    @SerialName("present") PRESENT,
    @SerialName("absent")  ABSENT,
    @SerialName("unknown") UNKNOWN,
}
```

보조 변환(표시용):
- `riskScore` → 화면엔 `%`로: `(riskScore * 100).roundToInt()`
- `detectedAt` → `Instant.parse(...)` 후 한국 시각/상대시간("방금", "10분 전")으로 포맷
- 사용자 확인 결과(정상/도움요청)는 별도 타입으로:

```kotlin
enum class Confirmation { NORMAL, HELP_NEEDED }
```

---

## 3. 화면 명세 (목업 기준 3화면)

### 화면 A — 알림 목록 (홈)
- 낙상 의심 이벤트 카드 리스트 (최신순)
- 카드 내용: 위험 아이콘 + "낙상 의심" + `room_id` + 상대시각 + 탭 유도
- 미확인 이벤트는 danger 색(빨강 계열)으로 강조
- 확인 완료된 과거 이벤트는 회색 톤으로 이력 표시
- 카드 탭 → 화면 B(상세)

### 화면 B — 상세
- 상단: 큰 경고 아이콘 + "{room_id}에서 낙상 의심" + 감지 일시
- 위험도 카드: `risk_score`를 %로 크게 표시
- 판단 근거 목록(evidence):
  - 움직임: `motion_label` (`motion_confidence`%)
  - 사람 존재: `presence_state` (`presence_probability`%)
  - 회복 없음: `no_recovery_sec`초 (없으면 행 숨김)
  - 감지 구간: `window_id`
- 하단 버튼 2개: **[정상]** / **[도움 필요]** → 화면 C
- "목록으로" 링크

### 화면 C — 확인 완료
- 정상 선택: 성공 아이콘 + "정상으로 확인했어요" + "오탐으로 기록해 정확도 개선"
- 도움 필요 선택: 경고 아이콘 + "도움 요청을 접수했어요" + "긴급 연락처로 전달 흐름"
- "목록으로 돌아가기" 버튼
- (동작) 확인 결과를 서버로 전송 — **단, 전송 형식·엔드포인트는 팀 미확정(4장 참고)** → 지금은 로컬 상태만 갱신하고 TODO 표시

---

## 4. 기술 스택 제안 (Claude Code에서 결정)

- 언어: Kotlin
- UI: **Jetpack Compose** (권장 — 화면 3개라 빠름)
- 아키텍처: 간단히 `ViewModel` + `StateFlow`, 화면 상태는 sealed class
- 직렬화: `kotlinx.serialization`
- 알림: **FCM(Firebase Cloud Messaging)** 가정 — 단, 서버 확정 전엔 목업 알림으로 테스트
- 최소 SDK: API 26+ (Android 8.0) 정도

---

## 5. 목업(mock) 데이터로 먼저 개발

서버·모델이 없으므로, 위 JSON 예시를 앱 내부에 하드코딩하거나 assets의 `mock_events.json`으로 읽어 개발한다.

```kotlin
val mockEvents = listOf(
    FallEvent(
        schemaVersion = "1.0", eventType = "fall_suspected",
        windowId = "trial-01:7", detectedAt = "2026-08-05T10:30:00+09:00",
        roomId = "living-room", riskScore = 0.9,
        evidence = Evidence("fall_like", 0.91, PresenceState.PRESENT, 0.95, 4.0)
    )
)
```

→ 실제 연동 시엔 이 목업 소스만 **서버/FCM 수신 데이터로 교체**하면 됨. 화면·모델은 그대로.

---

## 6. 개발 순서 (Claude Code 체크리스트)

1. Compose 프로젝트 생성 (빈 앱 빌드·실행 확인)
2. `FallEvent` / `Evidence` / `PresenceState` 데이터 모델 작성
3. `mock_events.json` + 파서로 목업 데이터 로드
4. 화면 A(목록) → B(상세) → C(확인) 네비게이션 구현
5. `detected_at` 시각 포맷, `risk_score` % 변환 등 표시 로직
6. 확인(정상/도움) 상태 처리 — 지금은 로컬, 서버 전송은 TODO
7. FCM 수신 구조 뼈대 (테스트는 가짜 알림으로)
8. (팀 답변 후) 서버 전송·수신 실제 연동

---

## 7. 팀에 확정받아야 할 것 (앱↔서버 경계)

아래가 정해지기 전엔 목업으로 진행. 정해지면 6-7,8번만 채우면 됨.

- `fall_event` 계약이 최종 어느 저장소에 있는지 (현재 csi-monitor엔 없음)
- 서버 → 앱 전송 방식: FCM 푸시 / REST / WebSocket
- 확인 결과(정상/도움) 되돌려 보내는 형식·엔드포인트
- `room_id` 의미(기기 1대=방 1개?), 사용자·인증 모델 범위

---

## 8. 디자인 원칙 (보호자·고령 대응)

- 큰 글씨, 강한 색 대비 (위험 = 빨강 계열 명확히)
- 낙상 의심은 "낙상"이 아니라 **"낙상 의심"**으로 표기 (팀 안전 원칙)
- 확인 흐름 단순 2버튼 (정상 / 도움 필요)
- 알림은 놓치면 안 되므로 눈에 띄게, 단 오탐 대비 "정상" 처리를 쉽게
