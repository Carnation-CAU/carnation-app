package com.carnation.fallalert.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.model.EventRecord
import com.carnation.fallalert.model.Evidence
import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.data.remote.ConnectionState
import com.carnation.fallalert.model.PresenceState
import com.carnation.fallalert.ui.AlertListUiState
import com.carnation.fallalert.ui.theme.CarnationTheme
import com.carnation.fallalert.util.detectedAtInstant
import com.carnation.fallalert.util.roomLabel
import com.carnation.fallalert.util.toPercent
import com.carnation.fallalert.util.toRelativeKorean

/** 화면 A — 알림 목록 (홈) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertListScreen(
    state: AlertListUiState,
    connection: ConnectionState,
    pendingSyncCount: Int,
    onEventClick: (String) -> Unit,
    onSimulatePush: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("낙상 의심 알림") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                ConnectionBanner(
                    state = connection,
                    pendingSyncCount = pendingSyncCount,
                    onRetry = onRetry,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            when (state) {
                AlertListUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                is AlertListUiState.Failed ->
                    FailedState(state.reason, onRetry, Modifier.align(Alignment.Center))

                AlertListUiState.Empty ->
                    EmptyState(onSimulatePush, Modifier.align(Alignment.Center))

                is AlertListUiState.Ready ->
                    ReadyList(state, onEventClick, onSimulatePush)
            }
        }
    }
}

@Composable
private fun ReadyList(
    state: AlertListUiState.Ready,
    onEventClick: (String) -> Unit,
    onSimulatePush: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.unconfirmed.isNotEmpty()) {
            item(key = "header-unconfirmed") {
                SectionHeader("확인이 필요해요 (${state.unconfirmed.size})")
            }
            items(state.unconfirmed, key = { it.id }) { record ->
                EventCard(record = record, onClick = { onEventClick(record.id) })
            }
        } else {
            item(key = "all-clear") { AllClearBanner() }
        }

        if (state.history.isNotEmpty()) {
            item(key = "header-history") {
                Spacer(Modifier.height(8.dp))
                SectionHeader("확인한 이력")
            }
            items(state.history, key = { it.id }) { record ->
                EventCard(record = record, onClick = { onEventClick(record.id) })
            }
        }

        item(key = "simulate") {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onSimulatePush,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                // 서버에 모의 이벤트 생성을 요청한다 — 결과는 WebSocket 으로 돌아오므로
                // 실제 수신 경로를 그대로 검증한다. 운영 빌드에서는 제거할 것.
                Text("테스트 이벤트 보내기 (개발용)")
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun EventCard(record: EventRecord, onClick: () -> Unit) {
    val unconfirmed = record.isUnconfirmed
    val container = if (unconfirmed) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val accent = if (unconfirmed) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val onContainer = if (unconfirmed) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = container,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (unconfirmed) {
                    Modifier.border(2.dp, accent, RoundedCornerShape(20.dp))
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = buildString {
                    append(roomLabel(record.event.roomId))
                    append("에서 낙상 의심, ")
                    append(record.event.detectedAtInstant().toRelativeKorean())
                    append(if (unconfirmed) ", 아직 확인하지 않음" else ", 확인 완료")
                }
            },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (unconfirmed) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = "낙상 의심",
                    style = MaterialTheme.typography.titleLarge,
                    color = onContainer,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${roomLabel(record.event.roomId)} · 위험도 ${record.event.riskScore.toPercent()}%",
                    style = MaterialTheme.typography.bodyLarge,
                    color = onContainer,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = record.event.detectedAtInstant().toRelativeKorean() +
                        when (record.confirmation) {
                            null -> " · 눌러서 확인하세요"
                            Confirmation.NORMAL -> " · 정상으로 확인함"
                            Confirmation.HELP_NEEDED -> " · 도움 요청 접수됨"
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = onContainer.copy(alpha = 0.8f),
                )
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = onContainer,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun AllClearBanner() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "확인이 필요한 알림이 없어요",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun EmptyState(onSimulatePush: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "아직 받은 알림이 없어요",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onSimulatePush) {
            Text("테스트 이벤트 보내기 (개발용)")
        }
    }
}

/**
 * 목록을 못 읽었을 때. 캐시를 두지 않기로 했으므로 여기서 보여줄 이전 데이터가 없다 —
 * 그래서 "알림이 없음"이 아니라 "확인할 수 없음"이라고 분명히 말해야 한다.
 */
@Composable
private fun FailedState(reason: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "알림을 불러올 수 없어요",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "새 알림이 없는 게 아니라, 서버를 확인하지 못한 상태예요.\n$reason",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, modifier = Modifier.heightIn(min = 60.dp)) {
            Text("다시 시도")
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun AlertListPreview() {
    val unconfirmed = EventRecord(
        FallEvent(
            "1.0", "fall_suspected", "trial-01:7", "2026-08-05T10:30:00+09:00",
            "living-room", 0.9,
            Evidence("fall_like", 0.91, PresenceState.PRESENT, 0.95, 4.0),
        )
    )
    val confirmed = EventRecord(
        FallEvent(
            "1.0", "fall_suspected", "trial-00:11", "2026-08-04T14:47:00+09:00",
            "kitchen", 0.83,
            Evidence("fall_like", 0.79, PresenceState.ABSENT, 0.12, 9.0),
        ),
        Confirmation.NORMAL,
    )
    CarnationTheme {
        AlertListScreen(
            state = AlertListUiState.Ready(listOf(unconfirmed), listOf(confirmed)),
            connection = ConnectionState.Connected,
            pendingSyncCount = 0,
            onEventClick = {},
            onSimulatePush = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900, name = "오프라인 + 미전송")
@Composable
private fun AlertListOfflinePreview() {
    CarnationTheme {
        AlertListScreen(
            state = AlertListUiState.Failed("서버에 연결할 수 없습니다"),
            connection = ConnectionState.Offline("연결할 수 없습니다", retryInSeconds = 8),
            pendingSyncCount = 1,
            onEventClick = {},
            onSimulatePush = {},
            onRetry = {},
        )
    }
}
