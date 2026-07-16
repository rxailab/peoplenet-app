package com.peoplenet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peoplenet.app.ui.theme.*
import com.peoplenet.app.viewmodel.PeopleNetViewModel
import com.peoplenet.app.viewmodel.Screen

@Composable
fun MeScreen(viewModel: PeopleNetViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)
    ) {
        // Profile header（登录态感知）
        item {
            val auth = viewModel.state.auth
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(60.dp)
                        .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = PurplePrimary.copy(alpha = 0.4f))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(colors = listOf(PurplePrimary, PurpleLight)))
                ) {
                    Text(
                        text = if (auth.loggedIn) auth.avatarChar else "客",
                        color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (auth.loggedIn) auth.nickname else "游客",
                            fontWeight = FontWeight.Bold, fontSize = 19.sp, color = DarkText
                        )
                        if (auth.loggedIn && auth.wechatBound) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "已绑定微信", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = PurplePrimary,
                                modifier = Modifier.background(TagBg, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = if (auth.loggedIn) "${auth.phoneMasked} · 正在用心经营 ${viewModel.contacts.size} 段关系"
                        else "登录后同步你的关系网",
                        fontSize = 12.5.sp, color = MediumText, modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (!auth.loggedIn) {
                    Text(
                        text = "登录/注册", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp)).background(PurplePrimary)
                            .clickable { viewModel.authExitGuest() }
                            .padding(horizontal = 13.dp, vertical = 7.dp)
                    )
                } else {
                    Text(text = "›", fontSize = 16.sp, color = NavInactiveText, modifier = Modifier.clickable { viewModel.openAccountSecurity() })
                }
            }
        }

        // Stats cards
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCard(
                    value = "${viewModel.doneCount}",
                    label = "本周已联系",
                    valueColor = PurplePrimary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = "${viewModel.remainingToday}",
                    label = "今天待联系",
                    valueColor = PinkAccent,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = "${viewModel.contacts.size}",
                    label = "维护中",
                    valueColor = GreenAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Settings menu
        item {
            Spacer(modifier = Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = PurplePrimary.copy(alpha = 0.15f),
                        spotColor = PurplePrimary.copy(alpha = 0.25f)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
            ) {
                val notif = viewModel.state.notif
                val importedSuffix = if (viewModel.state.importedCount > 0) "已导入 ${viewModel.state.importedCount} · " else ""
                Column {
                    if (viewModel.state.auth.loggedIn) {
                        SettingsItem(
                            label = "账号与安全",
                            detail = "手机号 · 微信 ›",
                            onClick = { viewModel.openAccountSecurity() }
                        )
                        SettingsDivider()
                    }
                    SettingsItem(
                        label = "提醒时间",
                        detail = "每天 ${notif.reminderTime} ›",
                        onClick = { viewModel.openSettings(Screen.Notifications) }
                    )
                    SettingsDivider()
                    SettingsItem(
                        label = "从通讯录导入",
                        detail = "$importedSuffix›",
                        onClick = { viewModel.openSettings(Screen.ImportContacts) }
                    )
                    SettingsDivider()
                    SettingsItem(
                        label = "关系分组管理",
                        detail = "${viewModel.groups().size} 个圈子 ›",
                        onClick = { viewModel.openSettings(Screen.Groups) }
                    )
                    SettingsDivider()
                    SettingsItem(
                        label = "通知与免打扰",
                        detail = if (notif.dndEnabled) "免打扰开 ›" else "›",
                        onClick = { viewModel.openSettings(Screen.Notifications) }
                    )
                    SettingsDivider()
                    SettingsItem(
                        label = "关于人际关系网",
                        detail = "›",
                        onClick = { viewModel.openSettings(Screen.About) }
                    )
                }
            }
        }

        // 退出登录（已登录才显示）
        if (viewModel.state.auth.loggedIn) {
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = PurplePrimary.copy(alpha = 0.12f))
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .clickable { viewModel.authRequestLogout() }
                        .padding(vertical = 14.dp)
                ) {
                    Text(text = "退出登录", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFF0567E))
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = PurplePrimary.copy(alpha = 0.15f),
                spotColor = PurplePrimary.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(vertical = 13.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                lineHeight = 24.sp
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MediumText,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun SettingsItem(label: String, detail: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF2E2545),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = detail,
            fontSize = 12.5.sp,
            color = NavInactiveText
        )
    }
}

@Composable
private fun SettingsDivider() {
    Divider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = SectionBorder
    )
}
