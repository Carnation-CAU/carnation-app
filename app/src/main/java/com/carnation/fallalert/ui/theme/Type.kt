package com.carnation.fallalert.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 굵기는 Regular / SemiBold / Bold **세 단계만** 쓴다. 단계를 늘리면 위계가 흐려지고
 * 결국 "글자가 많아 보이는" 화면이 된다. 위계는 크기와 색으로 만든다.
 *
 * 줄간격은 1.4~1.5배. 미니멀은 여백에서 나오는데 그 여백의 절반은 행간이다.
 *
 * 큰 숫자(위험도·무동작 시간)에만 음수 자간을 준다 — 굵은 숫자는 기본 자간이면 헐렁하다.
 */
val AppTypography = Typography(
    // 핵심 수치. 위험도 %처럼 "이 화면에서 제일 먼저 읽혀야 하는 값"
    displayLarge = TextStyle(
        fontSize = 40.sp, lineHeight = 48.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-1.0).sp,
    ),
    // 보조 수치. 무동작 시간, 미확인 건수
    displayMedium = TextStyle(
        fontSize = 30.sp, lineHeight = 38.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp,
    ),

    // 화면 큰 타이틀 24 / Bold
    headlineMedium = TextStyle(
        fontSize = 24.sp, lineHeight = 34.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp,
    ),
    // 빈 화면 안내처럼 조금 작은 제목
    headlineSmall = TextStyle(
        fontSize = 20.sp, lineHeight = 28.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp,
    ),

    // 섹션 헤더 18 / Bold
    titleLarge = TextStyle(
        fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold,
    ),
    // 카드 제목 17 / SemiBold
    titleMedium = TextStyle(
        fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold,
    ),

    // 본문 16 / Regular
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    // 보조·설명 14 / Regular
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    // 캡션 13 / Regular
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),

    // 버튼 16 / Bold
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold),
    // 배지
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
)
