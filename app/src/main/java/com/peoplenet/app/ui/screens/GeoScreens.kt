package com.peoplenet.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peoplenet.app.data.Contact
import com.peoplenet.app.data.geoPermissionsGranted
import com.peoplenet.app.ui.theme.*
import com.peoplenet.app.viewmodel.PeopleNetViewModel
import kotlinx.coroutines.delay

private fun pregreetScript(city: String) = "下周去${city}待几天，到时候见一面？"

// ---------- shared toast ----------

@Composable
fun BoxScope.GeoToast(viewModel: PeopleNetViewModel) {
    val text = viewModel.state.toastText
    val seq = viewModel.state.toastSeq
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(seq) {
        if (seq > 0 && text.isNotEmpty()) {
            visible = true
            delay(1900)
            visible = false
        }
    }
    if (visible && text.isNotEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xEB241A3D))
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(text = text, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------- 定位 · 日历授权引导（未授权时才显示） ----------

@Composable
fun GeoPermissionGate(viewModel: PeopleNetViewModel) {
    val context = LocalContext.current

    var hasPerms by remember { mutableStateOf(geoPermissionsGranted(context)) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPerms = geoPermissionsGranted(context)
        viewModel.refreshGeoData(context)
    }
    // 已授权则自动读取真实定位 + 日历，驱动行程预告 / 到达横幅
    LaunchedEffect(hasPerms) {
        if (hasPerms) viewModel.refreshGeoData(context)
    }

    if (!hasPerms) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 18.dp, end = 18.dp, top = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(GeoSoftBlue)
                .border(1.dp, GeoBluePillBg, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(GeoBluePillBg)
            ) {
                Icon(Icons.Rounded.LocationOn, null, tint = GeoBlueDeep, modifier = Modifier.size(19.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "开启定位与日历", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
                Text(
                    text = "出行到朋友的城市时，自动提醒你顺路见一面",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GeoSubText,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "开启",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(999.dp), ambientColor = GeoBlueDeep.copy(alpha = 0.3f))
                    .clip(RoundedCornerShape(999.dp))
                    .background(Brush.linearGradient(listOf(GeoBlueDeep, GeoBlue)))
                    .clickable {
                        permLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.READ_CALENDAR
                            )
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

// ---------- 行前：即将出行卡片 ----------

@Composable
fun TripPreviewCard(viewModel: PeopleNetViewModel) {
    val clipboard = LocalClipboardManager.current
    val geo = viewModel.state.geo
    val tripCity = geo.realTrip?.city ?: viewModel.geoCity
    Column(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = GeoBlueDeep.copy(alpha = 0.18f))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(17.dp)
    ) {
        // header
        Row(verticalAlignment = Alignment.CenterVertically) {
            GeoIconBox(size = 40, corner = 14)
            Spacer(modifier = Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "即将出行 · $tripCity", fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
                Text(text = geo.realTrip?.dateLabel ?: "来自日历", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MediumText, modifier = Modifier.padding(top = 2.dp))
            }
            BluePill(text = "${viewModel.geoTripFriends.size} 位朋友在那儿")
        }
        Spacer(modifier = Modifier.height(13.dp))
        viewModel.geoTripFriends.forEach { gf ->
            val c = viewModel.geoContact(gf.id) ?: return@forEach
            val on = geo.remind[gf.id] != false
            val pre = geo.pregreeted.contains(gf.id)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GeoSoftBlue)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                GradientAvatar(contact = c, size = 34, cornerRadius = 12)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = c.name, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
                    Text(text = c.context.ifEmpty { c.tagline }, fontSize = 10.5.sp, color = MediumText, maxLines = 1, modifier = Modifier.padding(top = 1.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                // pre-greet
                Text(
                    text = if (pre) "已问好" else "提前问好",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (pre) ContactedGreen else PurplePrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (pre) ContactedBg else Color(0xFFF4F0FF))
                        .border(1.5.dp, if (pre) Color(0xFFBFEBDC) else Color(0xFFD9C9F5), RoundedCornerShape(999.dp))
                        .clickable(enabled = !pre) {
                            clipboard.setText(AnnotatedString(pregreetScript(tripCity)))
                            viewModel.geoPregreet(gf.id)
                        }
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                )
                Spacer(modifier = Modifier.width(9.dp))
                // arrival-reminder toggle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MiniToggle(checked = on, onToggle = { viewModel.geoToggleRemind(gf.id) })
                    Text(text = "到达提醒", fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = GeoStatusText, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
        Text(
            text = "开了「到达提醒」的朋友，你到${tripCity}后会收到提示",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = GeoStatusText,
            lineHeight = 16.sp
        )
    }
}

// ---------- 在城：蓝色横幅 ----------

@Composable
fun CityBanner(viewModel: PeopleNetViewModel) {
    val clipboard = LocalClipboardManager.current
    val geo = viewModel.state.geo
    val expanded = geo.cityExpanded
    Column(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(24.dp), ambientColor = GeoBlueDeep.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(GeoBlueDeep, GeoBlue)))
            .clickable { viewModel.geoToggleCityExpand() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(15.dp)).background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(Icons.Rounded.LocationOn, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "你现在在${viewModel.arrivedCityName}", fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(
                    text = "${viewModel.geoArrivedFriends.size} 位朋友在这座城市 · 点开看看",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            // avatar cluster
            Row {
                viewModel.geoArrivedFriends.forEachIndexed { i, gf ->
                    val c = viewModel.geoContact(gf.id) ?: return@forEachIndexed
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .offset(x = if (i == 0) 0.dp else (-8).dp)
                            .size(30.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color.White.copy(alpha = 0.85f))
                            .padding(2.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Brush.linearGradient(listOf(c.color, c.colorSecondary)))
                    ) {
                        Text(text = c.av, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            // dismiss ✕
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable { viewModel.geoDismissCity() }
            ) {
                Icon(Icons.Rounded.Close, "关闭", tint = Color.White, modifier = Modifier.size(13.dp))
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 13.dp)) {
                viewModel.geoArrivedFriends.forEach { gf ->
                    val c = viewModel.geoContact(gf.id) ?: return@forEach
                    val kind = geo.acted[gf.id]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.94f))
                            // consume taps so tapping a row doesn't collapse the banner
                            .pointerInput(Unit) { detectTapGestures {} }
                            .padding(horizontal = 12.dp, vertical = 11.dp)
                    ) {
                        GradientAvatar(contact = c, size = 36, cornerRadius = 13)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = c.name, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "离你 ${gf.dist}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GeoBlueDeep,
                                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(GeoBluePillBg).padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Text(text = c.context.ifEmpty { c.tagline }, fontSize = 11.sp, color = MediumText, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
                        }
                        if (kind != null) {
                            ActedPill(kind = kind)
                        } else {
                            Text(
                                text = "打个招呼",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier
                                    .shadow(6.dp, RoundedCornerShape(999.dp), ambientColor = PurplePrimary.copy(alpha = 0.3f))
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(PurplePrimary)
                                    .clickable {
                                        clipboard.setText(AnnotatedString(viewModel.geoScript(gf.id)))
                                        viewModel.geoAct(gf.id, "greeted")
                                    }
                                    .padding(horizontal = 13.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------- 到达推送（顶部滑入通知）----------

@Composable
fun BoxScope.ArrivalPush(viewModel: PeopleNetViewModel) {
    val show = viewModel.state.geo.notifShown
    val names = viewModel.geoRemindNames().ifEmpty { "有朋友" }
    AnimatedVisibility(
        visible = show,
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
                .clickable { viewModel.geoOpenArrival() }
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
                Text(
                    text = "你到${viewModel.arrivedCityName}了 — ${names}都在这儿，顺路见一面？",
                    fontSize = 12.sp,
                    color = Color(0xFF4E4569),
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(text = "点开看看 →", fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = PurplePrimary, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

// ---------- 城市落地页「你到了 <城市>」----------

@Composable
fun CityArrivalScreen(viewModel: PeopleNetViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GeoLandingTop, GeoLandingBottom)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(6.dp, CircleShape, ambientColor = GeoBlueDeep.copy(alpha = 0.18f))
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { viewModel.handleBack() }
                ) {
                    Icon(Icons.Rounded.Close, "关闭", tint = GeoSubText, modifier = Modifier.size(18.dp))
                }
            }
            Column(modifier = Modifier.padding(start = 26.dp, end = 26.dp, top = 10.dp, bottom = 8.dp)) {
                Text(text = "行程提醒", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = GeoBlueDeep, letterSpacing = 1.5.sp)
                Text(text = "你到了${viewModel.arrivedCityName}", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = DarkText, modifier = Modifier.padding(top = 5.dp))
                Text(
                    text = "${viewModel.geoAllNames()}都在这座城市 · 顺路的话，见一面？",
                    fontSize = 13.sp,
                    color = GeoSubText,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
            Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                viewModel.geoArrivedFriends.forEach { gf ->
                    val c = viewModel.geoContact(gf.id) ?: return@forEach
                    ArriveFriendCard(viewModel, gf, c)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            Text(
                text = "本次到访不再提醒",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GeoStatusText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.geoMute() }
                    .padding(top = 6.dp, bottom = 44.dp)
            )
        }
        GeoToast(viewModel)
    }
}

@Composable
private fun ArriveFriendCard(viewModel: PeopleNetViewModel, gf: com.peoplenet.app.viewmodel.GeoFriend, c: Contact) {
    val clipboard = LocalClipboardManager.current
    val kind = viewModel.state.geo.acted[gf.id]
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (kind == "skip") 0.55f else 1f }
            .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = GeoBlueDeep.copy(alpha = 0.14f))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradientAvatar(contact = c, size = 46, cornerRadius = 16)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = c.name, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = c.rel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = c.color,
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(c.color.copy(alpha = 0.12f)).padding(horizontal = 9.dp, vertical = 2.dp)
                    )
                }
                Text(text = c.context.ifEmpty { c.tagline }, fontSize = 11.5.sp, color = MediumText, maxLines = 1, modifier = Modifier.padding(top = 3.dp))
            }
            if (kind == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(GeoBluePillBg).padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Icon(Icons.Rounded.LocationOn, null, tint = GeoBlueDeep, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = gf.dist, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = GeoBlueDeep)
                }
            }
        }

        if (kind == null) {
            // script
            Column(
                modifier = Modifier
                    .padding(top = 11.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GeoSoftBlue2)
                    .padding(horizontal = 13.dp, vertical = 11.dp)
            ) {
                Text(text = "顺手发一句", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = GeoSubText)
                Text(text = "「${gf.cityScript}」", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = DarkText, lineHeight = 20.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(modifier = Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "先不",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GeoStatusText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, Color(0xFFE1E6F0), RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .clickable { viewModel.geoAct(gf.id, "skip") }
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, Color(0xFFD9C9F5), RoundedCornerShape(14.dp))
                        .background(Color(0xFFF4F0FF))
                        .clickable {
                            clipboard.setText(AnnotatedString(gf.cityScript))
                            viewModel.geoAct(gf.id, "greeted")
                        }
                        .padding(vertical = 11.dp)
                ) {
                    Text(text = "发消息问好", fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = PurplePrimary)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = PurplePrimary.copy(alpha = 0.3f))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(PurplePrimary, PurpleLight)))
                        .clickable { viewModel.geoAct(gf.id, "met") }
                        .padding(vertical = 11.dp)
                ) {
                    Text(text = "约个见面", fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        } else {
            Box(modifier = Modifier.padding(top = 11.dp)) {
                ActedPill(kind = kind)
            }
        }
    }
}

// ---------- small shared pieces ----------

@Composable
private fun GeoIconBox(size: Int, corner: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .shadow(8.dp, RoundedCornerShape(corner.dp), ambientColor = GeoBlueDeep.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(corner.dp))
            .background(Brush.linearGradient(listOf(GeoBlueDeep, GeoBlue)))
    ) {
        Icon(Icons.Rounded.LocationOn, null, tint = Color.White, modifier = Modifier.size((size * 0.45).dp))
    }
}

@Composable
private fun BluePill(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        color = GeoBlueDeep,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(GeoBluePillBg).padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun ActedPill(kind: String) {
    val (label, bg, fg) = when (kind) {
        "greeted" -> Triple("已问好", ContactedBg, ContactedGreen)
        "met" -> Triple("已约见面", PurplePillBg, PurplePrimary)
        else -> Triple("这次先不", Color(0xFFF1F3F8), GeoStatusText)
    }
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = fg,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 13.dp, vertical = 6.dp)
    )
}

@Composable
private fun MiniToggle(checked: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 38.dp, height = 22.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (checked) GeoBlueDeep else Color(0xFFE1E6F0))
            .clickable(onClick = onToggle)
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .size(18.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
