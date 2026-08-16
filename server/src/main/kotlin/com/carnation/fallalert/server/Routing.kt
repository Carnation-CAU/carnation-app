package com.carnation.fallalert.server

import com.carnation.fallalert.api.ApiError
import com.carnation.fallalert.api.ApiPaths
import com.carnation.fallalert.api.ConfirmationReport
import com.carnation.fallalert.api.EventsResponse
import com.carnation.fallalert.api.StreamMessage
import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.model.FallEventJson
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.Serializable

@Serializable
private data class HealthResponse(
    val status: String,
    val version: String,
    val events: Int,
    val time: String,
)

fun Application.registerRoutes(store: EventStore) = routing {

    get(ApiPaths.HEALTH) {
        call.respond(
            HealthResponse(
                status = "ok",
                version = ApiPaths.VERSION,
                events = store.count(),
                time = nowIso(),
            )
        )
    }

    /** 앱 시작·새로고침 시 전체 목록. 이력이 적어서 페이지네이션은 아직 두지 않았다. */
    get(ApiPaths.EVENTS) {
        call.respond(EventsResponse(events = store.all(), serverTime = nowIso()))
    }

    /**
     * 모델·CSI 파이프라인 → 서버. 팀이 이 경로로 이벤트를 밀어 넣으면
     * 연결된 앱 전부에 WebSocket 으로 즉시 퍼진다.
     */
    post(ApiPaths.EVENTS) {
        val event = call.receive<FallEvent>()
        val envelope = store.add(event)
        call.application.log.info("이벤트 등록: {} ({})", envelope.windowId, envelope.event.roomId)
        call.respond(HttpStatusCode.Created, envelope)
    }

    /**
     * 보호자의 확인 결과. 멱등이므로 앱이 재시도해도 같은 결과를 준다.
     * 경로의 window_id 와 본문이 다르면 400 — 앱 버그를 조용히 삼키지 않는다.
     */
    post("${ApiPaths.EVENTS}/{windowId}/confirmation") {
        val pathWindowId = call.parameters["windowId"].orEmpty()
        val report = call.receive<ConfirmationReport>()

        if (report.windowId != pathWindowId) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("window_id_mismatch", "경로($pathWindowId)와 본문(${report.windowId})의 window_id 가 다릅니다."),
            )
            return@post
        }

        val envelope = store.confirm(report)
        if (envelope == null) {
            call.respond(
                HttpStatusCode.NotFound,
                ApiError("unknown_window_id", "모르는 window_id 입니다: $pathWindowId"),
            )
            return@post
        }

        call.application.log.info("확인 결과 수신: {} = {}", envelope.windowId, envelope.confirmation)
        call.respond(HttpStatusCode.OK, envelope)
    }

    /** 개발용: curl 한 번으로 새 이벤트를 만들어 연결된 앱에 밀어 넣는다. */
    post(ApiPaths.SIMULATE) {
        val envelope = store.add(MockEventFactory.random())
        call.application.log.info("모의 이벤트 생성: {}", envelope.windowId)
        call.respond(HttpStatusCode.Created, envelope)
    }

    /**
     * 실시간 스트림. 연결 즉시 hello 를 보내 앱이 "연결됨"을 판단할 수 있게 한다.
     * 앱은 hello 를 받으면 REST 로 전체 목록을 한 번 다시 읽어 끊긴 동안의 공백을 메운다.
     */
    webSocket(ApiPaths.STREAM) {
        val hello = StreamMessage.Hello(serverTime = nowIso(), eventCount = store.count())
        sendMessage(hello)
        val log = call.application.log
        log.info("WebSocket 연결됨 (이벤트 {}건)", hello.eventCount)

        try {
            store.stream.collect { message -> sendMessage(message) }
        } catch (e: ClosedReceiveChannelException) {
            log.info("WebSocket 정상 종료")
        } finally {
            log.info("WebSocket 해제")
        }
    }
}

private suspend fun io.ktor.websocket.WebSocketSession.sendMessage(message: StreamMessage) {
    send(Frame.Text(FallEventJson.encodeToString(StreamMessage.serializer(), message)))
}
