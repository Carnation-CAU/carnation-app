package com.carnation.fallalert

import com.carnation.fallalert.push.PushPayloadParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * FCM 붙이기 전에 페이로드 처리 경로만 먼저 검증한다.
 * (android.util.Log 호출 때문에 이 테스트는 실패 경로에서 unitTests.returnDefaultValues 가 필요할 수 있다.)
 */
class PushPayloadParserTest {

    private val payload = """
        {"schema_version":"1.0","event_type":"fall_suspected","window_id":"live:1",
         "detected_at":"2026-08-05T10:30:00+09:00","room_id":"bedroom","risk_score":0.8,
         "evidence":{"motion_label":"fall_like","motion_confidence":0.7,
         "presence_state":"present","presence_probability":0.9}}
    """.trimIndent()

    @Test
    fun `payload 키의 계약 JSON 을 이벤트로 만든다`() {
        val event = PushPayloadParser.parse(mapOf("payload" to payload))
        assertEquals("live:1", event?.windowId)
        assertEquals("bedroom", event?.roomId)
        assertNull(event?.evidence?.noRecoverySec)
    }

    @Test
    fun `payload 키가 없으면 무시한다`() {
        assertNull(PushPayloadParser.parse(mapOf("title" to "낙상 의심")))
    }

    @Test
    fun `깨진 JSON 은 무시한다`() {
        assertNull(PushPayloadParser.parse(mapOf("payload" to "{not json")))
    }
}
