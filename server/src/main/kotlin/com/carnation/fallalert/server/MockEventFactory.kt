package com.carnation.fallalert.server

import com.carnation.fallalert.model.Evidence
import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.model.FallEventJson
import com.carnation.fallalert.model.PresenceState
import com.carnation.fallalert.model.contractViolation
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import org.slf4j.LoggerFactory

/**
 * 모델·CSI 파이프라인이 붙기 전까지 이벤트를 만들어 내는 자리.
 * 연동되면 이 파일을 지우고 `POST /api/v1/events` 로 실제 이벤트가 들어온다.
 */
object MockEventFactory {

    private val logger = LoggerFactory.getLogger(MockEventFactory::class.java)
    private val counter = AtomicInteger(0)
    private val rooms = listOf("living-room", "bedroom", "bathroom", "kitchen", "entrance")

    fun random(random: Random = Random.Default): FallEvent {
        val n = counter.incrementAndGet()
        return FallEvent(
            schemaVersion = "1.0",
            eventType = "fall_suspected",
            windowId = "live-${System.currentTimeMillis() / 1000}:$n",
            detectedAt = nowIso(),
            roomId = rooms.random(random),
            riskScore = random.nextDouble(0.55, 0.99),
            evidence = Evidence(
                motionLabel = "fall_like",
                motionConfidence = random.nextDouble(0.6, 0.98),
                presenceState = PresenceState.entries.random(random),
                presenceProbability = random.nextDouble(0.3, 0.99),
                // 절반은 선택 필드를 비운다 — 앱이 행 숨김을 제대로 하는지 실제로 확인하려고.
                // 값은 일부러 반올림하지 않는다. 계약상 정밀도 제한이 없으므로,
                // 앱이 어떤 자릿수가 와도 화면을 깨지 않는지 여기서 계속 압박한다.
                noRecoverySec = if (random.nextBoolean()) random.nextDouble(1.0, 12.0) else null,
            ),
        )
    }

    /** `resources/mock_events.json` 시드. 명세 5장 목업을 서버가 그대로 들고 있는다. */
    fun seed(): List<FallEvent> {
        val raw = MockEventFactory::class.java.getResourceAsStream("/mock_events.json")
            ?.bufferedReader()?.use { it.readText() }
            ?: run {
                logger.warn("mock_events.json 없음 — 빈 상태로 시작합니다.")
                return emptyList()
            }
        return FallEventJson.decodeFromString<List<FallEvent>>(raw).filter { event ->
            val violation = event.contractViolation()
            if (violation != null) logger.warn("시드 이벤트 제외 ({}): {}", event.windowId, violation)
            violation == null
        }
    }
}
