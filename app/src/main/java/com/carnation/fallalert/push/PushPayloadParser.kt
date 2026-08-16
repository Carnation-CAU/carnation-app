package com.carnation.fallalert.push

import android.util.Log
import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.model.FallEventJson
import com.carnation.fallalert.model.contractViolation
import kotlinx.serialization.SerializationException

/**
 * FCM data 페이로드 → [FallEvent].
 *
 * Firebase SDK 에 의존하지 않는 순수 함수라, 서버 연동 전에도 단위 테스트로 검증할 수 있다.
 * 두 가지 형태를 모두 받아준다:
 *  1. `{"payload": "<계약 JSON 문자열 전체>"}`
 *  2. 계약 필드가 평평하게 펼쳐진 형태는 지원하지 않음 → 팀에 1번 형태를 요청한다. (명세 7장)
 */
object PushPayloadParser {

    private const val TAG = "PushPayloadParser"
    const val PAYLOAD_KEY = "payload"

    fun parse(data: Map<String, String>): FallEvent? {
        val raw = data[PAYLOAD_KEY] ?: run {
            Log.w(TAG, "'$PAYLOAD_KEY' 키가 없는 푸시 무시: ${data.keys}")
            return null
        }
        return try {
            FallEventJson.decodeFromString<FallEvent>(raw).takeIf { event ->
                val violation = event.contractViolation()
                if (violation != null) Log.w(TAG, "계약 위반 푸시 무시 (${event.windowId}): $violation")
                violation == null
            }
        } catch (e: SerializationException) {
            Log.e(TAG, "푸시 페이로드 파싱 실패", e)
            null
        }
    }
}
