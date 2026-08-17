package com.carnation.fallalert.ui.theme

import androidx.compose.ui.graphics.Color

// ─── 브랜드 ──────────────────────────────────────────────────────────────────
val NavyDeep = Color(0xFF0D1B3D)      // 헤드라인·강조 텍스트·다크 표면
val BluePrimary = Color(0xFF3A6CF4)   // 주요 버튼·활성 상태·링크·포인트
val BlueLight = Color(0xFFA8C5FF)     // 연한 강조·선택 칩·보조 배경

// ─── 뉴트럴 ──────────────────────────────────────────────────────────────────
val BgGray = Color(0xFFF2F4F6)        // 화면 배경 / 그룹 카드 배경
val SurfaceWhite = Color(0xFFFFFFFF)  // 카드·시트
val TextPrimary = Color(0xFF191F28)   // 본문 (거의 검정)
val TextSecondary = Color(0xFF6B7684) // 보조 회색
val TextMuted = Color(0xFFA0A8B4)     // 힌트·캡션
val Hairline = Color(0xFFE5E8EB)      // 얇은 구분선

// ─── 상태 ────────────────────────────────────────────────────────────────────
// 낙상은 안전 이슈라 빨강을 유지한다. 브랜드 블루와 역할이 완전히 다르다:
// 빨강은 "낙상 의심"에만, 파랑은 브랜드·일반 액션에만. 이 경계를 흐리면
// 차분한 화면에서 위험이 눈에 걸리지 않는다.
val DangerRed = Color(0xFFE94A4A)     // 낙상 의심·위험
val DangerBg = Color(0xFFFDECEC)      // 낙상 카드 배경
val WarnAmber = Color(0xFFF5A623)     // 무동작 경계
val SuccessGreen = Color(0xFF16A34A)  // 정상 확인
