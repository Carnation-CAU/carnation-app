package com.carnation.fallalert.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carnation.fallalert.data.remote.ConnectionState
import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.model.EventRecord
import com.carnation.fallalert.model.ParentProfile
import com.carnation.fallalert.ui.AlertListUiState
import com.carnation.fallalert.ui.theme.CarnationTheme
import com.carnation.fallalert.ui.theme.Elevation
import com.carnation.fallalert.ui.theme.Motion
import com.carnation.fallalert.ui.theme.Radius
import com.carnation.fallalert.ui.theme.Space
import com.carnation.fallalert.ui.theme.TouchTarget

/** 홈에 한 번에 보여줄 미확인 알림 수. 나머지는 "더 보기"로 넘긴다. */
private const val HOME_ALERT_LIMIT = 3

/**
 * 화면 A — 홈
 *
 * 위에서 아래로 읽히는 순서가 곧 보호자가 판단하는 순서다:
 *   지금 상태(무동작) → 확인할 알림 → 무엇을 할 것인가(전화 / 119)
 *
 * 확인이 끝난 알림은 여기 남기지 않는다. 홈은 "아직 처리 안 된 것"만 담고,
 * 지난 건은 [ConfirmedListScreen] 탭이 맡는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertListScreen(
    state: AlertListUiState,
    connection: ConnectionState,
    pendingSyncCount: Int,
    inactivityMinutes: Int,
    parentProfile: ParentProfile,
    onEventClick: (String) -> Unit,
    onSeeAll: () -> Unit,
    onOpenSettings: () -> Unit,
    onSimulatePush: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("낙상 의심 알림", style = MaterialTheme.typography.titleLarge) },
            actions = {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "부모님 정보 설정",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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

        Box(modifier = Modifier.weight(1f)) {
            when (state) {
                AlertListUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                is AlertListUiState.Failed ->
                    HomeBody(
                        inactivityMinutes = inactivityMinutes,
                        unconfirmed = emptyList(),
                        totalUnconfirmed = 0,
                        failure = state.reason,
                        onEventClick = onEventClick,
                        onSeeAll = onSeeAll,
                        onSimulatePush = onSimulatePush,
                        onRetry = onRetry,
                    )

                AlertListUiState.Empty ->
                    HomeBody(
                        inactivityMinutes = inactivityMinutes,
                        unconfirmed = emptyList(),
                        totalUnconfirmed = 0,
                        failure = null,
                        onEventClick = onEventClick,
                        onSeeAll = onSeeAll,
                        onSimulatePush = onSimulatePush,
                        onRetry = onRetry,
                    )

                is AlertListUiState.Ready ->
                    HomeBody(
                        inactivityMinutes = inactivityMinutes,
                        unconfirmed = state.unconfirmed.take(HOME_ALERT_LIMIT),
                        totalUnconfirmed = state.unconfirmed.size,
                        failure = null,
                        onEventClick = onEventClick,
                        onSeeAll = onSeeAll,
                        onSimulatePush = onSimulatePush,
                        onRetry = onRetry,
                    )
            }
        }

        // 대응 버튼은 스크롤과 무관하게 늘 같은 자리에 있어야 한다.
        Surface(
            color = MaterialTheme.colorScheme.background,
            shadowElevation = Elevation.raised,
        ) {
            ParentActionBar(
                profile = parentProfile,
                modifier = Modifier.padding(
                    start = Space.xl, end = Space.xl, top = Space.lg, bottom = Space.lg,
                ),
            )
        }
    }
}

@Composable
private fun HomeBody(
    inactivityMinutes: Int,
    unconfirmed: List<EventRecord>,
    totalUnconfirmed: Int,
    failure: String?,
    onEventClick: (String) -> Unit,
    onSeeAll: () -> Unit,
    onSimulatePush: () -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Space.xl, end = Space.xl, top = Space.sm, bottom = Space.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        item(key = "inactivity") {
            InactivityCard(minutes = inactivityMinutes)
            Spacer(Modifier.height(Space.sm))
        }

        item(key = "status") {
            StatusHeadline(unconfirmedCount = totalUnconfirmed, failure = failure)
        }

        items(unconfirmed, key = { it.id }) { record ->
            EventCard(record = record, onClick = { onEventClick(record.id) })
        }

        if (totalUnconfirmed > unconfirmed.size) {
            item(key = "more") {
                OutlinedButton(
                    onClick = onSeeAll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = TouchTarget.minimum),
                    shape = Radius.button,
                ) {
                    Text("나머지 ${totalUnconfirmed - unconfirmed.size}건 더 보기")
                }
            }
        }

        if (failure != null) {
            item(key = "retry") {
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = TouchTarget.minimum),
                    shape = Radius.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                ) {
                    Text("다시 시도", fontWeight = FontWeight.Bold)
                }
            }
        }

        item(key = "dev") {
            Spacer(Modifier.height(Space.md))
            // 개발용 도구는 CTA 가 아니다. 시선을 가져가지 않도록 링크로 낮춘다.
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
 * 세 상태는 색만 다른 게 아니라 문장 자체가 다르다. 특히 실패는 "알림 없음"과
 * 반드시 구분되어야 한다 — 사고가 없는 것과 확인을 못 한 것은 다르다.
 */
@Composable
private fun StatusHeadline(unconfirmedCount: Int, failure: String?) {
    AnimatedContent(
        targetState = Triple(unconfirmedCount > 0, failure != null, unconfirmedCount),
        transitionSpec = {
            fadeIn(tween(Motion.NORMAL)) togetherWith fadeOut(tween(Motion.FAST))
        },
        label = "status",
    ) { (hasWork, failed, count) ->
        Column(modifier = Modifier.padding(vertical = Space.md)) {
            when {
                failed -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.width(Space.sm))
                        Text(
                            text = "알림을 불러오지 못했어요",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = "새 알림이 없는 게 아니라, 서버를 확인하지 못한 상태예요.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                hasWork -> {
                    Text(
                        text = "확인이 필요해요",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = "${count}건",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                else -> {
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
                        text = "확인이 필요한 알림이 없습니다.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1000, name = "확인 필요 5건")
@Composable
private fun AlertListPreview() {
    CarnationTheme {
        AlertListScreen(
            state = AlertListUiState.Ready(
                unconfirmed = listOf(
                    previewEventRecord("a:1", "living-room", 0.9, "2026-08-16T13:30:00+09:00"),
                    previewEventRecord("a:2", "bathroom", 0.72, "2026-08-16T08:12:00+09:00"),
                    previewEventRecord("a:3", "bedroom", 0.55, "2026-08-15T21:05:00+09:00"),
                    previewEventRecord("a:4", "kitchen", 0.61, "2026-08-15T18:00:00+09:00"),
                    previewEventRecord("a:5", "entrance", 0.66, "2026-08-15T09:00:00+09:00"),
                ),
                history = emptyList(),
            ),
            connection = ConnectionState.Connected,
            pendingSyncCount = 0,
            inactivityMinutes = 192,
            parentProfile = ParentProfile.MOCK,
            onEventClick = {}, onSeeAll = {}, onOpenSettings = {},
            onSimulatePush = {}, onRetry = {},
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
                    previewEventRecord(
                        "a:9", "kitchen", 0.83, "2026-08-15T14:47:00+09:00", Confirmation.NORMAL,
                    ),
                ),
            ),
            connection = ConnectionState.Connected,
            pendingSyncCount = 0,
            inactivityMinutes = 42,
            parentProfile = ParentProfile.MOCK,
            onEventClick = {}, onSeeAll = {}, onOpenSettings = {},
            onSimulatePush = {}, onRetry = {},
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
            inactivityMinutes = 192,
            parentProfile = ParentProfile.MOCK,
            onEventClick = {}, onSeeAll = {}, onOpenSettings = {},
            onSimulatePush = {}, onRetry = {},
        )
    }
}
