package com.carnation.fallalert.data

import android.content.Context
import android.util.Log
import com.carnation.fallalert.model.Evidence
import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.model.PresenceState
import kotlinx.serialization.SerializationException

/**
 * 이벤트 공급자.
 *
 * TODO(팀 확정 후): 서버 연동 시 이 인터페이스의 구현만
 * REST/WebSocket/FCM 수신 버전으로 교체하면 화면·모델은 그대로 둔다.
 */
fun interface FallEventSource {
    suspend fun load(): List<FallEvent>
}

/** assets/mock_events.json 을 읽는 개발용 구현. */
class AssetsFallEventSource(
    private val context: Context,
    private val assetName: String = "mock_events.json",
) : FallEventSource {

    override suspend fun load(): List<FallEvent> = try {
        val raw = context.assets.open(assetName).bufferedReader().use { it.readText() }
        FallEventJson.decodeFromString<List<FallEvent>>(raw).filter { event ->
            event.isContractValid().also { valid ->
                if (!valid) Log.w(TAG, "계약 위반 이벤트 무시: ${event.windowId}")
            }
        }
    } catch (e: SerializationException) {
        Log.e(TAG, "목업 파싱 실패 — 하드코딩 목업으로 대체", e)
        HARDCODED_FALLBACK
    } catch (e: java.io.IOException) {
        Log.e(TAG, "목업 읽기 실패 — 하드코딩 목업으로 대체", e)
        HARDCODED_FALLBACK
    }

    private companion object {
        const val TAG = "AssetsFallEventSource"

        val HARDCODED_FALLBACK = listOf(
            FallEvent(
                schemaVersion = "1.0",
                eventType = "fall_suspected",
                windowId = "trial-01:7",
                detectedAt = "2026-08-05T10:30:00+09:00",
                roomId = "living-room",
                riskScore = 0.9,
                evidence = Evidence("fall_like", 0.91, PresenceState.PRESENT, 0.95, 4.0),
            ),
        )
    }
}
