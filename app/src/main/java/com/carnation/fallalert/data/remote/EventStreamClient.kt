package com.carnation.fallalert.data.remote

import android.util.Log
import com.carnation.fallalert.api.StreamMessage
import com.carnation.fallalert.model.FallEventJson
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** 스트림 연결 상태. 보호자에게 "지금 알림을 받을 수 있는지"를 알려주기 위한 것. */
sealed interface ConnectionState {
    data object Connecting : ConnectionState
    data object Connected : ConnectionState

    /** 끊김. [retryInSeconds] 뒤에 다시 시도한다. */
    data class Offline(val reason: String, val retryInSeconds: Long) : ConnectionState
}

/** 스트림에서 나오는 것: 서버 메시지이거나, 연결 상태 변화이거나. */
sealed interface StreamEvent {
    data class State(val state: ConnectionState) : StreamEvent
    data class Message(val message: StreamMessage) : StreamEvent
}

/**
 * WebSocket 구독. 끊기면 지수 백오프로 계속 다시 붙는다.
 *
 * 보호자 앱이라 "조용히 끊겨 있는 상태"가 가장 위험하다. 그래서 재연결을 포기하지 않고,
 * 대신 현재 상태를 [ConnectionState] 로 계속 흘려보내 화면에 표시하게 한다.
 */
class EventStreamClient(private val api: CarnationApi) {

    fun connect(): Flow<StreamEvent> = flow {
        var attempt = 0

        while (true) {
            emit(StreamEvent.State(ConnectionState.Connecting))

            val failure = try {
                val session = api.client.webSocketSession(api.webSocketUrl())
                attempt = 0
                emit(StreamEvent.State(ConnectionState.Connected))

                try {
                    for (frame in session.incoming) {
                        if (frame !is Frame.Text) continue
                        val text = frame.readText()
                        val message = runCatching {
                            FallEventJson.decodeFromString(StreamMessage.serializer(), text)
                        }.getOrElse { e ->
                            // 모르는 메시지 하나 때문에 연결을 끊지 않는다.
                            Log.w(TAG, "스트림 메시지 파싱 실패, 무시함: $text", e)
                            null
                        }
                        if (message != null) emit(StreamEvent.Message(message))
                    }
                    "서버가 연결을 닫았습니다"
                } finally {
                    runCatching { session.close() }
                }
            } catch (e: CancellationException) {
                throw e // 화면이 사라져 구독이 취소된 것. 재연결하면 안 된다.
            } catch (e: ClosedReceiveChannelException) {
                "서버가 연결을 닫았습니다"
            } catch (e: Exception) {
                e.message ?: "연결할 수 없습니다"
            }

            attempt += 1
            val waitSeconds = backoffSeconds(attempt)
            Log.i(TAG, "스트림 끊김($failure). ${waitSeconds}초 후 재시도 (${attempt}회차)")
            emit(StreamEvent.State(ConnectionState.Offline(failure, waitSeconds)))
            delay(waitSeconds * 1000)
        }
    }

    companion object {
        private const val TAG = "EventStreamClient"
        private const val MAX_BACKOFF_SECONDS = 30L

        /** 1, 2, 4, 8, 16, 30, 30... 서버가 잠깐 죽었을 때 재연결이 몰아치지 않게. */
        internal fun backoffSeconds(attempt: Int): Long =
            min(MAX_BACKOFF_SECONDS, 1L shl min(attempt - 1, 5))
    }
}
