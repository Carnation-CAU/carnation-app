package com.carnation.fallalert.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carnation.fallalert.data.remote.ConnectionState
import com.carnation.fallalert.ui.theme.CarnationTheme
import com.carnation.fallalert.ui.theme.Radius
import com.carnation.fallalert.ui.theme.Space

/**
 * 서버 연결 상태.
 *
 * 조용히 끊겨 있는 상태가 이 앱에서 가장 위험하다 — 알림이 안 온 건지 사고가 없었던 건지
 * 구분이 안 된다. 그래서 끊기면 반드시 보여준다.
 *
 * 다만 빨강을 쓰지 않는다. 이 화면에서 빨강은 "낙상 의심"의 색이고, 연결 문제는
 * 낙상이 아니다. 뉴트럴 카드에 블루 액션으로 처리한다.
 * 정상일 때는 아예 숨긴다 — 늘 띄워두면 경고 피로가 생긴다.
 */
@Composable
fun ConnectionBanner(
    state: ConnectionState,
    pendingSyncCount: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val offline = state as? ConnectionState.Offline
    val visible = state is ConnectionState.Connecting || offline != null || pendingSyncCount > 0

    AnimatedVisibility(visible = visible, modifier = modifier) {
        val text = when {
            offline != null -> "서버에 연결되지 않았어요" +
                if (pendingSyncCount > 0) " · 보낼 확인 ${pendingSyncCount}건" else ""

            state is ConnectionState.Connecting -> "연결하는 중이에요"
            else -> "확인 결과 ${pendingSyncCount}건을 보내는 중이에요"
        }
        val icon: ImageVector =
            if (offline != null) Icons.Filled.CloudOff else Icons.Filled.Sync

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.xl)
                .clip(Radius.chip)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = Space.lg, vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(Space.sm))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (offline != null) {
                TextButton(onClick = onRetry, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Space.sm)) {
                    Text(
                        "다시 시도",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, backgroundColor = 0xFFF2F4F6)
@Composable
private fun ConnectionBannerOfflinePreview() {
    CarnationTheme {
        ConnectionBanner(
            state = ConnectionState.Offline("연결할 수 없습니다", retryInSeconds = 4),
            pendingSyncCount = 2,
            onRetry = {},
        )
    }
}
