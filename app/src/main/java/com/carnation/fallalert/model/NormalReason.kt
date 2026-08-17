package com.carnation.fallalert.model

/**
 * "도움은 필요 없어요"를 고를 때 함께 알려주는 실제 상황.
 *
 * 오탐을 줄이는 데 쓰는 값이다. 모델이 `fall_like` 로 본 순간에 실제로 무슨 일이 있었는지가
 * 이 앱에서 유일하게 사람만 알 수 있는 정보다.
 *
 * [code] 는 서버로 나가는 값이라 한글 라벨과 분리한다 — 문구를 다듬어도 데이터는 안 깨진다.
 */
enum class NormalReason(val code: String, val label: String) {
    SAT_OR_LAY("sat_or_lay", "앉거나 누웠어요"),
    BENT_DOWN("bent_down", "몸을 숙이거나 물건을 집었어요"),
    DROPPED_OBJECT("dropped_object", "물건이 떨어졌어요"),
    OTHER_PERSON_OR_PET("other_person_or_pet", "다른 사람이나 반려동물이 움직였어요"),

    /**
     * 넘어지긴 했지만 스스로 일어난 경우. "낙상이 아님"과는 다르다 —
     * 실제 낙상이므로 모델 입장에서는 오탐이 아니고, 보호자 입장에서는 도움이 불필요하다.
     * 이 둘을 뭉개면 정확도 개선에 잘못된 신호가 들어간다.
     */
    FELL_BUT_RECOVERED("fell_but_recovered", "넘어졌지만 혼자 일어났어요"),

    NOT_AT_HOME("not_at_home", "집에 아무도 없었어요"),
    UNKNOWN("unknown", "잘 모르겠어요"),
}
