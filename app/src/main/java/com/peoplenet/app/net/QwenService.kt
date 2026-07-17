package com.peoplenet.app.net

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "QwenVoice"

/** Qwen 从一句话里抽取出来的原始结构（未做 App 侧的「关联已有借出」处理）。 */
data class QwenParse(
    val person: String?,
    // 新建联系人
    val newContactName: String?,
    val newContactRel: String?,
    val newContactNote: String?,
    // 标记已联系（“刚和妈妈通了电话”）
    val contacted: Boolean,
    // 提醒
    val reminderTitle: String?,
    val reminderDate: String?,
    val reminderTime: String?,
    // 人情账：kind = lend借出 / borrow借入 / give送礼 / receive收礼 / collect收回借出
    val amount: Int?,
    val kind: String?,
    val item: String?
) {
    val hasNewContact: Boolean get() = !newContactName.isNullOrBlank()
    val hasReminder: Boolean get() = !reminderTitle.isNullOrBlank()
    val hasMoney: Boolean get() = kind != null && ((amount != null && amount > 0) || !item.isNullOrBlank())
    val isEmpty: Boolean get() = !hasReminder && !hasMoney && !hasNewContact && !contacted
}

/**
 * 通义千问（DashScope，OpenAI 兼容模式）语音解析。
 * 网络在 IO 线程上跑；任何异常都往上抛，由调用方回退到本地解析。
 */
object QwenService {

    suspend fun parse(text: String, contactNames: List<String>, today: String): QwenParse? =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "parse input: $text")
            val payload = JSONObject().apply {
                put("model", QwenConfig.MODEL)
                put("temperature", 0.1)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", systemPrompt(contactNames, today)))
                    put(JSONObject().put("role", "user").put("content", text))
                })
            }

            val conn = (URL(QwenConfig.ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer ${QwenConfig.API_KEY}")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connectTimeout = 8000
                readTimeout = 14000
                doOutput = true
            }

            try {
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (code !in 200..299 || body.isNullOrBlank()) {
                    Log.w(TAG, "HTTP $code · ${body?.take(200)}")
                    return@withContext null
                }
                val content = JSONObject(body)
                    .getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content")
                Log.d(TAG, "HTTP 200 · content=$content")
                parseContent(content)
            } finally {
                conn.disconnect()
            }
        }

    private fun systemPrompt(names: List<String>, today: String): String = """
你是一个关系管理 App 的中文语音助手解析器。用户会说一句话，可能包含以下几类操作（可同时出现多个）：
1) 提醒 / 约见：「周六提醒我和老周钓鱼」
2) 人情往来：借出「借给老周两千」/ 借入「我找老周借了五百」/ 送礼「送了妈妈一条羊绒围巾」/ 收礼「收了王敏 600 的礼金」/ 收回借出「把老周借的两千收回来」
3) 新建联系人：「新建联系人王强，健身房认识的朋友」「加个联系人李姐，我们组新同事」
4) 标记已联系：「我刚和妈妈通了电话」「刚跟老周聊过了」

已知联系人：${names.joinToString("、")}。
语音转写常有同音错别字：句中人名与已知联系人读音相同或相近时（如 老州/老洲 ≈ 老周、临夕 ≈ 林夕），按已知联系人的写法返回。
新建联系人时 person 用新联系人的名字（不必在已知列表里）；其余操作 person 必须是已知联系人。
句子可能夹杂无关闲聊，只提取与上述操作相关的部分。
今天是 $today。

只输出一个 JSON 对象，不要任何解释或 Markdown 代码块：
{
  "person": "本句操作针对的联系人姓名；没有则 null",
  "new_contact": { "name": "王强", "relation": "家人/朋友/同事/同学 之一，判断不了用 朋友", "note": "一句备注，如 健身房认识" } 或 null,
  "contacted": true 或 false（用户表示刚联系过/聊过/打过电话）,
  "reminder": { "title": "简短标题，如 和老周钓鱼", "date_word": "用户原话里的日期词，原样返回、禁止换算：如 周六 / 明天 / 下周三 / 8月4日；没说则 null", "time": "如 上午 9:00，缺省用 上午 9:00" } 或 null,
  "money": { "amount": 金额数字(元,整数,没有则 null), "kind": "lend=借出 / borrow=借入 / give=送礼 / receive=收礼 / collect=收回借出", "item": "实物礼品名，如 羊绒围巾；非实物则 null" } 或 null
}
没有对应信息的字段用 null / false。person 为 null 且 new_contact 为 null 时，其它字段也应为 null。
""".trim()

    /** 解析服务端代理返回的同构 JSON（/api/voice/parse）。 */
    fun parseServerJson(content: String): QwenParse? = parseContent(content)

    private fun parseContent(content: String): QwenParse? {
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val json = JSONObject(content.substring(start, end + 1))

        fun JSONObject.str(key: String): String? =
            optString(key).takeIf { it.isNotBlank() && it != "null" }

        val person = json.str("person")
        val nc = json.optJSONObject("new_contact")
        val rem = json.optJSONObject("reminder")
        val mon = json.optJSONObject("money")

        val amount = mon?.let { if (it.has("amount") && !it.isNull("amount")) it.optInt("amount") else null }
            ?.takeIf { it > 0 }
        val kind = mon?.str("kind")?.lowercase()?.takeIf {
            it in listOf("lend", "borrow", "give", "receive", "collect")
        }

        return QwenParse(
            person = person,
            newContactName = nc?.str("name"),
            newContactRel = nc?.str("relation"),
            newContactNote = nc?.str("note"),
            contacted = json.optBoolean("contacted", false),
            reminderTitle = rem?.str("title"),
            reminderDate = rem?.str("date_word") ?: rem?.str("date"),
            reminderTime = rem?.str("time"),
            amount = amount,
            kind = kind,
            item = mon?.str("item")
        )
    }
}
