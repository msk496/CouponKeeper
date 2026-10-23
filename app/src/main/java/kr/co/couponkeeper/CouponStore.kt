package kr.co.couponkeeper

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Coupon(val title:String,val body:String,val expiry:Long)
object CouponStore {
    private const val KEY="items"
    fun save(ctx:Context,title:String,body:String,expiry:Long) {
        val p=ctx.getSharedPreferences("coupons",Context.MODE_PRIVATE)
        val old=JSONArray(p.getString(KEY,"[]"))
        for(i in 0 until old.length()) if(old.optJSONObject(i)?.optString("body")==body) return
        val obj=JSONObject().put("title",title).put("body",body).put("expiry",expiry)
        old.put(obj); p.edit().putString(KEY,old.toString()).apply()
    }
    fun all(ctx:Context):List<Coupon> {
        val a=JSONArray(ctx.getSharedPreferences("coupons",Context.MODE_PRIVATE).getString(KEY,"[]"))
        return (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let { Coupon(it.optString("title"),it.optString("body"),it.optLong("expiry")) } }.sortedBy { it.expiry }
    }
}
