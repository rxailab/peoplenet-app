package com.peoplenet.app.net

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * PeopleNet 线上后端（Render + MongoDB Atlas）。
 * 登录后持有 JWT；语音解析优先走服务端代理（DashScope key 在服务端）。
 * Render 免费实例冷启动约 50 秒——App 启动时先 [warmup] 唤醒。
 */
object ApiClient {

    const val BASE = "https://peoplenet-api.onrender.com"
    private const val TAG = "ApiClient"

    /** 云端登录后的 JWT（内存态，随进程存亡，与原型一致）。 */
    @Volatile
    var token: String? = null
        private set

    private suspend fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        timeoutMs: Int = 65000
    ): Pair<Int, String>? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(BASE + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15000
                readTimeout = timeoutMs
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                token?.let { setRequestProperty("Authorization", "Bearer $it") }
                if (body != null) doOutput = true
            }
            try {
                if (body != null) conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                Log.d(TAG, "$method $path → $code")
                code to text
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "$method $path failed: $e")
            null
        }
    }

    /** 唤醒 Render 免费实例；App 启动时调一次（结果只记日志）。 */
    suspend fun warmup() {
        request("GET", "/api/health", timeoutMs = 70000)
    }

    suspend fun sendCode(phone: String): Boolean =
        request("POST", "/api/auth/send-code", JSONObject().put("phone", phone))?.first == 200

    /** 验证码登录；成功后持有 JWT。 */
    suspend fun verify(phone: String, code: String, nickname: String? = null): Boolean {
        val body = JSONObject().put("phone", phone).put("code", code)
        if (!nickname.isNullOrBlank()) body.put("nickname", nickname)
        val r = request("POST", "/api/auth/verify", body) ?: return false
        if (r.first != 200) return false
        token = JSONObject(r.second).optString("token").takeIf { it.isNotBlank() }
        return token != null
    }

    /** 语音解析（服务端代理 Qwen）。返回模型 JSON 字符串；未登录云端或失败返回 null。 */
    suspend fun voiceParse(text: String, contacts: List<String>, today: String): String? {
        if (token == null) return null
        val body = JSONObject()
            .put("text", text)
            .put("contacts", JSONArray(contacts))
            .put("today", today)
        val r = request("POST", "/api/voice/parse", body, timeoutMs = 25000) ?: return null
        return if (r.first == 200) r.second else null
    }
}
