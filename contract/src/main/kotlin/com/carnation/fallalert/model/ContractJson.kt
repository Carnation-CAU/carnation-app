package com.carnation.fallalert.model

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlinx.serialization.json.Json

/**
 * 계약 JSON 설정. 앱·서버·테스트가 모두 이 인스턴스를 쓴다.
 *
 * - ignoreUnknownKeys: 서버가 계약에 필드를 추가해도 구버전 앱이 죽지 않도록.
 * - explicitNulls=false: `no_recovery_sec` 처럼 없는 선택 필드를 굳이 `null` 로 내보내지 않는다.
 * - classDiscriminator: WebSocket 스트림 메시지의 sealed 판별 키.
 */
val FallEventJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    classDiscriminator = "type"
}

/**
 * 계약 위반 이벤트를 화면·저장소까지 흘리지 않기 위한 최소 검증.
 * 타입 시스템으로 못 막는 규칙(값 범위, 고정 문자열)만 본다.
 */
fun FallEvent.contractViolation(): String? = when {
    schemaVersion != "1.0" -> "schema_version 이 \"1.0\" 이 아님: $schemaVersion"
    eventType != "fall_suspected" -> "event_type 이 \"fall_suspected\" 가 아님: $eventType"
    windowId.isBlank() -> "window_id 가 비어 있음"
    roomId.isBlank() -> "room_id 가 비어 있음"
    !isDetectedAtValid() ->
        "detected_at 이 타임존 포함 ISO-8601 이 아님: \"$detectedAt\" " +
            "(예: \"2026-08-05T10:30:00+09:00\")"
    riskScore !in 0.0..1.0 -> "risk_score 가 0.0~1.0 범위 밖: $riskScore"
    evidence.motionConfidence !in 0.0..1.0 -> "motion_confidence 가 범위 밖: ${evidence.motionConfidence}"
    evidence.presenceProbability !in 0.0..1.0 -> "presence_probability 가 범위 밖: ${evidence.presenceProbability}"
    evidence.noRecoverySec?.let { it < 0.0 } == true -> "no_recovery_sec 가 음수: ${evidence.noRecoverySec}"
    else -> null
}

fun FallEvent.isContractValid(): Boolean = contractViolation() == null

/**
 * 타임존 없는 `detected_at`("2026-08-05T10:30:00")은 조용히 망가지는 종류의 오류다.
 * 파싱에 실패하면 앱이 epoch 로 떨어뜨려 목록 맨 아래로 밀어 버리는데,
 * 보호자 입장에서는 "방금 난 사고가 안 보인다"가 된다. 그래서 서버가 입구에서 막는다.
 */
private fun FallEvent.isDetectedAtValid(): Boolean = try {
    OffsetDateTime.parse(detectedAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    true
} catch (e: DateTimeParseException) {
    false
}
