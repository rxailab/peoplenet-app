package com.peoplenet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peoplenet.app.data.MoneyType
import com.peoplenet.app.ui.theme.*
import com.peoplenet.app.viewmodel.PeopleNetViewModel

private val MoneyGreen = Color(0xFF0FA47C)
private val MoneyCardBorder = Color(0xFFD5EFEB)
private val NlBoxBg = Color(0xFFF8F6FF)
private val RecoChipBg = Color(0xFFF4F0FF)

/** 记一笔里的「金钱往来」可选段落：折叠→虚线入口；展开→一句话识别 / 手动填写。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoneySection(viewModel: PeopleNetViewModel) {
    val draft = viewModel.state.moneyDraft

    // 单一 Column 根；用 if/else（不要提前 return，会破坏 Compose 分组）
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!draft.expanded) {
            Spacer(Modifier.height(14.dp))
            DashedMoneyEntry { viewModel.moneyToggleExpand() }
        } else {
            Spacer(Modifier.height(22.dp))
            // 段落标题
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 11.dp)) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(MoneyGreen))
                Spacer(Modifier.width(7.dp))
                Text("金钱往来", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkText)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "收起 ˄",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LightText,
                    modifier = Modifier.clickable { viewModel.moneyToggleExpand() }.padding(4.dp)
                )
            }
            // 绿色卡片
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.5.dp, MoneyCardBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                if (draft.manual) ManualForm(viewModel) else NlForm(viewModel)
            }
        }
    }
}

@Composable
private fun DashedMoneyEntry(onClick: () -> Unit) {
    val stroke = Color(0xFFD9D2EC)
    Text(
        text = "＋ 金钱往来",
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

// ---- 一句话识别 ----

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NlForm(viewModel: PeopleNetViewModel) {
    val draft = viewModel.state.moneyDraft
    // 一句话输入框
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NlBoxBg)
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        BasicTextField(
            value = draft.nlText,
            onValueChange = { viewModel.moneySetNl(it) },
            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = DarkText),
            cursorBrush = SolidColor(PurplePrimary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (draft.nlText.isEmpty()) {
                    Text("一句话，如：借了他 2000，下个月还", fontSize = 14.sp, color = LightText)
                }
                inner()
            }
        )
    }

    if (draft.recognized) {
        Text(
            text = "已识别 · 点任意一项修改",
            fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MoneyGreen,
            modifier = Modifier.padding(top = 11.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            val (_, typeFg) = moneyTypeColors(draft.type)
            RecoChip(label = "类型", value = draft.type.label, valueColor = typeFg) { viewModel.moneyCycleType() }
            if (draft.isPhysical) {
                RecoChip(label = "物品", value = draft.itemName.ifBlank { "礼物" }, valueColor = DarkText) { viewModel.moneyToggleManual() }
            } else {
                val amt = draft.amountText.toIntOrNull() ?: 0
                RecoChip(label = "金额", value = formatYuan(amt), valueColor = DarkText) { viewModel.moneyToggleManual() }
            }
            if (draft.type.isLoan && draft.reminderOn) {
                RecoChip(label = "提醒", value = draft.reminderDate, valueColor = PurplePrimary) { viewModel.moneyToggleManual() }
            }
        }
        if (draft.type.isLoan) {
            ReminderRow(viewModel)
        }
    }

    Text(
        text = "识别不对？手动填写",
        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurplePrimary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clickable { viewModel.moneyToggleManual() },
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
private fun RecoChip(label: String, value: String, valueColor: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(RecoChipBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text("$label ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LightText)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
    }
}

// ---- 手动表单 ----

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManualForm(viewModel: PeopleNetViewModel) {
    val draft = viewModel.state.moneyDraft
    // 四类
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MoneyType.values().forEach { t ->
            ChipButton(label = t.label, active = draft.type == t, activeColor = MoneyGreen) { viewModel.moneySetType(t) }
        }
    }
    Spacer(Modifier.height(12.dp))
    // 金额 / 实物
    Segment(
        options = listOf("金额", "实物"),
        selected = if (draft.isPhysical) 1 else 0,
        onSelect = { viewModel.moneySetPhysical(it == 1) }
    )
    Spacer(Modifier.height(12.dp))
    if (!draft.isPhysical) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("¥", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MoneyGreen, modifier = Modifier.padding(end = 6.dp, bottom = 3.dp))
            BasicTextField(
                value = draft.amountText,
                onValueChange = { viewModel.moneySetAmount(it) },
                textStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = DarkText),
                cursorBrush = SolidColor(MoneyGreen),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (draft.amountText.isEmpty()) Text("0", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = FadedText)
                    inner()
                }
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
            listOf(200, 520, 1000, 2000).forEach { q ->
                val sel = draft.amountText == q.toString()
                Text(
                    text = "¥$q",
                    fontSize = 12.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (sel) MoneyGreen else Color(0xFF8A8378),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .border(1.5.dp, if (sel) MoneyGreen else Color(0xFFE1DCD2), RoundedCornerShape(999.dp))
                        .clickable { viewModel.moneySetAmount(q.toString()) }
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                )
            }
        }
    } else {
        FieldBox(value = draft.itemName, placeholder = "礼物名称，如 茶具一套", onValueChange = { viewModel.moneySetItem(it) })
        Spacer(Modifier.height(8.dp))
        FieldBox(value = draft.estValueText, placeholder = "估值（元，可选）", number = true, onValueChange = { viewModel.moneySetEstValue(it) })
    }
    Spacer(Modifier.height(12.dp))
    FieldBox(value = draft.note, placeholder = "备注（可选），如 他儿子装修", onValueChange = { viewModel.moneySetNote(it) })
    if (draft.type.isLoan) {
        ReminderRow(viewModel)
    }
    Text(
        text = "← 用一句话记",
        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurplePrimary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .clickable { viewModel.moneyToggleManual() },
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
private fun ReminderRow(viewModel: PeopleNetViewModel) {
    val draft = viewModel.state.moneyDraft
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .drawBehind {
                drawLine(
                    color = SectionBorder,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(top = 12.dp)
    ) {
        Text("到期提醒", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = DarkText)
        if (draft.reminderOn) {
            Text(
                text = draft.reminderDate,
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = PurplePrimary,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(PurplePillBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        MoneyToggle(on = draft.reminderOn) { viewModel.moneyToggleReminder() }
    }
}

@Composable
fun MoneyToggle(on: Boolean, onToggle: () -> Unit) {
    Box(
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
        modifier = Modifier
            .width(40.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (on) MoneyGreen else Color(0xFFD9D2EC))
            .clickable(onClick = onToggle)
            .padding(2.dp)
    ) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(Color.White))
    }
}

@Composable
private fun Segment(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
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
private fun FieldBox(value: String, placeholder: String, number: Boolean = false, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF4F0FF))
            .padding(horizontal = 12.dp, vertical = 11.dp)
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
