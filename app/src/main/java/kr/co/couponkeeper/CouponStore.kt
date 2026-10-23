package kr.co.couponkeeper

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Coupon(
    val id: String,
    val title: String,
    val body: String,
    val expiry: Long,
    val used: Boolean = false
)

object CouponStore {
    private const val KEY = "items"

    fun save(ctx: Context, title: String, body: String, expiry: Long): Coupon? {
        val old = all(ctx)
        if (old.any { it.body == body }) return null
        val coupon = Coupon(UUID.randomUUID().toString(), title, body, expiry, false)
        saveOrUpdate(ctx, coupon)
        return coupon
    }

    fun saveOrUpdate(ctx: Context, coupon: Coupon) {
        val items = all(ctx).associateBy { it.id }.toMutableMap()
        items[coupon.id] = coupon
        val array = JSONArray()
        items.values.sortedBy { it.expiry }.forEach {
            array.put(JSONObject().put("id", it.id).put("title", it.title)
                .put("body", it.body).put("expiry", it.expiry).put("used", it.used))
        }
        ctx.getSharedPreferences("coupons", Context.MODE_PRIVATE)
            .edit().putString(KEY, array.toString()).apply()
    }

    fun markUsed(ctx: Context, id: String): Coupon? {
        val coupon = all(ctx).firstOrNull { it.id == id } ?: return null
        return coupon.copy(used = true).also { saveOrUpdate(ctx, it) }
    }

    fun all(ctx: Context): List<Coupon> {
        val array = JSONArray(ctx.getSharedPreferences("coupons", Context.MODE_PRIVATE).getString(KEY, "[]"))
        return (0 until array.length()).mapNotNull { i ->
            array.optJSONObject(i)?.let {
                Coupon(
                    id = it.optString("id").ifBlank { UUID.randomUUID().toString() },
                    title = it.optString("title"),
                    body = it.optString("body"),
                    expiry = it.optLong("expiry"),
                    used = it.optBoolean("used", false)
                )
            }
        }.sortedBy { it.expiry }
    }
}