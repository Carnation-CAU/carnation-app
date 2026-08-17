package com.carnation.fallalert.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * 간격 토큰. 미니멀은 여백에서 나오므로 이 숫자들이 디자인의 절반이다.
 * 밀도를 조정할 땐 화면을 고치지 말고 여기만 만진다.
 */
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp

    /** 화면 좌우 패딩 · 카드 안쪽 패딩. 둘을 같은 값으로 두면 리듬이 맞는다. */
    val xl = 20.dp

    /** 섹션 사이 */
    val xxl = 28.dp
    val section = 32.dp
}

object Radius {
    val card = RoundedCornerShape(16.dp)
    val button = RoundedCornerShape(14.dp)
    val chip = RoundedCornerShape(10.dp)
    val pill = RoundedCornerShape(percent = 50)
}

/**
 * 그림자를 거의 쓰지 않는다.
 *
 * 카드는 회색 배경(BgGray) 위의 흰 면으로 이미 분리된다. 거기에 그림자를 얹으면
 * 카드마다 뜬 느낌이 나고, 그게 "AI가 만든 화면" 특유의 무거움이다.
 * 예외는 하단 고정 바처럼 콘텐츠가 실제로 밑을 지나가는 경우뿐이다.
 */
object Elevation {
    val none = 0.dp

    /** 활성 세그먼트·하단 고정 바. 있는지 모를 정도로만. */
    val subtle = 1.dp
}

object Motion {
    const val FAST = 120
    const val NORMAL = 220
}

/** 터치 목표. 사용자는 보호자(성인)라 Material 기준을 따르고, 주 액션만 넉넉하게. */
object TouchTarget {
    val minimum = 48.dp

    /** 하단 풀와이드 버튼 높이 */
    val primaryAction = 56.dp
}

/** 얇은 구분선 두께. 1dp 는 고밀도 화면에서 두껍게 보인다. */
val HairlineThickness = 1.dp
