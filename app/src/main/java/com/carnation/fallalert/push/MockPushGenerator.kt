package com.carnation.fallalert.push

import com.carnation.fallalert.model.Evidence
import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.model.PresenceState
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * 서버·FCM 없이 "새 낙상 의심 이벤트 도착"을 재현하는 개발용 생성기.
 * 명세 6장 7번(FCM 뼈대는 가짜 알림으로 테스트)에 해당한다.
 */
class MockPushGenerator(private val random: Random = Random.Default) {

    private var counter = 0

    fun next(): FallEvent {
        counter += 1
        val now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"))
        return FallEvent(
            schemaVersion = "1.0",
            eventType = "fall_suspected",
            windowId = "live-${now.toEpochSecond()}:$counter",
            detectedAt = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            roomId = ROOMS.random(random),
            riskScore = random.nextDouble(0.55, 0.99),
            evidence = Evidence(
                motionLabel = "fall_like",
                motionConfidence = random.nextDouble(0.6, 0.98),
                presenceState = PresenceState.entries.random(random),
                presenceProbability = random.nextDouble(0.3, 0.99),
                // 선택 필드가 없는 경우도 화면이 견디는지 확인하려고 절반은 null 로 둔다.
                noRecoverySec = if (random.nextBoolean()) random.nextDouble(1.0, 12.0) else null,
            ),
        )
    }

    private companion object {
        val ROOMS = listOf("living-room", "bedroom", "bathroom", "kitchen", "entrance")
    }
}
