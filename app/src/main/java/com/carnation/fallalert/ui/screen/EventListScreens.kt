package com.carnation.fallalert.ui.screen

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.model.EventRecord
import com.carnation.fallalert.ui.theme.CarnationTheme
import com.carnation.fallalert.ui.theme.Space

/**
 * 확인한 알림 탭. 확인 완료된 이벤트만 최신순으로.
 *
 * 홈에서 빠진 것들이 사라지지 않고 여기 쌓인다는 걸 보여주는 게 목적이다.
 * 보호자가 "내가 그때 뭐라고 눌렀더라"를 되짚을 수 있어야 한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmedListScreen(
    records: List<EventRecord>,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("확인한 알림", style = MaterialTheme.typography.titleLarge) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )

        if (records.isEmpty()) {
            EmptyMessage(
                title = "확인한 알림이 없어요",
                detail = "알림을 확인하면 여기에 쌓여요.",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = Space.xl, end = Space.xl, top = Space.sm, bottom = Space.section,
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

/**
 * 홈의 "더 보기"에서 오는 전체 미확인 목록.
 * 홈은 최근 3건만 보여주므로, 나머지를 볼 곳이 필요하다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllAlertsScreen(
    records: List<EventRecord>,
    onEventClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text("확인이 필요한 알림", style = MaterialTheme.typography.titleLarge)
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )

        if (records.isEmpty()) {
            EmptyMessage(
                title = "모두 확인했어요",
                detail = "확인이 필요한 알림이 없습니다.",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = Space.xl, end = Space.xl, top = Space.sm, bottom = Space.section,
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
            modifier = Modifier.padding(Space.xxl),
        ) {
            Icon(
                imageVector = Icons.Filled.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(Space.lg))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyLarge,
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
                previewEventRecord("c:1", "living-room", 0.9, "2026-08-16T10:30:00+09:00", Confirmation.HELP_NEEDED),
                previewEventRecord("c:2", "kitchen", 0.6, "2026-08-15T14:00:00+09:00", Confirmation.NORMAL),
            ),
            onEventClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 600, name = "비어 있음")
@Composable
private fun ConfirmedListEmptyPreview() {
    CarnationTheme { ConfirmedListScreen(records = emptyList(), onEventClick = {}) }
}
