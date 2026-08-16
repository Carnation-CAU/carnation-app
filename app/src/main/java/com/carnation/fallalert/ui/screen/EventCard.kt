package com.carnation.fallalert.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.model.EventRecord
import com.carnation.fallalert.model.Evidence
import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.model.PresenceState
import com.carnation.fallalert.ui.theme.Elevation
import com.carnation.fallalert.ui.theme.Radius
import com.carnation.fallalert.ui.theme.Space
import com.carnation.fallalert.util.detectedAtInstant
import com.carnation.fallalert.util.roomLabel
import com.carnation.fallalert.util.toPercent
import com.carnation.fallalert.util.toRelativeKorean

/**
 * 이벤트 카드. 홈·확인한 알림·전체 목록이 모두 이걸 쓴다.
 *
 * 코랄은 미확인 상태에만 나타난다. 확인이 끝난 카드는 흰 표면에 회색 톤으로 가라앉는다 —
 * 목록을 훑을 때 눈에 걸려야 하는 건 아직 처리 안 된 것뿐이다.
 */
@Composable
fun EventCard(record: EventRecord, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val unconfirmed = record.isUnconfirmed
    val scheme = MaterialTheme.colorScheme

    val container = if (unconfirmed) scheme.primaryContainer else scheme.surface
    val accent = if (unconfirmed) scheme.primary else scheme.onSurfaceVariant
    val heading = if (unconfirmed) scheme.onPrimaryContainer else scheme.onSurface

    Surface(
        color = container,
        shape = Radius.card,
        shadowElevation = if (unconfirmed) Elevation.raised else Elevation.card,
        modifier = modifier
            .fillMaxWidth()
            .then(
                // 색만으로 구분하지 않는다. 테두리가 있어야 색각 이상·야외에서도 읽힌다.
                if (unconfirmed) Modifier.border(2.dp, accent, Radius.card) else Modifier
            )
            .clip(Radius.card)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = buildString {
                    append(roomLabel(record.event.roomId))
                    append("에서 낙상 의심, 위험도 ")
                    append(record.event.riskScore.toPercent())
                    append("퍼센트, ")
                    append(record.event.detectedAtInstant().toRelativeKorean())
                    append(if (unconfirmed) ", 아직 확인하지 않음" else ", 확인 완료")
                }
            },
    ) {
        Row(
            modifier = Modifier.padding(Space.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (unconfirmed) accent else scheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (unconfirmed) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (unconfirmed) Color.White else scheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp),
                )
            }

            Spacer(Modifier.width(Space.lg))

            Column(Modifier.weight(1f)) {
                Text(
                    text = roomLabel(record.event.roomId),
                    style = MaterialTheme.typography.titleLarge,
                    color = heading,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = record.event.detectedAtInstant().toRelativeKorean(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (unconfirmed) heading.copy(alpha = 0.75f) else scheme.onSurfaceVariant,
                )
                if (!unconfirmed) {
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = when (record.confirmation) {
                            Confirmation.HELP_NEEDED -> "도움 요청함"
                            else -> "정상으로 확인함"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.width(Space.sm))

            if (unconfirmed) {
                // 위험도는 미확인 카드에만. 확인이 끝난 건에는 이미 판단이 내려졌다.
                Text(
                    text = "${record.event.riskScore.toPercent()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    color = accent,
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
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
