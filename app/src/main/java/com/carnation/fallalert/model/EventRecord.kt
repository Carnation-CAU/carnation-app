package com.carnation.fallalert.model

import com.carnation.fallalert.api.EventEnvelope

/**
 * 화면이 다루는 단위: 서버가 아는 이벤트 + 확인 상태.
 *
 * [FallEvent] 와 [Confirmation] 은 `:contract` 모듈에 있다 — 서버와 공유하는 계약이라서다.
 * 이 타입은 앱 전용이므로 여기 남는다.
 */
data class EventRecord(
    val event: FallEvent,
    val confirmation: Confirmation? = null,
) {
    val id: String get() = event.windowId
    val isUnconfirmed: Boolean get() = confirmation == null
}

fun EventEnvelope.toRecord(): EventRecord = EventRecord(event, confirmation)
