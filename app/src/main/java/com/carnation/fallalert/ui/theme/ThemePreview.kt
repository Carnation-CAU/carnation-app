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
 * 팔레트·타이포를 한눈에 보는 프리뷰. [Theme.kt] · [Color.kt] · [Type.kt] 를 고치면 즉시 반영된다.
 * 앱 동작에는 관여하지 않는다.
 */
@Composable
private fun Swatch(name: String, color: Color, usage: String, onColor: Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(Radius.chip)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            if (onColor != null) {
                Text("가", style = MaterialTheme.typography.bodyLarge, color = onColor)
            }
        }
        Spacer(Modifier.width(Space.lg))
        Column {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(
                usage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TypeSample(name: String, style: TextStyle, sample: String = "낙상 의심 90%") {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Space.xs),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp),
        )
        Text(sample, style = style)
    }
}

@Preview(name = "팔레트 · 타이포", showBackground = true, heightDp = 1500, widthDp = 400)
@Composable
private fun DesignSystemPreview() {
    CarnationTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(Space.xl),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text("브랜드", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Space.sm))
            Swatch("BluePrimary", BluePrimary, "주요 버튼 · 링크 · 포인트", Color.White)
            Swatch("BlueLight", BlueLight, "연한 강조 · 선택 칩", NavyDeep)
            Swatch("NavyDeep", NavyDeep, "헤드라인 · 강조 텍스트", Color.White)

            Spacer(Modifier.height(Space.xl))
            Text("뉴트럴", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Space.sm))
            Swatch("BgGray", BgGray, "화면 배경", TextPrimary)
            Swatch("SurfaceWhite", SurfaceWhite, "카드 · 시트", TextPrimary)
            Swatch("TextPrimary", TextPrimary, "본문", Color.White)
            Swatch("TextSecondary", TextSecondary, "보조 회색", Color.White)
            Swatch("TextMuted", TextMuted, "힌트 · 캡션", Color.White)
            Swatch("Hairline", Hairline, "얇은 구분선", TextPrimary)

            Spacer(Modifier.height(Space.xl))
            Text("상태", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Space.sm))
            Swatch("DangerRed", DangerRed, "낙상 의심 — 이것만 빨강", Color.White)
            Swatch("DangerBg", DangerBg, "낙상 카드 배경", DangerRed)
            Swatch("WarnAmber", WarnAmber, "무동작 경계", Color.White)
            Swatch("SuccessGreen", SuccessGreen, "정상 확인", Color.White)

            Spacer(Modifier.height(Space.xl))
            Text("타이포", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Space.sm))
            val t = MaterialTheme.typography
            TypeSample("displayLarge", t.displayLarge, "90%")
            TypeSample("displayMedium", t.displayMedium, "3시간 12분")
            TypeSample("headlineMedium", t.headlineMedium, "낙상 의심 알림")
            TypeSample("headlineSmall", t.headlineSmall, "확인한 알림이 없어요")
            TypeSample("titleLarge", t.titleLarge, "확인이 필요해요")
            TypeSample("titleMedium", t.titleMedium, "거실")
            TypeSample("bodyLarge", t.bodyLarge, "넘어짐과 유사한 움직임")
            TypeSample("bodyMedium", t.bodyMedium, "8월 5일 오전 10:30")
            TypeSample("bodySmall", t.bodySmall, "화면만 열려요")
            TypeSample("labelLarge", t.labelLarge, "119에 정보 보내기")
        }
    }
}
