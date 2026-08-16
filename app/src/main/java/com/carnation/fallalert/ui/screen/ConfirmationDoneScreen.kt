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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.carnation.fallalert.ui.theme.Radius
import com.carnation.fallalert.ui.theme.Space
import com.carnation.fallalert.ui.theme.TouchTarget

/**
 * 화면 C — 확인 완료
 *
 * 확인 결과 메시지만 남긴다. 서버 전달 여부는 화면에 노출하지 않는다.
 * **전송·재시도 로직은 그대로 살아 있다** — 실패하면 아웃박스가 붙들고 있다가 재연결 시
 * 다시 보낸다([com.carnation.fallalert.data.ConfirmationOutbox]). 화면에서 뺀 것뿐이다.
 */
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
            .padding(Space.xl),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 하단 고정 버튼과 겹치지 않게 비워 둔다.
                .padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .background(accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isNormal) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp),
                )
            }

            Spacer(Modifier.height(Space.xl))

            Text(
                text = if (isNormal) "정상으로 확인했어요" else "도움 요청을 접수했어요",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Space.md))

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
        }

        Button(
            onClick = onBackToList,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(min = TouchTarget.primaryAction),
            shape = Radius.button,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
        ) {
            Text("목록으로 돌아가기")
        }
    }
}

@Preview(showBackground = true, heightDp = 800, name = "정상")
@Composable
private fun ConfirmationDoneNormalPreview() {
    CarnationTheme { ConfirmationDoneScreen(Confirmation.NORMAL, onBackToList = {}) }
}

@Preview(showBackground = true, heightDp = 800, name = "도움 필요")
@Composable
private fun ConfirmationDoneHelpPreview() {
    CarnationTheme { ConfirmationDoneScreen(Confirmation.HELP_NEEDED, onBackToList = {}) }
}
