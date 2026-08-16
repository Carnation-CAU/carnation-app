package com.carnation.fallalert.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carnation.fallalert.data.remote.ConnectionState
import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.model.EventRecord
import com.carnation.fallalert.model.Evidence
import com.carnation.fallalert.model.FallEvent
import com.carnation.fallalert.model.PresenceState
import com.carnation.fallalert.ui.AlertListUiState
import com.carnation.fallalert.ui.theme.CarnationTheme
import com.carnation.fallalert.ui.theme.Elevation
import com.carnation.fallalert.ui.theme.Motion
import com.carnation.fallalert.ui.theme.Radius
import com.carnation.fallalert.ui.theme.Space
import com.carnation.fallalert.ui.theme.TouchTarget
import com.carnation.fallalert.util.detectedAtInstant
import com.carnation.fallalert.util.roomLabel
import com.carnation.fallalert.util.toPercent
import com.carnation.fallalert.util.toRelativeKorean

/**
 * 화면 A — 알림 목록 (홈)
 *
 * 이 화면의 임무는 하나다: **"지금 내가 확인해야 할 일이 있는가"에 1초 안에 답하기.**
 * 그래서 목록보다 먼저 답을 말하는 상태 헤더가 맨 위에 온다. 카드는 그 답의 근거다.
 *
 * 코랄은 이 화면에서 오직 "미확인 낙상 의심"에만 쓴다. 호버·배지·장식에 쓰지 않는 이유는
 * 절제 그 자체가 아니라, 코랄이 다른 데 한 번이라도 나오면 경고로서의 힘을 잃기 때문이다.
 * 나머지 전부(제목, 이력, 링크, 연결 상태)는 네이비와 블루가 맡는다.
 */
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "낙상 의심 알림",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
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
                .padding(innerPadding),
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
        contentPadding = PaddingValues(
            start = Space.xl,
            end = Space.xl,
            top = Space.sm,
            bottom = Space.section,
        ),
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        item(key = "status") {
            StatusHeadline(unconfirmedCount = state.unconfirmed.size)
        }

        items(state.unconfirmed, key = { it.id }) { record ->
            EventCard(record = record, onClick = { onEventClick(record.id) })
        }

        if (state.history.isNotEmpty()) {
            item(key = "history-label") {
                Spacer(Modifier.height(Space.xl))
                Text(
                    text = "확인한 알림",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Space.xs),
                )
            }
            items(state.history, key = { it.id }) { record ->
                EventCard(record = record, onClick = { onEventClick(record.id) })
            }
        }

        item(key = "dev") {
            Spacer(Modifier.height(Space.xl))
            // 개발용 도구는 CTA 가 아니다. 화면의 시선을 가져가지 않도록 링크로 낮춘다.
            TextButton(
                onClick = onSimulatePush,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text("테스트 이벤트 보내기", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * 이 화면의 signature. 목록을 읽기 전에 결론을 먼저 말한다.
 *
 * 두 상태는 색만 다른 게 아니라 문장 자체가 다르다. 숫자를 크게 쓰는 건
 * 멀리서도, 안경 없이도, 스크롤하기 전에 읽히게 하려는 것이다.
 */
@Composable
private fun StatusHeadline(unconfirmedCount: Int) {
    val hasWork = unconfirmedCount > 0

    AnimatedContent(
        targetState = hasWork,
        transitionSpec = {
            fadeIn(tween(Motion.NORMAL)) togetherWith fadeOut(tween(Motion.FAST))
        },
        label = "status",
    ) { pending ->
        Column(modifier = Modifier.padding(vertical = Space.lg)) {
            if (pending) {
                Text(
                    text = "확인이 필요해요",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = "${unconfirmedCount}건",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(30.dp),
                    )
                    Spacer(Modifier.width(Space.sm))
                    Text(
                        text = "모두 확인했어요",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = "새로 확인할 알림이 없습니다.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EventCard(record: EventRecord, onClick: () -> Unit) {
    val unconfirmed = record.isUnconfirmed
    val scheme = MaterialTheme.colorScheme

    val container = if (unconfirmed) scheme.primaryContainer else scheme.surface
    val accent = if (unconfirmed) scheme.primary else scheme.onSurfaceVariant
    val heading = if (unconfirmed) scheme.onPrimaryContainer else scheme.onSurface

    Surface(
        color = container,
        shape = Radius.card,
        shadowElevation = if (unconfirmed) Elevation.raised else Elevation.card,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                // 색만으로 구분하지 않는다. 테두리가 있어야 색각 이상·야외 화면에서도 읽힌다.
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
                // 위험도는 미확인 카드에서만 보여준다. 이미 확인한 건에는 판단이 끝났다.
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

@Composable
private fun EmptyState(onSimulatePush: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(Space.lg))
        Text(
            text = "받은 알림이 없어요",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Space.xl))
        TextButton(
            onClick = onSimulatePush,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Text("테스트 이벤트 보내기", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * 목록을 못 읽었을 때. 캐시를 두지 않기로 했으므로 보여줄 이전 데이터가 없다 —
 * "알림 없음"이 아니라 "확인할 수 없음"이라고 분명히 말해야 한다.
 */
@Composable
private fun FailedState(reason: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(Space.lg))
        Text(
            text = "알림을 불러오지 못했어요",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Space.sm))
        Text(
            text = "새 알림이 없는 게 아니라, 서버를 확인하지 못한 상태예요.\n$reason",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Space.xl))
        Button(
            onClick = onRetry,
            shape = Radius.button,
            modifier = Modifier.heightIn(min = TouchTarget.minimum),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
        ) {
            Text("다시 시도", fontWeight = FontWeight.Bold)
        }
    }
}

private fun previewRecord(
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

@Preview(showBackground = true, heightDp = 1000, name = "확인 필요")
@Composable
private fun AlertListPreview() {
    CarnationTheme {
        AlertListScreen(
            state = AlertListUiState.Ready(
                unconfirmed = listOf(
                    previewRecord("a:1", "living-room", 0.9, "2026-08-16T13:30:00+09:00"),
                    previewRecord("a:2", "bathroom", 0.72, "2026-08-16T08:12:00+09:00"),
                ),
                history = listOf(
                    previewRecord(
                        "a:3", "kitchen", 0.83, "2026-08-15T14:47:00+09:00",
                        Confirmation.NORMAL,
                    ),
                ),
            ),
            connection = ConnectionState.Connected,
            pendingSyncCount = 0,
            onEventClick = {},
            onSimulatePush = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1000, name = "모두 확인함")
@Composable
private fun AlertListAllClearPreview() {
    CarnationTheme {
        AlertListScreen(
            state = AlertListUiState.Ready(
                unconfirmed = emptyList(),
                history = listOf(
                    previewRecord(
                        "a:3", "kitchen", 0.83, "2026-08-15T14:47:00+09:00",
                        Confirmation.HELP_NEEDED,
                    ),
                ),
            ),
            connection = ConnectionState.Connected,
            pendingSyncCount = 0,
            onEventClick = {},
            onSimulatePush = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1000, name = "오프라인")
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
