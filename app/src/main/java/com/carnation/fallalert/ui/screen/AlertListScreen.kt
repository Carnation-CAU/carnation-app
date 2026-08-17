package com.carnation.fallalert.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carnation.fallalert.data.remote.ConnectionState
import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.model.EventRecord
import com.carnation.fallalert.model.ParentProfile
import com.carnation.fallalert.ui.AlertListUiState
import com.carnation.fallalert.ui.theme.BgGray
import com.carnation.fallalert.ui.theme.BlueLight
import com.carnation.fallalert.ui.theme.CarnationTheme
import com.carnation.fallalert.ui.theme.DangerRed
import com.carnation.fallalert.ui.theme.Motion
import com.carnation.fallalert.ui.theme.NavyDeep
import com.carnation.fallalert.ui.theme.Space
import com.carnation.fallalert.ui.theme.SuccessGreen
import com.carnation.fallalert.ui.theme.TextSecondary

/** 홈에 한 번에 보여줄 미확인 알림 수. 나머지는 "더 보기"로 넘긴다. */
private const val HOME_ALERT_LIMIT = 3

/**
 * 화면 A — 홈
 *
 * 위에서 아래로 읽히는 순서가 곧 보호자가 판단하는 순서다:
 *   지금 상태(무동작) → 확인할 알림 → 무엇을 할 것인가(전화 / 119)
 *
 * 화면 전체가 뉴트럴이고 **낙상 카드만 색을 갖는다.** 차분한 바탕이 목적이 아니라,
 * 그래야 위험이 눈에 걸리기 때문이다.
 */
@Composable
fun AlertListScreen(
    state: AlertListUiState,
    connection: ConnectionState,
    pendingSyncCount: Int,
    inactivityMinutes: Int,
    parentProfile: ParentProfile,
    confirmedCount: Int,
    onEventClick: (String) -> Unit,
    onSeeAll: () -> Unit,
    onOpenConfirmed: () -> Unit,
    onOpenSettings: () -> Unit,
    onSimulatePush: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenTitle(
            title = "낙상 의심 알림",
            actions = {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "부모님 정보 설정",
                    )
                }
            },
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
                        confirmedCount = confirmedCount,
                        onOpenConfirmed = onOpenConfirmed,
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
                        confirmedCount = confirmedCount,
                        onOpenConfirmed = onOpenConfirmed,
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
                        confirmedCount = confirmedCount,
                        onOpenConfirmed = onOpenConfirmed,
                        failure = null,
                        onEventClick = onEventClick,
                        onSeeAll = onSeeAll,
                        onSimulatePush = onSimulatePush,
                        onRetry = onRetry,
                    )
            }
        }

        BottomBarSurface {
            ParentActionBar(profile = parentProfile)
        }
    }
}

@Composable
private fun HomeBody(
    inactivityMinutes: Int,
    unconfirmed: List<EventRecord>,
    totalUnconfirmed: Int,
    confirmedCount: Int,
    onOpenConfirmed: () -> Unit,
    failure: String?,
    onEventClick: (String) -> Unit,
    onSeeAll: () -> Unit,
    onSimulatePush: () -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Space.xl, end = Space.xl, top = Space.md, bottom = Space.section,
        ),
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        item(key = "inactivity") {
            InactivityCard(minutes = inactivityMinutes)
        }

        item(key = "status") {
            Spacer(Modifier.height(Space.sm))
            StatusHeadline(unconfirmedCount = totalUnconfirmed, failure = failure)
        }

        items(unconfirmed, key = { it.id }) { record ->
            EventCard(record = record, onClick = { onEventClick(record.id) })
        }

        if (totalUnconfirmed > unconfirmed.size) {
            item(key = "more") {
                SecondaryActionButton(
                    text = "나머지 ${totalUnconfirmed - unconfirmed.size}건 더 보기",
                    onClick = onSeeAll,
                )
            }
        }

        if (failure != null) {
            item(key = "retry") {
                PrimaryActionButton(text = "다시 시도", onClick = onRetry)
            }
        }

        // 확인한 알림은 가끔 되짚어 보는 이력이다. 목록 행 하나로 조용히 둔다.
        item(key = "confirmed-entry") {
            Spacer(Modifier.height(Space.sm))
            AppCard(onClick = onOpenConfirmed, contentPadding = Space.lg) {
                IconListRow(
                    icon = Icons.Filled.TaskAlt,
                    iconTint = TextSecondary,
                    iconBackground = BgGray,
                    title = "확인한 알림",
                    trailing = {
                        if (confirmedCount > 0) {
                            Text(
                                text = "${confirmedCount}건",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
            }
        }

        item(key = "dev") {
            Spacer(Modifier.height(Space.sm))
            // 개발용 도구는 CTA 가 아니다. 캡션 크기 링크로 낮춘다.
            TextButton(onClick = onSimulatePush, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "테스트 이벤트 보내기",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 목록을 읽기 전에 결론을 먼저 말한다.
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
        when {
            failed -> Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(Space.sm))
                    Text(
                        text = "알림을 불러오지 못했어요",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = "새 알림이 없는 게 아니라, 서버를 확인하지 못한 상태예요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            hasWork -> LabeledValue(
                label = "확인이 필요해요",
                value = "${count}건",
                valueColor = DangerRed,
            )

            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(Space.sm))
                Column {
                    Text(
                        text = "모두 확인했어요",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "확인이 필요한 알림이 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    previewEventRecord("a:1", "living-room", 0.9, "2026-08-17T13:30:00+09:00"),
                    previewEventRecord("a:2", "bathroom", 0.72, "2026-08-17T08:12:00+09:00"),
                    previewEventRecord("a:3", "bedroom", 0.55, "2026-08-16T21:05:00+09:00"),
                    previewEventRecord("a:4", "kitchen", 0.61, "2026-08-16T18:00:00+09:00"),
                    previewEventRecord("a:5", "entrance", 0.66, "2026-08-16T09:00:00+09:00"),
                ),
                history = emptyList(),
            ),
            connection = ConnectionState.Connected,
            pendingSyncCount = 0,
            inactivityMinutes = 192,
            parentProfile = ParentProfile.MOCK,
            confirmedCount = 2,
            onEventClick = {}, onSeeAll = {}, onOpenConfirmed = {}, onOpenSettings = {},
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
                        "a:9", "kitchen", 0.83, "2026-08-16T14:47:00+09:00", Confirmation.NORMAL,
                    ),
                ),
            ),
            connection = ConnectionState.Connected,
            pendingSyncCount = 0,
            inactivityMinutes = 42,
            parentProfile = ParentProfile.MOCK,
            confirmedCount = 1,
            onEventClick = {}, onSeeAll = {}, onOpenConfirmed = {}, onOpenSettings = {},
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
            confirmedCount = 0,
            onEventClick = {}, onSeeAll = {}, onOpenConfirmed = {}, onOpenSettings = {},
            onSimulatePush = {}, onRetry = {},
        )
    }
}
