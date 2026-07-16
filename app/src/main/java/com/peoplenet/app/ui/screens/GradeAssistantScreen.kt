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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peoplenet.app.data.Contact
import com.peoplenet.app.ui.theme.*
import com.peoplenet.app.viewmodel.Closeness
import com.peoplenet.app.viewmodel.PeopleNetViewModel

private val LavRow = Color(0xFFF4F0FF)
private val PinkDeep = Color(0xFFF0567E)
private val GrayHeart = Color(0xFF7A88A8)
private val ReasonGray = Color(0xFFB6ACD6)
private val SkipGray = Color(0xFFA79CC9)
private val ChangeBorder = Color(0xFFD9C9F5)
private val GreenPill = Color(0xFF0FA47C)
private val GreenPillBg = Color(0xFFE1F8F0)

@Composable
fun GradeAssistantScreen(viewModel: PeopleNetViewModel) {
    val queue = viewModel.gradeQueue()
    val done = viewModel.closenessSetCount
    val total = viewModel.contacts.size

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶栏返回
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
            Text(text = "联系人", fontSize = 12.sp, color = LightText, fontWeight = FontWeight.Bold)
        }

        // 标题 + 进度
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "分级助手", fontWeight = FontWeight.Bold, fontSize = 23.sp, color = DarkText)
                Text(
                    text = "已整理 $done/$total",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GreenPill,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(GreenPillBg)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
            Text(
                text = "根据期望频率和来往记录建议心值 · 采纳与否都由你决定",
                fontSize = 12.sp,
                color = MediumText,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }

        if (queue.isEmpty()) {
            // 队列清空：整理完成
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF0F3))
                ) {
                    Text(text = "♥", fontSize = 30.sp, color = Color(0xFFFF6F91))
                }
                Text(
                    text = "都整理好了",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DarkText,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = "$done 位联系人都已定亲密度 · 随时可在详情页调整",
                    fontSize = 12.5.sp,
                    color = MediumText,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    text = "返回联系人",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier
                        .padding(top = 22.dp)
                        .shadow(12.dp, RoundedCornerShape(999.dp), ambientColor = PurplePrimary.copy(alpha = 0.4f))
                        .clip(RoundedCornerShape(999.dp))
                        .background(Brush.linearGradient(listOf(PurplePrimary, PurpleLight)))
                        .clickable { viewModel.goBack() }
                        .padding(horizontal = 26.dp, vertical = 12.dp)
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(top = 14.dp, bottom = 24.dp)
            ) {
                queue.forEach { contact ->
                    GradeCard(viewModel = viewModel, contact = contact)
                }
                Text(
                    text = "一键采纳全部建议",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.gradeAdoptAll() }
                        .padding(vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun GradeCard(viewModel: PeopleNetViewModel, contact: Contact) {
    val tier = viewModel.gradeTierFor(contact)
    val isSuggest = tier == viewModel.suggestCloseness(contact)
    val reason = if (isSuggest) viewModel.suggestReason(contact) else "你选的档位 · 随时可改"
    val heartsLabel = if (tier == 0) Closeness.names[0] else "${Closeness.hearts(tier)} ${Closeness.names[tier]}"
    val lastTouch = contact.history.firstOrNull()?.first

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = PurplePrimary.copy(alpha = 0.14f))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(15.dp)
    ) {
        // 头像 + 名字 + 关系/频率
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradientAvatar(contact = contact, size = 42)
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = contact.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkText)
                Text(
                    text = "${contact.rel} · 期望${contact.freq}" +
                        (lastTouch?.let { " · ${it}联系过" } ?: "联系"),
                    fontSize = 11.5.sp,
                    color = MediumText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // 建议心值行
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 11.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(LavRow)
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Text(text = "建议心值", fontSize = 11.5.sp, color = MediumText)
            Spacer(Modifier.width(8.dp))
            Text(
                text = heartsLabel,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (tier == 0) GrayHeart else PinkDeep,
                maxLines = 1
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = reason,
                fontSize = 11.sp,
                color = ReasonGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // 采纳 / 换一档 / 跳过
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 11.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .shadow(6.dp, RoundedCornerShape(12.dp), ambientColor = PurplePrimary.copy(alpha = 0.3f))
                    .clip(RoundedCornerShape(12.dp))
                    .background(PurplePrimary)
                    .clickable { viewModel.gradeAdopt(contact.id) }
                    .padding(vertical = 10.dp)
            ) {
                Text(text = "采纳", fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, ChangeBorder, RoundedCornerShape(12.dp))
                    .clickable { viewModel.gradeChangeTier(contact.id) }
                    .padding(vertical = 10.dp)
            ) {
                Text(text = "换一档", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = PurplePrimary)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { viewModel.gradeSkip(contact.id) }
                    .padding(vertical = 10.dp)
            ) {
                Text(text = "跳过", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = SkipGray)
            }
        }
    }
}
