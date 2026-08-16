package com.carnation.fallalert.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.carnation.fallalert.api.ConfirmationReport
import com.carnation.fallalert.model.FallEventJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer

/**
 * 아직 서버에 못 보낸 확인 결과 큐.
 *
 * 디스크에 남기는 게 핵심이다. 보호자가 "도움 필요"를 눌렀는데 하필 그때 네트워크가 끊겼고
 * 앱까지 종료됐다면, 메모리 큐로는 그 의사표시가 그냥 사라진다. 그건 이 앱에서 제일 나쁜 실패다.
 *
 * 저장소로 SharedPreferences 를 쓴 건 큐가 항상 몇 건 수준이기 때문이다.
 * 커질 일이 생기면 Room 으로 바꾼다.
 */
/**
 * 큐를 어디에 적는지. 인터페이스로 뺀 이유는 큐 로직을 기기 없이 단위 테스트하기 위해서다.
 */
interface OutboxStorage {
    fun read(): String?
    fun write(raw: String)
    fun clear()
}

class SharedPrefsOutboxStorage(context: Context) : OutboxStorage {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun read(): String? = prefs.getString(KEY_QUEUE, null)

    // commit(): 여기서 앱이 죽어도 큐가 남아야 한다. apply() 는 그 보장을 못 준다.
    override fun write(raw: String) {
        prefs.edit().putString(KEY_QUEUE, raw).commit()
    }

    override fun clear() {
        prefs.edit().remove(KEY_QUEUE).commit()
    }

    private companion object {
        const val PREFS_NAME = "confirmation_outbox"
        const val KEY_QUEUE = "queue"
    }
}

class ConfirmationOutbox(private val storage: OutboxStorage) {

    constructor(context: Context) : this(SharedPrefsOutboxStorage(context))

    private val _pending = MutableStateFlow(load())

    /** 전송 대기 중인 window_id 들. 화면이 "전송 대기 중" 표시에 쓴다. */
    val pending: StateFlow<List<ConfirmationReport>> = _pending.asStateFlow()

    fun isPending(windowId: String): Boolean = _pending.value.any { it.windowId == windowId }

    /** 같은 window_id 는 마지막 것만 남긴다 — 보호자가 마음을 바꿨으면 그게 최종 의사다. */
    fun enqueue(report: ConfirmationReport) {
        update { current -> current.filterNot { it.windowId == report.windowId } + report }
    }

    fun remove(windowId: String) {
        update { current -> current.filterNot { it.windowId == windowId } }
    }

    fun snapshot(): List<ConfirmationReport> = _pending.value

    private fun update(transform: (List<ConfirmationReport>) -> List<ConfirmationReport>) {
        val next = transform(_pending.value)
        _pending.value = next
        persist(next)
    }

    private fun persist(reports: List<ConfirmationReport>) {
        storage.write(FallEventJson.encodeToString(SERIALIZER, reports))
    }

    private fun load(): List<ConfirmationReport> {
        val raw = storage.read() ?: return emptyList()
        return try {
            FallEventJson.decodeFromString(SERIALIZER, raw)
        } catch (e: SerializationException) {
            // 저장 형식이 바뀐 구버전 데이터. 큐를 비우는 편이 앱이 못 뜨는 것보다 낫다.
            Log.e(TAG, "아웃박스 복원 실패 — 큐를 비웁니다", e)
            storage.clear()
            emptyList()
        }
    }

    private companion object {
        const val TAG = "ConfirmationOutbox"
        val SERIALIZER = ListSerializer(ConfirmationReport.serializer())
    }
}
