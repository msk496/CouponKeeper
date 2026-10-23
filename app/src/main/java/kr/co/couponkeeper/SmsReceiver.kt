package kr.co.couponkeeper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.*

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        for (sms in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
            val body = sms.messageBody ?: continue
            val expiry = CouponParser.findExpiry(body) ?: continue
            val title = CouponParser.findTitle(body)
            val coupon = CouponStore.save(context, title, body, expiry) ?: continue
            Notifier.show(context, title, "쿠폰이 저장됐습니다.", coupon.id)
            CalendarHelper.addIfPermitted(context, title, body, expiry)
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<CouponSyncWorker>().build())
        }
    }
}