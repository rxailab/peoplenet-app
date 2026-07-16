package com.peoplenet.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peoplenet.app.data.Contact
import com.peoplenet.app.data.GiftReturn
import com.peoplenet.app.data.HistoryEntry
import com.peoplenet.app.data.LoanStatus
import com.peoplenet.app.data.MoneyRecord
import com.peoplenet.app.data.MoneyType
import com.peoplenet.app.ui.theme.*
import com.peoplenet.app.viewmodel.Closeness
import com.peoplenet.app.viewmodel.PeopleNetViewModel
import com.peoplenet.app.viewmodel.Screen

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ContactDetailScreen(viewModel: PeopleNetViewModel) {
    val contact = viewModel.selectedContact ?: return
    val history = viewModel.historyFor(contact)
    val money = viewModel.moneyFor(contact.id)
    val haptic = LocalHapticFeedback.current
    // 正在添加的目标分组："tags" | "diet" | "topic" | null；随联系人切换重置
    var editTarget by remember(contact.id) { mutableStateOf<String?>(null) }
    var draft by remember(contact.id) { mutableStateOf("") }
    // 长按删除的二次确认：(要删的文字, 确认后执行的删除)
    var confirmDelete by remember(contact.id) { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 8.dp)
        ) {
            Text(
                text = "‹",
                fontSize = 24.sp,
                color = PurplePrimary,
                modifier = Modifier
                    .clickable { viewModel.goBack() }
                    .padding(end = 10.dp)
            )
            Text(
                text = when (viewModel.state.previousTab) {
                    Screen.Home -> "今天"
                    Screen.Me -> "我"
                    else -> "联系人"
                },
                fontSize = 12.sp,
                color = LightText,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)
        ) {
            // Avatar + name
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                ) {
                    GradientAvatar(contact = contact, size = 76, cornerRadius = 24)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = contact.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = DarkText,
                        letterSpacing = 0.01.sp
                    )
                    Text(
                        text = "${contact.rel} · 期望${contact.freq}联系",
                        fontSize = 12.5.sp,
                        color = MediumText,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                    if (contact.city.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(GeoBluePillBg)
                                .padding(horizontal = 11.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Rounded.LocationOn, null, tint = GeoBlueDeep, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = contact.city, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = GeoBlueDeep)
                        }
                    }
                }
            }

            // 亲密度心值（详情页定级 + 助手建议）
            item { ClosenessCard(viewModel, contact) }

            // Job section
            if (contact.job != null) {
                item {
                    SectionDot(title = "职业 · 单位")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = PurplePrimary.copy(alpha = 0.15f))
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .padding(13.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = contact.job.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = DarkText
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = contact.job.industry,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PurplePrimary,
                                    modifier = Modifier
                                        .background(TagBg, RoundedCornerShape(999.dp))
                                        .padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = contact.job.org,
                                fontSize = 12.sp,
                                color = MediumText,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }

            // Dates
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                ) {
                    contact.dates.forEach { (label, value) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = PurplePrimary.copy(alpha = 0.15f))
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(11.dp)
                        ) {
                            Column {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = LightText,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = value,
                                    fontSize = 13.sp,
                                    color = DarkText2,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Tags（喜好）· 可编辑
            item {
                Column {
                    SectionDot(title = "喜好 · 长按删除")
                    androidx.compose.foundation.layout.FlowRow {
                        contact.tags.forEach { tag ->
                            DeletableChip(
                                text = tag, textColor = PurplePrimary, bgColor = TagBg, elevated = false,
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    confirmDelete = tag to { viewModel.removeTag(contact.id, tag) }
                                }
                            )
                        }
                        PlusChip(color = PurplePrimary) {
                            editTarget = if (editTarget == "tags") null else "tags"; draft = ""
                        }
                    }
                    if (editTarget == "tags") {
                        InlineAddRow(
                            value = draft, onValueChange = { draft = it }, accent = PurplePrimary,
                            placeholder = "添加一个喜好",
                            onConfirm = { viewModel.addTag(contact.id, draft); draft = ""; editTarget = null }
                        )
                    }
                }
            }

            // Avoid section（禁忌）· 可编辑（始终显示，便于添加）
            item {
                val avoid = contact.avoid
                Column {
                    SectionDot(title = "禁忌 · 别踩雷 · 长按删除", dotColor = PinkAccent)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(AvoidBg)
                            .padding(13.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                            AvoidEditRow(
                                label = "饮食", items = avoid?.diet ?: emptyList(),
                                onDelete = { item -> haptic.performHapticFeedback(HapticFeedbackType.LongPress); confirmDelete = item to { viewModel.removeAvoid(contact.id, true, item) } },
                                onAdd = { editTarget = if (editTarget == "diet") null else "diet"; draft = "" }
                            )
                            if (editTarget == "diet") {
                                InlineAddRow(
                                    value = draft, onValueChange = { draft = it }, accent = PinkAccent,
                                    placeholder = "添加一条饮食禁忌",
                                    onConfirm = { viewModel.addAvoid(contact.id, true, draft); draft = ""; editTarget = null }
                                )
                            }
                            AvoidEditRow(
                                label = "话题", items = avoid?.topics ?: emptyList(),
                                onDelete = { item -> haptic.performHapticFeedback(HapticFeedbackType.LongPress); confirmDelete = item to { viewModel.removeAvoid(contact.id, false, item) } },
                                onAdd = { editTarget = if (editTarget == "topic") null else "topic"; draft = "" }
                            )
                            if (editTarget == "topic") {
                                InlineAddRow(
                                    value = draft, onValueChange = { draft = it }, accent = PinkAccent,
                                    placeholder = "添加一条话题禁忌",
                                    onConfirm = { viewModel.addAvoid(contact.id, false, draft); draft = ""; editTarget = null }
                                )
                            }
                        }
                    }
                }
            }

            // 人情往来（2a 融合方案）：放在禁忌之后；无记录时给一个低调的添加入口
            item {
                MoneySectionDetail(
                    viewModel = viewModel,
                    contactId = contact.id,
                    records = money,
                    onRequestDelete = { r ->
                        val label = "${r.type.label} · " + if (r.isPhysical) r.itemName else formatYuan(r.amount)
                        confirmDelete = label to { viewModel.deleteMoneyRecord(r.id) }
                    }
                )
            }

            // Timeline
            item {
                SectionDot(title = "来往时间线", dotColor = PinkAccent)
            }
            itemsIndexed(history) { index, entry ->
                TimelineItem(
                    entry = entry,
                    isLast = index == history.size - 1,
                    contact = contact
                )
            }
        }

        // Bottom CTA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(14.dp, RoundedCornerShape(18.dp), ambientColor = PurplePrimary.copy(alpha = 0.4f))
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PurplePrimary, PurpleLight)
                        )
                    )
                    .clickable { viewModel.toRecord() }
                    .padding(15.dp)
            ) {
                Text(
                    text = "记一笔 · 标记已联系",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
        }
    }

    // 长按删除的二次确认，避免误删
    confirmDelete?.let { (label, onConfirm) ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(text = "删除「$label」？", fontWeight = FontWeight.Bold, color = DarkText) },
            text = { Text(text = "删除后可以重新添加。", color = MediumText) },
            confirmButton = {
                TextButton(onClick = { onConfirm(); confirmDelete = null }) {
                    Text(text = "删除", color = Color(0xFFF0567E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) {
                    Text(text = "取消", color = MediumText)
                }
            }
        )
    }

    // 记金钱往来底部弹层（有记录时从「＋」进入）
    MoneySheetOverlay(viewModel)
}

@Composable
private fun SectionDot(title: String, dotColor: Color = PurplePrimary) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 20.dp, bottom = 11.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = DarkText,
            letterSpacing = 0.03.sp
        )
    }
}

@Composable
private fun ClosenessCard(viewModel: PeopleNetViewModel, contact: Contact) {
    val pink = Color(0xFFFF6F91)
    val pinkDeep = Color(0xFFF0567E)
    val pinkBg = Color(0xFFFFF0F3)
    val pinkBorder = Color(0xFFFFD9E1)
    val heartFaint = Color(0xFFFFB3C4)
    val lavBg = Color(0xFFF4F0FF)
    val faint = Color(0xFFB6ACD6)
    val grayName = Color(0xFFA79CC9)
    val green = Color(0xFF0FA47C)
    val greenBg = Color(0xFFE1F8F0)

    val level = contact.closeness
    val suggest = viewModel.suggestCloseness(contact)
    val reason = viewModel.suggestReason(contact)
    val adopted = level == suggest

    Column {
        // 分区标题 + 未定时的「新增」标
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 20.dp, bottom = 11.dp)
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(pink))
            Spacer(Modifier.width(7.dp))
            Text(
                text = "亲密度心值",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText,
                letterSpacing = 0.03.sp
            )
            if (level == null) {
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "待定",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = pinkDeep,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(pinkBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        // 卡片：四档选择器 + 助手建议行
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = pink.copy(alpha = 0.18f))
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .border(1.5.dp, pinkBorder, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..3).forEach { i ->
                    val sel = level == i
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (sel) pinkBg else Color.Transparent)
                            .then(if (sel) Modifier.border(1.5.dp, pink, RoundedCornerShape(13.dp)) else Modifier)
                            .clickable { viewModel.setCloseness(contact.id, i) }
                            .padding(vertical = 10.dp, horizontal = 3.dp)
                    ) {
                        Text(
                            text = if (i == 0) "—" else "♥".repeat(i),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sel) pink else heartFaint,
                            maxLines = 1
                        )
                        Text(
                            text = Closeness.names[i],
                            fontSize = 10.5.sp,
                            fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (sel) pinkDeep else grayName,
                            maxLines = 1
                        )
                    }
                }
            }
            // 助手建议行
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(lavBg)
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text("助手建议", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurplePrimary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (suggest == 0) Closeness.names[0] else "${Closeness.hearts(suggest)} ${Closeness.names[suggest]}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (suggest == 0) faint else pinkDeep,
                    maxLines = 1
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = reason,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = faint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                if (adopted) {
                    Text(
                        text = "已采纳",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = green,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(greenBg)
                            .padding(horizontal = 11.dp, vertical = 4.dp)
                    )
                } else {
                    Text(
                        text = "采纳",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(PurplePrimary)
                            .clickable { viewModel.setCloseness(contact.id, suggest) }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
            Text(
                text = "仅用于排序和节日祝福顺序 · 不影响提醒",
                fontSize = 10.5.sp,
                color = faint,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun AvoidRow(label: String, items: List<String>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AvoidText,
            modifier = Modifier
                .width(26.dp)
                .padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        FlowRow(items = items) { item ->
            Text(
                text = item,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AvoidText,
                modifier = Modifier
                    .padding(end = 6.dp, bottom = 6.dp)
                    .shadow(4.dp, RoundedCornerShape(999.dp), ambientColor = PinkAccent.copy(alpha = 0.3f))
                    .background(Color.White, RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeletableChip(
    text: String,
    textColor: Color,
    bgColor: Color,
    elevated: Boolean,
    onLongPress: () -> Unit
) {
    Text(
        text = text,
        fontSize = if (elevated) 11.5.sp else 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = textColor,
        modifier = Modifier
            .padding(end = 7.dp, bottom = 7.dp)
            .then(if (elevated) Modifier.shadow(4.dp, RoundedCornerShape(999.dp), ambientColor = PinkAccent.copy(alpha = 0.3f)) else Modifier)
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(horizontal = if (elevated) 12.dp else 13.dp, vertical = if (elevated) 5.dp else 6.dp)
    )
}

@Composable
private fun PlusChip(color: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(end = 7.dp, bottom = 7.dp)
            .clip(RoundedCornerShape(999.dp))
            .border(1.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp)
    ) {
        Icon(Icons.Rounded.Add, contentDescription = "添加", tint = color, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun InlineAddRow(
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    placeholder: String,
    onConfirm: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text(text = placeholder, color = LightText, fontSize = 13.sp) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = accent,
                focusedTextColor = DarkText,
                unfocusedTextColor = DarkText
            ),
            textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).shadow(6.dp, RoundedCornerShape(12.dp), ambientColor = accent.copy(alpha = 0.15f))
        )
        Spacer(modifier = Modifier.width(8.dp))
        val canAdd = value.trim().isNotEmpty()
        Text(
            text = "添加",
            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (canAdd) accent else accent.copy(alpha = 0.4f))
                .clickable(enabled = canAdd, onClick = onConfirm)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AvoidEditRow(
    label: String,
    items: List<String>,
    onDelete: (String) -> Unit,
    onAdd: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AvoidText,
            modifier = Modifier.width(26.dp).padding(top = 6.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        androidx.compose.foundation.layout.FlowRow(modifier = Modifier.weight(1f)) {
            items.forEach { item ->
                DeletableChip(
                    text = item, textColor = AvoidText, bgColor = Color.White, elevated = true,
                    onLongPress = { onDelete(item) }
                )
            }
            PlusChip(color = PinkAccent, onClick = onAdd)
        }
    }
}

@Composable
private fun TimelineItem(entry: HistoryEntry, isLast: Boolean, contact: Contact) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Dot + line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (entry.fresh) 11.dp else 9.dp)
                    .clip(CircleShape)
                    .background(
                        if (entry.fresh) PurplePrimary else PurplePrimary.copy(alpha = 0.25f)
                    )
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .defaultMinSize(minHeight = 30.dp)
                        .background(Color(0xFFECE6F6), RoundedCornerShape(2.dp))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = entry.date,
                fontSize = 11.sp,
                color = LightText,
                fontWeight = FontWeight.SemiBold
            )
            if (entry.methods.isNotEmpty()) {
                FlowRow(
                    items = entry.methods,
                    modifier = Modifier.padding(top = 6.dp)
                ) { method ->
                    Text(
                        text = method,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PurplePrimary,
                        modifier = Modifier
                            .padding(end = 6.dp, bottom = 6.dp)
                            .background(TagBg, RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }
            if (entry.text.isNotEmpty()) {
                Text(
                    text = entry.text,
                    fontSize = 13.sp,
                    color = Color(0xFF473E63),
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    items: List<String>,
    modifier: Modifier = Modifier,
    content: @Composable (String) -> Unit
) {
    // Simple flow layout using Compose's built-in FlowRow
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier
    ) {
        items.forEach { item ->
            content(item)
        }
    }
}

// ==== 人情往来板块（详情页）====

@Composable
private fun MoneySectionDetail(
    viewModel: PeopleNetViewModel,
    contactId: String,
    records: List<MoneyRecord>,
    onRequestDelete: (MoneyRecord) -> Unit
) {
    // 无记录：只放一个低调的虚线入口，不喧宾夺主
    if (records.isEmpty()) {
        SubtleAddMoneyEntry { viewModel.openMoneySheet(contactId) }
    } else {
        Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 20.dp, bottom = 11.dp)
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(ContactedGreen))
            Spacer(Modifier.width(7.dp))
            Text("人情往来", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkText, letterSpacing = 0.03.sp)
            Spacer(Modifier.width(7.dp))
            Text(
                text = "${records.size} 笔",
                fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = ContactedGreen,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFE1F8F0))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "点按编辑 · 长按删除",
                fontSize = 10.sp, color = LightText
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = PurplePrimary.copy(alpha = 0.12f))
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
        ) {
            // 每行下方都留分隔线（含最后一行，与「＋」按钮分隔），沿用设计 F3
            records.forEach { r ->
                MoneyRow(
                    viewModel = viewModel,
                    r = r,
                    onEdit = { viewModel.openMoneySheetForEdit(r) },
                    onDelete = { onRequestDelete(r) }
                )
            }
            Text(
                text = "＋ 记一笔金钱往来",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PurplePrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openMoneySheet(contactId) }
                    .padding(vertical = 12.dp)
            )
        }
        }
    }
}

@Composable
private fun SubtleAddMoneyEntry(onClick: () -> Unit) {
    val stroke = Color(0xFFD9D2EC)
    Row(modifier = Modifier.padding(top = 20.dp, bottom = 2.dp)) {
        Text(
            text = "＋ 记一笔金钱往来",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MediumText,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .drawBehind {
                    drawRoundRect(
                        color = stroke,
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(11f, 8f), 0f)),
                        cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                    )
                }
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MoneyRow(
    viewModel: PeopleNetViewModel,
    r: MoneyRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onEdit,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                }
            )
            .drawBehind {
                drawLine(
                    color = SectionBorder,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        TypePill(r.type)
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            // 主行：金额 / 实物名（+估值）
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = when {
                        r.isPhysical -> r.itemName
                        r.type.isGift -> "礼金 ${formatYuan(r.amount)}"
                        else -> formatYuan(r.amount)
                    },
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkText
                )
                if (r.isPhysical && r.estValue > 0) {
                    Text(
                        text = "  估值 ${formatYuan(r.estValue)}",
                        fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = MediumText
                    )
                }
            }
            val sub = listOf(r.date, r.note.ifBlank { r.event }).filter { it.isNotBlank() }.joinToString(" · ")
            if (sub.isNotBlank()) {
                Text(text = sub, fontSize = 11.sp, color = LightText, modifier = Modifier.padding(top = 3.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        // 右侧状态
        when {
            r.type.isLoan && r.loanStatus != null -> {
                Column(horizontalAlignment = Alignment.End) {
                    val (bg, fg) = loanStatusColors(r.loanStatus)
                    // 已还是终态：不再可点，避免误触把结清的往来又翻回未还（并复活提醒）
                    SmallPill(
                        text = r.loanStatus.label, bg = bg, fg = fg,
                        modifier = Modifier.clickable(enabled = r.loanStatus != LoanStatus.Paid) { viewModel.cycleLoanStatus(r.id) }
                    )
                    if (r.reminderDate.isNotBlank() && r.loanStatus != LoanStatus.Paid) {
                        Text(
                            text = "提醒 ${r.reminderDate}",
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PurplePrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            r.type == MoneyType.ReceiveGift && r.giftReturn != null -> {
                val (bg, fg) = giftReturnColors(r.giftReturn)
                SmallPill(
                    text = r.giftReturn.label, bg = bg, fg = fg,
                    modifier = Modifier.clickable(enabled = r.giftReturn == GiftReturn.Pending) { viewModel.markGiftReturned(r.id) }
                )
            }
        }
    }
}

// ==== 记金钱往来 · 底部弹层 ====

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoneySheetOverlay(viewModel: PeopleNetViewModel) {
    val form = viewModel.state.moneySheet ?: return
    val name = viewModel.selectedContact?.name ?: ""
    val editing = viewModel.state.moneySheetEditId != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x59241A3D))
            .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { viewModel.closeMoneySheet() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color.White)
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {}
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 22.dp)
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 14.dp)
                    .width(36.dp).height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFECE6F6))
            )
            Text(
                text = (if (editing) "编辑金钱往来 · " else "记金钱往来 · ") + name,
                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = DarkText2
            )

            // 四类卡片 2x2
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                listOf(
                    MoneyType.Lend to MoneyType.Borrow,
                    MoneyType.GiveGift to MoneyType.ReceiveGift
                ).forEach { (a, b) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        TypeCard(a, form.type == a, Modifier.weight(1f)) { viewModel.sheetSetType(a) }
                        TypeCard(b, form.type == b, Modifier.weight(1f)) { viewModel.sheetSetType(b) }
                    }
                }
            }

            // 金额 / 实物
            Spacer(Modifier.height(16.dp))
            SheetSegment(
                options = listOf("金额", "实物"),
                selected = if (form.isPhysical) 1 else 0,
                onSelect = { viewModel.sheetSetPhysical(it == 1) }
            )

            Spacer(Modifier.height(14.dp))
            if (!form.isPhysical) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawLine(
                                color = Color(0xFFECE6F6),
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                        .padding(bottom = 8.dp)
                ) {
                    Text("¥", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PurplePrimary, modifier = Modifier.padding(end = 6.dp, bottom = 3.dp))
                    BasicTextField(
                        value = form.amountText,
                        onValueChange = { viewModel.sheetSetAmount(it) },
                        textStyle = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = DarkText),
                        cursorBrush = SolidColor(PurplePrimary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (form.amountText.isEmpty()) Text("0", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = FadedText)
                            inner()
                        }
                    )
                }
            } else {
                SheetField(form.itemName, "礼物名称，如 茶具一套") { viewModel.sheetSetItem(it) }
                Spacer(Modifier.height(9.dp))
                SheetField(form.estValueText, "估值（元，可选）", number = true) { viewModel.sheetSetEstValue(it) }
            }

            Spacer(Modifier.height(12.dp))
            SheetField(form.note, "备注（可选），如 他儿子装修") { viewModel.sheetSetNote(it) }

            if (form.type.isLoan) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                ) {
                    Text("到期提醒", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = DarkText)
                    if (form.reminderOn) {
                        Text(
                            text = form.reminderDate,
                            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = PurplePrimary,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(PurplePillBg)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    MoneyToggle(on = form.reminderOn) { viewModel.sheetToggleReminder() }
                }
            }

            Spacer(Modifier.height(18.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(14.dp, RoundedCornerShape(18.dp), ambientColor = PurplePrimary.copy(alpha = 0.4f))
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(PurplePrimary, PurpleLight)))
                    .clickable { viewModel.saveMoneySheet() }
                    .padding(15.dp)
            ) {
                Text(if (editing) "保存修改" else "保存这一笔", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun TypeCard(type: MoneyType, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFFF4F0FF) else Color.White)
            .border(1.5.dp, if (selected) PurplePrimary else SectionBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp)
    ) {
        Text(type.label, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (selected) PurplePrimary else DarkText)
        Text(type.hint, fontSize = 10.5.sp, color = if (selected) MediumText else LightText, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun SheetSegment(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(TagBg)
            .padding(3.dp)
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (sel) FontWeight.Bold else FontWeight.SemiBold,
                color = if (sel) Color.White else MediumText,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (sel) PurplePrimary else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun SheetField(value: String, placeholder: String, number: Boolean = false, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF4F0FF))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF473E63)),
            cursorBrush = SolidColor(PurplePrimary),
            keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, fontSize = 13.sp, color = LightText)
                inner()
            }
        )
    }
}
