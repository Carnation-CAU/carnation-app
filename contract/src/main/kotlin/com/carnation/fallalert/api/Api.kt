package com.carnation.fallalert.api

import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.model.FallEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 앱↔서버 API 계약. **팀 계약(fall_event)과는 다른 층이다.**
 *
 * `fall_event` 는 모델·서버가 정하는 것이고(명세 1장), 여기 있는 타입은 보호자 앱과
 * 알림 서버 사이에서만 쓰는 봉투(envelope)다. 팀이 7장 항목을 확정하면 이 파일만 바뀐다.
 */
object ApiPaths {
    const val VERSION = "v1"
    const val HEALTH = "/health"
    const val EVENTS = "/api/$VERSION/events"
    const val STREAM = "/api/$VERSION/stream"
    const val SIMULATE = "/api/$VERSION/debug/simulate"

    fun confirmation(windowId: String) = "$EVENTS/$windowId/confirmation"
}

/** 이벤트 + 서버가 아는 확인 상태. 목록·상세 화면이 필요한 전부. */
@Serializable
data class EventEnvelope(
    val event: FallEvent,
    val confirmation: Confirmation? = null,
    @SerialName("confirmed_at") val confirmedAt: String? = null,
) {
    val windowId: String get() = event.windowId
}

@Serializable
data class EventsResponse(
    val events: List<EventEnvelope>,
    @SerialName("server_time") val serverTime: String,
)

/**
 * 확인 결과 보고. `window_id` 기준 멱등 — 같은 내용을 여러 번 보내도 안전하다.
 * 앱이 실패 시 재시도하기 때문에 멱등성이 필수다.
 */
@Serializable
data class ConfirmationReport(
    @SerialName("window_id") val windowId: String,
    val confirmation: Confirmation,
    /** 보호자가 실제로 버튼을 누른 시각. 재시도로 늦게 도착해도 원래 시각이 남는다. */
    @SerialName("reported_at") val reportedAt: String,
    @SerialName("client_id") val clientId: String,
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
)

/** WebSocket 스트림 프레임. `type` 키로 판별한다. */
@Serializable
sealed interface StreamMessage {

    /** 연결 직후 서버가 먼저 보낸다. 앱은 이걸 받아야 "연결됨"으로 친다. */
    @Serializable
    @SerialName("hello")
    data class Hello(
        @SerialName("server_time") val serverTime: String,
        @SerialName("event_count") val eventCount: Int,
    ) : StreamMessage

    /** 새 낙상 의심 이벤트 도착. */
    @Serializable
    @SerialName("event")
    data class NewEvent(val envelope: EventEnvelope) : StreamMessage

    /** 다른 기기에서 확인했을 때 등, 확인 상태만 바뀐 경우. */
    @Serializable
    @SerialName("confirmation")
    data class ConfirmationUpdated(val envelope: EventEnvelope) : StreamMessage
}
