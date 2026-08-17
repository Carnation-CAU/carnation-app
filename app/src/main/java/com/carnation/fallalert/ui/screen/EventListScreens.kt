package com.carnation.fallalert.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.model.EventRecord
import com.carnation.fallalert.ui.theme.BgGray
import com.carnation.fallalert.ui.theme.CarnationTheme
import com.carnation.fallalert.ui.theme.Space
import com.carnation.fallalert.ui.theme.TextMuted

/**
 * 확인한 알림.
 *
 * **빨강을 쓰지 않는다.** 이미 처리가 끝난 목록이라 눈에 걸릴 이유가 없다.
 * 회색 배경 위 흰 카드에 결과 배지만 작게 붙인다.
 */
@Composable
fun ConfirmedListScreen(
    records: List<EventRecord>,
    onEventClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EventListScaffold(
        title = "확인한 알림",
        onBack = onBack,
        records = records,
        emptyTitle = "확인한 알림이 없어요",
        emptyDetail = "알림을 확인하면 여기에 쌓여요.",
        onEventClick = onEventClick,
        modifier = modifier,
    )
}

/**
 * 홈의 "더 보기"에서 오는 전체 미확인 목록.
 * 홈은 최근 3건만 보여주므로 나머지를 볼 곳이 필요하다.
 */
@Composable
fun AllAlertsScreen(
    records: List<EventRecord>,
    onEventClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EventListScaffold(
        title = "확인이 필요한 알림",
        onBack = onBack,
        records = records,
        emptyTitle = "모두 확인했어요",
        emptyDetail = "확인이 필요한 알림이 없습니다.",
        onEventClick = onEventClick,
        modifier = modifier,
    )
}

@Composable
private fun EventListScaffold(
    title: String,
    onBack: () -> Unit,
    records: List<EventRecord>,
    emptyTitle: String,
    emptyDetail: String,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenTitle(
            title = title,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                }
            },
        )

        if (records.isEmpty()) {
            EmptyMessage(emptyTitle, emptyDetail, Modifier.fillMaxSize())
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = Space.xl, end = Space.xl, top = Space.md, bottom = Space.section,
                ),
                verticalArrangement = Arrangement.spacedBy(Space.md),
            ) {
                items(records, key = { it.id }) { record ->
                    EventCard(record = record, onClick = { onEventClick(record.id) })
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(title: String, detail: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(Space.section),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(BgGray),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Inbox,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.height(Space.xl))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ConfirmedListPreview() {
    CarnationTheme {
        ConfirmedListScreen(
            records = listOf(
                previewEventRecord(
                    "c:1", "living-room", 0.9, "2026-08-17T10:30:00+09:00",
                    Confirmation.HELP_NEEDED,
                ),
                previewEventRecord(
                    "c:2", "kitchen", 0.6, "2026-08-16T14:00:00+09:00", Confirmation.NORMAL,
                ),
            ),
            onEventClick = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 600, name = "비어 있음")
@Composable
private fun ConfirmedListEmptyPreview() {
    CarnationTheme {
        ConfirmedListScreen(records = emptyList(), onEventClick = {}, onBack = {})
    }
}
