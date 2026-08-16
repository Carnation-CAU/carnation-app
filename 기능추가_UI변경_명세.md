# carnation-app 기능 추가 · UI 변경 명세 (Claude Code용)

> 기존 앱(목업 3화면)에 기능 추가 + UI 변경. 아래 6개 항목을 반영한다.
> 관련 파일: `ui/screen/AlertListScreen.kt`(홈), `ui/screen/EventDetailScreen.kt`(상세),
> `ui/screen/ConfirmationDoneScreen.kt`(확인 완료), `ui/FallAlertNavHost.kt`(내비), `ui/FallAlertViewModel.kt`(상태)

---

## 1. 장시간 무동작 경보 — 홈 화면 카드
- 홈 화면(`AlertListScreen`) 상단에 **무동작 경보 카드** 추가.
- **현재 무동작 시간을 표시**: 예) "3시간 12분째 움직임 없음".
- 데이터: 서버 연동 전이므로 `FallAlertViewModel`에 **목업 상태** `inactivityMinutes: Int`로 두고 표시(예: 192분 → "3시간 12분"). 나중에 서버 실시간 값으로 교체.
- 낙상 알림과 **별개 카드**로, 항상 홈 최상단에 보이게.
- (색 구간은 선택) 길어질수록 주목되게 하려면 회색→주황 정도만. 필수는 아님.

## 2. 하단 액션 버튼 2개 (좌우) — 홈 + 상세 화면
- `AlertListScreen`(홈)과 `EventDetailScreen`(상세) **하단**에 버튼 2개를 좌우로 배치.
  - 왼쪽: **[부모님께 전화]** → 전화 앱 열기 (`Intent.ACTION_DIAL`, `tel:` — 자동 발신 아님, 사용자가 통화 버튼 누름)
  - 오른쪽: **[119에 부모님 정보 보내기]**
- **119 버튼 동작(확정)**: 부모님 기본정보(설정에 저장된 `ParentProfile`)를 **미리 채운 문자 작성 화면**을 연다(`ACTION_SENDTO`, `smsto:`). 사용자가 수신번호·내용 확인 후 **직접 전송**.
  - 자동 전송 금지(오발송·법적 문제). 반드시 사용자 확인 단계를 거친다.
  - 문자 본문 예시: "낙상 의심 발생. {이름}({나이}세), 주소: {주소}, 현관 비밀번호: {door_code}, 특이사항: {건강}. 확인 부탁드립니다."
- 부모님 정보는 아래 **7번 설정 화면**의 `ParentProfile`에서 가져온다.

## 3. 확인한 알림 → 별도 탭
- 확인 완료된 알림은 홈에 나열하지 않는다.
- **하단 탭 내비게이션 도입**: `홈` / `확인한 알림` 2탭 (`FallAlertNavHost` 수정).
- `확인한 알림` 탭 = 확인 완료 이벤트만 최신순 리스트로 표시.
- 홈에서 "확인한 알림 보기" 진입 동선도 함께(탭 or 버튼).

## 4. 홈 알림은 최근 몇 개만
- 홈에는 **미확인 낙상 알림 최근 3개**만 표시.
- 3개 초과 시 "더 보기" → 전체 목록(또는 별도 화면).
- 확인된 것은 3번 규칙대로 홈에서 제외.

## 5. 상세 화면에서 "사람 존재" 삭제
- `EventDetailScreen`의 판단 근거에서 **presence(사람 존재/확률) 행 제거**.
- 남기는 근거: 움직임(motion), 회복 없음(no_recovery), 감지 구간(window_id), 위험도.
- 데이터 모델(`FallEvent`/`Evidence`)의 필드는 유지하되 **화면 표시만 제거**(서버 계약은 안 바꿈).

## 6. 확인 완료 화면에서 서버 전달 여부 표시 삭제
- `ConfirmationDoneScreen`에서 "서버에 전달됨/안 됨" 상태 문구·표시 제거.
- 확인 완료 메시지(정상/도움)만 남김.
- (내부적으로 서버 전송·재시도 로직은 유지, **화면 노출만 제거**)

## 7. 설정 화면 (부모님 정보)
- 홈 화면 **오른쪽 상단에 설정 버튼**(톱니 아이콘 `Icons.Default.Settings`) 추가.
- 탭하면 **설정 화면**(`SettingsScreen.kt` 새로 생성) 이동. `FallAlertNavHost`에 라우트 추가.
- 설정에서 **부모님 정보를 입력·수정**하는 폼:
  - 이름, 나이, 주소, **집 현관 비밀번호(door_code)**, 부모님 연락처, 응급연락처(자녀), 건강 특이사항, 혈액형 등
- 데이터 모델 `ParentProfile`(새 data class) 정의. **MVP는 하드코딩 목업 1개**로 시작(아래), 입력값은 앱 상태(ViewModel)에만 저장하면 됨. 영구 저장(DataStore)은 이후.
- 이 정보를 2번(119 정보 보내기)에서 사용.

### 하드코딩 목업 ParentProfile (그대로 사용)
```kotlin
data class ParentProfile(
    val name: String,
    val age: Int,
    val address: String,
    val doorCode: String,
    val parentPhone: String,
    val guardianPhone: String,
    val healthNotes: String,
    val bloodType: String,
)

val mockParent = ParentProfile(
    name = "이순자",
    age = 74,
    address = "서울시 관악구 신림로 123, 행복아파트 101동 302호",
    doorCode = "1234*",
    parentPhone = "010-2345-6789",
    guardianPhone = "010-9876-5432",
    healthNotes = "고혈압, 무릎 관절염, 청력 저하",
    bloodType = "B형",
)
```

---

## 화면 구조 변경 요약
```
홈 화면 (상단 우측: ⚙ 설정 버튼 → 설정 화면):
  ├─ 무동작 경보 카드 — "N시간 M분째 움직임 없음" (1)
  ├─ 미확인 낙상 알림 최근 3개 (4)
  ├─ [부모님께 전화] [119에 정보 보내기] (2)
[하단 탭]  홈  |  확인한 알림
확인한 알림 탭:
  └─ 확인 완료 이벤트 리스트 (3)
설정 화면(SettingsScreen, 신규):
  └─ 부모님 정보 폼 (주소·현관 비밀번호 등) (7)
상세 화면(EventDetailScreen):
  ├─ 위험도·근거 (presence 제거) (5)
  └─ [부모님께 전화] [119에 정보 보내기] (2)
확인 완료(ConfirmationDoneScreen):
  └─ 정상/도움 메시지만 (서버 상태 제거) (6)
```

## 참고 — 이후로 미룬 것
- 부모님 정보 **영구 저장**(DataStore) — 지금은 앱 상태 저장으로 충분.
- 무동작 경보 **색 구간/임계값** — 지금은 시간만 표시. 경계·위험 기준은 이후 서버·모델과 협의.

## 작업 원칙
- 서버 계약(`FallEvent` 형식)은 바꾸지 말 것. 5·6번은 **화면 표시만** 변경.
- 목업 데이터로 먼저 동작 확인 → 서버 연동은 기존대로 유지.
