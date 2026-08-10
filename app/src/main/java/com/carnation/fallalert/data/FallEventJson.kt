package com.carnation.fallalert.data

import com.carnation.fallalert.model.FallEvent
import kotlinx.serialization.json.Json

/**
 * 계약 JSON 파서. assets 목업이든 FCM 페이로드든 같은 인스턴스를 쓴다.
 *
 * - ignoreUnknownKeys: 서버가 계약에 필드를 추가해도 앱이 죽지 않도록.
 * - explicitNulls=false: `no_recovery_sec` 같은 선택 필드가 빠져도 정상 파싱.
 */
val FallEventJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** 계약 위반 이벤트를 화면까지 흘리지 않기 위한 최소 검증. */
fun FallEvent.isContractValid(): Boolean =
    schemaVersion == "1.0" &&
        eventType == "fall_suspected" &&
        windowId.isNotBlank() &&
        roomId.isNotBlank() &&
        riskScore in 0.0..1.0
