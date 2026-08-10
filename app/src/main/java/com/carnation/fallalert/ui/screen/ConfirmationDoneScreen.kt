package com.carnation.fallalert.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
            PendingSyncNotice()
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
 * 확인 결과를 서버로 보내야 하지만 형식·엔드포인트가 팀 미확정이다. (명세 3장 화면 C / 7장)
 * 지금은 로컬 상태만 갱신했다는 사실을 화면에도 솔직히 드러낸다.
 */
@Composable
private fun PendingSyncNotice() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            // TODO(팀 확정 후): 실제 전송 성공/실패 상태로 교체.
            text = "이 기기에만 저장했어요. 서버 전송은 연동 후 동작합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ConfirmationDoneNormalPreview() {
    CarnationTheme {
        ConfirmationDoneScreen(Confirmation.NORMAL, onBackToList = {})
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ConfirmationDoneHelpPreview() {
    CarnationTheme {
        ConfirmationDoneScreen(Confirmation.HELP_NEEDED, onBackToList = {})
    }
}
