package com.carnation.fallalert.server

import com.carnation.fallalert.api.ApiError
import com.carnation.fallalert.model.FallEventJson
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import org.slf4j.event.Level

private const val DEFAULT_PORT = 8080

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: DEFAULT_PORT

    // 0.0.0.0 이어야 에뮬레이터(10.0.2.2)와 같은 Wi-Fi 의 실기기가 붙을 수 있다.
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    val seed = MockEventFactory.seed()
    val store = EventStore(seed = seed)

    install(ContentNegotiation) {
        json(FallEventJson)
    }

    install(WebSockets) {
        // 모바일 네트워크에서 조용히 끊긴 연결을 서버가 먼저 알아채도록 ping 을 보낸다.
        pingPeriod = 15.seconds
        timeout = 30.seconds
    }

    install(CallLogging) {
        level = Level.INFO
        // WebSocket ping 까지 찍히면 로그가 시끄러워진다.
        filter { call -> !call.request.local.uri.startsWith("/api/v1/stream") }
    }

    install(CORS) {
        anyHost() // 로컬 개발 전용. 배포 시 반드시 좁힐 것.
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowMethod(io.ktor.http.HttpMethod.Post)
    }

    install(StatusPages) {
        exception<ContractViolationException> { call, cause ->
            call.application.log.warn("계약 위반 이벤트 거부: {}", cause.reason)
            call.respond(HttpStatusCode.UnprocessableEntity, ApiError("contract_violation", cause.reason))
        }
        exception<SerializationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("malformed_json", cause.message ?: "본문을 파싱할 수 없습니다."),
            )
        }
        exception<BadRequestException> { call, cause ->
            // Ktor 가 파싱 실패를 BadRequestException 으로 감싸면 "Failed to convert request
            // body to class ..." 라는 쓸모없는 문구만 남는다. 안쪽 직렬화 예외를 꺼내야
            // "presence_state 에 'PRESENT' 라는 값이 없다, 위치는 $.evidence.presence_state"
            // 같은 실제로 고칠 수 있는 정보가 나온다.
            val detail = cause.rootSerializationMessage()
            call.application.log.warn("잘못된 요청: {}", detail ?: cause.message)
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("malformed_json", detail ?: cause.message ?: "잘못된 요청입니다."),
            )
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("처리되지 않은 오류", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiError("internal_error", "서버 오류"))
        }
    }

    registerRoutes(store)
    startSimulatorIfEnabled(store)

    log.info("낙상 의심 알림 서버 시작 — 시드 이벤트 {}건", seed.size)
}

/** 예외 체인을 따라가며 직렬화 실패 메시지를 찾는다. 순환 참조에 대비해 깊이를 제한한다. */
private fun Throwable.rootSerializationMessage(): String? =
    generateSequence(this as Throwable?) { it.cause.takeIf { c -> c !== it } }
        .take(10)
        .firstOrNull { it is SerializationException }
        ?.message

/**
 * `SIMULATE_INTERVAL_SEC=20` 처럼 주면 주기적으로 이벤트를 만든다.
 * 기본값은 꺼짐 — 켜두면 테스트 중에 목록이 계속 늘어나 방해가 된다.
 */
private fun Application.startSimulatorIfEnabled(store: EventStore) {
    val interval = System.getenv("SIMULATE_INTERVAL_SEC")?.toLongOrNull() ?: return
    if (interval <= 0) return

    log.info("모의 이벤트 생성기 켜짐: {}초마다", interval)
    launch {
        while (true) {
            delay(interval * 1000)
            runCatching { store.add(MockEventFactory.random()) }
                .onFailure { log.error("모의 이벤트 생성 실패", it) }
        }
    }
}
