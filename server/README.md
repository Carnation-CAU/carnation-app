# 낙상 의심 알림 서버 (로컬 개발용)

Ktor + Netty. **인메모리 저장소** — 재시작하면 `resources/mock_events.json` 시드로 돌아간다.
DB를 안 붙인 건 의도적이다: 팀이 저장소·인증 모델을 확정하기 전(명세 7장)에 스키마를 굳히면 두 번 만들게 된다.

## 실행

```bash
.\gradlew.bat :server:run
```

기본 포트 8080, 호스트 `0.0.0.0`. 콘솔에 한글이 깨지면 `chcp 65001` 먼저 실행.

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `PORT` | `8080` | 리슨 포트 |
| `SIMULATE_INTERVAL_SEC` | (꺼짐) | N초마다 모의 이벤트 자동 생성 |

## 앱에서 접속할 주소

| 실행 환경 | 주소 |
|---|---|
| Android 에뮬레이터 | `http://10.0.2.2:8080` (에뮬레이터가 호스트 PC를 보는 주소) |
| 실기기 (같은 Wi-Fi) | `http://<PC의 사설 IP>:8080` — `ipconfig` 로 확인. 방화벽 8080 인바운드 허용 필요 |
| 같은 PC의 브라우저·curl | `http://localhost:8080` |

## API

계약 타입은 전부 `:contract` 모듈에 있다. 앱과 서버가 **같은 Kotlin 파일**을 쓰므로
계약이 어긋나면 컴파일이 깨진다.

### `GET /health`
```json
{"status":"ok","version":"v1","events":4,"time":"2026-08-15T19:45:37+09:00"}
```

### `GET /api/v1/events`
전체 이벤트를 `detected_at` 최신순으로. 확인 상태가 함께 온다.
```json
{
  "events": [
    { "event": { "schema_version": "1.0", "...": "..." },
      "confirmation": "help_needed",
      "confirmed_at": "2026-08-15T19:45:00+09:00" }
  ],
  "server_time": "2026-08-15T19:45:37+09:00"
}
```

### `POST /api/v1/events`
**모델·CSI 파이프라인이 쓰는 입구.** 본문은 팀 계약(`fall_event`) 그대로.
등록되면 연결된 앱 전부에 WebSocket 으로 즉시 퍼진다.

- `201` 등록됨
- `422 contract_violation` — 계약 위반 (이유가 message 에 들어감)
- `400 malformed_json`

같은 `window_id` 를 다시 보내면 **기존 확인 상태를 보존한 채** 내용만 갱신한다.

### `POST /api/v1/events/{window_id}/confirmation`
보호자의 확인 결과. `window_id` 기준 **멱등** — 앱이 실패 시 재시도하기 때문에 필수다.

```json
{"window_id":"trial-01:7","confirmation":"help_needed",
 "reported_at":"2026-08-15T19:45:00+09:00","client_id":"device-a"}
```

- `200` 갱신된 envelope
- `404 unknown_window_id` — 앱은 이걸 받으면 재시도 큐에서 버린다
- `400 window_id_mismatch` — 경로와 본문 불일치

`confirmed_at` 은 **첫 보고 시각을 유지한다.** 재시도로 늦게 도착해도 보호자가 실제로 누른 시각이 남는다.

### `WS /api/v1/stream`
연결 즉시 `hello` 를 보내고, 이후 변경을 밀어 준다. `type` 키로 판별.

```json
{"type":"hello","server_time":"...","event_count":5}
{"type":"event","envelope":{...}}
{"type":"confirmation","envelope":{...}}
```

앱은 `hello` 를 받으면 REST 로 전체 목록을 한 번 다시 읽어 끊긴 동안의 공백을 메운다.
서버는 15초마다 ping 을 보내 죽은 연결을 정리한다.

### `POST /api/v1/debug/simulate`
모의 이벤트 하나를 만들어 스트림에 밀어 넣는다. 개발용.

## 수동 확인

```bash
curl http://localhost:8080/api/v1/events
```

```bash
curl -X POST http://localhost:8080/api/v1/debug/simulate
```

```bash
curl -X POST http://localhost:8080/api/v1/events/trial-01:7/confirmation -H "Content-Type: application/json" -d "{\"window_id\":\"trial-01:7\",\"confirmation\":\"normal\",\"reported_at\":\"2026-08-15T19:45:00+09:00\",\"client_id\":\"curl\"}"
```

## 배포 전에 반드시 고칠 것

- `CORS.anyHost()` — 로컬 개발 전용이다. 배포 시 좁힐 것.
- 인증 없음. 누구나 이벤트를 등록하고 확인 결과를 바꿀 수 있다 (명세 7장 인증 모델 미확정).
- 인메모리 저장 — 재시작하면 확인 이력이 사라진다.
