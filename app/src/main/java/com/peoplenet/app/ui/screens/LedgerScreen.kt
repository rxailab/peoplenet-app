package com.peoplenet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peoplenet.app.data.MoneyRecord
import com.peoplenet.app.data.MoneyType
import com.peoplenet.app.ui.theme.*
import com.peoplenet.app.viewmodel.PeopleNetViewModel

/**
 * 「人情」tab（1d 四 tab 信息架构）：借出/回礼提醒 · 借还（紫）/ 礼簿（橙）双页签 · 节日礼物清单。
 * 从底部导航第 3 个 tab 进入，不再藏在「我」里。
 */
@Composable
fun PeopleScreen(viewModel: PeopleNetViewModel) {
    val tab = viewModel.state.ledgerTab
    val giftTab = tab == 1

    val bg = if (giftTab)
        Brush.verticalGradient(listOf(FestListTop, FestListBottom))
    else
        Brush.verticalGradient(listOf(BgGradientStart, BgGradientEnd))

    // 借出/回礼提醒摘要（副标题里点出来，与角标同口径：设了提醒的借出 + 待回礼）
    val toCollect = viewModel.loanReminders().size
    val toReturn = viewModel.giftReminders().size
    val remindSummary = buildList {
        if (toCollect > 0) add("$toCollect 笔借出到期")
        if (toReturn > 0) add("$toReturn 笔待回礼")
    }.joinToString(" · ")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        // 返回「我」
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 2.dp)
        ) {
            Text(
                text = "‹",
                fontSize = 24.sp,
                color = if (giftTab) FestOrange else PurplePrimary,
                modifier = Modifier.clickable { viewModel.goBack() }.padding(end = 10.dp)
            )
            Text(text = "我", fontSize = 12.sp, color = LightText, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 4.dp)) {
            Text("人情", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
            Text(
                text = remindSummary.ifBlank { "借还 · 礼簿 · 只记心意，不算净值" },
                fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
                color = if (remindSummary.isBlank()) LightText else PinkAccent,
                modifier = Modifier.padding(top = 5.dp)
            )

            // 节日礼物清单入口（节日临近时才出现；礼物清单 / 参谋都在这儿）
            if (viewModel.festPrepBannerOn) {
                Spacer(Modifier.height(12.dp))
                FestivalGiftEntry(viewModel)
            }

            // 页签
            Row(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.85f))
                    .padding(3.dp)
            ) {
                LedgerTab("借还", selected = !giftTab, accent = PurplePrimary) { viewModel.setLedgerTab(0) }
                LedgerTab("礼簿", selected = giftTab, accent = FestOrange) { viewModel.setLedgerTab(1) }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 28.dp)
        ) {
            if (giftTab) GiftLedger(viewModel) else LoanLedger(viewModel)
        }
    }
}

/** 节日礼物入口：中秋摘要 + 去「礼物清单 / 参谋」。 */
@Composable
private fun FestivalGiftEntry(viewModel: PeopleNetViewModel) {
    val cart = viewModel.festCartCount
    val sub = if (cart > 0)
        "清单 $cart 件 · 点开继续挑"
    else
        "AI 按关系和忌口，帮你挑好了礼"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = FestOrange.copy(alpha = 0.28f))
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(FestOrange, FestOrangeLight)))
            .clickable { viewModel.festOpenGiftList() }
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Text("🎁", fontSize = 20.sp)
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("中秋礼物清单", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(sub, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.padding(top = 2.dp))
        }
        Text("去挑 ›", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
    }
}

@Composable
private fun LedgerTab(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 12.5.sp,
        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
        color = if (selected) Color.White else FestSubText,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 7.dp)
    )
}

// ---- 礼簿：按事件归档 ----

@Composable
private fun GiftLedger(viewModel: PeopleNetViewModel) {
    val events = viewModel.giftEvents()
    val loose = viewModel.looseGifts()

    if (events.isEmpty() && loose.isEmpty()) {
        EmptyHint("还没有礼簿记录", "记一笔时选「送礼 / 收礼」，就会出现在这里")
        return
    }

    events.forEach { (event, meta, records) ->
        EventHeader(name = event, date = meta.first, place = meta.second)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = FestOrangeDeep.copy(alpha = 0.10f))
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
        ) {
            records.forEachIndexed { i, r ->
                GiftRow(viewModel, r, showDivider = i < records.lastIndex)
            }
        }
    }

    if (loose.isNotEmpty()) {
        EventHeader(name = "其他", date = "", place = "")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = FestOrangeDeep.copy(alpha = 0.10f))
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
        ) {
            loose.forEachIndexed { i, r ->
                GiftRow(viewModel, r, showDivider = i < loose.lastIndex)
            }
        }
    }

    Text(
        text = "按事件归档 · 回礼时自动关联原记录 · 不显示净值",
        fontSize = 10.5.sp, color = FestSubText, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
    ) {
        Text(
            text = "＋ 新建事件 · 记一笔",
            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
            modifier = Modifier
                .shadow(10.dp, RoundedCornerShape(999.dp), ambientColor = FestOrange.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(999.dp))
                .background(Brush.linearGradient(listOf(FestOrange, FestOrangeLight)))
                .clickable { viewModel.ledgerAddEntry() }
                .padding(horizontal = 24.dp, vertical = 11.dp)
        )
    }
}

@Composable
private fun EventHeader(name: String, date: String, place: String) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.padding(top = 18.dp, bottom = 11.dp)
    ) {
        Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
        val meta = listOf(date, place).filter { it.isNotBlank() }.joinToString(" · ")
        if (meta.isNotBlank()) {
            Text(text = "  $meta", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = FestSubText)
        }
    }
}

@Composable
private fun GiftRow(viewModel: PeopleNetViewModel, r: MoneyRecord, showDivider: Boolean) {
    val contact = viewModel.contactById(r.contactId)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (showDivider) Modifier.drawBehind {
                    drawLine(FestListTop, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                } else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        if (contact != null) GradientAvatar(contact = contact, size = 34, cornerRadius = 12)
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = (contact?.name?.let { "$it · " } ?: "") +
                        if (r.isPhysical) r.itemName else "礼金 ${formatYuan(r.amount)}",
                    fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = DarkText
                )
                if (r.isPhysical && r.estValue > 0) {
                    Text("  估值 ${formatYuan(r.estValue)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = FestSubText)
                }
            }
            val sub = r.note.ifBlank { "${r.type.label} · ${if (r.isPhysical) "实物" else "礼金"}" }
            Text(text = sub, fontSize = 10.5.sp, color = FestSubText, modifier = Modifier.padding(top = 3.dp))
        }
        Spacer(Modifier.width(8.dp))
        when {
            r.type == MoneyType.ReceiveGift && r.giftReturn != null -> {
                val (bg, fg) = giftReturnColors(r.giftReturn)
                if (r.giftReturn == com.peoplenet.app.data.GiftReturn.Pending) {
                    Text(
                        text = "记回礼",
                        fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(PurplePrimary)
                            .clickable { viewModel.markGiftReturned(r.id) }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                } else {
                    SmallPill(text = r.giftReturn.label, bg = bg, fg = fg)
                }
            }
            else -> TypePill(r.type)
        }
    }
}

// ---- 借还：按状态分组 ----

@Composable
private fun LoanLedger(viewModel: PeopleNetViewModel) {
    val (toCollect, toRepay, settled) = viewModel.loansByStatus()

    if (toCollect.isEmpty() && toRepay.isEmpty() && settled.isEmpty()) {
        EmptyHint("还没有借还记录", "记一笔时选「借出 / 借入」，就会出现在这里")
        return
    }

    if (toCollect.isNotEmpty()) {
        LoanSectionHeader(PinkAccent, "待收回", toCollect.size)
        toCollect.forEach { LoanCard(viewModel, it, collect = true) }
    }
    if (toRepay.isNotEmpty()) {
        LoanSectionHeader(FestOrange, "待归还", toRepay.size)
        toRepay.forEach { LoanCard(viewModel, it, collect = false) }
    }
    if (settled.isNotEmpty()) {
        LoanSectionHeader(GreenAccent, "已结清", settled.size)
        settled.forEach { LoanCard(viewModel, it, collect = false) }
    }
}

@Composable
private fun LoanSectionHeader(dot: Color, title: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 18.dp, bottom = 11.dp)
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(7.dp))
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkText)
        Spacer(Modifier.width(7.dp))
        Text(
            text = "$count 笔",
            fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = dot,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(dot.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun LoanCard(viewModel: PeopleNetViewModel, r: MoneyRecord, collect: Boolean) {
    val contact = viewModel.contactById(r.contactId)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = PurplePrimary.copy(alpha = 0.12f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        if (contact != null) GradientAvatar(contact = contact, size = 38, cornerRadius = 13)
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${contact?.name ?: ""} · ${r.type.label} ${formatYuan(r.amount)}",
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkText
            )
            val sub = listOf(
                r.date,
                if (r.reminderDate.isNotBlank()) "提醒 ${r.reminderDate}" else "未设提醒"
            ).joinToString(" · ")
            Text(text = sub, fontSize = 11.sp, color = LightText, modifier = Modifier.padding(top = 3.dp))
        }
        // 行内状态操作
        val status = r.loanStatus
        if (status == com.peoplenet.app.data.LoanStatus.Paid) {
            SmallPill(text = "已结清", bg = ContactedBg, fg = ContactedGreen)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "已还",
                    fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(ContactedGreen)
                        .clickable { viewModel.markLoanPaid(r.id) }
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                )
                if (status != com.peoplenet.app.data.LoanStatus.Partial) {
                    Text(
                        text = "部分",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MediumText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .border(1.5.dp, SectionBorder, RoundedCornerShape(999.dp))
                            .clickable { viewModel.markLoanPartial(r.id) }
                            .padding(horizontal = 11.dp, vertical = 5.dp)
                    )
                } else {
                    SmallPill(text = "部分还", bg = Color(0xFFFFE9CE), fg = Color(0xFFD96F1B))
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(title: String, sub: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 60.dp, start = 24.dp, end = 24.dp)
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
        Text(
            text = sub,
            fontSize = 12.sp, color = MediumText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
