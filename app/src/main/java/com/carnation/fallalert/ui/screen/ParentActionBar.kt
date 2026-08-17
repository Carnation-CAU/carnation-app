package com.carnation.fallalert.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.carnation.fallalert.model.ParentProfile
import com.carnation.fallalert.ui.theme.CarnationTheme
import com.carnation.fallalert.ui.theme.DangerRed
import com.carnation.fallalert.ui.theme.Space
import com.carnation.fallalert.util.EmergencyActions

/**
 * 홈 · 상세 하단에 고정되는 대응 버튼 두 개.
 *
 * 두 버튼은 폭 · 모서리 · 테두리 두께가 같고 색만 다르다. 전화는 흰 면 + 회색 테두리,
 * 119 는 빨강 채움.
 *
 * 두 버튼 모두 앱이 직접 걸거나 보내지 않는다. 전화 앱 · 문자 앱을 열어줄 뿐이다.
 */
@Composable
fun ParentActionBar(
    profile: ParentProfile,
    modifier: Modifier = Modifier,
    /**
     * 119 문자 화면을 연 직후 호출된다. 상세 화면에서는 이걸로 "도움 필요"를 기록한다.
     * 홈에서는 대상 이벤트가 특정되지 않으므로 넘기지 않는다.
     */
    onEmergencyComposed: (() -> Unit)? = null,
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            // 두 버튼은 같은 폭 · 같은 모서리 · 같은 1dp 테두리다. 색만 다르다.
            // 아이콘을 뺀 건 폭을 균등하게 나누면서 "119에 정보 보내기"를 한 줄로
            // 유지하려면 글자 자리가 필요했기 때문이다. 라벨이 아이콘보다 명확하다.
            SecondaryActionButton(
                text = "부모님께 전화",
                onClick = { EmergencyActions.dial(context, profile.parentPhone) },
                modifier = Modifier.weight(1f),
                horizontalPadding = Space.sm,
            )
            PrimaryActionButton(
                text = "119에 정보 보내기",
                onClick = {
                    EmergencyActions.composeEmergencySms(context, profile)
                    // 문자 앱으로 넘어가는 순간 기록한다. 실제 전송 여부는 앱이 알 수 없지만,
                    // 보호자가 119를 부르기로 결정한 사실 자체가 "도움 필요"다.
                    onEmergencyComposed?.invoke()
                },
                modifier = Modifier.weight(1f),
                containerColor = DangerRed,
                contentColor = Color.White,
                borderColor = DangerRed,
                horizontalPadding = Space.sm,
            )
        }

        Spacer(Modifier.height(Space.sm))
        // 버튼을 누르는 순간 통화나 전송이 일어난다고 오해하면, 급할 때 누르기를 망설인다.
        Text(
            text = "화면만 열려요 · 보내기는 직접 눌러요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true, widthDp = 400, backgroundColor = 0xFFF2F4F6)
@Composable
private fun ParentActionBarPreview() {
    CarnationTheme {
        BottomBarSurface {
            ParentActionBar(profile = ParentProfile.MOCK)
        }
    }
}
