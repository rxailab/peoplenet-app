package com.peoplenet.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peoplenet.app.ui.theme.*
import com.peoplenet.app.viewmodel.AuthStage
import com.peoplenet.app.viewmodel.PeopleNetViewModel
import com.peoplenet.app.wxapi.WeChatAuth
import kotlinx.coroutines.delay

private val AuthBg = Brush.verticalGradient(listOf(Color(0xFFF4F0FF), Color(0xFFFBF9FF)))
private val AuthPink = Color(0xFFF0567E)
private val AuthCardBorder = Color(0xFFF1EDFB)
private val AuthHint = Color(0xFFB6ACD6)
private val AuthSub = Color(0xFF8478A8)
private val AuthFaint = Color(0xFFA79CC9)
private val AuthPurpleGrad = Brush.linearGradient(listOf(PurplePrimary, Color(0xFF9C72E8)))

// ============ 登录流程（未登录时替换整个 App）============

@Composable
fun AuthFlow(viewModel: PeopleNetViewModel) {
    val stage = viewModel.state.auth.stage
    BackHandler(enabled = stage != AuthStage.Phone) { viewModel.authBackStep() }
    Box(modifier = Modifier.fillMaxSize().background(AuthBg)) {
        when (stage) {
            AuthStage.Phone -> PhoneLoginScreen(viewModel)
            AuthStage.Otp -> OtpScreen(viewModel)
            AuthStage.Profile -> ProfileSetupScreen(viewModel)
        }
        GeoToast(viewModel)   // 复用统一 toast（登录校验提示等）
    }
}

// ① 登录 / 注册
@Composable
private fun PhoneLoginScreen(viewModel: PeopleNetViewModel) {
    val auth = viewModel.state.auth
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 28.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top = 90.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(76.dp)
                    .shadow(14.dp, RoundedCornerShape(24.dp), ambientColor = PurplePrimary.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(24.dp)).background(AuthPurpleGrad)
            ) { Text("网", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold) }
            Text("人际关系网", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = DarkText, modifier = Modifier.padding(top = 20.dp))
            Text("把重要的人，放在心上", fontSize = 13.5.sp, color = AuthSub, modifier = Modifier.padding(top = 8.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 52.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = PurplePrimary.copy(alpha = 0.08f))
                    .clip(RoundedCornerShape(16.dp)).background(Color.White)
                    .border(1.dp, AuthCardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Text("+86", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = DarkText)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.width(1.dp).height(16.dp).background(AuthCardBorder))
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = auth.phone,
                    onValueChange = { viewModel.authSetPhone(it) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle = TextStyle(fontSize = 14.5.sp, color = DarkText, fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (auth.phone.isEmpty()) Text("请输入手机号", color = AuthHint, fontSize = 14.5.sp)
                        inner()
                    }
                )
            }
            Text(
                text = "获取验证码", textAlign = TextAlign.Center,
                fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = PurplePrimary.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(16.dp)).background(PurplePrimary)
                    .clickable { viewModel.authSendCode() }.padding(vertical = 16.dp)
            )
            Row(
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
                    .border(1.dp, AuthCardBorder, RoundedCornerShape(16.dp))
                    .clickable {
                        if (auth.agreed) WeChatAuth.login(context)
                        else viewModel.showToastPublic("请先阅读并同意用户协议与隐私政策")
                    }.padding(vertical = 15.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF1FC9A0))
                ) { Text("微", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) }
                Spacer(Modifier.width(8.dp))
                Text("微信一键登录", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = DarkText)
            }
        }

        Text(
            "先逛逛，稍后登录 ›", textAlign = TextAlign.Center, fontSize = 13.sp, color = AuthSub,
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp).clickable { viewModel.authGuest() }
        )

        Spacer(Modifier.weight(1f))

        Row(modifier = Modifier.padding(bottom = 26.dp)) {
            AgreeCheckbox(checked = auth.agreed, onToggle = { viewModel.authToggleAgree() })
            Spacer(Modifier.width(7.dp))
            Text(
                text = buildAnnotatedString {
                    append("我已阅读并同意 ")
                    withStyle(SpanStyle(color = PurplePrimary, fontWeight = FontWeight.SemiBold)) { append("《用户协议》") }
                    append("和")
                    withStyle(SpanStyle(color = PurplePrimary, fontWeight = FontWeight.SemiBold)) { append("《隐私政策》") }
                    append("，未注册手机号将自动创建账号")
                },
                fontSize = 11.5.sp, color = AuthFaint, lineHeight = 17.sp
            )
        }
    }
}

// ② 输入验证码
@Composable
private fun OtpScreen(viewModel: PeopleNetViewModel) {
    val auth = viewModel.state.auth
    val focus = remember { FocusRequester() }
    var secs by remember { mutableStateOf(60) }
    var resendTick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(resendTick) {
        secs = 60
        while (secs > 0) { delay(1000); secs-- }
    }
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 28.dp)
    ) {
        Text("‹", fontSize = 26.sp, color = DarkText, modifier = Modifier.padding(top = 8.dp).clickable { viewModel.authBackStep() })
        Text("输入验证码", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = DarkText, modifier = Modifier.padding(top = 28.dp))
        Text(
            text = buildAnnotatedString {
                append("验证码已发送至 ")
                withStyle(SpanStyle(color = DarkText, fontWeight = FontWeight.Bold)) { append("+86 ${auth.phoneMasked}") }
            },
            fontSize = 13.5.sp, color = AuthSub, modifier = Modifier.padding(top = 10.dp)
        )
        BasicTextField(
            value = auth.otp,
            onValueChange = { viewModel.authSetOtp(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth().padding(top = 36.dp).focusRequester(focus),
            decorationBox = { _ ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(6) { i ->
                        val ch = auth.otp.getOrNull(i)?.toString() ?: ""
                        val active = ch.isNotEmpty() || i == auth.otp.length
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.weight(1f).height(56.dp)
                                .then(if (active) Modifier.shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = PurplePrimary.copy(alpha = 0.12f)) else Modifier)
                                .clip(RoundedCornerShape(14.dp)).background(Color.White)
                                .border(if (active) 1.5.dp else 1.dp, if (active) PurplePrimary else AuthCardBorder, RoundedCornerShape(14.dp))
                        ) { Text(ch, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = DarkText) }
                    }
                }
            }
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        ) {
            if (secs > 0) {
                Text("$secs 秒后可重新发送", fontSize = 13.sp, color = AuthFaint)
            } else {
                Text("重新发送验证码", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PurplePrimary,
                    modifier = Modifier.clickable { viewModel.authResendCode(); resendTick++ })
            }
            Text("收不到验证码？", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PurplePrimary)
        }
    }
}

// ③ 完善资料
@Composable
private fun ProfileSetupScreen(viewModel: PeopleNetViewModel) {
    val auth = viewModel.state.auth
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 28.dp)
    ) {
        Text("很高兴认识你", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = DarkText,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 56.dp))
        Text("设置头像和昵称，让关系网更像你", fontSize = 13.5.sp, color = AuthSub,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))

        Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.padding(top = 44.dp).align(Alignment.CenterHorizontally)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(104.dp)
                    .shadow(14.dp, RoundedCornerShape(34.dp), ambientColor = PurplePrimary.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(34.dp)).background(AuthPurpleGrad)
            ) { Text(auth.nickname.takeLast(1).ifEmpty { "友" }, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold) }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(34.dp).offset(x = 4.dp, y = 4.dp)
                    .shadow(4.dp, CircleShape).clip(CircleShape).background(Color.White)
                    .border(1.dp, AuthCardBorder, CircleShape)
            ) { Icon(Icons.Rounded.Edit, contentDescription = "编辑头像", tint = PurplePrimary, modifier = Modifier.size(15.dp)) }
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 44.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = PurplePrimary.copy(alpha = 0.08f))
                    .clip(RoundedCornerShape(16.dp)).background(Color.White)
                    .border(1.dp, AuthCardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                BasicTextField(
                    value = auth.nickname,
                    onValueChange = { viewModel.authSetNickname(it) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.5.sp, color = DarkText, fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (auth.nickname.isEmpty()) Text("给自己起个昵称", color = AuthHint, fontSize = 14.5.sp)
                        inner()
                    }
                )
                Text("${auth.nickname.length}/12", fontSize = 12.sp, color = AuthHint)
            }
            Text(
                text = "进入我的关系网", textAlign = TextAlign.Center,
                fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = PurplePrimary.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(16.dp)).background(PurplePrimary)
                    .clickable { viewModel.authFinishProfile() }.padding(vertical = 16.dp)
            )
        }
        Text("先跳过，稍后再设置", textAlign = TextAlign.Center, fontSize = 13.sp, color = AuthFaint,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp).clickable { viewModel.authFinishProfile() })
    }
}

// ⑤ 账号与安全
@Composable
fun AccountSecurityScreen(viewModel: PeopleNetViewModel) {
    val auth = viewModel.state.auth
    Column(
        modifier = Modifier.fillMaxSize().background(AuthBg).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp)) {
            Text("‹", fontSize = 24.sp, color = DarkText, modifier = Modifier.clickable { viewModel.handleBack() }.padding(end = 10.dp))
            Text("账号与安全", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = DarkText)
        }
        SecurityCard(modifier = Modifier.padding(top = 22.dp)) {
            SecurityRow(label = "手机号", detail = "${auth.phoneMasked} · 更换 ›")
            SecurityDivider()
            SecurityRow(label = "微信", detail = null, trailing = {
                if (auth.wechatBound) BadgePill("已绑定", ContactedGreen, ContactedBg)
                else Text("去绑定 ›", fontSize = 12.5.sp, color = AuthFaint)
            })
            SecurityDivider()
            SecurityRow(label = "登录密码", detail = "未设置 · 去设置 ›")
            SecurityDivider()
            SecurityRow(label = "登录设备管理", detail = "2 台设备 ›")
        }
        SecurityCard(modifier = Modifier.padding(top = 14.dp)) {
            SecurityRow(label = "数据备份与同步", detail = "今天 09:12 已备份 ›")
            SecurityDivider()
            SecurityRow(label = "导出我的数据", detail = "›")
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = PurplePrimary.copy(alpha = 0.12f))
                .clip(RoundedCornerShape(18.dp)).background(Color.White)
                .clickable { viewModel.openDeleteAccount() }.padding(horizontal = 16.dp, vertical = 15.dp)
        ) {
            Text("注销账号", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AuthPink)
            Text("›", fontSize = 14.sp, color = AuthFaint)
        }
        Text(
            "关系数据仅保存在你的账号中，注销后将彻底删除且无法恢复。",
            fontSize = 11.5.sp, color = AuthHint, lineHeight = 18.sp,
            modifier = Modifier.padding(top = 16.dp, start = 4.dp, bottom = 24.dp)
        )
    }
}

// ⑦ 注销账号
@Composable
fun DeleteAccountScreen(viewModel: PeopleNetViewModel) {
    var acknowledged by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().background(AuthBg).padding(horizontal = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp)) {
            Text("‹", fontSize = 24.sp, color = DarkText, modifier = Modifier.clickable { viewModel.handleBack() }.padding(end = 10.dp))
            Text("注销账号", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = DarkText)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(top = 36.dp).align(Alignment.CenterHorizontally)
                .size(72.dp).clip(CircleShape).background(Color(0xFFFFF0F3)).border(1.dp, Color(0xFFFFD9E1), CircleShape)
        ) { Text("!", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = AuthPink) }
        Text("注销后，这些将永久消失", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = DarkText,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 20.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = PurplePrimary.copy(alpha = 0.1f))
                .clip(RoundedCornerShape(18.dp)).background(Color.White)
                .border(1.dp, AuthCardBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp)
        ) {
            DeleteBullet("${viewModel.contacts.size} 位联系人及全部联系记录")
            SecurityDivider(inset = 0.dp)
            DeleteBullet("提醒计划、分组与备注")
            SecurityDivider(inset = 0.dp)
            DeleteBullet("云端备份数据（不可恢复）")
        }

        Row(modifier = Modifier.padding(top = 20.dp, start = 4.dp)) {
            AgreeCheckbox(checked = acknowledged, onToggle = { acknowledged = !acknowledged })
            Spacer(Modifier.width(7.dp))
            Text(
                "我已知晓注销后果，账号将进入 7 天冷静期，期间重新登录可撤销注销",
                fontSize = 12.sp, color = AuthSub, lineHeight = 18.sp
            )
        }
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 26.dp)) {
            Text(
                "保留账号，返回", textAlign = TextAlign.Center,
                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = PurplePrimary.copy(alpha = 0.3f))
                    .clip(RoundedCornerShape(16.dp)).background(PurplePrimary)
                    .clickable { viewModel.handleBack() }.padding(vertical = 15.dp)
            )
            Text(
                "继续注销", textAlign = TextAlign.Center, fontSize = 13.5.sp,
                color = if (acknowledged) AuthPink else AuthFaint,
                modifier = Modifier.fillMaxWidth()
                    .clickable(enabled = acknowledged) { viewModel.authDeleteAccount() }.padding(vertical = 13.dp)
            )
        }
    }
}

// ⑥ 退出登录确认（覆盖层弹层）
@Composable
fun LogoutSheet(viewModel: PeopleNetViewModel) {
    if (!viewModel.state.auth.showLogoutSheet) return
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0x59241A3D)).clickable { viewModel.authCancelLogout() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(Color.White)
                .clickable(enabled = false) {}
                .navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 20.dp)
        ) {
            Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(999.dp)).background(AuthCardBorder))
            Spacer(Modifier.height(16.dp))
            Text("确定要退出登录吗？", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
            Text(
                "退出后联系提醒会暂停，数据已安全备份，下次登录即可恢复。",
                fontSize = 13.sp, color = AuthSub, textAlign = TextAlign.Center, lineHeight = 20.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 16.dp).fillMaxWidth()) {
                Text(
                    "退出登录", textAlign = TextAlign.Center, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AuthPink,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFF0F3))
                        .border(1.dp, Color(0xFFFFD9E1), RoundedCornerShape(16.dp))
                        .clickable { viewModel.authLogout() }.padding(vertical = 15.dp)
                )
                Text(
                    "再想想", textAlign = TextAlign.Center, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = PurplePrimary.copy(alpha = 0.3f))
                        .clip(RoundedCornerShape(16.dp)).background(PurplePrimary)
                        .clickable { viewModel.authCancelLogout() }.padding(vertical = 15.dp)
                )
            }
        }
    }
}

// ---------- shared bits ----------

@Composable
private fun AgreeCheckbox(checked: Boolean, onToggle: () -> Unit) {
    // 外层放大点击热区（16dp 图标 + 8dp 内边距 ≈ 32dp 触达区）
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.offset(y = 1.dp).clip(CircleShape).clickable { onToggle() }.padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(16.dp).clip(CircleShape)
                .then(if (checked) Modifier.background(PurplePrimary) else Modifier.border(1.5.dp, AuthFaint, CircleShape))
        ) {
            if (checked) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
        }
    }
}

@Composable
private fun SecurityCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = PurplePrimary.copy(alpha = 0.12f))
            .clip(RoundedCornerShape(18.dp)).background(Color.White),
        content = content
    )
}

@Composable
private fun SecurityRow(label: String, detail: String?, trailing: @Composable (() -> Unit)? = null, onClick: (() -> Unit)? = null) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF2E2545))
        if (trailing != null) trailing()
        else if (detail != null) Text(detail, fontSize = 12.5.sp, color = AuthFaint)
    }
}

@Composable
private fun SecurityDivider(inset: androidx.compose.ui.unit.Dp = 16.dp) {
    Box(Modifier.fillMaxWidth().padding(horizontal = inset).height(0.5.dp).background(AuthCardBorder))
}

@Composable
private fun BadgePill(text: String, color: Color, bg: Color) {
    Text(text, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = color,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 10.dp, vertical = 3.dp))
}

@Composable
private fun DeleteBullet(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 13.dp)) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(AuthPink))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 13.5.sp, color = Color(0xFF2E2545))
    }
}
