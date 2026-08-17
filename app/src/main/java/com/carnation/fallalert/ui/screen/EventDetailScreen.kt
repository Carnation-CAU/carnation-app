package com.carnation.fallalert.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.model.EventRecord
import com.carnation.fallalert.model.Evidence
import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.model.NormalReason
import com.carnation.fallalert.model.ParentProfile
import com.carnation.fallalert.model.PresenceState
import com.carnation.fallalert.ui.EventDetailUiState
import com.carnation.fallalert.ui.theme.BgGray
import com.carnation.fallalert.ui.theme.CarnationTheme
import com.carnation.fallalert.ui.theme.DangerBg
import com.carnation.fallalert.ui.theme.DangerRed
import com.carnation.fallalert.ui.theme.Space
import com.carnation.fallalert.ui.theme.TextSecondary
import com.carnation.fallalert.util.detectedAtInstant
import com.carnation.fallalert.util.formatSeconds
import com.carnation.fallalert.util.motionLabel
import com.carnation.fallalert.util.roomLabel
import com.carnation.fallalert.util.toAbsoluteKorean
import com.carnation.fallalert.util.toPercent

/**
 * 화면 B — 상세
 *
 * 위험도 숫자가 화면의 주인공이다. 명세서에서 금액을 다루듯 크게 두고, 근거는 그 아래
 * 라벨-값 리스트로 얇은 Hairline 으로만 나눈다.
 *
 * 확인 버튼은 하나다. "도움 필요"는 하단 119 버튼이 겸한다 — 119에 정보를 보내기로 했다는
 * 것 자체가 도움이 필요하다는 판단이고, 같은 결정을 두 번 누르게 할 이유가 없다.
 *
 * `presence`(사람 존재)와 `window_id`(감지 구간)는 화면에서 뺐다. 계약(`FallEvent`)에는
 * 그대로 있고 서버도 계속 보낸다 — 보호자가 판단에 쓰지 않는 값이라 표시만 하지 않는다.
 */
@Composable
fun EventDetailScreen(
    state: EventDetailUiState,
    parentProfile: ParentProfile,
    onHelpNeeded: () -> Unit,
    onNoHelpNeeded: (List<NormalReason>) -> Unit,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var askingReasons by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenTitle(
            title = "상세 내용",
            navigationIcon = {
                IconButton(onClick = onBackToList) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "목록으로")
                }
            },
        )

        Box(modifier = Modifier.weight(1f)) {
            when (state) {
                EventDetailUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                EventDetailUiState.NotFound ->
                    NotFoundState(onBackToList, Modifier.align(Alignment.Center))

                is EventDetailUiState.Ready ->
                    DetailContent(
                        record = state.record,
                        pendingSync = state.pendingSync,
                        onAskReasons = { askingReasons = true },
                    )
            }
        }

        BottomBarSurface {
            ParentActionBar(
                profile = parentProfile,
                onEmergencyComposed = onHelpNeeded,
            )
        }
    }

    if (askingReasons) {
        NormalReasonSheet(
            onDismiss = { askingReasons = false },
            onSubmit = { reasons ->
                askingReasons = false
                onNoHelpNeeded(reasons)
            },
        )
    }
}

@Composable
private fun DetailContent(
    record: EventRecord,
    pendingSync: Boolean,
    onAskReasons: () -> Unit,
) {
    val event = record.event
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.xl),
    ) {
        // 어디서 · 언제. 상단 타이틀이 "상세 내용"이라 여기서 사건을 말한다.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DangerBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(Space.md))
            Column {
                Text(
                    text = "${roomLabel(event.roomId)}에서 낙상 의심",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = event.detectedAtInstant().toAbsoluteKorean(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Space.xl))

        AppCard {
            LabeledValue(
                label = "위험도",
                value = "${event.riskScore.toPercent()}%",
                valueColor = DangerRed,
                valueStyle = MaterialTheme.typography.displayLarge,
            )
            Spacer(Modifier.height(Space.lg))
            RiskBar(event.riskScore)

            Spacer(Modifier.height(Space.xl))
            HairlineDivider()
            Spacer(Modifier.height(Space.lg))

            Text(
                text = "판단 근거",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Space.md))

            EvidenceRow(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                label = "움직임",
                value = motionLabel(event.evidence.motionLabel),
                detail = "${event.evidence.motionConfidence.toPercent()}%",
            )
            // 선택 필드 — 값이 없으면 행 자체를 숨긴다.
            event.evidence.noRecoverySec?.let { seconds ->
                Spacer(Modifier.height(Space.md))
                HairlineDivider()
                Spacer(Modifier.height(Space.md))
                EvidenceRow(
                    icon = Icons.Filled.AccessTime,
                    label = "회복 없음",
                    value = "${formatSeconds(seconds)}초 동안 일어나지 못함",
                    detail = null,
                )
            }
        }

        Spacer(Modifier.height(Space.xxl))

        if (record.confirmation == null) {
            SecondaryActionButton(
                text = "도움은 필요 없어요",
                onClick = onAskReasons,
            )
            Spacer(Modifier.height(Space.sm))
            Text(
                text = "실제로 어떤 상황이었는지 알려주면 잘못된 알림을 줄일 수 있어요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            AlreadyConfirmedNotice(record.confirmation, pendingSync)
        }

        Spacer(Modifier.height(Space.xl))
    }
}

@Composable
private fun RiskBar(riskScore: Double) {
    val fraction = riskScore.coerceIn(0.0, 1.0).toFloat()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(BgGray),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DangerRed),
        )
    }
}

/** 라벨-값 한 행. 아이콘은 작게, 값은 본문 크기로. */
@Composable
private fun EvidenceRow(
    icon: ImageVector,
    label: String,
    value: String,
    detail: String?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun AlreadyConfirmedNotice(confirmation: Confirmation, pendingSync: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AppCard(contentPadding = Space.lg) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ConfirmationBadge(confirmation)
                Spacer(Modifier.width(Space.md))
                Text(
                    text = if (confirmation == Confirmation.NORMAL) {
                        "정상으로 확인한 알림이에요"
                    } else {
                        "도움 요청을 접수한 알림이에요"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (pendingSync) {
            Spacer(Modifier.height(Space.sm))
            Text(
                text = "아직 전송하지 못했어요. 연결되면 자동으로 다시 보냅니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NotFoundState(onBackToList: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Space.section),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "알림을 찾을 수 없어요",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Space.lg))
        TextButton(onClick = onBackToList) {
            Text("목록으로", color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun detailPreviewRecord(
    noRecoverySec: Double? = 4.0,
    confirmation: Confirmation? = null,
) = EventRecord(
    FallEvent(
        "1.0", "fall_suspected", "trial-01:7", "2026-08-17T10:30:00+09:00",
        "living-room", 0.9,
        Evidence("fall_like", 0.91, PresenceState.PRESENT, 0.95, noRecoverySec),
    ),
    confirmation,
)

@Preview(showBackground = true, heightDp = 900, name = "미확인")
@Composable
private fun EventDetailPreview() {
    CarnationTheme {
        EventDetailScreen(
            state = EventDetailUiState.Ready(detailPreviewRecord(), pendingSync = false),
            parentProfile = ParentProfile.MOCK,
            onHelpNeeded = {},
            onNoHelpNeeded = {},
            onBackToList = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900, name = "회복 없음 값 없을 때")
@Composable
private fun EventDetailNoRecoveryPreview() {
    CarnationTheme {
        EventDetailScreen(
            state = EventDetailUiState.Ready(
                detailPreviewRecord(noRecoverySec = null),
                pendingSync = false,
            ),
            parentProfile = ParentProfile.MOCK,
            onHelpNeeded = {},
            onNoHelpNeeded = {},
            onBackToList = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900, name = "확인 완료")
@Composable
private fun EventDetailConfirmedPreview() {
    CarnationTheme {
        EventDetailScreen(
            state = EventDetailUiState.Ready(
                detailPreviewRecord(confirmation = Confirmation.HELP_NEEDED),
                pendingSync = true,
            ),
            parentProfile = ParentProfile.MOCK,
            onHelpNeeded = {},
            onNoHelpNeeded = {},
            onBackToList = {},
        )
    }
}
