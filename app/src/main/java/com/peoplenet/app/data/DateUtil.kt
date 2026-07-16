package com.peoplenet.app.data

import java.util.Calendar

/** 真实系统日期工具：语音解析与首页「今天」都据此显示，不再写死 mock 日期。 */
object DateUtil {

    private val WEEKDAYS = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

    private fun cal(offsetDays: Int = 0): Calendar =
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offsetDays) }

    /** "7月16日"（偏移 offsetDays 天）。 */
    fun monthDay(offsetDays: Int = 0): String {
        val c = cal(offsetDays)
        return "${c.get(Calendar.MONTH) + 1}月${c.get(Calendar.DAY_OF_MONTH)}日"
    }

    /** "周四"（偏移 offsetDays 天）。 */
    fun weekday(offsetDays: Int = 0): String = WEEKDAYS[cal(offsetDays).get(Calendar.DAY_OF_WEEK) - 1]

    /** 给 Qwen 的「今天」："7月16日 周四"。日期换算不交给模型，由 [resolveDateWord] 本地确定性计算。 */
    fun todayForPrompt(): String = "${monthDay()} ${weekday()}"

    private val WEEK_CHAR = mapOf(
        "一" to Calendar.MONDAY, "二" to Calendar.TUESDAY, "三" to Calendar.WEDNESDAY,
        "四" to Calendar.THURSDAY, "五" to Calendar.FRIDAY, "六" to Calendar.SATURDAY,
        "日" to Calendar.SUNDAY, "天" to Calendar.SUNDAY
    )

    /**
     * 把口语日期词确定性换算成「周X M月D日」（小模型算日期不可靠，一律本地算）：
     * 今天/明天/后天/大后天、周X（最近的将来）、下周X/下下周X、M月D日（补真实周几）。解析不了原样返回。
     */
    fun resolveDateWord(word: String): String {
        val w = word.trim()
        // 绝对日期 M月D日 → 补真实周几
        Regex("(\\d{1,2})月(\\d{1,2})").find(w)?.let { m ->
            val month = m.groupValues[1].toInt()
            val day = m.groupValues[2].toInt()
            val c = Calendar.getInstance()
            if (month < c.get(Calendar.MONTH) + 1) c.add(Calendar.YEAR, 1)   // 已过月份→明年
            c.set(Calendar.MONTH, month - 1); c.set(Calendar.DAY_OF_MONTH, day)
            return "${WEEKDAYS[c.get(Calendar.DAY_OF_WEEK) - 1]} ${month}月${day}日"
        }
        if (w.contains("大后天")) return "大后天 ${monthDay(3)}"
        if (w.contains("后天")) return "后天 ${monthDay(2)}"
        if (w.contains("明天")) return "明天 ${monthDay(1)}"
        if (w.contains("今天") || w.contains("今晚")) return "今天 ${monthDay(0)}"
        val m = Regex("(下+)?(?:周|星期|礼拜)([一二三四五六日天])").find(w) ?: return w
        val downs = m.groupValues[1].length          // 「下」的个数：0=本周最近，1=下周，2=下下周
        val target = WEEK_CHAR[m.groupValues[2]]!!
        val c = Calendar.getInstance()
        val diff: Int = if (downs == 0) {
            (target - c.get(Calendar.DAY_OF_WEEK) + 7) % 7   // 最近的将来（今天是周X就指今天）
        } else {
            // 下周X = 下个周一起算的那周；每多一个「下」再加 7 天
            var toNextMonday = (Calendar.MONDAY - c.get(Calendar.DAY_OF_WEEK) + 7) % 7
            if (toNextMonday == 0) toNextMonday = 7
            val fromMonday = (target - Calendar.MONDAY + 7) % 7
            toNextMonday + (downs - 1) * 7 + fromMonday
        }
        return "周${m.groupValues[2].replace("天", "日")} ${monthDay(diff)}"
    }

    /** 首页 hero 头："周四 · 7月16日 · 今天"。 */
    fun homeHeader(): String = "${weekday()} · ${monthDay()} · 今天"

    /** 完成态 hero 头："周四 · 7月16日"。 */
    fun homeHeaderShort(): String = "${weekday()} · ${monthDay()}"

    /** 下一个指定星期几（不含今天）的日期，如 nextWeekday(Calendar.SATURDAY) → "7月19日"。 */
    fun nextWeekday(target: Int): String {
        val c = Calendar.getInstance()
        var diff = (target - c.get(Calendar.DAY_OF_WEEK) + 7) % 7
        if (diff == 0) diff = 7
        return monthDay(diff)
    }
}
