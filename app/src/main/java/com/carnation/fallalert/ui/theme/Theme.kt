package com.carnation.fallalert.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 보호자·고령 사용자 대응: 강한 색 대비. 위험은 명확한 빨강 계열로 고정한다.
val Danger = Color(0xFFC62828)
val DangerContainer = Color(0xFFFFE5E3)
val OnDangerContainer = Color(0xFF6B0F0B)
val Safe = Color(0xFF1B6E3C)
val SafeContainer = Color(0xFFDFF3E5)
val Neutral = Color(0xFF5A5F66)
val NeutralContainer = Color(0xFFEDEEF1)

private val LightColors = lightColorScheme(
    primary = Danger,
    onPrimary = Color.White,
    primaryContainer = DangerContainer,
    onPrimaryContainer = OnDangerContainer,
    secondary = Safe,
    onSecondary = Color.White,
    secondaryContainer = SafeContainer,
    onSecondaryContainer = Color(0xFF0B3D20),
    background = Color(0xFFFBFBFD),
    onBackground = Color(0xFF15171A),
    surface = Color.White,
    onSurface = Color(0xFF15171A),
    surfaceVariant = NeutralContainer,
    onSurfaceVariant = Color(0xFF3C4046),
    error = Danger,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF6B60),
    onPrimary = Color(0xFF3A0704),
    primaryContainer = Color(0xFF5C120D),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFF7ED9A0),
    onSecondary = Color(0xFF06341A),
    secondaryContainer = Color(0xFF14512C),
    onSecondaryContainer = Color(0xFFC8F2D6),
    background = Color(0xFF121316),
    onBackground = Color(0xFFEDEEF1),
    surface = Color(0xFF1B1D21),
    onSurface = Color(0xFFEDEEF1),
    surfaceVariant = Color(0xFF2A2D32),
    onSurfaceVariant = Color(0xFFC7CAD0),
    error = Color(0xFFFF6B60),
    onError = Color(0xFF3A0704),
)

/** 기본 Material 타이포보다 전반적으로 크게 — 고령 사용자 가독성. */
private val LargeTypography = Typography(
    displaySmall = TextStyle(fontSize = 44.sp, lineHeight = 52.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 19.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
)

@Composable
fun CarnationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = LargeTypography,
        content = content,
    )
}
