package com.peoplenet.app.wxapi

import android.content.Context
import android.widget.Toast
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory

/**
 * 微信开放平台配置。
 *
 * ⚠️ 接入真实微信登录必须替换 [APP_ID]：
 *   1. 到 https://open.weixin.qq.com 注册「移动应用」，拿到 AppID（形如 wx1234567890abcdef）。
 *   2. 在开放平台后台填入本应用的包名 `com.peoplenet.app` 和应用签名（release keystore 的 MD5）。
 *      当前 release 用 debug 签名，签名 MD5 可用 `keytool -list -v -keystore ~/.android/debug.keystore` 获取。
 *   3. AppSecret 只能放在你的后端，用授权拿到的 code 换取 access_token + openid + 用户信息，
 *      千万不要写进 App。见 [com.peoplenet.app.viewmodel.PeopleNetViewModel.onWechatResult]。
 */
object WxConfig {
    const val APP_ID = "wxYOUR_WECHAT_APPID"  // TODO: 换成你在微信开放平台注册的真实 AppID
    fun isConfigured() = APP_ID.startsWith("wx") && !APP_ID.contains("YOUR")
}

/** 微信授权结果的进程内中转：WXEntryActivity 收到回调后回传给 ViewModel。 */
object WxAuthRelay {
    /** (errCode, code)：errCode 0=成功，code 为授权码（需后端换 token）。 */
    var onResult: ((Int, String?) -> Unit)? = null
}

object WeChatAuth {
    private var api: IWXAPI? = null

    fun api(context: Context): IWXAPI {
        return api ?: WXAPIFactory.createWXAPI(context.applicationContext, WxConfig.APP_ID, true).also {
            it.registerApp(WxConfig.APP_ID)
            api = it
        }
    }

    /** 发起微信授权登录（拉起微信 App 的授权页）。 */
    fun login(context: Context) {
        if (!WxConfig.isConfigured()) {
            Toast.makeText(context, "微信登录未配置 AppID（见 WeChatAuth.WxConfig）", Toast.LENGTH_LONG).show()
            return
        }
        val wx = api(context)
        if (!wx.isWXAppInstalled) {
            Toast.makeText(context, "未安装微信", Toast.LENGTH_SHORT).show()
            return
        }
        val req = SendAuth.Req().apply {
            scope = "snsapi_userinfo"
            state = "peoplenet_login"
        }
        wx.sendReq(req)
    }
}
