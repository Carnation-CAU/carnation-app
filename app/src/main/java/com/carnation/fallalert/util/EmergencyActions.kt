package com.carnation.fallalert.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.carnation.fallalert.model.ParentProfile

/**
 * 전화·문자 앱을 여는 동작.
 *
 * **둘 다 앱이 직접 걸거나 보내지 않는다.** 다이얼러와 문자 작성 화면을 열어 줄 뿐이고,
 * 마지막 발신 버튼은 사람이 누른다. 오발송이 곧바로 119 출동으로 이어질 수 있는 기능이라
 * `CALL_PHONE` / `SEND_SMS` 권한은 일부러 요청하지 않았다. 권한이 없으니 코드가
 * 나중에 바뀌어도 자동 발신은 불가능하다.
 */
object EmergencyActions {

    private const val TAG = "EmergencyActions"
    const val EMERGENCY_NUMBER = "119"

    /** 다이얼러에 번호를 채워서 연다. 통화 버튼은 사용자가 누른다. */
    fun dial(context: Context, phoneNumber: String) {
        val digits = phoneNumber.filter { it.isDigit() || it == '+' }
        if (digits.isBlank()) {
            toast(context, "전화번호가 비어 있어요. 설정에서 입력해 주세요.")
            return
        }
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits"))
        start(context, intent, "전화 앱을 열 수 없어요.")
    }

    /**
     * 119 로 보낼 문자를 **미리 채운 작성 화면**을 연다. 전송은 사용자가 한다.
     * 자동 전송하지 않는 이유는 위 클래스 주석 참고.
     */
    fun composeEmergencySms(context: Context, profile: ParentProfile) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$EMERGENCY_NUMBER")).apply {
            putExtra("sms_body", profile.emergencyMessage())
        }
        start(context, intent, "문자 앱을 열 수 없어요.")
    }

    private fun start(context: Context, intent: Intent, failureMessage: String) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "처리할 앱이 없음: ${intent.action}", e)
            toast(context, failureMessage)
        }
    }

    private fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
