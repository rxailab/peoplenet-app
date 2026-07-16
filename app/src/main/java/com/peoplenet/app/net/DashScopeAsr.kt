package com.peoplenet.app.net

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

/**
 * 通义千问 Paraformer 实时语音识别（DashScope WebSocket）。
 * 边录边传 16k 单声道 PCM，边回增量文字 + 实时音量（驱动波形）。
 *
 * 每次 [start] 建一个独立 Session（独立 task_id / WebSocket / AudioRecord），
 * 旧会话立即作废——快速 停止→重开 不会再出现 finish-task 抢跑 run-task
 * 导致的 "Missing task_group" 协议错误，也不会误释放新会话的麦克风。
 *
 * 调用 [start] 前必须已获得 RECORD_AUDIO 权限。
 */
class DashScopeAsr {

    private val main = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var session: Session? = null

    /**
     * @param onText     (显示文本, 该句是否结束)
     * @param onLevel    实时音量 0f..1f（每 ~100ms 一次，主线程回调）
     * @param onFinished 任务结束（含服务器静音超时等服务器侧结束；主线程回调），调用方可无缝重连
     * @param onError    出错（主线程回调），UI 回退到手动打字
     */
    fun start(
        onText: (String, Boolean) -> Unit,
        onLevel: (Float) -> Unit,
        onFinished: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!QwenConfig.enabled) { onError("未配置 API-KEY"); return }
        session?.close()
        session = Session(onText, onLevel, onFinished, onError).also { it.open() }
    }

    /** 说完了：停录；已建立的任务发 finish-task 等服务器收尾，未建立的直接关。 */
    fun stop() { session?.finish() }

    /** 面板关闭 / VM 销毁：立即释放。 */
    fun release() { session?.close(); session = null }

    private inner class Session(
        val onText: (String, Boolean) -> Unit,
        val onLevel: (Float) -> Unit,
        val onFinished: () -> Unit,
        val onError: (String) -> Unit
    ) : WebSocketListener() {

        private val taskId = UUID.randomUUID().toString().replace("-", "")
        private var ws: WebSocket? = null
        private var record: AudioRecord? = null
        @Volatile private var recording = false
        @Volatile private var taskStarted = false
        @Volatile private var closed = false
        private val committed = StringBuilder()

        private val isCurrent: Boolean get() = session === this && !closed

        fun open() {
            val req = Request.Builder()
                .url(QwenConfig.ASR_WS_ENDPOINT)
                .header("Authorization", "bearer ${QwenConfig.API_KEY}")
                .header("X-DashScope-DataInspection", "enable")
                .build()
            ws = client.newWebSocket(req, this)
        }

        fun finish() {
            recording = false
            releaseRecord()
            // run-task 尚未确认时不能发 finish-task（会先于 run-task 到达 → 协议错误），直接关连接
            if (taskStarted) {
                try { ws?.send(control("finish-task")) } catch (_: Exception) { close() }
            } else close()
        }

        fun close() {
            closed = true
            recording = false
            releaseRecord()
            try { ws?.close(1000, "bye") } catch (_: Exception) {}
        }

        private fun releaseRecord() {
            val r = record ?: return
            record = null
            try { if (r.recordingState == AudioRecord.RECORDSTATE_RECORDING) r.stop() } catch (_: Exception) {}
            try { r.release() } catch (_: Exception) {}
        }

        private fun control(action: String) = JSONObject().apply {
            put("header", JSONObject().put("action", action).put("task_id", taskId).put("streaming", "duplex"))
            put("payload", JSONObject().put("input", JSONObject()))
        }.toString()

        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (closed) return
            val run = JSONObject().apply {
                put("header", JSONObject().put("action", "run-task").put("task_id", taskId).put("streaming", "duplex"))
                put("payload", JSONObject().apply {
                    put("task_group", "audio"); put("task", "asr"); put("function", "recognition")
                    put("model", "paraformer-realtime-v2")
                    put("parameters", JSONObject().put("sample_rate", 16000).put("format", "pcm"))
                    put("input", JSONObject())
                })
            }
            webSocket.send(run.toString())
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val ev = JSONObject(text)
                when (ev.getJSONObject("header").optString("event")) {
                    "task-started" -> {
                        Log.d(TAG, "task-started ${taskId.take(8)}")
                        taskStarted = true
                        if (!closed) startRecording(webSocket)
                    }
                    "result-generated" -> {
                        val sentence = ev.getJSONObject("payload").getJSONObject("output").optJSONObject("sentence")
                        val t = sentence?.optString("text") ?: return
                        if (t.isBlank()) return
                        val end = sentence.optBoolean("sentence_end", false)
                        val display = committed.toString() + t
                        main.post { if (isCurrent) onText(display, end) }
                        if (end) committed.append(t)
                    }
                    "task-finished" -> {
                        Log.d(TAG, "task-finished ${taskId.take(8)}")
                        recording = false; releaseRecord()
                        try { webSocket.close(1000, "done") } catch (_: Exception) {}
                        main.post { if (isCurrent) onFinished() }
                    }
                    "task-failed" -> {
                        val msg = ev.getJSONObject("header").optString("error_message", "识别失败")
                        Log.w(TAG, "task-failed ${taskId.take(8)}: $msg")
                        recording = false; releaseRecord()
                        main.post { if (isCurrent) onError(msg) }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "parse: $e")
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (closed) return   // 主动关闭旧会话触发的失败不当错误
            Log.w(TAG, "ws failure: ${t.message} · code=${response?.code}")
            recording = false; releaseRecord()
            main.post { if (isCurrent) onError("语音连接失败") }
        }

        @SuppressLint("MissingPermission")
        private fun startRecording(webSocket: WebSocket) {
            if (recording || closed) return
            val minBuf = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val bufSize = maxOf(minBuf, FRAME * 4)
            val rec = try {
                AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize)
            } catch (e: Exception) {
                Log.w(TAG, "AudioRecord init: $e"); main.post { if (isCurrent) onError("麦克风初始化失败") }; return
            }
            if (rec.state != AudioRecord.STATE_INITIALIZED) {
                rec.release(); main.post { if (isCurrent) onError("麦克风不可用") }; return
            }
            record = rec
            recording = true
            rec.startRecording()
            Thread {
                val buf = ByteArray(FRAME)
                while (recording && !closed) {
                    val n = rec.read(buf, 0, buf.size)
                    if (n > 0) {
                        // 实时音量（RMS → 0..1），驱动 UI 波形
                        var sum = 0L
                        var i = 0
                        while (i + 1 < n) {
                            val s = ((buf[i + 1].toInt() shl 8) or (buf[i].toInt() and 0xFF)).toShort().toInt()
                            sum += s.toLong() * s
                            i += 2
                        }
                        val rms = sqrt(sum.toDouble() / (n / 2))
                        val level = (rms / 6000.0).coerceIn(0.0, 1.0).toFloat()
                        main.post { if (isCurrent) onLevel(level) }
                        try { webSocket.send(buf.toByteString(0, n)) } catch (_: Exception) { break }
                    } else if (n < 0) break
                }
            }.apply { isDaemon = true; start() }
        }
    }

    companion object {
        private const val TAG = "QwenAsr"
        private const val FRAME = 3200   // 100ms @ 16k mono 16-bit
    }
}
