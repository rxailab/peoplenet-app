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

    /** 发送验证码结果。devCode 非空 = 短信通道未配置，服务端回传验证码联调。 */
    data class SendCodeResult(val ok: Boolean, val devCode: String, val retryAfter: Int)

    suspend fun sendCode(phone: String): SendCodeResult? {
        val r = request("POST", "/api/auth/send-code", JSONObject().put("phone", phone)) ?: return null
        val json = try { JSONObject(r.second) } catch (e: Exception) { JSONObject() }
        return SendCodeResult(
            ok = r.first == 200,
            devCode = json.optString("devCode"),
            retryAfter = json.optInt("retryAfter", 0)
        )
    }

    /** 验证码校验结果。 */
    sealed class VerifyOutcome {
        data class Ok(val nickname: String, val isNew: Boolean) : VerifyOutcome()
        data class Rejected(val message: String, val attemptsLeft: Int) : VerifyOutcome()
        object Unreachable : VerifyOutcome()
    }

    /** 验证码登录/注册；成功后持有 JWT。 */
    suspend fun verify(phone: String, code: String): VerifyOutcome {
        val body = JSONObject().put("phone", phone).put("code", code)
        val r = request("POST", "/api/auth/verify", body) ?: return VerifyOutcome.Unreachable
        val json = try { JSONObject(r.second) } catch (e: Exception) { JSONObject() }
        if (r.first != 200) {
            return VerifyOutcome.Rejected(
                message = json.optString("error", "验证失败"),
                attemptsLeft = json.optInt("attemptsLeft", -1)
            )
        }
        token = json.optString("token").takeIf { it.isNotBlank() }
        val user = json.optJSONObject("user")
        return VerifyOutcome.Ok(
            nickname = user?.optString("nickname") ?: "",
            isNew = json.optBoolean("isNew", true)
        )
    }

    /** 注册后完善昵称。 */
    suspend fun updateProfile(nickname: String): Boolean =
        request("PUT", "/api/auth/profile", JSONObject().put("nickname", nickname))?.first == 200

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
