package com.carnation.fallalert.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carnation.fallalert.model.NormalReason
import com.carnation.fallalert.ui.theme.BluePrimary
import com.carnation.fallalert.ui.theme.CarnationTheme
import com.carnation.fallalert.ui.theme.Hairline
import com.carnation.fallalert.ui.theme.Radius
import com.carnation.fallalert.ui.theme.Space
import com.carnation.fallalert.ui.theme.TouchTarget

/**
 * "도움은 필요 없어요"를 누른 뒤, 실제로 무슨 상황이었는지 고르는 시트.
 *
 * 이 화면이 앱에서 유일하게 **사람만 아는 정보를 받아 오는 자리**다. 모델은 `fall_like` 로
 * 본 순간에 실제로 뭐가 있었는지 알 수 없고, 그걸 아는 건 보호자뿐이다.
 *
 * 건너뛰기를 막지 않는다. 고르지 않아도 확인은 기록된다 — 입력을 강제하면
 * 급한 사람이 아무거나 찍고, 그 데이터가 정확도를 오히려 망친다.
 *
 * 체크박스는 Material 기본 대신 직접 그렸다. 기본 체크박스는 사각 테두리가 굵어서
 * 목록이 빽빽해 보인다. 선택된 항목만 블루로 채워 대비를 만든다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NormalReasonSheet(
    onDismiss: () -> Unit,
    onSubmit: (List<NormalReason>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by rememberSaveable { mutableStateOf(emptySet<String>()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.xl)
                .padding(bottom = Space.xl),
        ) {
            Text(
                text = "그때 어떤 상황이었나요?",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                text = "알려주시면 잘못된 알림을 줄이는 데 씁니다. 해당하는 것을 모두 골라 주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Space.lg))

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                NormalReason.entries.forEach { reason ->
                    ReasonRow(
                        label = reason.label,
                        checked = reason.code in selected,
                        onToggle = {
                            selected = if (reason.code in selected) {
                                selected - reason.code
                            } else {
                                selected + reason.code
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(Space.xl))

            PrimaryActionButton(
                // 아무것도 안 골라도 진행할 수 있다는 걸 라벨로 알린다.
                text = if (selected.isEmpty()) "선택 없이 확인" else "확인",
                onClick = { onSubmit(NormalReason.entries.filter { it.code in selected }) },
            )
        }
    }
}

@Composable
private fun ReasonRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TouchTarget.minimum)
            .clip(Radius.chip)
            .clickable(onClick = onToggle)
            .padding(vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckMark(checked = checked)
        Spacer(Modifier.width(Space.md))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CheckMark(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (checked) BluePrimary else Color.Transparent)
            .then(
                if (checked) Modifier
                else Modifier.border(
                    1.5.dp, Hairline, RoundedCornerShape(7.dp),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun ReasonRowsPreview() {
    CarnationTheme {
        Column(Modifier.padding(Space.xl)) {
            NormalReason.entries.forEachIndexed { i, reason ->
                ReasonRow(label = reason.label, checked = i % 3 == 0, onToggle = {})
            }
        }
    }
}
