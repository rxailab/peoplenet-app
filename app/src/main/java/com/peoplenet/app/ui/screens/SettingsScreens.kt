package com.peoplenet.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peoplenet.app.data.Contact
import com.peoplenet.app.ui.theme.*
import com.peoplenet.app.viewmodel.PeopleNetViewModel

// ---------- shared pieces ----------

@Composable
private fun SettingsHeader(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 20.dp, top = 6.dp, bottom = 2.dp)
    ) {
        Text(
            text = "‹",
            fontSize = 26.sp,
            color = PurplePrimary,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(text = "我", fontSize = 12.sp, color = LightText, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ScreenTitle(text: String, sub: String? = null) {
    Column(modifier = Modifier.padding(start = 22.dp, top = 4.dp, bottom = 14.dp)) {
        Text(text = text, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = DarkText)
        if (sub != null) {
            Text(text = sub, fontSize = 12.sp, color = MediumText, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = FadedText,
        letterSpacing = 0.08.sp,
        modifier = Modifier.padding(start = 26.dp, top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = PurplePrimary.copy(alpha = 0.12f))
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White),
        content = content
    )
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(0.6.dp)
            .background(SectionBorder)
    )
}

@Composable
private fun PurpleSwitch(checked: Boolean) {
    // Display-only: the enclosing row's clickable owns the toggle, so the switch must
    // NOT also handle the tap (that double-fired and cancelled itself out).
    Switch(
        checked = checked,
        onCheckedChange = null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = PurplePrimary,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = FadedText.copy(alpha = 0.5f),
            uncheckedBorderColor = Color.Transparent
        )
    )
}

@Composable
private fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, color = Color(0xFF2E2545), fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 11.5.sp, color = MediumText, modifier = Modifier.padding(top = 2.dp))
            }
        }
        PurpleSwitch(checked = checked)
    }
}

@Composable
private fun ValueRow(title: String, value: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = if (enabled) Color(0xFF2E2545) else FadedText,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            color = if (enabled) PurplePrimary else FadedText,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MiniAvatar(contact: Contact, size: Int = 34) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.32).dp))
            .background(Brush.linearGradient(listOf(contact.color, contact.colorSecondary)))
    ) {
        val pic = contact.avatar
        if (pic != null) {
            Image(
                bitmap = pic,
                contentDescription = contact.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(text = contact.av, color = Color.White, fontSize = (size * 0.4).sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------- 关于人际关系网 ----------

@Composable
fun AboutScreen(viewModel: PeopleNetViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        SettingsHeader(title = "关于", onBack = { viewModel.backToMe() })
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(78.dp)
                            .shadow(18.dp, RoundedCornerShape(24.dp), ambientColor = PurplePrimary.copy(alpha = 0.4f))
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(listOf(PurplePrimary, PurpleLight)))
                    ) {
                        Text(text = "网", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "人际关系网", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkText)
                    Text(
                        text = "版本 1.0.0 · 原型演示",
                        fontSize = 12.sp,
                        color = MediumText,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            item {
                Card {
                    Text(
                        text = "把「想起了却没联系」，变成「今天已经问候过」。",
                        fontSize = 14.sp,
                        color = DarkText,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            item { SectionLabel(text = "它能帮你") }
            item {
                Card {
                    val features = listOf(
                        Icons.Rounded.NotificationsNone to "到点提醒，按你设定的节奏联系每个人",
                        Icons.Rounded.ChatBubbleOutline to "记住上次聊了什么，给你现成的开场白",
                        Icons.Rounded.Explore to "标注雷区，避开不该提的话题",
                        Icons.Rounded.TrendingUp to "每条问候进时间线，关系看得见"
                    )
                    features.forEachIndexed { i, (icon, text) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(TagBg)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = PurplePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = text, fontSize = 13.5.sp, color = Color(0xFF2E2545), lineHeight = 19.sp)
                        }
                        if (i < features.lastIndex) RowDivider()
                    }
                }
            }

            item {
                Text(
                    text = "用心经营每一段关系",
                    fontSize = 12.sp,
                    color = FadedText,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

// ---------- 通知与免打扰 ----------

@Composable
fun NotificationsScreen(viewModel: PeopleNetViewModel) {
    val notif = viewModel.state.notif
    Column(modifier = Modifier.fillMaxSize()) {
        SettingsHeader(title = "通知", onBack = { viewModel.backToMe() })
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item { ScreenTitle(text = "通知与免打扰", sub = "决定什么时候、以什么方式提醒你") }

            item { SectionLabel(text = "每日提醒") }
            item {
                Card {
                    ToggleRow(
                        title = "每日联系提醒",
                        subtitle = "每天挑出该联系的人",
                        checked = notif.dailyReminder,
                        onToggle = { viewModel.toggleDailyReminder() }
                    )
                    RowDivider()
                    ValueRow(
                        title = "提醒时间",
                        value = "每天 ${notif.reminderTime} ›",
                        enabled = notif.dailyReminder,
                        onClick = { viewModel.cycleReminderTime() }
                    )
                }
            }

            item { SectionLabel(text = "其他提醒") }
            item {
                Card {
                    ToggleRow(
                        title = "生日与纪念日",
                        subtitle = "重要日子提前一天提醒",
                        checked = notif.birthdayReminder,
                        onToggle = { viewModel.toggleBirthdayReminder() }
                    )
                    RowDivider()
                    ToggleRow(
                        title = "逾期提醒",
                        subtitle = "太久没联系时轻轻提醒",
                        checked = notif.overdueReminder,
                        onToggle = { viewModel.toggleOverdueReminder() }
                    )
                }
            }

            item { SectionLabel(text = "免打扰") }
            item {
                Card {
                    ToggleRow(
                        title = "开启免打扰",
                        subtitle = "这段时间内不推送任何提醒",
                        checked = notif.dndEnabled,
                        onToggle = { viewModel.toggleDnd() }
                    )
                    RowDivider()
                    ValueRow(
                        title = "免打扰时段",
                        value = "${notif.dndRange} ›",
                        enabled = notif.dndEnabled,
                        onClick = { viewModel.cycleDndRange() }
                    )
                }
            }
        }
    }
}

// ---------- 关系分组管理 ----------

@Composable
fun GroupsScreen(viewModel: PeopleNetViewModel) {
    val groups = viewModel.groups()
    Column(modifier = Modifier.fillMaxSize()) {
        SettingsHeader(title = "分组", onBack = { viewModel.backToMe() })
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                ScreenTitle(
                    text = "关系分组管理",
                    sub = "${groups.size} 个圈子 · 共 ${viewModel.contacts.size} 位联系人"
                )
            }

            items(groups, key = { it.first }) { (name, members) ->
                var expanded by remember { mutableStateOf(false) }
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = PurplePrimary.copy(alpha = 0.12f))
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .clickable { expanded = !expanded }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(members.first().color)
                            )
                            Spacer(modifier = Modifier.width(9.dp))
                            Text(text = name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkText)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${members.size} 位",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(PurplePillBg)
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            // avatar cluster preview
                            if (!expanded) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    members.take(4).forEach { MiniAvatar(contact = it, size = 26) }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = FadedText,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (expanded) {
                            Spacer(modifier = Modifier.height(6.dp))
                            members.forEach { c ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewModel.openContact(c.id) }
                                        .padding(vertical = 7.dp)
                                ) {
                                    MiniAvatar(contact = c, size = 34)
                                    Spacer(modifier = Modifier.width(11.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = c.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DarkText)
                                        Text(text = c.tagline, fontSize = 11.5.sp, color = MediumText, maxLines = 1)
                                    }
                                    Text(text = c.rel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = c.color)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "分组按亲疏排序，展开可查看成员 · 点成员进主页",
                    fontSize = 11.5.sp,
                    color = FadedText,
                    modifier = Modifier.padding(horizontal = 26.dp, vertical = 16.dp)
                )
            }
        }
    }
}

// ---------- 从通讯录导入 ----------

private data class DeviceContact(val name: String, val hint: String, val av: String, val color: Color, val color2: Color)

private val mockDeviceContacts = listOf(
    DeviceContact("赵明", "手机通讯录", "赵", Color(0xFF6C7BE0), Color(0xFF8E9BF0)),
    DeviceContact("孙倩", "手机通讯录", "孙", Color(0xFFE0699A), Color(0xFFF08FB5)),
    DeviceContact("钱伟", "微信好友", "钱", Color(0xFF3FA98C), Color(0xFF5FC7A8)),
    DeviceContact("周琳", "手机通讯录", "周", Color(0xFFD98A5B), Color(0xFFE8A878)),
    DeviceContact("吴桐", "微信好友", "吴", Color(0xFF8367C7), Color(0xFFA98BE0)),
    DeviceContact("郑刚", "手机通讯录", "郑", Color(0xFF4C9A8E), Color(0xFF6FB8AD)),
    DeviceContact("冯小雨", "微信好友", "冯", Color(0xFFC77BBF), Color(0xFFD89BD2))
)

@Composable
fun ImportContactsScreen(viewModel: PeopleNetViewModel) {
    val candidates = remember { mockDeviceContacts }
    var selected by remember { mutableStateOf(setOf<Int>()) }
    var imported by remember { mutableStateOf(false) }
    var importedN by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        SettingsHeader(title = "导入", onBack = { viewModel.backToMe() })

        if (imported) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(start = 30.dp, end = 30.dp, bottom = 60.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(84.dp)
                        .shadow(18.dp, RoundedCornerShape(26.dp), ambientColor = GreenAccent.copy(alpha = 0.4f))
                        .clip(RoundedCornerShape(26.dp))
                        .background(Brush.linearGradient(listOf(GreenAccent, Color(0xFF43D6AF))))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }
                Text(
                    text = "已导入 $importedN 位",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText,
                    modifier = Modifier.padding(top = 20.dp)
                )
                Text(
                    text = "去为他们设定联系频率，就能收到提醒啦",
                    fontSize = 13.sp,
                    color = MediumText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(top = 26.dp)
                        .shadow(14.dp, RoundedCornerShape(16.dp), ambientColor = PurplePrimary.copy(alpha = 0.4f))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(PurplePrimary, PurpleLight)))
                        .clickable { viewModel.backToMe() }
                        .padding(horizontal = 42.dp, vertical = 13.dp)
                ) {
                    Text(text = "完成", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        } else {

        ScreenTitle(text = "从通讯录导入", sub = "勾选想长期维护的人，加入你的关系网")

        // select-all row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    selected = if (selected.size == candidates.size) emptySet()
                    else candidates.indices.toSet()
                }
                .padding(horizontal = 26.dp, vertical = 4.dp)
        ) {
            Text(
                text = "共 ${candidates.size} 位可导入",
                fontSize = 12.sp,
                color = MediumText,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (selected.size == candidates.size) "取消全选" else "全选",
                fontSize = 12.sp,
                color = PurplePrimary,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)) {
            items(candidates.size) { i ->
                val c = candidates[i]
                val isSel = selected.contains(i)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 5.dp)
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = PurplePrimary.copy(alpha = 0.1f))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .clickable {
                            selected = if (isSel) selected - i else selected + i
                        }
                        .padding(horizontal = 13.dp, vertical = 11.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(c.color, c.color2)))
                    ) {
                        Text(text = c.av, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = c.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkText)
                        Text(text = c.hint, fontSize = 11.5.sp, color = MediumText, modifier = Modifier.padding(top = 2.dp))
                    }
                    // checkbox
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSel) PurplePrimary else Color.Transparent)
                            .border(
                                width = 1.5.dp,
                                color = if (isSel) PurplePrimary else FadedText,
                                shape = CircleShape
                            )
                    ) {
                        if (isSel) Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        // bottom import button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            val enabled = selected.isNotEmpty()
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (enabled) Modifier.shadow(14.dp, RoundedCornerShape(16.dp), ambientColor = PurplePrimary.copy(alpha = 0.4f))
                        else Modifier
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (enabled) Brush.linearGradient(listOf(PurplePrimary, PurpleLight))
                        else Brush.linearGradient(listOf(FadedText.copy(alpha = 0.5f), FadedText.copy(alpha = 0.5f)))
                    )
                    .clickable(enabled = enabled) {
                        importedN = selected.size
                        viewModel.confirmImport(importedN)
                        imported = true
                    }
                    .padding(vertical = 15.dp)
            ) {
                Text(
                    text = if (enabled) "导入 ${selected.size} 位" else "请选择联系人",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        } // end else (picker)
    }
}
