package com.carnation.fallalert.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carnation.fallalert.model.ParentProfile
import com.carnation.fallalert.ui.theme.CarnationTheme
import com.carnation.fallalert.ui.theme.Radius
import com.carnation.fallalert.ui.theme.Space
import com.carnation.fallalert.ui.theme.TouchTarget
import com.carnation.fallalert.util.EmergencyActions

/**
 * 홈·상세 하단에 함께 놓이는 대응 버튼 두 개.
 *
 * 보호자가 알림을 보고 **다음에 할 행동**이 이 둘이다. 그래서 두 화면에서 같은 자리에
 * 같은 모양으로 있어야 한다 — 급할 때 위치를 다시 찾게 하면 안 된다.
 *
 * 두 버튼 모두 앱이 직접 걸거나 보내지 않는다. 전화 앱·문자 앱을 열어줄 뿐이다.
 */
@Composable
fun ParentActionBar(
    profile: ParentProfile,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            Button(
                onClick = { EmergencyActions.dial(context, profile.parentPhone) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = TouchTarget.primaryAction),
                shape = Radius.button,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = Space.sm,
                    vertical = Space.md,
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
            ) {
                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(Space.sm))
                Text("부모님께 전화", textAlign = TextAlign.Center)
            }

            Button(
                onClick = { EmergencyActions.composeEmergencySms(context, profile) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = TouchTarget.primaryAction),
                shape = Radius.button,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = Space.sm,
                    vertical = Space.md,
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    Icons.Filled.LocalHospital,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(Space.sm))
                Text("119에 정보 보내기", textAlign = TextAlign.Center)
            }
        }

        Spacer(Modifier.height(Space.sm))
        // 버튼을 누르는 순간 통화나 전송이 일어난다고 오해하면, 급할 때 누르기를 망설인다.
        Text(
            text = "누르면 전화 · 문자 화면이 열려요. 보내기는 직접 눌러야 해요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.sm),
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun ParentActionBarPreview() {
    CarnationTheme {
        ParentActionBar(
            profile = ParentProfile.MOCK,
            modifier = Modifier.padding(Space.xl),
        )
    }
}
