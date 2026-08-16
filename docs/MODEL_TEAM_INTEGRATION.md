# 모델·CSI 팀 → 보호자 앱 연동 가이드

낙상 의심 이벤트를 만들어 내는 쪽에서 읽는 문서입니다.
**HTTP POST 한 번**이면 보호자 폰에 즉시 알림이 뜹니다.

---

## 1. 한 줄 요약

```bash
curl -X POST http://<앱-서버-IP>:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d @event.json
```

- `201` → 성공. 연결된 보호자 앱 전부에 WebSocket 으로 **즉시** 전달됩니다.
- `422` → 계약 위반 (값 범위·형식). **어디가 틀렸는지 알려줍니다.**
- `400` → JSON 이 깨졌거나 타입이 안 맞음. **어느 필드인지 경로까지 알려줍니다.**

에러는 추측할 필요 없이 `message` 만 읽으면 됩니다:

```json
{"code":"contract_violation","message":"risk_score 가 0.0~1.0 범위 밖: 90.0"}
{"code":"malformed_json","message":"...PresenceState does not contain element with name 'PRESENT' at path $.evidence.presence_state"}
```

주소는 앱 담당자에게 물어보세요 (같은 Wi-Fi 의 노트북 사설 IP, 예: `192.168.0.5`).
`GET http://<IP>:8080/health` 가 뜨면 연결된 겁니다.

---

## 2. 보내야 하는 형식

팀 계약 `fall_event` v1.0 그대로입니다. 앱이 아는 형식은 이것뿐입니다.

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

| 필드 | 규칙 |
|---|---|
| `schema_version` | 항상 `"1.0"` |
| `event_type` | 항상 `"fall_suspected"` |
| `window_id` | 비어있지 않은 문자열. **이벤트의 고유 키** (아래 3장) |
| `detected_at` | ISO-8601, **타임존 필수** |
| `room_id` | 비어있지 않은 문자열 |
| `risk_score` | 0.0 ~ 1.0 |
| `evidence.motion_label` | 문자열. 앱이 아는 값은 `fall_like`, `sudden_drop`, `normal` — 모르는 값은 원문 그대로 표시됩니다 |
| `evidence.motion_confidence` | 0.0 ~ 1.0 |
| `evidence.presence_state` | `present` / `absent` / `unknown` 중 하나 |
| `evidence.presence_probability` | 0.0 ~ 1.0 |
| `evidence.no_recovery_sec` | **선택 필드.** 없으면 키를 빼거나 `null`. 0 이상 |

계약에 없는 필드를 추가로 보내도 앱은 무시합니다 (죽지 않습니다).
필요한 필드가 더 있으면 앱 담당자에게 알려주세요 — 계약을 같이 고쳐야 합니다.

### 기계로 검증하기

[`contract/fall_event.schema.json`](../contract/fall_event.schema.json) 에 JSON Schema (draft-07) 가 있습니다.
서버에 보내기 전에 로컬에서 먼저 걸러낼 수 있습니다:

```python
import json, jsonschema     # pip install jsonschema

schema = json.load(open("fall_event.schema.json", encoding="utf-8"))
jsonschema.validate(payload, schema)   # 틀리면 어디가 틀렸는지 예외로 알려줌
```

이 스키마 파일은 Kotlin 구현의 사본이지만, 두 쪽이 어긋나면 앱 팀 CI 가 깨지도록
자동 검사를 걸어 뒀습니다. 즉 **이 파일을 믿어도 됩니다.**

---

## 3. 자주 막히는 것 네 가지

### `detected_at` 에 타임존을 빼먹는 것 — 제일 흔합니다

```python
datetime.now().isoformat()                          # ✗ "2026-08-05T10:30:00"
datetime.now().astimezone().isoformat()             # ✓ "2026-08-05T10:30:00+09:00"
```

Python `datetime.now()` 는 타임존이 없습니다. 서버가 `422` 로 막습니다.
(막지 않으면 앱이 이 이벤트를 1970년으로 취급해 목록 맨 아래로 밀어버립니다.
보호자 눈에는 "방금 난 사고가 안 보인다"가 됩니다.)

### `window_id` 를 매번 새로 만들지 않는 것

`window_id` 는 이벤트의 **고유 키**입니다. 같은 값으로 다시 POST 하면 새 알림이 아니라
기존 이벤트를 갱신합니다 (보호자가 이미 확인한 상태는 유지됩니다).

- 같은 낙상을 **재전송**하는 것 → 같은 `window_id` (중복 알림 안 뜸, 의도된 동작)
- **다른 낙상** → 반드시 다른 `window_id`

### `risk_score` 를 0~100 으로 보내는 것

**0.0 ~ 1.0** 입니다. `%` 변환은 앱이 합니다. `90` 을 보내면 `422` 입니다.

### `presence_state` 대소문자

소문자 `present` / `absent` / `unknown` 입니다. `PRESENT` 를 보내면 `400` 과 함께
`at path $.evidence.presence_state` 로 위치까지 알려줍니다.

---

## 4. Python 예제

```python
import requests
from datetime import datetime

SERVER = "http://192.168.0.5:8080"   # 앱 담당자에게 받은 주소

def send_fall_event(window_id, room_id, risk, motion_conf,
                    presence, presence_prob, no_recovery_sec=None):
    evidence = {
        "motion_label": "fall_like",
        "motion_confidence": motion_conf,
        "presence_state": presence,          # present | absent | unknown
        "presence_probability": presence_prob,
    }
    # 선택 필드는 값이 있을 때만 넣는다.
    if no_recovery_sec is not None:
        evidence["no_recovery_sec"] = no_recovery_sec

    payload = {
        "schema_version": "1.0",
        "event_type": "fall_suspected",
        "window_id": window_id,
        # astimezone() 이 핵심 — 타임존 없으면 서버가 거부한다.
        "detected_at": datetime.now().astimezone().isoformat(),
        "room_id": room_id,
        "risk_score": risk,                  # 0.0 ~ 1.0
        "evidence": evidence,
    }

    r = requests.post(f"{SERVER}/api/v1/events", json=payload, timeout=5)
    if r.status_code == 201:
        print("전송됨:", window_id)
    else:
        # 422 면 message 에 무엇이 틀렸는지 한국어로 들어 있다.
        print("거부됨", r.status_code, r.json())
    return r


if __name__ == "__main__":
    send_fall_event("trial-01:7", "living-room", 0.9, 0.91, "present", 0.95, 4.0)
```

---

## 5. 잘 갔는지 확인하는 법

```bash
curl http://<IP>:8080/api/v1/events
```

방금 보낸 `window_id` 가 목록에 있으면 성공입니다.
`confirmation` 필드는 보호자가 앱에서 [정상] / [도움 필요] 를 누르면 채워집니다.

거부당했을 때 `422` 응답 예시:

```json
{"code":"contract_violation","message":"risk_score 가 0.0~1.0 범위 밖: 90.0"}
```

---

## 6. 지금 확정 안 된 것 (명세 7장)

- **인증이 없습니다.** 같은 네트워크 안에서 개발용으로만 쓰는 상태입니다.
  외부에 열거나 실제 배포하려면 인증을 먼저 붙여야 합니다.
- **서버를 최종적으로 누가 운영할지 미정입니다.** 지금 서버는 앱 팀이 개발·시연용으로
  띄운 것이고, 인메모리라 재시작하면 이력이 사라집니다.
- `room_id` 의 의미(기기 1대 = 방 1개?)도 아직 확정 전입니다.

이 세 가지가 정해지면 엔드포인트가 바뀔 수 있습니다. 형식(`fall_event`)은 안 바뀝니다.

---

## 7. 앱 팀에 확인해 주셨으면 하는 것

명세 7장에 "`fall_event` 계약이 최종 어느 저장소에 있는지 (현재 csi-monitor 엔 없음)" 이
미확정으로 남아 있습니다. 지금은 **이 저장소의 [`contract/`](../contract) 가 사실상 원본**입니다
(Kotlin 구현 + JSON Schema + 검증 테스트). 팀이 다른 곳을 정본으로 정하면 알려주세요.

그 외에 답을 주셨으면 하는 것:

- `room_id` 의 의미 — 기기 1대 = 방 1개인가요? 값 목록이 고정인가요?
- `motion_label` 로 실제로 나올 수 있는 값 전부 (앱이 한글 라벨을 붙여 둘 수 있게)
- `no_recovery_sec` 을 못 채우는 경우가 어떤 상황인지 (앱은 이때 행을 숨깁니다)
- 이벤트 발생 빈도 대략 — 오탐 포함 하루 몇 건 정도인지

## 8. 참고

- 서버 코드와 API 전체 명세: [`server/README.md`](../server/README.md)
- 계약 모델 (Kotlin, 앱·서버 공유): [`contract/`](../contract/src/main/kotlin/com/carnation/fallalert)
- JSON Schema: [`contract/fall_event.schema.json`](../contract/fall_event.schema.json)
