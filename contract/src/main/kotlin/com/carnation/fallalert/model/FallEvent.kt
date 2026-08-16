package com.carnation.fallalert.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 팀 계약 `fall_event.schema.json` v1.0 과 1:1 매핑되는 모델.
 *
 * 앱과 서버가 **이 파일 하나를** 함께 쓴다. 계약이 바뀌면 여기만 고치고,
 * 양쪽이 동시에 컴파일 에러로 알게 된다.
 */
@Serializable
data class FallEvent(
    @SerialName("schema_version") val schemaVersion: String,   // 항상 "1.0"
    @SerialName("event_type") val eventType: String,           // 항상 "fall_suspected"
    @SerialName("window_id") val windowId: String,
    @SerialName("detected_at") val detectedAt: String,         // ISO-8601 (타임존 포함)
    @SerialName("room_id") val roomId: String,
    @SerialName("risk_score") val riskScore: Double,           // 0.0 ~ 1.0
    val evidence: Evidence,
)

@Serializable
data class Evidence(
    @SerialName("motion_label") val motionLabel: String,
    @SerialName("motion_confidence") val motionConfidence: Double,
    @SerialName("presence_state") val presenceState: PresenceState,
    @SerialName("presence_probability") val presenceProbability: Double,
    /** 선택 필드 — 없을 수 있으므로 nullable. */
    @SerialName("no_recovery_sec") val noRecoverySec: Double? = null,
)

@Serializable
enum class PresenceState {
    @SerialName("present") PRESENT,
    @SerialName("absent") ABSENT,
    @SerialName("unknown") UNKNOWN,
}

/**
 * 보호자가 남긴 확인 결과.
 *
 * 계약(fall_event) 밖의 타입이지만 앱↔서버 경계를 오가므로 이 모듈에 둔다.
 * 와이어에서는 snake_case 문자열로 나간다.
 */
@Serializable
enum class Confirmation {
    @SerialName("normal") NORMAL,
    @SerialName("help_needed") HELP_NEEDED,
}
