package com.peoplenet.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peoplenet.app.data.Contact
import com.peoplenet.app.ui.theme.*
import com.peoplenet.app.viewmodel.FestFriend
import com.peoplenet.app.viewmodel.GiftRec
import com.peoplenet.app.viewmodel.PeopleNetViewModel

// ---------- demo trigger（▶ 模拟 · 两段式推进）----------

@Composable
fun FestSimControls(viewModel: PeopleNetViewModel) {
    val step = viewModel.state.fest.cdStep
    val idle = step >= 2
    val simLabel = when (step) {
        0 -> "模拟 · 到中秋前 7 天"
        1 -> "模拟 · 到中秋当天"
        else -> "已是中秋当天"
    }
    val status = when (step) {
        0 -> "今天 · 距中秋 20 天"
        1 -> "中秋前 7 天"
        else -> "中秋节当天"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .then(
                    if (idle) Modifier
                    else Modifier.shadow(8.dp, RoundedCornerShape(999.dp), ambientColor = FestOrange.copy(alpha = 0.3f))
                )
                .clip(RoundedCornerShape(999.dp))
                .background(
                    if (idle) Brush.linearGradient(listOf(ContactedBg, ContactedBg))
                    else Brush.linearGradient(listOf(FestOrange, FestOrangeLight))
                )
                .clickable { viewModel.festSimulate() }
                .padding(horizontal = 15.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = if (idle) Icons.Rounded.Check else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = if (idle) ContactedGreen else Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = simLabel,
                color = if (idle) ContactedGreen else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "重置",
            color = MediumText,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .border(1.5.dp, Color(0xFFD8D1EA), RoundedCornerShape(999.dp))
                .background(Color.White)
                .clickable { viewModel.festReset() }
                .padding(horizontal = 14.dp, vertical = 7.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(text = status, color = GeoStatusText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------- 第一段：前 7 天 · 倒计时卡（入口 → 礼物清单 3a）----------

@Composable
fun FestPrepBanner(viewModel: PeopleNetViewModel) {
    val cart = viewModel.festCartCount
    val sub = if (cart > 0)
        "已备好 ${viewModel.festGiftDoneCount}/${viewModel.festGiftPeopleCount} 人 · 清单 $cart 件 · 点开继续挑"
    else
        "${viewModel.festGiftPeopleCount} 位朋友 · 按关系推荐礼物 · 先准备起来"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(24.dp), ambientColor = FestOrange.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(FestOrange, FestOrangeLight)))
            .clickable { viewModel.festOpenGiftList() }
            .padding(16.dp)
    ) {
        FestIconBox(size = 42, corner = 15, fontSize = 17)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "中秋节 · 还有 7 天", fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(text = sub, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.88f), modifier = Modifier.padding(top = 2.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "挑礼物 →",
            fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
            modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White.copy(alpha = 0.22f)).padding(horizontal = 11.dp, vertical = 6.dp)
        )
    }
}

// ---------- 第二段：当天 · 节日祝福卡 ----------

@Composable
fun FestDayBanner(viewModel: PeopleNetViewModel) {
    val handled = viewModel.festHandledCount
    val sent = viewModel.festSentCount
    val total = viewModel.festFriends.size
    val sub = if (handled > 0) "已送出 $sent 位 · 还差 ${total - handled} 位" else "$total 位朋友值得一句祝福"
    Column(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = FestOrangeDeep.copy(alpha = 0.36f))
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(FestOrangeDeep, FestOrangeDeep2)))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FestIconBox(size = 46, corner = 16, fontSize = 19)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "今天 · 中秋节", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(text = sub, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.padding(top = 2.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "给 $total 位朋友发祝福 →",
            textAlign = TextAlign.Center,
            fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = FestOrangeDeep,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(15.dp), ambientColor = Color(0x33341A0A))
                .clip(RoundedCornerShape(15.dp))
                .background(Color.White)
                .clickable { viewModel.festOpenBless() }
                .padding(vertical = 12.dp)
        )
    }
}

// ---------- 当天推送（顶部滑入）----------

@Composable
fun BoxScope.FestPush(viewModel: PeopleNetViewModel) {
    val fest = viewModel.state.fest
    val cartN = viewModel.festCartCount
    val names = viewModel.festFriends.mapNotNull { viewModel.festContact(it.id)?.name }.joinToString("、")
    val body = if (cartN > 0) "今天是中秋节 — 祝福和清单里的 $cartN 件礼物都在这儿"
    else "今天是中秋节 — ${names}都值得一句祝福"
    AnimatedVisibility(
        visible = fest.notifShown,
        enter = slideInVertically(tween(420)) { -it - 60 } + fadeIn(tween(320)),
        exit = slideOutVertically(tween(220)) { -it - 60 } + fadeOut(tween(180)),
        modifier = Modifier.align(Alignment.TopCenter)
    ) {
        Row(
            modifier = Modifier
                .padding(start = 10.dp, end = 10.dp, top = 8.dp)
                .fillMaxWidth()
                .shadow(18.dp, RoundedCornerShape(20.dp), ambientColor = DarkText.copy(alpha = 0.24f))
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.97f))
                .clickable { viewModel.festOpenBless() }
                .padding(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp), ambientColor = PurplePrimary.copy(alpha = 0.3f))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(PurplePrimary, Color(0xFFA678EC))))
            ) {
                Text(text = "朋", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "朋友等你", fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = "现在", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = GeoStatusText)
                }
                Text(text = body, fontSize = 12.sp, color = Color(0xFF4E4569), lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
                Text(text = "点开看看 →", fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = PurplePrimary, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

// ---------- 祝福清单页（推送 / 节日卡同源）----------

@Composable
fun BlessListScreen(viewModel: PeopleNetViewModel) {
    val clipboard = LocalClipboardManager.current
    val fest = viewModel.state.fest
    val sent = viewModel.festSentCount
    val total = viewModel.festFriends.size
    val allDone = viewModel.festAllDone
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(FestListTop, FestListBottom)))
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(6.dp, CircleShape, ambientColor = FestOrange.copy(alpha = 0.2f))
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { viewModel.handleBack() }
                ) {
                    Icon(Icons.Rounded.Close, "关闭", tint = FestSubText, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "已送出 $sent/$total",
                    fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = FestOrangeDeep,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(FestPillBg).padding(horizontal = 13.dp, vertical = 6.dp)
                )
            }
            Column(modifier = Modifier.padding(start = 26.dp, end = 26.dp, top = 10.dp, bottom = 8.dp)) {
                Text(text = "节日祝福", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = FestOrangeDeep, letterSpacing = 1.5.sp)
                Text(text = "中秋节", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = DarkText, modifier = Modifier.padding(top = 5.dp))
                Text(
                    text = "每个人一句定制的祝福 · 发完自动记进时间线",
                    fontSize = 13.sp, fontWeight = FontWeight.Medium, color = FestSubText,
                    lineHeight = 20.sp, modifier = Modifier.padding(top = 5.dp)
                )
            }
            if (!allDone) {
                Text(
                    text = "一键复制全部祝福 · 逐个粘贴群发",
                    textAlign = TextAlign.Center,
                    fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = FestOrangeDeep,
                    modifier = Modifier
                        .padding(start = 18.dp, end = 18.dp, top = 2.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFF9F0))
                        .border(1.5.dp, Color(0xFFEFC894), RoundedCornerShape(16.dp))
                        .clickable {
                            val all = viewModel.festFriends.joinToString("\n") { (viewModel.festContact(it.id)?.name ?: "") + "：" + it.script }
                            clipboard.setText(AnnotatedString(all))
                            viewModel.festBatchSend()
                        }
                        .padding(11.dp)
                )
            }
            Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp)) {
                viewModel.festFriends.forEach { f ->
                    val c = viewModel.festContact(f.id) ?: return@forEach
                    BlessCard(viewModel, f, c, kind = fest.acted[f.id], clipboard)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            if (allDone) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(start = 18.dp, end = 18.dp, top = 2.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFDFF7EE), Color(0xFFF2FCF8))))
                        .padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(38.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF1FC9A0), Color(0xFF43D6AF))))
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "祝福都送出去啦 · 每一笔都记进了时间线", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = ContactedGreen)
                }
            }
            Spacer(modifier = Modifier.height(44.dp))
        }
        GeoToast(viewModel)
    }
}

@Composable
private fun BlessCard(
    viewModel: PeopleNetViewModel,
    f: FestFriend,
    c: Contact,
    kind: String?,
    clipboard: androidx.compose.ui.platform.ClipboardManager
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (kind == "skip") 0.55f else 1f }
            .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = FestOrange.copy(alpha = 0.12f))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradientAvatar(contact = c, size = 42, cornerRadius = 15)
            Spacer(modifier = Modifier.width(11.dp))
            Text(text = c.name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = c.rel, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = c.color,
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(c.color.copy(alpha = 0.12f)).padding(horizontal = 9.dp, vertical = 2.dp)
            )
            if (kind != null) {
                Spacer(modifier = Modifier.weight(1f))
                val sent = kind == "sent"
                Text(
                    text = if (sent) "已送出" else "这次跳过",
                    fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (sent) ContactedGreen else GeoStatusText,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (sent) ContactedBg else Color(0xFFF1F3F8)).padding(horizontal = 11.dp, vertical = 5.dp)
                )
            }
        }
        if (kind == null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "「${f.script}」",
                fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = FestScriptText, lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(FestScriptBg).padding(horizontal = 12.dp, vertical = 10.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row {
                Text(
                    text = "跳过",
                    fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB3A28A),
                    modifier = Modifier
                        .clip(RoundedCornerShape(13.dp))
                        .border(1.5.dp, Color(0xFFEFE4D2), RoundedCornerShape(13.dp))
                        .background(Color.White)
                        .clickable { viewModel.festAct(f.id, "skip") }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "复制并发送",
                    textAlign = TextAlign.Center,
                    fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .shadow(8.dp, RoundedCornerShape(13.dp), ambientColor = FestOrange.copy(alpha = 0.3f))
                        .clip(RoundedCornerShape(13.dp))
                        .background(Brush.linearGradient(listOf(FestOrange, FestOrangeLight)))
                        .clickable {
                            clipboard.setText(AnnotatedString(f.script))
                            viewModel.festAct(f.id, "sent")
                        }
                        .padding(vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun FestIconBox(size: Int, corner: Int, fontSize: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size.dp).clip(RoundedCornerShape(corner.dp)).background(Color.White.copy(alpha = 0.22f))
    ) {
        Text(text = "秋", color = Color.White, fontSize = fontSize.sp, fontWeight = FontWeight.ExtraBold)
    }
}

// ---------- 礼物清单页（3a：手风琴 + 选人搜索）----------

@Composable
fun GiftListScreen(viewModel: PeopleNetViewModel) {
    val query = viewModel.state.fest.giftSearch
    val pending = viewModel.festGifts.filter { !viewModel.festPersonDone(it.friendId) && viewModel.festGiftMatches(it.friendId, query) }
    val done = viewModel.festGifts.filter { viewModel.festPersonDone(it.friendId) && viewModel.festGiftMatches(it.friendId, query) }
    val noHit = pending.isEmpty() && done.isEmpty()
    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(FestListTop, FestListBottom)))
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(6.dp, CircleShape, ambientColor = FestOrange.copy(alpha = 0.2f))
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { viewModel.handleBack() }
                ) { Icon(Icons.Rounded.Close, "关闭", tint = FestSubText, modifier = Modifier.size(18.dp)) }
            }
            // header
            Column(modifier = Modifier.padding(start = 26.dp, end = 26.dp, top = 8.dp)) {
                Text(text = "节日准备 · 还有 7 天", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = FestOrangeDeep, letterSpacing = 1.5.sp)
                Text(text = "中秋礼物清单", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = DarkText, modifier = Modifier.padding(top = 4.dp))
                Text(
                    text = "已备好 ${viewModel.festGiftDoneCount}/${viewModel.festGiftPeopleCount} 人 · 清单 ${viewModel.festCartCount} 件 · 合计 ¥${viewModel.festCartTotal}",
                    fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = FestSubText, modifier = Modifier.padding(top = 4.dp)
                )
            }
            // search
            TextField(
                value = query,
                onValueChange = { viewModel.festSetGiftSearch(it) },
                singleLine = true,
                placeholder = { Text(text = "搜名字或关系，如「妈」「同事」", color = FestSubText, fontSize = 12.5.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = FestOrangeDeep,
                    focusedTextColor = DarkText,
                    unfocusedTextColor = DarkText
                ),
                textStyle = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .padding(start = 18.dp, end = 18.dp, top = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, Color(0xFFEFC894), RoundedCornerShape(14.dp))
            )
            // list
            Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp)) {
                if (noHit) {
                    Text(
                        text = "没有匹配的联系人",
                        textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FestSubText,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    )
                }
                pending.forEach { rec ->
                    GiftPersonCard(viewModel, rec, done = false)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (done.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)) {
                        Text(text = "已备好 · ${done.size} 人", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = ContactedGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(ContactedGreen.copy(alpha = 0.2f)))
                    }
                    done.forEach { rec ->
                        GiftPersonCard(viewModel, rec, done = true)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(44.dp))
        }
        GeoToast(viewModel)
    }
}

@Composable
private fun GiftPersonCard(viewModel: PeopleNetViewModel, rec: GiftRec, done: Boolean) {
    val c = viewModel.festContact(rec.friendId) ?: return
    val open = viewModel.festIsGiftOpen(rec.friendId)
    val n = viewModel.festPersonCartCount(rec.friendId)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x14341A0A))
            .clip(RoundedCornerShape(16.dp))
            .background(if (done) Color.White.copy(alpha = 0.7f) else Color.White)
    ) {
        // header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .graphicsLayer { alpha = if (done) 0.85f else 1f }
                .clickable { viewModel.festToggleGiftOpen(rec.friendId) }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            GradientAvatar(contact = c, size = 36, cornerRadius = 12)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "给${c.name}", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
                if (!done) {
                    Text(text = rec.reason, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = FestSubText, maxLines = 1, modifier = Modifier.padding(top = 1.dp))
                }
            }
            Text(
                text = if (n > 0) "已备 $n 件" else "待挑",
                fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                color = if (n > 0) ContactedGreen else Color(0xFF9AA6BF),
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (n > 0) ContactedBg else Color(0xFFF1F3F8)).padding(horizontal = 9.dp, vertical = 3.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = if (open) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = null, tint = Color(0xFFC9B79B), modifier = Modifier.size(18.dp)
            )
        }
        if (open) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            ) {
                rec.items.forEachIndexed { i, g ->
                    val inCart = viewModel.festInCart(rec.friendId, i)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color(0xFFFFF9F0))
                            .padding(6.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(g.c1, g.c2)))
                        ) { Text(text = g.char, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold) }
                        Text(
                            text = g.name,
                            fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = DarkText, lineHeight = 14.sp, maxLines = 2,
                            modifier = Modifier.padding(top = 5.dp, start = 2.dp, end = 2.dp).heightIn(min = 28.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp, start = 2.dp, end = 2.dp)) {
                            Text(text = "¥${g.price}", fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = FestOrangeDeep, modifier = Modifier.weight(1f))
                            Text(
                                text = if (inCart) "已加" else "加清单",
                                fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold,
                                color = if (inCart) ContactedGreen else Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .then(if (inCart) Modifier.background(ContactedBg) else Modifier.background(Brush.linearGradient(listOf(FestOrange, FestOrangeLight))))
                                    .clickable { viewModel.festToggleCart(rec.friendId, i) }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
