package com.peoplenet.app.net

/**
 * 通义千问（Qwen · 阿里云 DashScope）接入配置。
 *
 * 使用步骤：
 *  1. 在阿里云百炼 / DashScope 控制台开通「通义千问」并创建 API-KEY。
 *  2. 把 KEY 粘到下面的 [API_KEY]（引号之间）。
 *  3. 账号在海外（Model Studio / International）用默认的 dashscope-intl 端点；
 *     国内账号请把 [ENDPOINT] 换成注释里的国内地址。
 *
 * 未填 KEY 时，语音助手自动回退到本地规则解析（离线可用），不影响其它功能。
 */
object QwenConfig {

    /** DashScope API-KEY（形如 "sk-xxxxxxxx"）。留空 = 走本地解析。 */
    const val API_KEY = "sk-b6484ef7dd11452aa471ecc8b09cb712"

    /** 模型：qwen-turbo（快，抽取类任务够用）/ qwen-plus（均衡）/ qwen-max（最强）。 */
    const val MODEL = "qwen-turbo"

    /**
     * OpenAI 兼容端点。
     * 国内账号（当前 KEY）：https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
     * 海外账号：           https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions
     */
    const val ENDPOINT = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"

    /**
     * Paraformer 实时语音识别 WebSocket 端点。
     * 国内账号（当前）：wss://dashscope.aliyuncs.com/api-ws/v1/inference/
     * 海外账号：       wss://dashscope-intl.aliyuncs.com/api-ws/v1/inference/
     */
    const val ASR_WS_ENDPOINT = "wss://dashscope.aliyuncs.com/api-ws/v1/inference/"

    /** 「今天」取真实系统日期，供模型换算相对日期（周六 / 明天 …）。 */
    val TODAY: String get() = com.peoplenet.app.data.DateUtil.todayForPrompt()

    val enabled: Boolean get() = API_KEY.isNotBlank()
}
