package com.carnation.fallalert

import com.carnation.fallalert.api.ConfirmationReport
import com.carnation.fallalert.api.StreamMessage
import com.carnation.fallalert.data.remote.CarnationApi
import com.carnation.fallalert.data.remote.ConfirmResult
import com.carnation.fallalert.data.remote.ConnectionState
import com.carnation.fallalert.data.remote.EventStreamClient
import com.carnation.fallalert.data.remote.StreamEvent
import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.server.module
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 앱의 실제 클라이언트(`CarnationApi`, `EventStreamClient`)를 **진짜 Ktor 서버**에 붙여 검증한다.
 *
 * 목적은 계약 검증이다. 모델은 `:contract` 로 공유하니 필드명은 어긋날 수 없지만,
 * 경로·상태코드·WebSocket 프레임 형식은 여전히 어긋날 수 있다.
 * 에뮬레이터 없이 돌아가므로 CI 에서도 그대로 쓸 수 있다.
 */
class ServerIntegrationTest {

    private lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>
    private lateinit var api: CarnationApi

    @Before
    fun startServer() {
        // port 0 = OS 가 빈 포트를 골라 준다. 테스트끼리 포트 충돌하지 않게.
        server = embeddedServer(Netty, port = 0, host = "127.0.0.1") { module() }
        server.start(wait = false)

        val port = runCatching {
            kotlinx.coroutines.runBlocking { server.engine.resolvedConnectors().first().port }
        }.getOrElse { error("서버 포트를 알 수 없습니다: $it") }

        api = CarnationApi("http://127.0.0.1:$port")
    }

    @After
    fun stopServer() {
        api.client.close()
        server.stop(gracePeriodMillis = 0, timeoutMillis = 1000)
    }

    private fun report(windowId: String, confirmation: Confirmation) = ConfirmationReport(
        windowId = windowId,
        confirmation = confirmation,
        reportedAt = "2026-08-15T19:45:00+09:00",
        clientId = "integration-test",
    )

    @Test
    fun `앱이 서버 목록을 파싱한다`() = runBlocking {
        val events = withTimeout(TIMEOUT) { api.fetchEvents() }

        assertEquals(4, events.size)
        assertEquals("trial-01:7", events.first().windowId)
        // 선택 필드가 없는 시드가 섞여 있어야 앱의 행 숨김 로직이 실제로 검증된다.
        assertTrue(events.any { it.event.evidence.noRecoverySec == null })
    }

    @Test
    fun `확인 결과 전송이 성공으로 분류된다`() = runBlocking {
        val result = withTimeout(TIMEOUT) {
            api.sendConfirmation(report("trial-01:7", Confirmation.HELP_NEEDED))
        }

        assertTrue("실제 결과: $result", result is ConfirmResult.Success)
        assertEquals(
            Confirmation.HELP_NEEDED,
            (result as ConfirmResult.Success).envelope.confirmation,
        )
    }

    @Test
    fun `모르는 이벤트는 재시도하지 않을 실패로 분류된다`() = runBlocking {
        val result = withTimeout(TIMEOUT) {
            api.sendConfirmation(report("없는-이벤트", Confirmation.NORMAL))
        }

        // 이 구분이 틀리면 아웃박스가 영원히 안 될 요청을 붙들고 재시도한다.
        assertTrue("실제 결과: $result", result is ConfirmResult.Permanent)
    }

    @Test
    fun `서버가 없으면 재시도할 실패로 분류된다`() = runBlocking {
        val deadApi = CarnationApi("http://127.0.0.1:1") // 아무도 안 듣는 포트
        try {
            val result = withTimeout(TIMEOUT) {
                deadApi.sendConfirmation(report("trial-01:7", Confirmation.NORMAL))
            }
            assertTrue("실제 결과: $result", result is ConfirmResult.Retryable)
        } finally {
            deadApi.client.close()
        }
    }

    @Test
    fun `스트림에 붙으면 hello 를 받고 연결됨이 된다`() = runBlocking {
        val events = withTimeout(TIMEOUT) {
            EventStreamClient(api).connect().take(3).toList()
        }

        assertEquals(ConnectionState.Connecting, (events[0] as StreamEvent.State).state)
        assertEquals(ConnectionState.Connected, (events[1] as StreamEvent.State).state)
        assertTrue(
            "3번째는 hello 여야 한다: ${events[2]}",
            (events[2] as StreamEvent.Message).message is StreamMessage.Hello,
        )
    }

    /**
     * hello 를 받은 뒤에 [trigger] 를 실행하고, 원하는 프레임이 올 때까지 기다린다.
     * 미리 실행하면 연결 전에 이벤트가 지나가 버려 테스트가 간헐적으로 깨진다.
     */
    private suspend inline fun <reified T : StreamMessage> awaitFrameAfter(
        crossinline trigger: suspend () -> Unit,
    ): T = withTimeout(TIMEOUT) {
        coroutineScope {
            EventStreamClient(api).connect()
                .onEach { event ->
                    if ((event as? StreamEvent.Message)?.message is StreamMessage.Hello) {
                        launch { trigger() }
                    }
                }
                .mapNotNull { (it as? StreamEvent.Message)?.message as? T }
                .first()
        }
    }

    @Test
    fun `서버가 만든 새 이벤트가 스트림으로 도착한다`() = runBlocking {
        val frame = awaitFrameAfter<StreamMessage.NewEvent> { api.simulateEvent() }

        assertEquals("fall_suspected", frame.envelope.event.eventType)
        assertEquals(null, frame.envelope.confirmation)
    }

    @Test
    fun `다른 기기의 확인이 스트림으로 전파된다`() = runBlocking {
        val frame = awaitFrameAfter<StreamMessage.ConfirmationUpdated> {
            api.sendConfirmation(report("trial-01:3", Confirmation.NORMAL))
        }

        assertEquals("trial-01:3", frame.envelope.windowId)
        assertEquals(Confirmation.NORMAL, frame.envelope.confirmation)
    }

    private companion object {
        const val TIMEOUT = 15_000L
    }
}
