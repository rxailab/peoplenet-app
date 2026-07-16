package com.peoplenet.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peoplenet.app.ui.theme.*
import com.peoplenet.app.viewmodel.PeopleNetViewModel
import com.peoplenet.app.viewmodel.VoiceMoney
import com.peoplenet.app.viewmodel.VoiceNewContact
import com.peoplenet.app.viewmodel.VoiceReminder
import kotlinx.coroutines.delay

private val PanelScrim = Color(0x73241A3D)
private val ChipDashPurple = Color(0xFFC9B8F0)
private val ChipBgPurple = Color(0xFFF7F5FF)
private val ChipDashGreen = Color(0xFFA8E0CE)
private val ChipBgGreen = Color(0xFFF0FBF7)

// ==== 5b 整行点开的底部操作单 ====

@Composable
fun HomeActionSheet(viewModel: PeopleNetViewModel) {
    val c = viewModel.homeActionContact ?: return
    val clipboard = LocalClipboardManager.current
    Box(
        modifier = Modifier.fillMaxSize().background(PanelScrim)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.closeHomeAction() }
    ) {
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(Color.White)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 24.dp)
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 14.dp).width(38.dp).height(4.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFFE3DDF2)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GradientAvatar(contact = c, size = 44, cornerRadius = 14)
                Spacer(Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.name, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
                        if (c.overdue && !viewModel.isContacted(c.id)) {
                            Spacer(Modifier.width(7.dp))
                            Text("逾期 5 天", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PinkAccentLight, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(OverduePinkBg).padding(horizontal = 9.dp, vertical = 2.dp))
                        }
                    }
                    Text("${c.rel} · 上次联系 5 周前", fontSize = 11.5.sp, color = MediumText, modifier = Modifier.padding(top = 2.dp))
                }
            }
            // 话术
            Column(modifier = Modifier.padding(top = 13.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ChipBgPurple).padding(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AiBadge()
                    Spacer(Modifier.width(6.dp))
                    Text("话术已备好", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MediumText)
                }
                Text("「${viewModel.scriptFor(c)}」", fontSize = 12.5.sp, lineHeight = 19.sp, color = DarkText, modifier = Modifier.padding(top = 7.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(PurplePrimary)
                        .clickable { clipboard.setText(AnnotatedString(viewModel.scriptFor(c))); viewModel.showToastPublic("已复制，去微信发给${c.name}吧"); viewModel.closeHomeAction() }
                        .padding(vertical = 12.dp)
                ) { Text("复制并打开微信", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).border(1.5.dp, Color(0xFFD9C9F5), RoundedCornerShape(14.dp))
                        .clickable { viewModel.showToastPublic("拨号给${c.name}") }.padding(horizontal = 16.dp, vertical = 12.dp)
                ) { Text("打电话", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PurplePrimary) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(ContactedBg).clickable { viewModel.homeMarkContacted(c.id) }.padding(vertical = 10.dp)
                ) { Text("✓ 已经联系过了", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ContactedGreen) }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF4F1FA)).clickable { viewModel.homeMarkTomorrow(c.id) }.padding(vertical = 10.dp)
                ) { Text("改天再说", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MediumText) }
            }
        }
    }
}

// ==== 6c 落库确认条（顶部，可撤销，自动消失）====

@Composable
fun VoiceConfirmBar(viewModel: PeopleNetViewModel) {
    val text = viewModel.state.voiceConfirm
    val seq = viewModel.state.voiceConfirmSeq
    LaunchedEffect(seq) {
        if (seq > 0 && viewModel.state.voiceConfirm.isNotEmpty()) { delay(4500); viewModel.voiceDismissConfirm() }
    }
    if (text.isEmpty()) return
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp).fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = ContactedGreen.copy(alpha = 0.4f)).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0FA47C)).padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Text("✓", fontSize = 13.sp, color = Color.White)
            Spacer(Modifier.width(9.dp))
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
            Text("撤销", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.clickable { viewModel.voiceUndo() }.padding(4.dp))
        }
    }
}

// ==== 6a/6b 语音助手底部面板 ====

@Composable
fun VoiceAssistant(viewModel: PeopleNetViewModel) {
    val stage = viewModel.state.voiceStage
    if (stage == 0) return

    // 新开面板 / 重说（voiceOpenSeq 自增）→ 申请麦克风并开始实时识别；拒绝则回退手动打字。
    // 解析失败返回听写页（seq 不变）不会自动重启——停在「已暂停」，由用户改字或点「继续」。
    val context = LocalContext.current
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.voiceStartAsr() else viewModel.voiceAsrDenied()
    }
    LaunchedEffect(viewModel.state.voiceOpenSeq) {
        if (viewModel.state.voiceStage == 1) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                viewModel.voiceStartAsr()
            else
                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0x73241A3D))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { if (stage != 2) viewModel.voiceClose() }
    ) {
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(Color.White)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 26.dp)
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 14.dp).width(38.dp).height(4.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFFE3DDF2)))
            when (stage) {
                1 -> VoiceListening(viewModel)
                2 -> Text("✦ 识别中…", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = PurplePrimary, modifier = Modifier.padding(vertical = 22.dp).align(Alignment.CenterHorizontally))
                else -> VoiceResult(viewModel)
            }
        }
    }
}

@Composable
private fun ColumnScope.VoiceListening(viewModel: PeopleNetViewModel) {
    val text = viewModel.state.voiceText
    val listening = viewModel.voiceListening
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (listening) PinkAccentLight else Color(0xFFC9C0E0)))
        Spacer(Modifier.width(8.dp))
        Text(if (listening) "正在听…" else "已暂停", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
        Spacer(Modifier.weight(1f))
        Text(if (listening) "2 秒没新内容自动结束" else "点「继续听写」或直接打字", fontSize = 10.5.sp, color = LightText)
    }
    // 波形：跟麦克风实时音量联动的滚动电平条（右进左出）
    val levels = remember { mutableStateListOf<Float>().also { l -> repeat(26) { l.add(0f) } } }
    LaunchedEffect(Unit) {
        while (true) {
            levels.removeAt(0)
            levels.add(viewModel.voiceLevel)
            delay(80)
        }
    }
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 12.dp).fillMaxWidth().height(44.dp),
    ) {
        levels.forEach { lv ->
            val h = 5f + lv * 38f
            Box(Modifier.width(3.dp).height(h.dp).clip(RoundedCornerShape(999.dp)).background(if (lv > 0.3f) PurplePrimary else Color(0xFFA98BE0)))
        }
    }
    // 识别文字（可编辑=真实输入）
    Box(modifier = Modifier.padding(top = 12.dp).fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF7F5FF)).padding(14.dp)) {
        BasicTextField(
            value = text,
            onValueChange = { viewModel.voiceSetText(it) },
            textStyle = TextStyle(fontSize = 15.sp, color = DarkText, lineHeight = 24.sp),
            cursorBrush = SolidColor(PurplePrimary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (text.isEmpty()) Text("说一句就能：设提醒 / 记人情账 / 新建联系人 / 标记已联系\n如「周六提醒我和老周钓鱼，收他两千」「新建联系人王强，健身房认识的」", fontSize = 12.5.sp, color = LightText, lineHeight = 20.sp)
                inner()
            }
        )
    }
    if (text.isEmpty()) {
        Text(
            "▸ 试试语音示例", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = PurplePrimary,
            modifier = Modifier.padding(top = 10.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFFF4F0FF)).clickable { viewModel.voiceRunDemo() }.padding(horizontal = 13.dp, vertical = 6.dp)
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 14.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFFF4F1FA)).clickable { viewModel.voiceClose() }.padding(horizontal = 16.dp, vertical = 12.dp)
        ) { Text("取消", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MediumText) }
        // ⏹ 停止 / 🎙 继续：只控制麦克风，不离开面板，文字保留可编辑
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (listening) Color(0xFFFFEDF2) else Color(0xFFF4F0FF))
                .clickable { if (listening) viewModel.voiceStopAsr() else viewModel.voiceStartAsr() }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                if (listening) "⏹ 停止" else "🎙 继续",
                fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                color = if (listening) PinkAccent else PurplePrimary
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(PurplePrimary).clickable { viewModel.voiceFinish() }.padding(vertical = 12.dp)
        ) { Text("说完了 ✓", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
    }
}

@Composable
private fun ColumnScope.VoiceResult(viewModel: PeopleNetViewModel) {
    val r = viewModel.state.voiceReminder
    val m = viewModel.state.voiceMoney
    val nc = viewModel.state.voiceNewContact
    val cp = viewModel.state.voiceContactedPerson
    val n = listOfNotNull(r, m, nc, cp).size
    Row(verticalAlignment = Alignment.CenterVertically) {
        AiBadge()
        Spacer(Modifier.width(7.dp))
        Text("听到了 $n 件事", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
        Spacer(Modifier.weight(1f))
        Text("点字段可改", fontSize = 11.sp, color = LightText)
    }
    Text(
        "「${viewModel.state.voiceText}」",
        fontSize = 11.sp, color = LightText, maxLines = 3, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF7F5FF)).padding(horizontal = 11.dp, vertical = 8.dp)
    )
    if (nc != null) NewContactCard(nc) { viewModel.voiceDropNewContact() }
    if (r != null) ReminderCard(r) { viewModel.voiceDropReminder() }
    if (m != null) MoneyCard(m) { viewModel.voiceDropMoney() }
    if (cp != null) ContactedCard(cp) { viewModel.voiceDropContacted() }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 14.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFFF4F1FA)).clickable { viewModel.voiceRedo() }.padding(horizontal = 16.dp, vertical = 12.dp)
        ) { Text("🎙 重说", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MediumText) }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (n == 0) PurplePrimary.copy(alpha = 0.4f) else PurplePrimary).clickable(enabled = n > 0) { viewModel.voiceSave() }.padding(vertical = 12.dp)
        ) { Text(if (n == 0) "都删了" else "都对，保存 $n 条", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
    }
}

@Composable
private fun ReminderCard(r: VoiceReminder, onDrop: () -> Unit) {
    Column(modifier = Modifier.padding(top = 12.dp).fillMaxWidth().border(1.5.dp, Color(0xFFE3DDF2), RoundedCornerShape(16.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("提醒", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = PurplePrimary, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(PurplePillBg).padding(horizontal = 9.dp, vertical = 3.dp))
            Spacer(Modifier.width(7.dp))
            Text(r.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkText, modifier = Modifier.weight(1f))
            Text("✕", fontSize = 12.sp, color = Color(0xFFC9C0E0), modifier = Modifier.clickable(onClick = onDrop).padding(4.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 9.dp)) {
            DashChip("📅 ${r.date}", ChipBgPurple, ChipDashPurple, PurplePrimary)
            DashChip("👤 ${r.person}", ChipBgPurple, ChipDashPurple, PurplePrimary)
            DashChip("🕘 ${r.time} ▾", ChipBgPurple, Color(0xFFE3DDF2), LightText)
        }
    }
}

@Composable
private fun MoneyCard(m: VoiceMoney, onDrop: () -> Unit) {
    Column(modifier = Modifier.padding(top = 8.dp).fillMaxWidth().border(1.5.dp, Color(0xFFD5EFEB), RoundedCornerShape(16.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("人情账", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = ContactedGreen, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(ContactedBg).padding(horizontal = 9.dp, vertical = 3.dp))
            Spacer(Modifier.width(7.dp))
            Text(m.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkText, modifier = Modifier.weight(1f))
            Text("✕", fontSize = 12.sp, color = Color(0xFFC9C0E0), modifier = Modifier.clickable(onClick = onDrop).padding(4.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 9.dp)) {
            DashChip("👤 ${m.person}", ChipBgGreen, ChipDashGreen, ContactedGreen)
            if (m.item.isNotBlank()) DashChip("🎁 ${m.item}", ChipBgGreen, ChipDashGreen, ContactedGreen)
            if (m.amount > 0) DashChip((if (m.item.isNotBlank()) "估值 ¥" else "¥ ") + "%,d".format(m.amount), ChipBgGreen, ChipDashGreen, ContactedGreen)
            if (m.linked) DashChip("已有记录 · 关联 ▾", ChipBgGreen, Color(0xFFD5EFEB), LightText)
        }
        if (m.linkNote.isNotBlank()) {
            Text(m.linkNote, fontSize = 10.5.sp, color = MediumText, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun NewContactCard(nc: VoiceNewContact, onDrop: () -> Unit) {
    Column(modifier = Modifier.padding(top = 12.dp).fillMaxWidth().border(1.5.dp, Color(0xFFF6DFC2), RoundedCornerShape(16.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("新联系人", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = FestOrangeDeep, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(FestPillBg).padding(horizontal = 9.dp, vertical = 3.dp))
            Spacer(Modifier.width(7.dp))
            Text(nc.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkText, modifier = Modifier.weight(1f))
            Text("✕", fontSize = 12.sp, color = Color(0xFFC9C0E0), modifier = Modifier.clickable(onClick = onDrop).padding(4.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 9.dp)) {
            DashChip("👥 ${nc.relation}", Color(0xFFFFF7EE), Color(0xFFF6DFC2), FestOrangeDeep)
            if (nc.note.isNotBlank()) DashChip("📝 ${nc.note}", Color(0xFFFFF7EE), Color(0xFFF6DFC2), FestOrangeDeep)
        }
    }
}

@Composable
private fun ContactedCard(person: String, onDrop: () -> Unit) {
    Column(modifier = Modifier.padding(top = 8.dp).fillMaxWidth().border(1.5.dp, Color(0xFFD5EFEB), RoundedCornerShape(16.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("已联系", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = ContactedGreen, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(ContactedBg).padding(horizontal = 9.dp, vertical = 3.dp))
            Spacer(Modifier.width(7.dp))
            Text("标记 $person 今天已联系", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkText, modifier = Modifier.weight(1f))
            Text("✕", fontSize = 12.sp, color = Color(0xFFC9C0E0), modifier = Modifier.clickable(onClick = onDrop).padding(4.dp))
        }
    }
}

@Composable
private fun DashChip(text: String, bg: Color, border: Color, fg: Color) {
    Text(
        text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg).border(1.dp, border, RoundedCornerShape(999.dp)).padding(horizontal = 11.dp, vertical = 4.dp)
    )
}
