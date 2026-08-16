package com.carnation.fallalert.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * 간격·모서리·그림자 토큰.
 *
 * 값을 화면마다 직접 적으면 "여백 넉넉하게"가 화면마다 다른 뜻이 된다.
 * 밀도를 조정할 때는 여기 숫자만 만진다.
 */
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val section = 40.dp
}

object Radius {
    /** 카드. 크게 굴려야 정보 밀도가 낮아 보인다. */
    val card = RoundedCornerShape(24.dp)
    val button = RoundedCornerShape(18.dp)
    val chip = RoundedCornerShape(12.dp)
    val pill = RoundedCornerShape(percent = 50)
}

object Elevation {
    /**
     * 그림자는 은은하게. 카드를 띄우는 게 목적이 아니라 바탕에서 살짝 떼어내는 게 목적이다.
     * 2dp 를 넘기면 화면이 무거워진다.
     */
    val card = 1.dp
    val raised = 3.dp
}

object Motion {
    /** 상태 전환. 짧게 — 알림 앱에서 느린 전환은 초조함이 된다. */
    const val FAST = 120
    const val NORMAL = 220
    const val SLOW = 360
}

/** 터치 목표 최소 높이. 명세 8장(고령 대응) 기준으로 Material 기본 48dp 보다 크게 잡는다. */
object TouchTarget {
    val minimum = 56.dp
    val primaryAction = 64.dp
}
