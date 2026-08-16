package com.carnation.fallalert.model

/**
 * 보호 대상자(부모님) 정보.
 *
 * 서버 계약(`FallEvent`) 과 무관한 **앱 전용** 타입이다. 그래서 `:contract` 가 아니라
 * 앱 모듈에 둔다 — 계약 모듈에 넣으면 서버가 알아야 하는 정보처럼 보인다.
 *
 * 지금은 ViewModel 메모리에만 있다. 앱을 껐다 켜면 목업 값으로 돌아간다.
 * TODO: DataStore 영구 저장. 현관 비밀번호가 들어가므로 저장할 때 암호화를 같이 검토할 것.
 */
data class ParentProfile(
    val name: String,
    val age: Int,
    val address: String,
    val doorCode: String,
    val parentPhone: String,
    val guardianPhone: String,
    val healthNotes: String,
    val bloodType: String,
) {
    /** 119 로 보낼 문자 본문. 구조대가 현장에 도착해서 바로 필요한 순서로 적는다. */
    fun emergencyMessage(): String = buildString {
        append("낙상 의심 발생. ")
        append("$name(${age}세), ")
        append("주소: $address, ")
        append("현관 비밀번호: $doorCode, ")
        append("특이사항: $healthNotes")
        if (bloodType.isNotBlank()) append(", 혈액형: $bloodType")
        append(". 확인 부탁드립니다.")
    }

    companion object {
        /** MVP 목업. 설정 화면에서 수정하면 이 값에서 출발해 바뀐다. */
        val MOCK = ParentProfile(
            name = "이순자",
            age = 74,
            address = "서울시 관악구 신림로 123, 행복아파트 101동 302호",
            doorCode = "1234*",
            parentPhone = "010-2345-6789",
            guardianPhone = "010-9876-5432",
            healthNotes = "고혈압, 무릎 관절염, 청력 저하",
            bloodType = "B형",
        )
    }
}
