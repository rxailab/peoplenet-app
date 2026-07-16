package com.peoplenet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peoplenet.app.data.SampleData
import com.peoplenet.app.ui.theme.*
import com.peoplenet.app.viewmodel.PeopleNetViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordScreen(viewModel: PeopleNetViewModel) {
    val contact = viewModel.selectedContact ?: return

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
                text = "记一笔",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = DarkText2
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
        ) {
            item {
                // AI 一句话记一笔（1b）
                AiRecordCard(viewModel)

                Spacer(modifier = Modifier.height(18.dp))

                // Prompt
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "和 ", fontSize = 14.sp, color = Color(0xFF6B6390))
                    Text(text = contact.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkText2)
                    Text(text = " 做了什么？", fontSize = 14.sp, color = Color(0xFF6B6390))
                    if (viewModel.state.aiStage == 3) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✦ 已识别 · 帮你填好了",
                            fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0FA47C)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(13.dp))

                // Chips
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SampleData.chipOptions.forEach { label ->
                        val active = viewModel.state.selectedChips.contains(label)
                        ChipButton(
                            label = label,
                            active = active,
                            activeColor = PurplePrimary,
                            onClick = { viewModel.toggleChip(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Note input
                TextField(
                    value = viewModel.state.note,
                    onValueChange = { viewModel.setNote(it) },
                    placeholder = {
                        Text(
                            text = "想记点什么…（可选）",
                            color = LightText,
                            fontSize = 14.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = PurplePrimary,
                        focusedTextColor = DarkText2,
                        unfocusedTextColor = DarkText2
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = PurplePrimary.copy(alpha = 0.2f))
                )

                // 金钱往来 · 可选段落（2a 融合方案：折叠→一句话识别）
                MoneySection(viewModel)

                // AI 识别到与已有借出重复时的关联提示（1b）
                if (viewModel.state.aiStage == 3 && viewModel.state.aiMoneyLinked) {
                    Text(
                        text = "✦ 已关联 5月30日「他儿子装修」那笔借出，不会记重",
                        fontSize = 10.5.sp, color = MediumText,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Frequency picker
                Text(
                    text = "下次多久联系一次？",
                    fontSize = 13.sp,
                    color = Color(0xFF6B6390),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(11.dp))

                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SampleData.freqOptions.forEach { label ->
                        val active = viewModel.state.selectedFreq == label
                        val suggested = viewModel.state.aiFreqSuggested && active
                        Box {
                            ChipButton(
                                label = label,
                                active = active,
                                activeColor = PurplePrimary,
                                onClick = { viewModel.pickFreq(label) }
                            )
                            if (suggested) {
                                Text(
                                    text = "✦ 建议",
                                    fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-8).dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Brush.linearGradient(listOf(PurplePrimary, PurpleLight)))
                                        .border(1.5.dp, Color(0xFFFBF9FF), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                if (viewModel.state.aiFreqSuggested) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "你们约了周六钓鱼，保持每月的节奏刚刚好",
                        fontSize = 10.5.sp, color = LightText
                    )
                }
            }
        }

        // Save button
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
                    .clickable { viewModel.save() }
                    .padding(15.dp)
            ) {
                Text(
                    text = "保存这一笔",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun AiRecordCard(viewModel: PeopleNetViewModel) {
    val stage = viewModel.state.aiStage
    val text = viewModel.state.aiText
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = PurplePrimary.copy(alpha = 0.12f))
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.5.dp, PurplePrimary.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AiBadge()
            Spacer(Modifier.width(6.dp))
            Text(text = "一句话，或者说给我听", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MediumText)
        }
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(top = 11.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8F6FF))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                if (stage == 0) {
                    BasicTextField(
                        value = text,
                        onValueChange = { viewModel.aiSetText(it) },
                        textStyle = TextStyle(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = DarkText2),
                        cursorBrush = SolidColor(PurplePrimary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (text.isEmpty()) Text("如：跟老周打了电话，他借的两千下月还", fontSize = 13.5.sp, color = LightText)
                            inner()
                        }
                    )
                } else {
                    Text(
                        text = text + if (stage == 1) " |" else "",
                        fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = DarkText2, lineHeight = 22.sp
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .shadow(8.dp, CircleShape, ambientColor = PurplePrimary.copy(alpha = 0.35f))
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PurplePrimary, PurpleLight)))
                    .clickable {
                        if (text.isBlank() || stage == 3) viewModel.aiRunVoiceDemo()
                        else viewModel.aiParseTyped(text)
                    }
            ) {
                Icon(Icons.Rounded.Mic, contentDescription = "语音", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        when (stage) {
            0 -> Text(
                text = "▸ 试试语音示例",
                fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = PurplePrimary,
                modifier = Modifier
                    .padding(top = 11.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFF4F0FF))
                    .clickable { viewModel.aiRunVoiceDemo() }
                    .padding(horizontal = 13.dp, vertical = 6.dp)
            )
            1 -> Text(text = "● 正在听…", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurplePrimary, modifier = Modifier.padding(top = 11.dp))
            2 -> Text(text = "✦ 识别中…", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurplePrimary, modifier = Modifier.padding(top = 11.dp))
            else -> Text(
                text = "识别不对？清空重来",
                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurplePrimary,
                modifier = Modifier
                    .padding(top = 11.dp)
                    .clickable { viewModel.aiReset() }
            )
        }
    }
}

@Composable
fun ChipButton(
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val bgColor = if (active) activeColor else Color.Transparent
    val textColor = if (active) Color.White else Color(0xFF8A8378)
    val borderColor = if (active) activeColor else Color(0xFFE1DCD2)

    Text(
        text = label,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = textColor,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
