package com.carnation.fallalert.data

import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.model.EventRecord
import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.util.detectedAtInstant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 이벤트 + 확인 상태의 단일 진실 공급원. 지금은 전부 메모리에만 있다.
 *
 * TODO(팀 확정 후): 확인 결과(정상/도움 필요)를 서버로 되돌려 보내야 한다.
 * 형식·엔드포인트가 미확정이라 현재는 로컬 상태만 갱신한다. (명세 4·7장)
 */
class FallEventRepository(private val source: FallEventSource) {

    private val _records = MutableStateFlow<List<EventRecord>>(emptyList())
    val records: StateFlow<List<EventRecord>> = _records.asStateFlow()

    suspend fun refresh() {
        _records.value = source.load().map(::EventRecord).sortedByRecency()
    }

    /** FCM/테스트 알림으로 들어온 새 이벤트를 목록 맨 위에 얹는다. */
    fun upsert(event: FallEvent) {
        _records.update { current ->
            val existing = current.firstOrNull { it.id == event.windowId }
            val merged = EventRecord(event, existing?.confirmation)
            (current.filterNot { it.id == event.windowId } + merged).sortedByRecency()
        }
    }

    fun confirm(windowId: String, confirmation: Confirmation) {
        _records.update { current ->
            current.map { if (it.id == windowId) it.copy(confirmation = confirmation) else it }
        }
        // TODO(팀 확정 후): POST /events/{window_id}/confirmation 같은 전송 추가.
    }

    private fun List<EventRecord>.sortedByRecency(): List<EventRecord> =
        sortedByDescending { it.event.detectedAtInstant() }
}
