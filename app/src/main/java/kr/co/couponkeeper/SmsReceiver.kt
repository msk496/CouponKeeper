package kr.co.couponkeeper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import java.text.SimpleDateFormat
import java.util.*

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        for (sms in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
            val body = sms.messageBody ?: continue
            val expiry = CouponParser.findExpiry(body) ?: continue
            val title = CouponParser.findTitle(body)
            CouponStore.save(context, title, body, expiry)
            Notifier.show(context, title, "쿠폰 만료일: ${SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date(expiry))}")
            CalendarHelper.addIfPermitted(context, title, body, expiry)
        }
    }
}
