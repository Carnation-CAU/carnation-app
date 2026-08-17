package com.carnation.fallalert.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.carnation.fallalert.model.ParentProfile
import com.carnation.fallalert.ui.theme.CarnationTheme
import com.carnation.fallalert.ui.theme.Hairline
import com.carnation.fallalert.ui.theme.Radius
import com.carnation.fallalert.ui.theme.Space

/**
 * 설정 — 부모님 정보.
 *
 * 입력 화면이 아니라 **구조대가 받게 될 내용을 미리 보는 화면**에 가깝게 만들었다:
 * 폼 아래에 실제 전송될 문장을 그대로 보여준다. 주소나 현관 비밀번호가 틀린 걸
 * 사고 난 뒤에 발견하면 늦다.
 *
 * 항목을 섹션으로 묶었다. 한 덩어리로 세워두면 열 칸 넘는 폼이 벽처럼 보인다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profile: ParentProfile,
    onSave: (ParentProfile) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable(profile) { mutableStateOf(profile.name) }
    var age by rememberSaveable(profile) { mutableStateOf(profile.age.toString()) }
    var address by rememberSaveable(profile) { mutableStateOf(profile.address) }
    var doorCode by rememberSaveable(profile) { mutableStateOf(profile.doorCode) }
    var parentPhone by rememberSaveable(profile) { mutableStateOf(profile.parentPhone) }
    var guardianPhone by rememberSaveable(profile) { mutableStateOf(profile.guardianPhone) }
    var healthNotes by rememberSaveable(profile) { mutableStateOf(profile.healthNotes) }
    var bloodType by rememberSaveable(profile) { mutableStateOf(profile.bloodType) }

    val edited = remember(
        name, age, address, doorCode, parentPhone, guardianPhone, healthNotes, bloodType, profile,
    ) {
        profile.copy(
            name = name.trim(),
            age = age.toIntOrNull() ?: profile.age,
            address = address.trim(),
            doorCode = doorCode.trim(),
            parentPhone = parentPhone.trim(),
            guardianPhone = guardianPhone.trim(),
            healthNotes = healthNotes.trim(),
            bloodType = bloodType.trim(),
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenTitle(
            title = "부모님 정보",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                }
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.xl),
        ) {
            Text(
                text = "119에 보내는 문자와 전화 걸기에 이 정보를 사용해요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Space.xl))
            FormSection("기본 정보") {
                Field("이름", name, { name = it })
                Field("나이", age, { age = it.filter(Char::isDigit).take(3) }, KeyboardType.Number)
                Field("혈액형", bloodType, { bloodType = it })
            }

            Spacer(Modifier.height(Space.lg))
            FormSection("집") {
                Field("주소", address, { address = it }, singleLine = false)
                Field("현관 비밀번호", doorCode, { doorCode = it })
            }

            Spacer(Modifier.height(Space.lg))
            FormSection("연락처") {
                Field("부모님", parentPhone, { parentPhone = it }, KeyboardType.Phone)
                Field("응급 연락처 (자녀)", guardianPhone, { guardianPhone = it }, KeyboardType.Phone)
            }

            Spacer(Modifier.height(Space.lg))
            FormSection("건강") {
                Field(
                    "특이사항", healthNotes, { healthNotes = it },
                    singleLine = false, imeAction = ImeAction.Done,
                )
            }

            Spacer(Modifier.height(Space.xl))
            MessagePreview(edited)
            Spacer(Modifier.height(Space.xl))
        }

        BottomBarSurface {
            PrimaryActionButton(text = "저장", onClick = { onSave(edited) })
        }
    }
}

/** 섹션 헤더(작은 회색) + 흰 카드 안에 입력칸들. */
@Composable
private fun FormSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = Space.sm, start = Space.xs),
    )
    AppCard(contentPadding = Space.lg) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(label, style = MaterialTheme.typography.bodyMedium)
        },
        singleLine = singleLine,
        shape = Radius.chip,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        colors = OutlinedTextFieldDefaults.colors(
            // 평소엔 테두리를 거의 안 보이게 두고, 포커스된 칸만 블루로 드러난다.
            unfocusedBorderColor = Hairline,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Space.xs),
    )
}

/** 실제로 119에 갈 문장. 폼 값이 바뀌면 여기도 즉시 바뀐다. */
@Composable
private fun MessagePreview(profile: ParentProfile) {
    Text(
        text = "119에 이렇게 전달돼요",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = Space.sm, start = Space.xs),
    )
    AppCard {
        Text(
            text = profile.emergencyMessage(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun SettingsPreview() {
    CarnationTheme {
        SettingsScreen(profile = ParentProfile.MOCK, onSave = {}, onBack = {})
    }
}
