package com.carnation.fallalert.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carnation.fallalert.model.Confirmation
import com.carnation.fallalert.ui.theme.CarnationTheme

/** 화면 C — 확인 완료 */
@Composable
fun ConfirmationDoneScreen(
    confirmation: Confirmation,
    pendingSync: Boolean,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isNormal = confirmation == Confirmation.NORMAL
    val accent = if (isNormal) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 하단 고정 버튼과 겹치지 않게 비워 둔다.
                .padding(bottom = 88.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isNormal) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(72.dp),
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = if (isNormal) "정상으로 확인했어요" else "도움 요청을 접수했어요",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (isNormal) {
                    "오탐으로 기록해 감지 정확도를 개선하는 데 사용해요."
                } else {
                    "긴급 연락처로 전달하는 흐름을 시작해요."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))
            SyncNotice(pendingSync)
        }

        Button(
            onClick = onBackToList,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(min = 68.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("목록으로 돌아가기")
        }
    }
}

/**
 * 실제 전송 상태를 그대로 보여준다.
 *
 * "접수했어요"라고 해놓고 사실은 큐에만 있는 상태가 이 앱에서 가장 위험한 거짓말이다.
 * 특히 "도움 필요"는 보호자가 이걸 보고 다음 행동(직접 전화 등)을 결정한다.
 */
@Composable
private fun SyncNotice(pendingSync: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (pendingSync) Icons.Filled.CloudOff else Icons.Filled.CloudDone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (pendingSync) {
                    "아직 서버에 전달하지 못했어요. 연결되면 자동으로 다시 보냅니다."
                } else {
                    "서버에 전달했어요."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 800, name = "정상 · 전송됨")
@Composable
private fun ConfirmationDoneNormalPreview() {
    CarnationTheme {
        ConfirmationDoneScreen(Confirmation.NORMAL, pendingSync = false, onBackToList = {})
    }
}

@Preview(showBackground = true, heightDp = 800, name = "도움 필요 · 전송 대기")
@Composable
private fun ConfirmationDoneHelpPreview() {
    CarnationTheme {
        ConfirmationDoneScreen(Confirmation.HELP_NEEDED, pendingSync = true, onBackToList = {})
    }
}
