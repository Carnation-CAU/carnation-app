package com.carnation.fallalert.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carnation.fallalert.ui.theme.BgGray
import com.carnation.fallalert.ui.theme.CarnationTheme
import com.carnation.fallalert.ui.theme.Space
import com.carnation.fallalert.ui.theme.TextSecondary
import com.carnation.fallalert.ui.theme.WarnAmber

/**
 * 장시간 무동작. 홈 최상단 고정.
 *
 * 낙상 카드와 **다른 층위**다. 낙상은 "사건이 일어났다", 이건 "아무 일도 안 일어나고 있다".
 * 그래서 색을 쓰지 않고 흰 카드에 큰 숫자만 둔다. 길어지면 아이콘과 라벨에만
 * WarnAmber 를 얹는다 — 카드 전체를 물들이면 낙상 카드와 급이 같아 보인다.
 */
@Composable
fun InactivityCard(
    minutes: Int,
    modifier: Modifier = Modifier,
) {
    val prolonged = minutes >= PROLONGED_THRESHOLD_MINUTES
    val accent = if (prolonged) WarnAmber else TextSecondary

    AppCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (prolonged) WarnAmber.copy(alpha = 0.12f) else BgGray),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (prolonged) {
                        Icons.Filled.HourglassBottom
                    } else {
                        Icons.Filled.Bedtime
                    },
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(Space.lg))

            Column {
                Text(
                    text = "마지막으로 움직인 지",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent,
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = formatDuration(minutes),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** 192 → "3시간 12분", 45 → "45분", 120 → "2시간" */
fun formatDuration(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val hours = safe / 60
    val mins = safe % 60
    return when {
        hours == 0 -> "${mins}분"
        mins == 0 -> "${hours}시간"
        else -> "${hours}시간 ${mins}분"
    }
}

/** 3시간. 잠자는 시간과 구분되기 시작하는 지점으로 잡은 임시값. */
private const val PROLONGED_THRESHOLD_MINUTES = 180

@Preview(showBackground = true, widthDp = 400, backgroundColor = 0xFFF2F4F6, name = "짧음")
@Composable
private fun InactivityShortPreview() {
    CarnationTheme { InactivityCard(minutes = 45, modifier = Modifier.padding(Space.xl)) }
}

@Preview(showBackground = true, widthDp = 400, backgroundColor = 0xFFF2F4F6, name = "길어짐")
@Composable
private fun InactivityLongPreview() {
    CarnationTheme { InactivityCard(minutes = 192, modifier = Modifier.padding(Space.xl)) }
}
