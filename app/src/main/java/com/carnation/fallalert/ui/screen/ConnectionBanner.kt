package com.carnation.fallalert.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carnation.fallalert.data.remote.ConnectionState
import com.carnation.fallalert.ui.theme.CarnationTheme

/**
 * 서버 연결 상태 표시줄.
 *
 * 보호자 앱에서 제일 위험한 건 "조용히 끊겨 있는 상태"다. 알림이 안 온 건지 사고가 없었던 건지
 * 구분이 안 되기 때문에, 끊겨 있으면 반드시 눈에 보여야 한다.
 * 반대로 정상일 때 계속 띄워두면 경고 피로가 생기므로 연결됨 상태에서는 숨긴다.
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
        when {
            offline != null -> BannerRow(
                icon = Icons.Filled.CloudOff,
                text = "서버에 연결되지 않았어요" +
                    if (pendingSyncCount > 0) " · 보낼 확인 ${pendingSyncCount}건" else "",
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
                action = "다시 시도" to onRetry,
            )

            state is ConnectionState.Connecting -> BannerRow(
                icon = Icons.Filled.Sync,
                text = "연결하는 중…",
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurfaceVariant,
                action = null,
            )

            else -> BannerRow(
                icon = Icons.Filled.CloudDone,
                text = "확인 결과 ${pendingSyncCount}건을 보내는 중이에요",
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurfaceVariant,
                action = null,
            )
        }
    }
}

@Composable
private fun BannerRow(
    icon: ImageVector,
    text: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    action: Pair<String, () -> Unit>?,
) {
    Surface(color = container, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = content,
                modifier = Modifier.weight(1f),
            )
            if (action != null) {
                TextButton(onClick = action.second) {
                    Text(action.first, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Preview(showBackground = true)
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
