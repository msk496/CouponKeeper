package kr.co.couponkeeper

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("coupons", MODE_PRIVATE) }
    private val required = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS,
        Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28,36,28,24) }
        root.addView(TextView(this).apply { text="쿠폰지킴이"; textSize=28f; setPadding(0,0,0,8) })
        root.addView(TextView(this).apply { text="문자 쿠폰을 감지하고 만료일을 관리합니다."; textSize=15f })
        root.addView(Button(this).apply { text="권한 허용 및 자동 감지 시작"; setOnClickListener { askPermissions() } })
        root.addView(Button(this).apply { text="저장된 쿠폰 보기"; setOnClickListener { showCoupons() } })
        root.addView(Button(this).apply { text="테스트 쿠폰 추가"; setOnClickListener { CouponStore.save(this@MainActivity, "테스트 쿠폰", "테스트용 쿠폰", System.currentTimeMillis()+3*86400000L) ; showCoupons() } })
        root.addView(TextView(this).apply { text="안내: SMS 권한은 문자 내용을 읽기 위해 필요합니다. 이미지/MMS 쿠폰과 월렛 자동 등록은 아직 지원하지 않으며, 날짜 추출이 불확실하면 수동 확인이 필요합니다."; textSize=13f; setPadding(0,18,0,0) })
        setContentView(root)
        if (!hasPermissions()) askPermissions()
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
    private fun showCoupons() {
        val data = CouponStore.all(this)
        if (data.isEmpty()) { AlertDialog.Builder(this).setMessage("저장된 쿠폰이 없습니다. 쿠폰 문자가 도착하면 자동으로 감지합니다.").setPositiveButton("확인",null).show(); return }
        val lines = data.joinToString("\n\n") { c -> "${c.title}\n${c.body}\n만료일: ${SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date(c.expiry))}" }
        AlertDialog.Builder(this).setTitle("저장된 쿠폰").setMessage(lines).setPositiveButton("확인",null).show()
    }
}
