package com.carnation.fallalert.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.model.EventRecord
import com.carnation.fallalert.model.Evidence
import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.model.PresenceState
import com.carnation.fallalert.ui.theme.BgGray
import com.carnation.fallalert.ui.theme.CarnationTheme
import com.carnation.fallalert.ui.theme.DangerBg
import com.carnation.fallalert.ui.theme.DangerRed
import com.carnation.fallalert.ui.theme.Space
import com.carnation.fallalert.ui.theme.SuccessGreen
import com.carnation.fallalert.ui.theme.TextSecondary
import com.carnation.fallalert.util.detectedAtInstant
import com.carnation.fallalert.util.roomLabel
import com.carnation.fallalert.util.toPercent
import com.carnation.fallalert.util.toRelativeKorean

/**
 * 이벤트 카드. 홈 · 확인한 알림 · 전체 목록이 모두 이걸 쓴다.
 *
 * 미확인이면 카드 전체가 DangerBg 로 깔린다. 화면의 나머지가 흰/회색뿐이라
 * **이 카드만 색을 갖는다** — 그게 이 디자인에서 위험을 알리는 방식이고,
 * 그래서 다른 곳에 빨강을 쓰지 않는다.
 *
 * 확인이 끝난 카드는 흰 면 + 회색 텍스트로 가라앉는다. 위험도도 감춘다 — 판단이 끝났다.
 */
@Composable
fun EventCard(record: EventRecord, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val unconfirmed = record.isUnconfirmed
    val event = record.event

    AppCard(
        modifier = modifier.semantics {
            contentDescription = buildString {
                append(roomLabel(event.roomId))
                append("에서 낙상 의심, 위험도 ")
                append(event.riskScore.toPercent())
                append("퍼센트, ")
                append(event.detectedAtInstant().toRelativeKorean())
                append(if (unconfirmed) ", 아직 확인하지 않음" else ", 확인 완료")
            }
        },
        color = if (unconfirmed) DangerBg else MaterialTheme.colorScheme.surface,
        onClick = onClick,
        contentPadding = Space.xl,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (unconfirmed) {
                IconListRow(
                    icon = Icons.Filled.Warning,
                    iconTint = Color.White,
                    iconBackground = DangerRed,
                    title = roomLabel(event.roomId),
                    subtitle = event.detectedAtInstant().toRelativeKorean(),
                    titleColor = DangerRed,
                    trailing = {
                        Text(
                            text = "${event.riskScore.toPercent()}%",
                            style = MaterialTheme.typography.titleLarge,
                            color = DangerRed,
                        )
                    },
                )
            } else {
                IconListRow(
                    icon = Icons.Filled.Check,
                    iconTint = TextSecondary,
                    iconBackground = BgGray,
                    title = roomLabel(event.roomId),
                    subtitle = event.detectedAtInstant().toRelativeKorean(),
                    trailing = {
                        ConfirmationBadge(record.confirmation)
                    },
                )
            }
        }
    }
}

/** 확인 결과 배지. 정상은 초록, 도움은 빨강 — 이미 지난 일이라 배경은 아주 옅게. */
@Composable
fun ConfirmationBadge(confirmation: Confirmation?, modifier: Modifier = Modifier) {
    when (confirmation) {
        Confirmation.HELP_NEEDED -> StatusBadge(
            text = "도움 요청",
            contentColor = DangerRed,
            containerColor = DangerBg,
            modifier = modifier,
        )

        else -> StatusBadge(
            text = "정상",
            contentColor = SuccessGreen,
            containerColor = SuccessGreen.copy(alpha = 0.10f),
            modifier = modifier,
        )
    }
}

/** 프리뷰용 레코드 생성기. 화면 프리뷰들이 공유한다. */
internal fun previewEventRecord(
    windowId: String,
    room: String,
    risk: Double,
    detectedAt: String,
    confirmation: Confirmation? = null,
) = EventRecord(
    FallEvent(
        "1.0", "fall_suspected", windowId, detectedAt, room, risk,
        Evidence("fall_like", 0.91, PresenceState.PRESENT, 0.95, 4.0),
    ),
    confirmation,
)

@Preview(showBackground = true, widthDp = 400, backgroundColor = 0xFFF2F4F6)
@Composable
private fun EventCardPreview() {
    CarnationTheme {
        Column(Modifier.padding(Space.xl)) {
            EventCard(
                previewEventRecord("p:1", "living-room", 0.9, "2026-08-17T10:30:00+09:00"),
                onClick = {},
            )
            Spacer(Modifier.height(Space.md))
            EventCard(
                previewEventRecord(
                    "p:2", "kitchen", 0.6, "2026-08-16T14:00:00+09:00", Confirmation.NORMAL,
                ),
                onClick = {},
            )
            Spacer(Modifier.height(Space.md))
            EventCard(
                previewEventRecord(
                    "p:3", "bathroom", 0.8, "2026-08-15T09:00:00+09:00", Confirmation.HELP_NEEDED,
                ),
                onClick = {},
            )
        }
    }
}
