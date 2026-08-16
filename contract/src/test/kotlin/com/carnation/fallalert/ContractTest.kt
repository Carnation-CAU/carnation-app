package com.carnation.fallalert

import com.carnation.fallalert.api.ConfirmationReport
import com.carnation.fallalert.api.EventEnvelope
import com.carnation.fallalert.api.StreamMessage
import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.model.FallEventJson
import com.carnation.fallalert.model.PresenceState
import com.carnation.fallalert.model.contractViolation
import com.carnation.fallalert.model.isContractValid
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContractTest {

    private val sample = """
        {
          "schema_version": "1.0",
          "event_type": "fall_suspected",
          "window_id": "trial-01:7",
          "detected_at": "2026-08-05T10:30:00+09:00",
          "room_id": "living-room",
          "risk_score": 0.9,
          "evidence": {
            "motion_label": "fall_like",
            "motion_confidence": 0.91,
            "presence_state": "present",
            "presence_probability": 0.95,
            "no_recovery_sec": 4.0
          }
        }
    """.trimIndent()

    @Test
    fun `명세 1장 예시를 그대로 파싱한다`() {
        val event = FallEventJson.decodeFromString<FallEvent>(sample)

        assertEquals("trial-01:7", event.windowId)
        assertEquals("living-room", event.roomId)
        assertEquals(0.9, event.riskScore, 1e-9)
        assertEquals(PresenceState.PRESENT, event.evidence.presenceState)
        assertEquals(4.0, event.evidence.noRecoverySec!!, 1e-9)
        assertNull(event.contractViolation())
    }

    @Test
    fun `no_recovery_sec 가 없어도 파싱된다`() {
        val event = FallEventJson.decodeFromString<FallEvent>(
            """
            {
              "schema_version": "1.0", "event_type": "fall_suspected",
              "window_id": "w:1", "detected_at": "2026-08-05T10:30:00+09:00",
              "room_id": "bathroom", "risk_score": 0.7,
              "evidence": {
                "motion_label": "fall_like", "motion_confidence": 0.6,
                "presence_state": "unknown", "presence_probability": 0.4
              }
            }
            """.trimIndent()
        )
        assertNull(event.evidence.noRecoverySec)
        assertTrue(event.isContractValid())
    }

    @Test
    fun `알 수 없는 필드가 추가돼도 죽지 않는다`() {
        val withExtra = sample.replaceFirst("{", "{\n  \"server_note\": \"v2 실험\",")
        assertEquals("trial-01:7", FallEventJson.decodeFromString<FallEvent>(withExtra).windowId)
    }

    @Test
    fun `직렬화 왕복 후에도 동일하다`() {
        val event = FallEventJson.decodeFromString<FallEvent>(sample)
        val roundTrip = FallEventJson.decodeFromString<FallEvent>(FallEventJson.encodeToString(event))
        assertEquals(event, roundTrip)
    }

    @Test
    fun `선택 필드가 없으면 인코딩 결과에도 나타나지 않는다`() {
        val event = FallEventJson.decodeFromString<FallEvent>(sample)
            .let { it.copy(evidence = it.evidence.copy(noRecoverySec = null)) }
        assertFalse(FallEventJson.encodeToString(event).contains("no_recovery_sec"))
    }

    @Test
    fun `계약 위반은 이유와 함께 걸러진다`() {
        fun violationOf(json: String) =
            FallEventJson.decodeFromString<FallEvent>(json).contractViolation()

        assertTrue(violationOf(sample.replace("\"1.0\"", "\"2.0\""))!!.contains("schema_version"))
        assertTrue(violationOf(sample.replace("0.9,", "1.4,"))!!.contains("risk_score"))
        assertTrue(violationOf(sample.replace("\"living-room\"", "\"\""))!!.contains("room_id"))
        assertTrue(violationOf(sample.replace("\"no_recovery_sec\": 4.0", "\"no_recovery_sec\": -1.0"))!!
            .contains("no_recovery_sec"))
    }

    @Test
    fun `타임존 없는 detected_at 은 거부한다`() {
        // 모델·서버 쪽에서 제일 흔한 실수. 통과시키면 앱이 epoch 로 떨어뜨려
        // 방금 난 사고가 목록 맨 아래로 밀린다 — 조용히 망가지는 종류의 오류다.
        fun violationOf(value: String) = FallEventJson
            .decodeFromString<FallEvent>(sample.replace("2026-08-05T10:30:00+09:00", value))
            .contractViolation()

        assertTrue(violationOf("2026-08-05T10:30:00")!!.contains("detected_at"))
        assertTrue(violationOf("2026-08-05 10:30:00+09:00")!!.contains("detected_at"))
        assertTrue(violationOf("1786790748")!!.contains("detected_at"))
        assertTrue(violationOf("")!!.contains("detected_at"))

        // 오프셋만 붙어 있으면 어떤 타임존이든 받는다.
        assertNull(violationOf("2026-08-05T01:30:00Z"))
        assertNull(violationOf("2026-08-04T21:30:00-05:00"))
    }

    @Test
    fun `확인 결과는 snake_case 로 나간다`() {
        val report = ConfirmationReport(
            windowId = "trial-01:7",
            confirmation = Confirmation.HELP_NEEDED,
            reportedAt = "2026-08-05T10:31:00+09:00",
            clientId = "device-a",
        )
        val json = FallEventJson.encodeToString(report)
        assertTrue(json.contains("\"help_needed\""), json)
        assertTrue(json.contains("\"window_id\""), json)
        assertEquals(report, FallEventJson.decodeFromString<ConfirmationReport>(json))
    }

    @Test
    fun `스트림 메시지는 type 키로 판별된다`() {
        val event = FallEventJson.decodeFromString<FallEvent>(sample)
        val message: StreamMessage = StreamMessage.NewEvent(EventEnvelope(event))
        val json = FallEventJson.encodeToString(message)

        assertTrue(json.contains("\"type\":\"event\""), json)
        assertEquals(message, FallEventJson.decodeFromString<StreamMessage>(json))
    }
}
