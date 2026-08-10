package com.carnation.fallalert.util

import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.model.PresenceState
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.roundToInt

private val KST: ZoneId = ZoneId.of("Asia/Seoul")

private val ABSOLUTE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M월 d일 a h:mm", Locale.KOREAN)

/**
 * `detected_at`(ISO-8601, 타임존 포함) 파싱.
 * 계약 위반 문자열이 오면 화면을 죽이지 않고 epoch 를 돌려준다.
 */
fun FallEvent.detectedAtInstant(): Instant = try {
    OffsetDateTime.parse(detectedAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
} catch (e: DateTimeParseException) {
    Instant.EPOCH
}

/** "방금", "10분 전", "3일 전" — 목록 카드용. */
fun Instant.toRelativeKorean(now: Instant = Instant.now()): String {
    val elapsed = Duration.between(this, now)
    if (elapsed.isNegative) return "방금"
    val minutes = elapsed.toMinutes()
    return when {
        minutes < 1 -> "방금"
        minutes < 60 -> "${minutes}분 전"
        elapsed.toHours() < 24 -> "${elapsed.toHours()}시간 전"
        elapsed.toDays() < 7 -> "${elapsed.toDays()}일 전"
        else -> toAbsoluteKorean()
    }
}

/** "8월 5일 오전 10:30" — 상세 화면용. */
fun Instant.toAbsoluteKorean(): String = ABSOLUTE_FORMAT.format(atZone(KST))

/** 0.0~1.0 확률 → 정수 %. */
fun Double.toPercent(): Int = (this * 100).roundToInt()

/** room_id 는 계약상 임의 문자열이라, 아는 값만 한글로 바꾸고 나머지는 그대로 보여준다. */
fun roomLabel(roomId: String): String = when (roomId) {
    "living-room" -> "거실"
    "bedroom" -> "침실"
    "bathroom" -> "욕실"
    "kitchen" -> "주방"
    "entrance" -> "현관"
    else -> roomId
}

fun motionLabel(motionLabel: String): String = when (motionLabel) {
    "fall_like" -> "넘어짐과 유사한 움직임"
    "sudden_drop" -> "갑작스러운 하강"
    "normal" -> "일반적인 움직임"
    else -> motionLabel
}

fun presenceLabel(state: PresenceState): String = when (state) {
    PresenceState.PRESENT -> "사람 있음"
    PresenceState.ABSENT -> "사람 없음"
    PresenceState.UNKNOWN -> "알 수 없음"
}
