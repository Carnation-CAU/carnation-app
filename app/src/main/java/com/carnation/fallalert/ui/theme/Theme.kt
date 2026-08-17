package com.carnation.fallalert.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 팔레트 역할 배분.
 *
 * `primary` 는 **블루**다. 빨강을 primary 로 두면 Material 기본값을 쓰는 모든 컴포넌트
 * (버튼, 스위치, 커서, 리플)가 빨개진다. 그러면 "낙상 의심만 빨강"이 지켜지지 않는다.
 * 위험 색은 `error` 로 두고, 필요한 곳에서만 명시적으로 가져다 쓴다.
 */
private val LightColors = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    primaryContainer = BlueLight,
    onPrimaryContainer = NavyDeep,

    secondary = NavyDeep,
    onSecondary = Color.White,
    secondaryContainer = BgGray,
    onSecondaryContainer = TextPrimary,

    tertiary = SuccessGreen,
    onTertiary = Color.White,

    background = BgGray,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,

    // 카드 위 보조 텍스트. TextSecondary 를 여기 두면 화면 전체에서 일관되게 나온다.
    surfaceVariant = BgGray,
    onSurfaceVariant = TextSecondary,

    outline = Hairline,
    outlineVariant = Hairline,

    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerBg,
    onErrorContainer = DangerRed,

    scrim = NavyDeep.copy(alpha = 0.32f),
)

/**
 * 다크 테마는 넣지 않았다.
 *
 * 리뉴얼 명세가 라이트 팔레트 하나만 정의하고 "큰 면적에 진한 네이비/파랑 칠하지 않기"를
 * 요구한다. 다크를 임의로 만들면 그 원칙을 내가 추측으로 대신 정하는 셈이고,
 * 지금은 명세대로 보이는 게 우선이다.
 *
 * TODO: 다크 팔레트가 정해지면 darkColorScheme 을 추가하고 isSystemInDarkTheme 으로 분기한다.
 */
@Composable
fun CarnationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}
