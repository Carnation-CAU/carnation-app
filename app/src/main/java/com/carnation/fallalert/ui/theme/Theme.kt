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

// ─────────────────────────────────────────────────────────────────────────────
// 팔레트: 네이비 베이스 + 시그널 블루 포인트 + 따뜻한 코랄.
//
// 역할 배분
//   네이비  → 글자·상단바. 화면 전체의 차분한 바탕 톤을 만든다.
//   블루    → 신뢰·안심. "정상" 확인, 정보성 강조.
//   코랄    → 경고. 따뜻하지만 채도를 낮추지 않았다.
//
// 명세 8장이 "위험 = 빨강 계열 명확히"를 요구한다. 코랄을 파스텔로 빼면 이 요구가
// 무너지므로, 경고색은 흰 글자가 AA 를 통과하는 깊은 코랄레드로 잡았다.
// 카드 강조(테두리 + 틴트 배경)도 그대로 두어 미확인 알림이 묻히지 않게 한다.
//
// "정상" 버튼을 초록 대신 블루로 바꾼 건 팔레트 통일 때문만이 아니다.
// 초록/빨강 조합은 적록색약(남성 약 8%)에게 구분이 어렵다. 블루/코랄은 색상뿐 아니라
// 명도까지 갈리므로 오히려 더 안전하다.
// ─────────────────────────────────────────────────────────────────────────────

val Navy = Color(0xFF2E3A59)          // 베이스. 본문 글자색이자 상단바 색
val NavyDeep = Color(0xFF1F2740)
val NavySoft = Color(0xFF64718E)      // 보조 텍스트. 흰 배경에서 4.8:1 — 그레이톤이되 흐리지 않게

val SignalBlue = Color(0xFF5B8DEF)    // 포인트
val BlueAction = Color(0xFF3A6FD8)    // 흰 글자를 얹는 버튼용 (AA 통과)
val BlueTint = Color(0xFFE4EDFD)
val BlueInk = Color(0xFF14315F)

val Coral = Color(0xFFC9453A)         // 경고. 흰 글자 AA 통과
val CoralSoft = Color(0xFFF2836E)     // 볼터치. 글자를 얹지 않는 곳에만
val CoralTint = Color(0xFFFFE9E3)
val CoralInk = Color(0xFF5C1C14)

val Canvas = Color(0xFFF7F8FB)        // 살짝 푸른 기가 도는 바탕
val Mist = Color(0xFFE9ECF4)          // 확인 완료 이력·배너

private val LightColors = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    primaryContainer = CoralTint,
    onPrimaryContainer = CoralInk,
    secondary = BlueAction,
    onSecondary = Color.White,
    secondaryContainer = BlueTint,
    onSecondaryContainer = BlueInk,
    tertiary = Navy,
    onTertiary = Color.White,
    background = Canvas,
    onBackground = Navy,
    surface = Color.White,
    onSurface = Navy,
    surfaceVariant = Mist,
    onSurfaceVariant = NavySoft,
    outline = Color(0xFFB9C2D6),
    error = Coral,
    onError = Color.White,
)

// 다크도 같은 네이비 계열 위에서 굴린다. 회색 다크가 아니라 "밤의 네이비".
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8A75),
    onPrimary = Color(0xFF4A140C),
    primaryContainer = Color(0xFF6E241A),
    onPrimaryContainer = Color(0xFFFFDAD2),
    secondary = Color(0xFF8FB4FF),
    onSecondary = Color(0xFF14315F),
    secondaryContainer = Color(0xFF2A4780),
    onSecondaryContainer = Color(0xFFD8E4FF),
    tertiary = Color(0xFFB6C3E0),
    onTertiary = NavyDeep,
    background = Color(0xFF171C2B),
    onBackground = Color(0xFFE8EBF2),
    surface = Color(0xFF202638),
    onSurface = Color(0xFFE8EBF2),
    surfaceVariant = Color(0xFF2C3447),
    onSurfaceVariant = Color(0xFFA9B3CA),
    outline = Color(0xFF505C77),
    error = Color(0xFFFF8A75),
    onError = Color(0xFF4A140C),
)

/**
 * 위계는 크기가 아니라 **크기 차이**로 만든다.
 *
 * 제목은 굵고 크게, 본문은 절제된 네이비 그레이로 떨어뜨려 정보 밀도를 낮춰 보이게 한다.
 * 다만 본문 하한은 18sp 로 잡았다 — 명세 8장(고령 사용자)이 있는 앱이라
 * 일반적인 금융앱 본문(15~16sp)까지 내리지 않는다.
 *
 * 큰 숫자에는 음수 자간을 준다. 굵은 숫자는 기본 자간이면 헐렁해 보인다.
 */
private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 44.sp, lineHeight = 50.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-1.2).sp,
    ),
    displaySmall = TextStyle(
        fontSize = 34.sp, lineHeight = 40.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 27.sp, lineHeight = 36.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 23.sp, lineHeight = 31.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontSize = 21.sp, lineHeight = 29.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp,
    ),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold),
)

@Composable
fun CarnationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
