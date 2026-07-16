package com.peoplenet.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peoplenet.app.data.Contact
import com.peoplenet.app.ui.theme.*
import com.peoplenet.app.viewmodel.PeopleNetViewModel

private val RowChevron = Color(0xFFC9C0E0)
private val CardBorderLight = Color(0xFFECE6F6)
private val HeroGrad = listOf(Color(0xFF6C5CE7), Color(0xFF7E6BF0), Color(0xFFA678EC))

@Composable
fun HomeScreen(viewModel: PeopleNetViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp)
        ) {
            if (viewModel.homeAllDone) {
                // 修正④ 完成态
                item { CelebrationHero(viewModel) }
                item { CompletedFoldedRow(viewModel) }
                if (viewModel.festDayBannerOn) item { FestDayBanner(viewModel) }
                if (viewModel.festPrepBannerOn) item { FestivalCompactRow(viewModel) }
                item { TomorrowPreview(viewModel) }
            } else {
                // 5a 默认态
                if (viewModel.authGuideOn) item { AuthGuideBanner(viewModel) }
                if (viewModel.festDayBannerOn) item { FestDayBanner(viewModel) }
                item { UnifiedTodayCard(viewModel) }
                if (!viewModel.state.weekPlanAdopted) item { AiPlanChip(viewModel) }
                if (viewModel.showTripCard) item { TripCardRedesign(viewModel) }
                if (viewModel.festPrepBannerOn) item { FestivalCard(viewModel) }
                item { WeekFreeSection(viewModel) }
            }
        }

        // 到达/节日推送 + toast
        ArrivalPush(viewModel)
        FestPush(viewModel)
        GeoToast(viewModel)

        // Turn 6 语音助手：底部面板 / 常驻 FAB / 确认条（确认条最后画，压在遮罩之上，撤销可点）
        HomeActionSheet(viewModel)
        VoiceAssistant(viewModel)
        VoiceFab(viewModel)
        VoiceConfirmBar(viewModel)
    }
}

// ---- 修正③ 授权引导（可关闭）----

@Composable
private fun AuthGuideBanner(viewModel: PeopleNetViewModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, CardBorderLight, RoundedCornerShape(16.dp))
            .padding(start = 14.dp, end = 10.dp, top = 11.dp, bottom = 11.dp)
    ) {
        Icon(Icons.Rounded.LocationOn, null, tint = MediumText, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(9.dp))
        Text("开启定位与日历，出行时提醒你顺路见面", fontSize = 11.sp, color = MediumText, modifier = Modifier.weight(1f))
        Text(
            "开启", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PurplePrimary,
            modifier = Modifier.clickable { viewModel.dismissAuthGuide() }.padding(horizontal = 6.dp, vertical = 4.dp)
        )
        Spacer(Modifier.width(4.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(22.dp).clip(CircleShape).background(Color(0xFFF4F1FA))
                .clickable { viewModel.dismissAuthGuide() }
        ) { Icon(Icons.Rounded.Close, "关闭", tint = LightText, modifier = Modifier.size(12.dp)) }
    }
}

// ---- 5a 一体化今天卡（整行可点）----

@Composable
private fun UnifiedTodayCard(viewModel: PeopleNetViewModel) {
    val today = viewModel.todayContacts
    val handled = viewModel.handledToday
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(24.dp), ambientColor = PurplePrimary.copy(alpha = 0.18f))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
    ) {
        // 紫色 hero 头（点击 → 专注流整页卡片滑动）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.openFlow() }
                .background(Brush.linearGradient(HeroGrad))
                .padding(18.dp)
        ) {
            Box(
                modifier = Modifier.size(118.dp).offset(x = 250.dp, y = (-56).dp)
                    .clip(CircleShape).background(Color.White.copy(alpha = 0.12f))
            )
            Column {
                Text(com.peoplenet.app.data.DateUtil.homeHeader(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.82f))
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
                    Text("${viewModel.remainingToday}", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 31.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("个朋友在等你", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.padding(bottom = 2.dp))
                    Spacer(Modifier.weight(1f))
                    Text("$handled / ${today.size}", fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(bottom = 3.dp))
                }
            }
        }
        // 联系人整行
        today.forEachIndexed { i, c ->
            TodayRow(viewModel, c, showDivider = i < today.lastIndex)
        }
    }
}

@Composable
private fun TodayRow(viewModel: PeopleNetViewModel, c: Contact, showDivider: Boolean) {
    val contacted = viewModel.isContacted(c.id)
    val overdue = c.overdue && !contacted
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showDivider) Modifier.drawBehind {
                drawLine(Color(0xFFF1EDFB), androidx.compose.ui.geometry.Offset(14.dp.toPx(), size.height), androidx.compose.ui.geometry.Offset(size.width, size.height), 1.dp.toPx())
            } else Modifier)
            .clickable { viewModel.openFlow() }
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            GradientAvatar(contact = c, size = 44, cornerRadius = 14)
            if (overdue) {
                Box(Modifier.size(13.dp).offset(x = 3.dp, y = (-3).dp).align(Alignment.TopEnd).clip(CircleShape).background(PinkAccent).border(2.dp, Color.White, CircleShape))
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkText)
                Spacer(Modifier.width(7.dp))
                when {
                    contacted -> Text("已联系", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ContactedGreen, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(ContactedBg).padding(horizontal = 9.dp, vertical = 2.dp))
                    overdue -> Text("逾期 5 天", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PinkAccentLight, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(OverduePinkBg).padding(horizontal = 9.dp, vertical = 2.dp))
                    else -> Text(c.rel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = c.color, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(c.color.copy(alpha = 0.12f)).padding(horizontal = 9.dp, vertical = 2.dp))
                }
            }
            val sub = viewModel.homeRowSubtitle(c)
            Text(
                sub, fontSize = 12.sp,
                color = if (sub.startsWith("🔔")) PurplePrimary else MediumText,
                fontWeight = if (sub.startsWith("🔔")) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("›", fontSize = 16.sp, color = RowChevron)
    }
}

// ---- 修正② AI 计划一行 chip ----

@Composable
private fun AiPlanChip(viewModel: PeopleNetViewModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, CardBorderLight, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        AiBadge()
        Spacer(Modifier.width(9.dp))
        Text("本周已排好：今天妈妈 · 周六老周 · 周日林夕", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DarkText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text("排入 ›", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PurplePrimary, modifier = Modifier.clickable { viewModel.adoptWeekPlan() })
    }
}

// ---- 行程卡（降级授权在卡内一行）----

@Composable
private fun TripCardRedesign(viewModel: PeopleNetViewModel) {
    val trip = viewModel.state.geo.realTrip ?: return
    val friends = trip.friendIds.mapNotNull { viewModel.contactById(it) }
    Column(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, CardBorderLight, RoundedCornerShape(18.dp))
            .padding(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(GeoBlueDeep))
            Spacer(Modifier.width(8.dp))
            Text("即将出行 · ${trip.city}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkText)
            Spacer(Modifier.width(8.dp))
            Text("来自日历", fontSize = 11.sp, color = LightText)
            Spacer(Modifier.weight(1f))
            Text("${friends.size} 位朋友在那儿", fontSize = 11.sp, color = MediumText)
        }
        friends.forEach { f ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp).clickable { viewModel.openContact(f.id) }) {
                GradientAvatar(contact = f, size = 32, cornerRadius = 11)
                Spacer(Modifier.width(10.dp))
                Text(f.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DarkText)
                Text("  · ${f.tagline}", fontSize = 13.sp, color = MediumText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("›", fontSize = 15.sp, color = RowChevron)
            }
        }
        if (!viewModel.authGuideOn) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 11.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GeoSoftBlue).padding(horizontal = 11.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Rounded.LocationOn, null, tint = GeoBlueDeep, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text("开定位，到${trip.city}时自动提醒你见面", fontSize = 11.sp, color = Color(0xFF5B7BA6), modifier = Modifier.weight(1f))
                Text("开启 ›", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GeoBlueDeep)
            }
        }
    }
}

// ---- 节日卡（compact，下单进度）----

@Composable
private fun FestivalCard(viewModel: PeopleNetViewModel) {
    Column(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, CardBorderLight, RoundedCornerShape(18.dp))
            .clickable { viewModel.festOpenGiftList() }
            .padding(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(FestOrange))
            Spacer(Modifier.width(8.dp))
            Text("中秋节 · 还有 7 天", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkText)
            Spacer(Modifier.weight(1f))
            Text("挑礼物 ›", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FestOrangeDeep)
        }
        Text("3 位要送礼 · AI 按关系和忌口挑好了 3 件，¥129–289", fontSize = 11.sp, color = MediumText, modifier = Modifier.padding(top = 5.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFF7EE)).padding(horizontal = 11.dp, vertical = 8.dp)
        ) {
            Text("0 / 3 已下单", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = FestOrangeDeep, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(FestPillBg).padding(horizontal = 8.dp, vertical = 2.dp))
            Spacer(Modifier.width(8.dp))
            Text("快递约 3–4 天，建议本周内定下来", fontSize = 11.sp, color = FestSubText)
        }
    }
}

// ---- 本周有空 ----

@Composable
private fun WeekFreeSection(viewModel: PeopleNetViewModel) {
    val week = viewModel.weekContacts
    if (week.isEmpty()) return
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 22.dp, top = 12.dp, bottom = 6.dp)) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(GreenAccent))
            Spacer(Modifier.width(7.dp))
            Text("本周有空", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkText)
        }
        week.forEach { c ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = DarkText.copy(alpha = 0.06f))
                    .clip(RoundedCornerShape(20.dp)).background(Color.White).clickable { viewModel.openHomeAction(c.id) }.padding(14.dp)
            ) {
                GradientAvatar(contact = c, size = 44, cornerRadius = 14)
                Spacer(Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkText)
                        Spacer(Modifier.width(7.dp))
                        Text(c.rel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = c.color, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(c.color.copy(alpha = 0.12f)).padding(horizontal = 9.dp, vertical = 2.dp))
                    }
                    Text(c.tagline, fontSize = 12.sp, color = MediumText, modifier = Modifier.padding(top = 3.dp))
                }
                Text("›", fontSize = 16.sp, color = RowChevron)
            }
        }
    }
}

// ---- 修正④ 完成态：庆祝 hero + 已完成折叠 + 明天预告 ----

@Composable
private fun CelebrationHero(viewModel: PeopleNetViewModel) {
    val total = viewModel.todayContacts.size
    val tmr = viewModel.tomorrowCountToday
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(24.dp), ambientColor = PurplePrimary.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(HeroGrad))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(com.peoplenet.app.data.DateUtil.homeHeaderShort(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.82f))
            Spacer(Modifier.weight(1f))
            Text("$total / $total ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
        }
        Text("今天都聊完了 🎉", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.padding(top = 10.dp))
        Text(
            "妈妈、老周、林夕都问候过了" + if (tmr > 0) " · 明天有 $tmr 位在等你" else "",
            fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(top = 5.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 13.dp)) {
            repeat(total) { Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(999.dp)).background(Color.White)) }
        }
    }
}

@Composable
private fun CompletedFoldedRow(viewModel: PeopleNetViewModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp).fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)).background(Color.White).border(1.dp, CardBorderLight, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Text("✓ 3", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = ContactedGreen, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(ContactedBg).padding(horizontal = 9.dp, vertical = 3.dp))
        Spacer(Modifier.width(9.dp))
        Text("已完成 · 妈妈 视频 22 分钟 · 老周 约了周六 · 林夕 已回复", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MediumText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(" ⌄", fontSize = 13.sp, color = LightText)
    }
}

@Composable
private fun FestivalCompactRow(viewModel: PeopleNetViewModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp).fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)).background(Color.White).border(1.dp, CardBorderLight, RoundedCornerShape(16.dp))
            .clickable { viewModel.festOpenGiftList() }.padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(FestOrange))
        Spacer(Modifier.width(9.dp))
        Text("中秋还有 7 天 · 礼物清单差 1 件没定", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DarkText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text("去定 ›", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FestOrangeDeep)
    }
}

@Composable
private fun TomorrowPreview(viewModel: PeopleNetViewModel) {
    val week = viewModel.weekContacts.take(2)
    if (week.isEmpty()) return
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 22.dp, top = 12.dp, bottom = 6.dp)) {
            Text("明天", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkText)
            Spacer(Modifier.width(6.dp))
            Text("预告", fontSize = 11.sp, color = LightText)
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().shadow(4.dp, RoundedCornerShape(18.dp), ambientColor = DarkText.copy(alpha = 0.06f)).clip(RoundedCornerShape(18.dp)).background(Color.White)) {
            week.forEachIndexed { i, c ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.then(if (i < week.lastIndex) Modifier.drawBehind { drawLine(Color(0xFFF1EDFB), androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height), 1.dp.toPx()) } else Modifier)
                        .clickable { viewModel.openContact(c.id) }.padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    GradientAvatar(contact = c, size = 36, cornerRadius = 13)
                    Spacer(Modifier.width(11.dp))
                    Text(c.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DarkText)
                    Text("  · ${c.tagline}", fontSize = 13.sp, color = MediumText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("›", fontSize = 15.sp, color = RowChevron)
                }
            }
        }
    }
}

// ---- 语音助手 FAB（常驻）----

@Composable
private fun BoxScope.VoiceFab(viewModel: PeopleNetViewModel) {
    val s = viewModel.state
    if (s.homeActionFor != null || s.voiceStage != 0) return
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 210.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 3.dp))
                .background(Color.White)
                .border(1.dp, CardBorderLight, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 3.dp))
                .padding(horizontal = 11.dp, vertical = 8.dp)
        ) { Text("说一句：「周六提醒我和老周钓鱼」", fontSize = 10.5.sp, color = MediumText) }
        Spacer(Modifier.height(7.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.align(Alignment.End).size(54.dp)
                .shadow(12.dp, CircleShape, ambientColor = PurplePrimary.copy(alpha = 0.4f))
                .clip(CircleShape).background(Brush.linearGradient(listOf(PurplePrimary, PurpleLight)))
                .clickable { viewModel.voiceOpen() }
        ) { Icon(Icons.Rounded.Mic, "语音助手", tint = Color.White, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
fun GradientAvatar(contact: Contact, size: Int, cornerRadius: Int = 15) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = contact.color.copy(alpha = 0.3f),
                spotColor = contact.color.copy(alpha = 0.5f)
            )
            .clip(shape)
            .background(Brush.linearGradient(colors = listOf(contact.color, contact.colorSecondary)))
    ) {
        val avatar = contact.avatar
        if (avatar != null) {
            Image(bitmap = avatar, contentDescription = contact.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(text = contact.av, color = Color.White, fontSize = (size * 0.39).sp, fontWeight = FontWeight.Bold)
        }
    }
}
