package kr.co.couponkeeper

import java.text.SimpleDateFormat
import java.util.*

object CouponParser {
    private val patterns = listOf(
        Regex("(20\\d{2})[년./-]\\s?(\\d{1,2})[월./-]\\s?(\\d{1,2})일?"),
        Regex("(\\d{4})\\s*년\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일")
    )
    fun findExpiry(text: String): Long? {
        val relevant = text.contains("유효기간") || text.contains("사용기한") || text.contains("만료") || text.contains("까지") || text.contains("쿠폰") || text.contains("교환권") || text.contains("상품권")
        if (!relevant) return null
        val lines = text.lines()
        val target = lines.firstOrNull { it.contains("유효기간") || it.contains("사용기한") || it.contains("만료") } ?: text
        for (p in patterns) {
            val m = p.find(target) ?: p.find(text) ?: continue
            val y=m.groupValues[1].toIntOrNull() ?: continue
            val mo=m.groupValues[2].toIntOrNull() ?: continue
            val d=m.groupValues[3].toIntOrNull() ?: continue
            val cal=Calendar.getInstance().apply { clear(); set(y,mo-1,d,23,59,0) }
            if (mo in 1..12 && d in 1..31 && cal.timeInMillis > System.currentTimeMillis()-86400000L) return cal.timeInMillis
        }
        return null
    }
    fun findTitle(text: String): String {
        val brands = listOf("파리바게뜨","배스킨라빈스","베스킨라빈스","스타벅스","올리브영","투썸플레이스","메가커피","컴포즈커피","이디야","CU","GS25","세븐일레븐")
        return brands.firstOrNull { text.contains(it, true) }?.let { "$it 쿠폰" } ?: "문자 쿠폰"
    }
}
