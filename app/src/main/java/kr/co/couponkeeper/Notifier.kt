package kr.co.couponkeeper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object Notifier {
    fun show(ctx:Context,title:String,message:String) {
        val nm=ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>=26) nm.createNotificationChannel(NotificationChannel("coupons","쿠폰 알림",NotificationManager.IMPORTANCE_DEFAULT))
        val n=if(Build.VERSION.SDK_INT>=26) android.app.Notification.Builder(ctx,"coupons") else android.app.Notification.Builder(ctx)
        n.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("쿠폰 자동 저장").setContentText("$title · $message").setAutoCancel(true)
        nm.notify((title+message).hashCode(),n.build())
    }
}
