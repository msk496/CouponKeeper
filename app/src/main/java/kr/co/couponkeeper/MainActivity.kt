package kr.co.couponkeeper

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val required = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28,36,28,24) }
        root.addView(TextView(this).apply { text="쿠폰지킴이"; textSize=28f; setPadding(0,0,0,8) })
        root.addView(TextView(this).apply { text="가족 공유 쿠폰 · 변경사항은 주기적으로 동기화됩니다."; textSize=15f })
        root.addView(Button(this).apply { text="권한 허용 및 자동 감지 시작"; setOnClickListener { askPermissions() } })
        root.addView(Button(this).apply { text="공유 시트 연결 설정"; setOnClickListener { configureSync() } })
        root.addView(Button(this).apply { text="지금 동기화"; setOnClickListener {
            if (CouponSync.endpoint(this@MainActivity).isBlank()) configureSync()
            else { CouponSync.enqueue(this@MainActivity); Toast.makeText(this@MainActivity,"동기화를 요청했습니다.",Toast.LENGTH_SHORT).show() }
        } })
        root.addView(Button(this).apply { text="쿠폰 목록 / 사용 처리"; setOnClickListener { showCoupons() } })
        root.addView(Button(this).apply { text="테스트 쿠폰 추가"; setOnClickListener {
            val coupon = CouponStore.save(this@MainActivity, "테스트 쿠폰", "테스트용 쿠폰", System.currentTimeMillis()+3*86400000L)
            if (coupon != null) { Notifier.show(this@MainActivity,coupon.title,"쿠폰이 저장됐습니다.",coupon.id); CouponSync.enqueue(this@MainActivity,coupon) }
            showCoupons()
        } })
        root.addView(TextView(this).apply { text="공유 설정은 가족 기기마다 한 번씩 해야 합니다. 서버 없는 방식이라 다른 기기에는 보통 15분 주기의 동기화 때 반영되며 즉시 PUSH는 아닙니다."; textSize=13f; setPadding(0,18,0,0) })
        setContentView(ScrollView(this).apply { addView(root) })
        if (!hasPermissions()) askPermissions()
        if (CouponSync.endpoint(this).isNotBlank()) CouponSync.enqueue(this)
    }
    private fun hasPermissions() = required.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    private fun askPermissions() {
        val perms = required.toMutableList()
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        requestPermissions(perms.distinct().toTypedArray(), 12)
    }
    override fun onRequestPermissionsResult(requestCode:Int, permissions:Array<out String>, grantResults:IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Toast.makeText(this, if (hasPermissions()) "문자 감지 권한이 설정되었습니다." else "일부 권한이 없어 자동 감지가 제한됩니다.", Toast.LENGTH_LONG).show()
    }
    private fun configureSync() {
        val layout = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(40,12,40,0) }
        val url = EditText(this).apply { hint="Apps Script 웹 앱 URL (/exec)"; setSingleLine(true); setText(CouponSync.endpoint(this@MainActivity)) }
        val key = EditText(this).apply { hint="가족 공유 키"; setSingleLine(true); setText(CouponSync.key(this@MainActivity)) }
        layout.addView(url); layout.addView(key)
        AlertDialog.Builder(this).setTitle("공유 시트 연결").setMessage("Google Sheets에 연결한 Apps Script 웹 앱 URL과 공유 키를 입력하세요.")
            .setView(layout).setNegativeButton("취소",null).setPositiveButton("저장") { _,_ ->
                val u=url.text.toString().trim(); val k=key.text.toString().trim()
                if (!u.startsWith("https://script.google.com/") || !u.endsWith("/exec") || k.isBlank()) Toast.makeText(this,"올바른 Apps Script /exec URL과 공유 키를 입력하세요.",Toast.LENGTH_LONG).show()
                else { CouponSync.configure(this,u,k); Toast.makeText(this,"공유 연결을 저장하고 동기화를 시작했습니다.",Toast.LENGTH_LONG).show() }
            }.show()
    }
    private fun showCoupons() {
        val data = CouponStore.all(this)
        if (data.isEmpty()) {
            AlertDialog.Builder(this).setMessage("저장된 쿠폰이 없습니다. 공유 연결을 설정한 뒤 동기화하거나 쿠폰 문자를 받아보세요.").setPositiveButton("확인",null).show(); return
        }
        val list = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(28,16,28,16) }
        data.forEach { coupon ->
            val item = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,12,0,12) }
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date(coupon.expiry))
            item.addView(TextView(this).apply { text="${coupon.title}\n만료일: $date\n${if(coupon.used) "사용 완료" else "미사용"}"; textSize=16f })
            item.addView(TextView(this).apply { text=coupon.body; textSize=12f; maxLines=3 })
            if (!coupon.used) item.addView(Button(this).apply {
                text="사용 완료 처리"
                setOnClickListener {
                    val updated=CouponStore.markUsed(this@MainActivity,coupon.id)
                    if (updated != null) {
                        Notifier.cancel(this@MainActivity,coupon.id)
                        CalendarHelper.removeForCoupon(this@MainActivity,coupon.id)
                        CouponSync.enqueue(this@MainActivity,updated)
                        Toast.makeText(this@MainActivity,"사용 처리했습니다. 다른 기기에는 다음 동기화 때 반영됩니다.",Toast.LENGTH_LONG).show()
                    }
                    showCoupons()
                }
            })
            list.addView(item, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            list.addView(android.view.View(this).apply { setBackgroundColor(0xFFDDDDDD.toInt()) }, ViewGroup.LayoutParams.MATCH_PARENT, 1)
        }
        AlertDialog.Builder(this).setTitle("쿠폰 목록").setView(ScrollView(this).apply { addView(list) }).setPositiveButton("닫기",null).show()
    }
}