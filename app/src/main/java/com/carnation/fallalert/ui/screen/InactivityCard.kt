package com.carnation.fallalert.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carnation.fallalert.ui.theme.CarnationTheme
import com.carnation.fallalert.ui.theme.Elevation
import com.carnation.fallalert.ui.theme.Radius
import com.carnation.fallalert.ui.theme.Space

/**
 * 장시간 무동작 경보. 홈 최상단 고정.
 *
 * 낙상 알림과 **별개**다. 낙상은 "사건이 일어났다"이고 이건 "아무 일도 안 일어나고 있다"라서,
 * 카드 모양도 색도 다르게 잡았다.
 *
 * 색 구간은 넣지 않았다(명세상 선택). 주황을 추가하면 경고색인 코랄과 색상이 붙어
 * "낙상 의심"과 "무동작"이 같은 급으로 보인다. 대신 시간이 길어지면 배경과 글자 굵기로
 * 무게를 올린다. 임계값·색 기준은 서버·모델 팀과 협의 후 정하기로 한 항목이다.
 */
@Composable
fun InactivityCard(
    minutes: Int,
    modifier: Modifier = Modifier,
) {
    val prolonged = minutes >= PROLONGED_THRESHOLD_MINUTES
    val scheme = MaterialTheme.colorScheme

    Surface(
        color = if (prolonged) scheme.surfaceVariant else scheme.surface,
        shape = Radius.card,
        shadowElevation = Elevation.card,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(Space.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (prolonged) Icons.Filled.HourglassBottom else Icons.Filled.Bedtime,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(Space.lg))
            Column {
                Text(
                    text = "움직임 없음",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${formatDuration(minutes)}째",
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onSurface,
                    fontWeight = if (prolonged) FontWeight.Bold else FontWeight.SemiBold,
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

@Preview(showBackground = true, widthDp = 400, name = "짧음")
@Composable
private fun InactivityShortPreview() {
    CarnationTheme { InactivityCard(minutes = 45, modifier = Modifier.padding(Space.xl)) }
}

@Preview(showBackground = true, widthDp = 400, name = "길어짐")
@Composable
private fun InactivityLongPreview() {
    CarnationTheme { InactivityCard(minutes = 192, modifier = Modifier.padding(Space.xl)) }
}
