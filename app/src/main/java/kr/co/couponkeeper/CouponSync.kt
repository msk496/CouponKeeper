package kr.co.couponkeeper

import android.content.Context
import androidx.work.*
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object CouponSync {
    private const val PREFS = "coupon_sync"
    fun configure(context: Context, endpoint: String, key: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("endpoint", endpoint.trim()).putString("key", key.trim()).apply()
        val request = PeriodicWorkRequestBuilder<CouponSyncWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("coupon-periodic-sync", ExistingPeriodicWorkPolicy.UPDATE, request)
        CouponStore.all(context).forEach { enqueue(context, it) }
        enqueue(context)
    }
    fun enqueue(context: Context, coupon: Coupon? = null) {
        val data = workDataOf("coupon_json" to coupon?.let { JSONObject().put("id",it.id).put("title",it.title).put("body",it.body).put("expiry",it.expiry).put("used",it.used).toString() })
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<CouponSyncWorker>().setInputData(data).build())
    }
    fun endpoint(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("endpoint", "") ?: ""
    fun key(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("key", "") ?: ""
}

class CouponSyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val endpoint = CouponSync.endpoint(applicationContext)
        val key = CouponSync.key(applicationContext)
        if (endpoint.isBlank() || key.isBlank()) return Result.success()
        return try {
            val outgoing = inputData.getString("coupon_json")
            if (outgoing != null) request("POST", endpoint, key, JSONObject(outgoing).put("key", key).let { JSONObject().put("key",key).put("action","upsert").put("coupon",JSONObject(outgoing)) })
            val response = JSONObject(request("GET", endpoint, key, null))
            val array = response.optJSONArray("coupons") ?: JSONArray()
            val existing = CouponStore.all(applicationContext).associateBy { it.id }
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val coupon = Coupon(obj.optString("id"),obj.optString("title"),obj.optString("body"),obj.optLong("expiry"),obj.optBoolean("used",false))
                if (coupon.id.isBlank()) continue
                val before = existing[coupon.id]
                if (before == null && !coupon.used) {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date(coupon.expiry))
                    Notifier.show(applicationContext, "공유 쿠폰 등록", "${coupon.title} · 만료일 $date", coupon.id)
                } else if (before != null && !before.used && coupon.used) {
                    Notifier.cancel(applicationContext, coupon.id)
                    CalendarHelper.removeForCoupon(applicationContext, coupon.id)
                }
                if (before == null && !coupon.used) CalendarHelper.addIfPermitted(applicationContext, coupon.id, coupon.title, coupon.body, coupon.expiry)
                CouponStore.saveOrUpdate(applicationContext, coupon)
            }
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
    private fun request(method: String, endpoint: String, key: String, payload: JSONObject?): String {
        val encoded = URLEncoder.encode(key, "UTF-8")
        val url = URL(endpoint + (if (endpoint.contains("?")) "&" else "?") + "key=" + encoded)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 12000; readTimeout = 12000
            setRequestProperty("Accept", "application/json")
            if (payload != null) { doOutput = true; setRequestProperty("Content-Type", "application/json; charset=utf-8") }
        }
        try {
            if (payload != null) connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) throw IllegalStateException("Sync HTTP $code")
            return body
        } finally { connection.disconnect() }
    }
}
