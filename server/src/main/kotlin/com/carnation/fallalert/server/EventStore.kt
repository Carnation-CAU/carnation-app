package com.carnation.fallalert.server

import com.carnation.fallalert.api.ConfirmationReport
import com.carnation.fallalert.api.EventEnvelope
import com.carnation.fallalert.api.StreamMessage
import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.model.contractViolation
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ContractViolationException(val reason: String) : IllegalArgumentException(reason)

/**
 * 이벤트와 확인 상태의 인메모리 저장소.
 *
 * DB 를 붙이지 않은 건 의도적이다 — 팀이 저장소·인증 모델을 확정하기 전(명세 7장)에
 * 스키마를 먼저 굳히면 나중에 두 번 만들게 된다. 재시작하면 시드 데이터로 돌아간다.
 */
class EventStore(seed: List<FallEvent> = emptyList()) {

    private val mutex = Mutex()
    private val envelopes = LinkedHashMap<String, EventEnvelope>()

    // extraBufferCapacity: 느린 WebSocket 구독자 때문에 이벤트 등록이 막히지 않도록.
    private val _stream = MutableSharedFlow<StreamMessage>(extraBufferCapacity = 64)
    val stream: SharedFlow<StreamMessage> = _stream.asSharedFlow()

    init {
        seed.forEach { envelopes[it.windowId] = EventEnvelope(it) }
    }

    suspend fun all(): List<EventEnvelope> = mutex.withLock {
        // 문자열 정렬은 오프셋이 섞이면 틀린다. 반드시 파싱해서 비교한다.
        envelopes.values.sortedByDescending { parseDetectedAt(it.event.detectedAt) }
    }

    suspend fun count(): Int = mutex.withLock { envelopes.size }

    /**
     * 모델·CSI 쪽에서 새 이벤트를 밀어 넣는 경로.
     * 계약 위반이면 저장하지 않고 이유를 던진다 — 잘못된 이벤트를 보호자 화면까지 보내면 안 된다.
     */
    suspend fun add(event: FallEvent): EventEnvelope {
        event.contractViolation()?.let { throw ContractViolationException(it) }

        val envelope = mutex.withLock {
            // 같은 window_id 재전송은 기존 확인 상태를 보존한 채 갱신한다.
            val existing = envelopes[event.windowId]
            EventEnvelope(event, existing?.confirmation, existing?.confirmedAt)
                .also { envelopes[event.windowId] = it }
        }
        _stream.emit(StreamMessage.NewEvent(envelope))
        return envelope
    }

    /**
     * 보호자의 확인 결과 기록. `window_id` 기준 멱등이라 앱이 몇 번을 재시도해도 안전하다.
     * 모르는 `window_id` 면 null — 앱은 404 를 받고 큐에서 버린다.
     */
    suspend fun confirm(report: ConfirmationReport): EventEnvelope? {
        val envelope = mutex.withLock {
            val existing = envelopes[report.windowId] ?: return@withLock null
            existing.copy(
                confirmation = report.confirmation,
                // 첫 보고 시각을 유지한다. 재시도로 늦게 도착해도 원래 누른 시각이 남는다.
                confirmedAt = existing.confirmedAt ?: report.reportedAt,
            ).also { envelopes[report.windowId] = it }
        } ?: return null

        _stream.emit(StreamMessage.ConfirmationUpdated(envelope))
        return envelope
    }
}

private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

fun nowIso(): String = OffsetDateTime.now(ZoneId.of("Asia/Seoul")).format(ISO)

/** 계약 검증을 통과한 값만 들어오지만, 방어적으로 파싱 실패는 epoch 로 떨어뜨린다. */
fun parseDetectedAt(value: String): Instant = try {
    OffsetDateTime.parse(value, ISO).toInstant()
} catch (e: DateTimeParseException) {
    Instant.EPOCH
}
