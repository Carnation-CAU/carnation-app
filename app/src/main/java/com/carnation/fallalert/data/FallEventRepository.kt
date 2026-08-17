package com.carnation.fallalert.data

import android.util.Log
import com.carnation.fallalert.api.ConfirmationReport
import com.carnation.fallalert.api.EventEnvelope
import com.carnation.fallalert.api.StreamMessage
import com.carnation.fallalert.data.remote.CarnationApi
import com.carnation.fallalert.data.remote.ConfirmResult
import com.carnation.fallalert.data.remote.ConnectionState
import com.carnation.fallalert.data.remote.EventStreamClient
import com.carnation.fallalert.data.remote.StreamEvent
import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.model.EventRecord
import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.model.toRecord
import com.carnation.fallalert.util.detectedAtInstant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 목록을 서버에서 읽어오는 데 실패했는지. */
sealed interface LoadState {
    data object Loading : LoadState
    data object Loaded : LoadState
    data class Failed(val reason: String) : LoadState
}

/**
 * 이벤트·확인 상태의 단일 진실 공급원.
 *
 * 서버가 진실이고 앱은 캐시를 남기지 않는다. 대신 **아직 못 보낸 확인 결과만** 디스크에 남긴다
 * (자세한 이유는 [ConfirmationOutbox]).
 */
class FallEventRepository(
    private val api: CarnationApi,
    private val outbox: ConfirmationOutbox,
    private val stream: EventStreamClient = EventStreamClient(api),
    private val clientId: String,
) {

    private val _records = MutableStateFlow<List<EventRecord>>(emptyList())
    val records: StateFlow<List<EventRecord>> = _records.asStateFlow()

    private val _loadState = MutableStateFlow<LoadState>(LoadState.Loading)
    val loadState: StateFlow<LoadState> = _loadState.asStateFlow()

    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)
    val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    val pendingSync: StateFlow<List<ConfirmationReport>> = outbox.pending

    // 새로 도착한 이벤트. ViewModel 이 받아 알림을 띄운다.
    // (Repository 는 Context 를 모르는 편이 테스트하기 쉽다.)
    private val _newEvents = MutableSharedFlow<FallEvent>(extraBufferCapacity = 16)
    val newEvents: SharedFlow<FallEvent> = _newEvents.asSharedFlow()

    // 여러 경로(사용자 확인, 재연결, 새로고침)에서 동시에 큐를 비우려 들면 중복 전송이 된다.
    private val flushLock = Mutex()

    /** 스트림 구독 + 초기 로드. ViewModel 이 자기 scope 로 한 번만 부른다. */
    fun start(scope: CoroutineScope) {
        scope.launch { refresh() }
        scope.launch {
            stream.connect().collect { event ->
                when (event) {
                    is StreamEvent.State -> onConnectionState(event.state)
                    is StreamEvent.Message -> onStreamMessage(event.message)
                }
            }
        }
    }

    private suspend fun onConnectionState(state: ConnectionState) {
        _connection.value = state
        if (state is ConnectionState.Connected) {
            // 끊겨 있던 동안 놓친 이벤트가 있을 수 있으니 전체를 다시 읽는다.
            refresh()
            flushOutbox()
        }
    }

    private suspend fun onStreamMessage(message: StreamMessage) {
        when (message) {
            is StreamMessage.Hello -> Unit // 연결 확인용. 실제 동기화는 onConnectionState 에서.

            is StreamMessage.NewEvent -> {
                val isNew = _records.value.none { it.id == message.envelope.windowId }
                upsert(message.envelope)
                // 이미 아는 이벤트의 재전송으로 알림을 또 띄우지 않는다.
                if (isNew && message.envelope.confirmation == null) {
                    _newEvents.emit(message.envelope.event)
                }
            }

            is StreamMessage.ConfirmationUpdated -> upsert(message.envelope)
        }
    }

    suspend fun refresh() {
        _loadState.value = LoadState.Loading
        try {
            val envelopes = api.fetchEvents()
            _records.value = envelopes.map { it.toRecord() }.sortedByRecency()
            _loadState.value = LoadState.Loaded
        } catch (e: Exception) {
            Log.w(TAG, "목록 조회 실패", e)
            _loadState.value = LoadState.Failed(e.message ?: "서버에 연결할 수 없습니다")
        }
    }

    /**
     * 보호자의 확인. 화면은 즉시 반응해야 하므로 로컬을 먼저 바꾸고(낙관적 갱신),
     * 전송은 아웃박스를 통해 보장한다.
     */
    suspend fun confirm(
        windowId: String,
        confirmation: Confirmation,
        reasons: List<String> = emptyList(),
    ) {
        _records.update { current ->
            current.map { if (it.id == windowId) it.copy(confirmation = confirmation) else it }
        }
        outbox.enqueue(
            ConfirmationReport(
                windowId = windowId,
                confirmation = confirmation,
                reportedAt = nowIso(),
                clientId = clientId,
                reasons = reasons,
            )
        )
        flushOutbox()
    }

    /** 대기 중인 확인 결과를 순서대로 전송. 실패해도 조용히 두지 않고 큐에 남긴다. */
    suspend fun flushOutbox() = flushLock.withLock {
        for (report in outbox.snapshot()) {
            when (val result = api.sendConfirmation(report)) {
                is ConfirmResult.Success -> {
                    outbox.remove(report.windowId)
                    upsert(result.envelope)
                }

                is ConfirmResult.Permanent -> {
                    // 다시 보내도 같은 결과다. 큐에 두면 영원히 남는다.
                    Log.w(TAG, "확인 결과 전송 영구 실패, 큐에서 제거 (${report.windowId}): ${result.reason}")
                    outbox.remove(report.windowId)
                }

                is ConfirmResult.Retryable -> {
                    Log.i(TAG, "확인 결과 전송 실패, 나중에 재시도 (${report.windowId}): ${result.reason}")
                    // 이번엔 네트워크가 안 되는 것이므로 뒤 항목도 어차피 실패한다.
                    return@withLock
                }
            }
        }
    }

    /** 개발용: 서버에 모의 이벤트 생성을 요청한다. 결과는 WebSocket 으로 되돌아온다. */
    suspend fun requestSimulatedEvent(): Boolean = api.simulateEvent()

    private fun upsert(envelope: EventEnvelope) {
        _records.update { current ->
            (current.filterNot { it.id == envelope.windowId } + envelope.toRecord())
                .sortedByRecency()
        }
    }

    private fun List<EventRecord>.sortedByRecency(): List<EventRecord> =
        sortedByDescending { it.event.detectedAtInstant() }

    private companion object {
        const val TAG = "FallEventRepository"
        val ISO: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

        fun nowIso(): String = OffsetDateTime.now(ZoneId.of("Asia/Seoul")).format(ISO)
    }
}
