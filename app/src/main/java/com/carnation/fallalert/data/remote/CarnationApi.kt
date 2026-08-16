package com.carnation.fallalert.data.remote

import com.carnation.fallalert.api.ApiPaths
import com.carnation.fallalert.api.ConfirmationReport
import com.carnation.fallalert.api.EventEnvelope
import com.carnation.fallalert.api.EventsResponse
import com.carnation.fallalert.model.FallEventJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

/**
 * 확인 결과 전송 결과. **재시도해야 하는 실패와 아닌 실패를 구분한다.**
 * 이 구분이 없으면 아웃박스가 영원히 안 될 요청을 계속 물고 있게 된다.
 */
sealed interface ConfirmResult {
    data class Success(val envelope: EventEnvelope) : ConfirmResult

    /** 네트워크 오류·5xx 등. 큐에 남겨 두고 나중에 다시 보낸다. */
    data class Retryable(val reason: String) : ConfirmResult

    /** 404·400 등. 다시 보내도 결과가 같으므로 큐에서 버린다. */
    data class Permanent(val reason: String) : ConfirmResult
}

class CarnationApi(
    private val baseUrl: String,
    val client: HttpClient = defaultClient(),
) {

    /** 전체 이벤트 목록. 실패하면 예외를 던진다 — 호출부가 오프라인 상태를 표시한다. */
    suspend fun fetchEvents(): List<EventEnvelope> {
        val response: EventsResponse = client.get(baseUrl + ApiPaths.EVENTS).body()
        return response.events
    }

    suspend fun health(): Boolean = try {
        client.get(baseUrl + ApiPaths.HEALTH).status == HttpStatusCode.OK
    } catch (e: Exception) {
        false
    }

    /** 개발용: 서버에 모의 이벤트를 하나 만들라고 시킨다. 진짜 수신 경로를 그대로 탄다. */
    suspend fun simulateEvent(): Boolean = try {
        client.post(baseUrl + ApiPaths.SIMULATE).status == HttpStatusCode.Created
    } catch (e: Exception) {
        false
    }

    suspend fun sendConfirmation(report: ConfirmationReport): ConfirmResult {
        val response: HttpResponse = try {
            client.post(baseUrl + ApiPaths.confirmation(report.windowId)) {
                contentType(ContentType.Application.Json)
                setBody(report)
            }
        } catch (e: Exception) {
            // 연결 실패·타임아웃 — 서버가 받았는지 알 수 없다. API 가 멱등이라 재시도해도 안전하다.
            return ConfirmResult.Retryable(e.message ?: e::class.simpleName.orEmpty())
        }

        return when {
            response.status.isSuccess() -> ConfirmResult.Success(response.body())

            // 서버가 모르는 window_id, 또는 앱이 잘못 만든 요청. 재시도해도 같다.
            response.status == HttpStatusCode.NotFound ->
                ConfirmResult.Permanent("서버가 모르는 이벤트입니다 (404)")

            response.status.value in 400..499 ->
                ConfirmResult.Permanent("요청이 거부됐습니다 (${response.status.value})")

            else -> ConfirmResult.Retryable("서버 오류 (${response.status.value})")
        }
    }

    fun webSocketUrl(): String =
        baseUrl.replaceFirst("http://", "ws://").replaceFirst("https://", "wss://") + ApiPaths.STREAM

    companion object {
        fun defaultClient(): HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) { json(FallEventJson) }
            install(WebSockets)
            install(HttpTimeout) {
                // 보호자가 버튼을 누르고 무한정 기다리게 두지 않는다.
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 5_000
            }
            // 4xx/5xx 를 예외로 바꾸지 않는다 — sendConfirmation 이 상태 코드로 분기해야 한다.
            expectSuccess = false
        }
    }
}

private fun HttpStatusCode.isSuccess(): Boolean = value in 200..299
