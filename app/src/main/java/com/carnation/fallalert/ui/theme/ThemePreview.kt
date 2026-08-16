package com.carnation.fallalert.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * [Theme.kt] 를 고치면서 결과를 바로 보라고 만든 프리뷰 전용 파일.
 *
 * Theme.kt 자체에는 그릴 것이 없어서 프리뷰가 뜨지 않는다. 색·글씨 크기를 만질 때는
 * 이 파일을 Split 으로 열어 두면 팔레트와 타이포가 한 화면에 보인다.
 * 앱 동작에는 관여하지 않는다.
 */
@Composable
private fun Swatch(name: String, color: Color, onColor: Color, usage: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text("가", style = MaterialTheme.typography.bodyLarge, color = onColor)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(
                usage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TypeSample(name: String, style: TextStyle) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(130.dp),
        )
        Text("낙상 의심 90%", style = style)
    }
}

@Composable
private fun DesignSystem() {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .background(scheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text("색", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Swatch("primary", scheme.primary, scheme.onPrimary, "경고 · 미확인 · 도움 필요 버튼 (코랄)")
        Swatch("primaryContainer", scheme.primaryContainer, scheme.onPrimaryContainer, "미확인 카드 배경")
        Swatch("secondary", scheme.secondary, scheme.onSecondary, "정상 버튼 · 안심 (블루)")
        Swatch("secondaryContainer", scheme.secondaryContainer, scheme.onSecondaryContainer, "정상 확인 배경")
        Swatch("tertiary", scheme.tertiary, scheme.onTertiary, "네이비 — 베이스 잉크")
        Swatch("surfaceVariant", scheme.surfaceVariant, scheme.onSurfaceVariant, "확인 완료 이력 · 배너")
        Swatch("surface", scheme.surface, scheme.onSurface, "카드 · 상단바")
        Swatch("background", scheme.background, scheme.onBackground, "화면 바탕")

        Spacer(Modifier.height(12.dp))
        Text("볼터치 (글자를 얹지 않는 곳에만)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(CoralSoft, SignalBlue, NavySoft).forEach { c ->
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(c))
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("글씨", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        val type = MaterialTheme.typography
        TypeSample("displaySmall", type.displaySmall)
        TypeSample("headlineMedium", type.headlineMedium)
        TypeSample("headlineSmall", type.headlineSmall)
        TypeSample("titleLarge", type.titleLarge)
        TypeSample("titleMedium", type.titleMedium)
        TypeSample("bodyLarge", type.bodyLarge)
        TypeSample("bodyMedium", type.bodyMedium)
        TypeSample("labelLarge", type.labelLarge)

        Spacer(Modifier.height(16.dp))
        Text(
            "고령 사용자 대응이라 본문이 19sp 부터다. 줄이기 전에 명세 8장을 확인할 것.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(name = "팔레트 · 라이트", showBackground = true, heightDp = 1100)
@Composable
private fun DesignSystemLightPreview() {
    CarnationTheme(darkTheme = false) { DesignSystem() }
}

@Preview(name = "팔레트 · 다크", showBackground = true, heightDp = 1100)
@Composable
private fun DesignSystemDarkPreview() {
    CarnationTheme(darkTheme = true) { DesignSystem() }
}
