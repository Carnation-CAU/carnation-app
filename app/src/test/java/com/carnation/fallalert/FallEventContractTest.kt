package com.carnation.fallalert

import com.carnation.fallalert.data.FallEventJson
import com.carnation.fallalert.data.isContractValid
import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.model.PresenceState
import com.carnation.fallalert.util.detectedAtInstant
import com.carnation.fallalert.util.toPercent
import com.carnation.fallalert.util.toRelativeKorean
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FallEventContractTest {

    private val contractSample = """
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
    fun `계약 예시를 그대로 파싱한다`() {
        val event = FallEventJson.decodeFromString<FallEvent>(contractSample)

        assertEquals("trial-01:7", event.windowId)
        assertEquals("living-room", event.roomId)
        assertEquals(0.9, event.riskScore, 1e-9)
        assertEquals(PresenceState.PRESENT, event.evidence.presenceState)
        assertEquals(4.0, event.evidence.noRecoverySec!!, 1e-9)
        assertTrue(event.isContractValid())
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
    }

    @Test
    fun `알 수 없는 필드가 추가돼도 죽지 않는다`() {
        val withExtra = contractSample.replaceFirst("{", "{\n  \"server_note\": \"v2 실험\",")
        val event = FallEventJson.decodeFromString<FallEvent>(withExtra)
        assertEquals("trial-01:7", event.windowId)
    }

    @Test
    fun `detected_at 은 오프셋을 포함해 파싱된다`() {
        val event = FallEventJson.decodeFromString<FallEvent>(contractSample)
        // 2026-08-05T10:30+09:00 == 2026-08-05T01:30Z
        assertEquals(Instant.parse("2026-08-05T01:30:00Z"), event.detectedAtInstant())
    }

    @Test
    fun `잘못된 detected_at 은 화면을 죽이지 않는다`() {
        val broken = contractSample.replace("2026-08-05T10:30:00+09:00", "어제 오전")
        val event = FallEventJson.decodeFromString<FallEvent>(broken)
        assertEquals(Instant.EPOCH, event.detectedAtInstant())
    }

    @Test
    fun `risk_score 는 정수 퍼센트로 변환된다`() {
        assertEquals(90, 0.9.toPercent())
        assertEquals(91, 0.905.toPercent())
        assertEquals(0, 0.0.toPercent())
        assertEquals(100, 1.0.toPercent())
    }

    @Test
    fun `상대 시각 포맷`() {
        val now = Instant.parse("2026-08-05T02:00:00Z")
        assertEquals("방금", now.minus(Duration.ofSeconds(20)).toRelativeKorean(now))
        assertEquals("30분 전", now.minus(Duration.ofMinutes(30)).toRelativeKorean(now))
        assertEquals("5시간 전", now.minus(Duration.ofHours(5)).toRelativeKorean(now))
        assertEquals("3일 전", now.minus(Duration.ofDays(3)).toRelativeKorean(now))
    }

    @Test
    fun `계약 위반 이벤트는 걸러진다`() {
        val wrongVersion = FallEventJson.decodeFromString<FallEvent>(
            contractSample.replace("\"1.0\"", "\"2.0\"")
        )
        assertTrue(!wrongVersion.isContractValid())

        val outOfRange = FallEventJson.decodeFromString<FallEvent>(
            contractSample.replace("\"risk_score\": 0.9", "\"risk_score\": 1.4")
        )
        assertTrue(!outOfRange.isContractValid())
    }
}
