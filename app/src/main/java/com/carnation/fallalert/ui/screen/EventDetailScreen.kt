package com.carnation.fallalert.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.model.EventRecord
import com.carnation.fallalert.model.Evidence
import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.model.PresenceState
import com.carnation.fallalert.ui.EventDetailUiState
import com.carnation.fallalert.ui.theme.CarnationTheme
import com.carnation.fallalert.util.detectedAtInstant
import com.carnation.fallalert.util.motionLabel
import com.carnation.fallalert.util.presenceLabel
import com.carnation.fallalert.util.roomLabel
import com.carnation.fallalert.util.toAbsoluteKorean
import com.carnation.fallalert.util.toPercent
import kotlin.math.roundToInt

/** 화면 B — 상세 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    state: EventDetailUiState,
    onConfirm: (Confirmation) -> Unit,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("상세 내용") },
                navigationIcon = {
                    IconButton(onClick = onBackToList) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "목록으로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            when (state) {
                EventDetailUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                EventDetailUiState.NotFound ->
                    NotFoundState(onBackToList, Modifier.align(Alignment.Center))

                is EventDetailUiState.Ready ->
                    DetailContent(state.record, onConfirm, onBackToList)
            }
        }
    }
}

@Composable
private fun DetailContent(
    record: EventRecord,
    onConfirm: (Confirmation) -> Unit,
    onBackToList: () -> Unit,
) {
    val event = record.event
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(56.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "${roomLabel(event.roomId)}에서 낙상 의심",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = event.detectedAtInstant().toAbsoluteKorean() + " 감지",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))
        RiskCard(event.riskScore)
        Spacer(Modifier.height(20.dp))
        EvidenceCard(event)
        Spacer(Modifier.height(28.dp))

        if (record.confirmation == null) {
            Text(
                text = "확인해 주세요",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onConfirm(Confirmation.NORMAL) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 68.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                ) {
                    Text("정상")
                }
                Button(
                    onClick = { onConfirm(Confirmation.HELP_NEEDED) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 68.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("도움 필요")
                }
            }
        } else {
            AlreadyConfirmedNotice(record.confirmation)
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBackToList) {
            Text("목록으로")
        }
    }
}

@Composable
private fun RiskCard(riskScore: Double) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "위험도",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "${riskScore.toPercent()}%",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(10.dp))
            RiskBar(riskScore)
        }
    }
}

@Composable
private fun RiskBar(riskScore: Double) {
    val fraction = riskScore.coerceIn(0.0, 1.0).toFloat()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .background(
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                RoundedCornerShape(7.dp),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(14.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(7.dp)),
        )
    }
}

@Composable
private fun EvidenceCard(event: FallEvent) {
    val evidence = event.evidence
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "이렇게 판단했어요",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))

            EvidenceRow(
                icon = Icons.Filled.DirectionsRun,
                label = "움직임",
                value = motionLabel(evidence.motionLabel),
                detail = "${evidence.motionConfidence.toPercent()}%",
            )
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            EvidenceRow(
                icon = Icons.Filled.Person,
                label = "사람 존재",
                value = presenceLabel(evidence.presenceState),
                detail = "${evidence.presenceProbability.toPercent()}%",
            )
            // 선택 필드 — 값이 없으면 행 자체를 숨긴다. (명세 3장 화면 B)
            evidence.noRecoverySec?.let { seconds ->
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                EvidenceRow(
                    icon = Icons.Filled.AccessTime,
                    label = "회복 없음",
                    value = "${formatSeconds(seconds)}초 동안 일어나지 못함",
                    detail = null,
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            EvidenceRow(
                icon = Icons.Filled.Timeline,
                label = "감지 구간",
                value = event.windowId,
                detail = null,
            )
        }
    }
}

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
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
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
private fun AlreadyConfirmedNotice(confirmation: Confirmation) {
    val isNormal = confirmation == Confirmation.NORMAL
    Surface(
        color = if (isNormal) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isNormal) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (isNormal) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (isNormal) "정상으로 확인한 알림이에요" else "도움 요청을 접수한 알림이에요",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isNormal) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
            )
        }
    }
}

@Composable
private fun NotFoundState(onBackToList: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "알림을 찾을 수 없어요",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBackToList) { Text("목록으로") }
    }
}

/** 4.0 → "4", 1.5 → "1.5" */
private fun formatSeconds(seconds: Double): String =
    if (seconds % 1.0 == 0.0) seconds.roundToInt().toString() else seconds.toString()

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun EventDetailPreview() {
    CarnationTheme {
        EventDetailScreen(
            state = EventDetailUiState.Ready(
                EventRecord(
                    FallEvent(
                        "1.0", "fall_suspected", "trial-01:7", "2026-08-05T10:30:00+09:00",
                        "living-room", 0.9,
                        Evidence("fall_like", 0.91, PresenceState.PRESENT, 0.95, 4.0),
                    )
                )
            ),
            onConfirm = {},
            onBackToList = {},
        )
    }
}
