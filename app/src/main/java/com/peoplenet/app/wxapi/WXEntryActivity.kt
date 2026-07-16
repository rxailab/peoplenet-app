package com.peoplenet.app.wxapi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler

/**
 * 微信授权/分享回调入口。微信 SDK 强制要求路径为
 * `<applicationId>.wxapi.WXEntryActivity`（即 com.peoplenet.app.wxapi.WXEntryActivity）。
 * 收到授权结果后通过 [WxAuthRelay] 回传给主界面的 ViewModel，然后立即 finish。
 */
class WXEntryActivity : ComponentActivity(), IWXAPIEventHandler {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            WeChatAuth.api(this).handleIntent(intent, this)
        } catch (e: Exception) {
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        WeChatAuth.api(this).handleIntent(intent, this)
    }

    override fun onReq(req: BaseReq) {}

    override fun onResp(resp: BaseResp) {
        if (resp.type == ConstantsAPI.COMMAND_SENDAUTH) {
            val code = (resp as? SendAuth.Resp)?.code
            WxAuthRelay.onResult?.invoke(resp.errCode, code)
        }
        finish()
    }
}
