package com.carnation.fallalert

import com.carnation.fallalert.model.Evidence
import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.model.PresenceState
import com.carnation.fallalert.util.detectedAtInstant
import com.carnation.fallalert.util.formatSeconds
import com.carnation.fallalert.util.toPercent
import com.carnation.fallalert.util.toRelativeKorean
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 표시 로직만 검증한다. 계약 파싱·검증은 `:contract` 모듈의 ContractTest 가 담당한다.
 */
class FormattingTest {

    private fun eventAt(detectedAt: String) = FallEvent(
        schemaVersion = "1.0",
        eventType = "fall_suspected",
        windowId = "trial-01:7",
        detectedAt = detectedAt,
        roomId = "living-room",
        riskScore = 0.9,
        evidence = Evidence("fall_like", 0.91, PresenceState.PRESENT, 0.95, 4.0),
    )

    @Test
    fun `detected_at 은 오프셋을 포함해 파싱된다`() {
        // 2026-08-05T10:30+09:00 == 2026-08-05T01:30Z
        assertEquals(
            Instant.parse("2026-08-05T01:30:00Z"),
            eventAt("2026-08-05T10:30:00+09:00").detectedAtInstant(),
        )
    }

    @Test
    fun `잘못된 detected_at 은 화면을 죽이지 않는다`() {
        assertEquals(Instant.EPOCH, eventAt("어제 오전").detectedAtInstant())
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
    fun `회복 없음 초는 소수점 한 자리까지만 보여 준다`() {
        // 서버가 실제로 보낸 값. 그대로 찍으면 "10.20427782593417초"가 화면에 나온다.
        assertEquals("10.2", formatSeconds(10.20427782593417))
        assertEquals("4", formatSeconds(4.0))
        assertEquals("1.5", formatSeconds(1.5))
        assertEquals("9", formatSeconds(8.98))
        assertEquals("0.1", formatSeconds(0.05))
    }

    @Test
    fun `미래 시각은 방금으로 표시한다`() {
        val now = Instant.parse("2026-08-05T02:00:00Z")
        assertEquals("방금", now.plus(Duration.ofMinutes(2)).toRelativeKorean(now))
    }
}
