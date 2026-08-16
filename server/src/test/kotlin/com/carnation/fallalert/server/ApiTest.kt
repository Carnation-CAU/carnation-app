package com.carnation.fallalert.server

import com.carnation.fallalert.api.ApiError
import com.carnation.fallalert.api.ApiPaths
import com.carnation.fallalert.api.ConfirmationReport
import com.carnation.fallalert.api.EventEnvelope
import com.carnation.fallalert.api.EventsResponse
import com.carnation.fallalert.api.StreamMessage
import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.model.Evidence
import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.model.FallEventJson
import com.carnation.fallalert.model.PresenceState
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.plugins.websocket.webSocket
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.withTimeout

class ApiTest {

    private fun sampleEvent(windowId: String = "test:1", riskScore: Double = 0.9) = FallEvent(
        schemaVersion = "1.0",
        eventType = "fall_suspected",
        windowId = windowId,
        detectedAt = "2026-08-05T10:30:00+09:00",
        roomId = "living-room",
        riskScore = riskScore,
        evidence = Evidence("fall_like", 0.91, PresenceState.PRESENT, 0.95, 4.0),
    )

    private fun report(windowId: String, confirmation: Confirmation) = ConfirmationReport(
        windowId = windowId,
        confirmation = confirmation,
        reportedAt = "2026-08-05T10:31:00+09:00",
        clientId = "test-client",
    )

    @Test
    fun `health 는 시드 이벤트 수를 알려준다`() = testApplication {
        application { module() }
        val body = client.get(ApiPaths.HEALTH).bodyAsText()
        assertTrue(body.contains("\"status\":\"ok\""), body)
    }

    @Test
    fun `시드 이벤트를 최신순으로 돌려준다`() = testApplication {
        application { module() }

        val response = FallEventJson.decodeFromString<EventsResponse>(
            client.get(ApiPaths.EVENTS).bodyAsText()
        )

        assertEquals(4, response.events.size)
        assertEquals("trial-01:7", response.events.first().windowId) // 08-05 10:30 이 가장 최신
        assertEquals("trial-00:11", response.events.last().windowId) // 08-04 14:47 이 가장 오래됨
        assertTrue(response.events.all { it.confirmation == null })
    }

    @Test
    fun `이벤트를 등록하면 목록에 나타난다`() = testApplication {
        application { module() }

        val created = client.post(ApiPaths.EVENTS) {
            contentType(ContentType.Application.Json)
            setBody(FallEventJson.encodeToString(sampleEvent()))
        }
        assertEquals(HttpStatusCode.Created, created.status)

        val response = FallEventJson.decodeFromString<EventsResponse>(
            client.get(ApiPaths.EVENTS).bodyAsText()
        )
        assertNotNull(response.events.firstOrNull { it.windowId == "test:1" })
    }

    @Test
    fun `계약 위반 이벤트는 422 로 거부한다`() = testApplication {
        application { module() }

        val response = client.post(ApiPaths.EVENTS) {
            contentType(ContentType.Application.Json)
            setBody(FallEventJson.encodeToString(sampleEvent(riskScore = 1.4)))
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        val error = FallEventJson.decodeFromString<ApiError>(response.bodyAsText())
        assertEquals("contract_violation", error.code)
        assertTrue(error.message.contains("risk_score"), error.message)
    }

    @Test
    fun `깨진 JSON 은 400 으로 거부한다`() = testApplication {
        application { module() }

        val response = client.post(ApiPaths.EVENTS) {
            contentType(ContentType.Application.Json)
            setBody("{not json")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `잘못된 enum 값은 무엇이 틀렸는지 알려준다`() = testApplication {
        application { module() }

        val response = client.post(ApiPaths.EVENTS) {
            contentType(ContentType.Application.Json)
            // 모델팀이 실제로 하기 쉬운 실수 — 소문자여야 한다.
            setBody(
                FallEventJson.encodeToString(sampleEvent())
                    .replace("\"present\"", "\"PRESENT\"")
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val message = FallEventJson.decodeFromString<ApiError>(response.bodyAsText()).message
        // "Failed to convert request body to class ..." 로는 아무것도 고칠 수 없다.
        assertTrue(message.contains("presence_state"), "어느 필드가 틀렸는지 나와야 한다: $message")
        assertTrue(message.contains("PRESENT"), "잘못된 값이 나와야 한다: $message")
    }

    @Test
    fun `타임존 없는 detected_at 은 422 로 막는다`() = testApplication {
        application { module() }

        val response = client.post(ApiPaths.EVENTS) {
            contentType(ContentType.Application.Json)
            setBody(
                FallEventJson.encodeToString(sampleEvent())
                    .replace("2026-08-05T10:30:00+09:00", "2026-08-05T10:30:00")
            )
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertTrue(
            FallEventJson.decodeFromString<ApiError>(response.bodyAsText())
                .message.contains("detected_at")
        )
    }

    @Test
    fun `확인 결과를 기록한다`() = testApplication {
        application { module() }

        val response = client.post(ApiPaths.confirmation("trial-01:7")) {
            contentType(ContentType.Application.Json)
            setBody(FallEventJson.encodeToString(report("trial-01:7", Confirmation.HELP_NEEDED)))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val envelope = FallEventJson.decodeFromString<EventEnvelope>(response.bodyAsText())
        assertEquals(Confirmation.HELP_NEEDED, envelope.confirmation)
        assertEquals("2026-08-05T10:31:00+09:00", envelope.confirmedAt)
    }

    @Test
    fun `확인 결과 재전송은 멱등이다`() = testApplication {
        application { module() }

        suspend fun send() = client.post(ApiPaths.confirmation("trial-01:7")) {
            contentType(ContentType.Application.Json)
            setBody(FallEventJson.encodeToString(report("trial-01:7", Confirmation.NORMAL)))
        }

        val first = FallEventJson.decodeFromString<EventEnvelope>(send().bodyAsText())
        val second = FallEventJson.decodeFromString<EventEnvelope>(send().bodyAsText())

        // 앱은 네트워크 실패 시 같은 보고를 다시 보낸다. 결과가 달라지면 안 된다.
        assertEquals(first, second)
    }

    @Test
    fun `모르는 window_id 확인은 404`() = testApplication {
        application { module() }

        val response = client.post(ApiPaths.confirmation("없는아이디")) {
            contentType(ContentType.Application.Json)
            setBody(FallEventJson.encodeToString(report("없는아이디", Confirmation.NORMAL)))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(
            "unknown_window_id",
            FallEventJson.decodeFromString<ApiError>(response.bodyAsText()).code,
        )
    }

    @Test
    fun `경로와 본문의 window_id 가 다르면 400`() = testApplication {
        application { module() }

        val response = client.post(ApiPaths.confirmation("trial-01:7")) {
            contentType(ContentType.Application.Json)
            setBody(FallEventJson.encodeToString(report("trial-01:3", Confirmation.NORMAL)))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            "window_id_mismatch",
            FallEventJson.decodeFromString<ApiError>(response.bodyAsText()).code,
        )
    }

    @Test
    fun `WebSocket 은 hello 후 새 이벤트를 밀어 준다`() = testApplication {
        application { module() }
        val wsClient = createClient { install(ClientWebSockets) }

        wsClient.webSocket(ApiPaths.STREAM) {
            val hello = receiveMessage()
            assertTrue(hello is StreamMessage.Hello, "첫 프레임은 hello 여야 한다: $hello")
            assertEquals(4, hello.eventCount)

            client.post(ApiPaths.EVENTS) {
                contentType(ContentType.Application.Json)
                setBody(FallEventJson.encodeToString(sampleEvent("pushed:1")))
            }

            val pushed = receiveMessage()
            assertTrue(pushed is StreamMessage.NewEvent, "새 이벤트 프레임이 와야 한다: $pushed")
            assertEquals("pushed:1", pushed.envelope.windowId)
            assertNull(pushed.envelope.confirmation)
        }
    }

    @Test
    fun `WebSocket 은 확인 상태 변경도 밀어 준다`() = testApplication {
        application { module() }
        val wsClient = createClient { install(ClientWebSockets) }

        wsClient.webSocket(ApiPaths.STREAM) {
            receiveMessage() // hello

            client.post(ApiPaths.confirmation("trial-01:7")) {
                contentType(ContentType.Application.Json)
                setBody(FallEventJson.encodeToString(report("trial-01:7", Confirmation.NORMAL)))
            }

            val updated = receiveMessage()
            assertTrue(updated is StreamMessage.ConfirmationUpdated, "확인 프레임이 와야 한다: $updated")
            assertEquals(Confirmation.NORMAL, updated.envelope.confirmation)
        }
    }
}

private suspend fun io.ktor.websocket.WebSocketSession.receiveMessage(): StreamMessage =
    withTimeout(5_000) {
        val frame = incoming.receive() as Frame.Text
        FallEventJson.decodeFromString(StreamMessage.serializer(), frame.readText())
    }
