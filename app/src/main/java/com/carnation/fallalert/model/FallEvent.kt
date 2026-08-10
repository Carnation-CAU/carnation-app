package com.carnation.fallalert.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 팀 계약 `fall_event.schema.json` v1.0 과 1:1 매핑되는 모델.
 * 이 파일은 계약이 바뀔 때만 수정한다.
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

/** 보호자가 남긴 확인 결과. 계약 밖의 앱 자체 타입. */
enum class Confirmation { NORMAL, HELP_NEEDED }

/** 이벤트 + 로컬 확인 상태. 화면이 실제로 다루는 단위. */
data class EventRecord(
    val event: FallEvent,
    val confirmation: Confirmation? = null,
) {
    val id: String get() = event.windowId
    val isUnconfirmed: Boolean get() = confirmation == null
}
