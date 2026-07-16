package com.peoplenet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peoplenet.app.ui.screens.*
import com.peoplenet.app.ui.theme.*
import com.peoplenet.app.viewmodel.PeopleNetViewModel
import com.peoplenet.app.viewmodel.Screen
import com.peoplenet.app.wxapi.WxAuthRelay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PeopleNetTheme {
                PeopleNetApp()
            }
        }
    }
}

@Composable
fun PeopleNetApp(viewModel: PeopleNetViewModel = viewModel()) {
    // 微信授权回调 → ViewModel（登录态期间也需生效，故放在门禁之前）
    LaunchedEffect(viewModel) {
        WxAuthRelay.onResult = { errCode, code -> viewModel.onWechatResult(errCode, code) }
    }

    // 退后台 → 停止语音识别并收起听写面板（后台不占麦克风）
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.onAppBackground()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 未登录（且非游客）→ 登录账号系统流程，替换整个 App
    if (!viewModel.inApp) {
        AuthFlow(viewModel)
        return
    }

    val showBottomBar = viewModel.state.screen in listOf(Screen.Home, Screen.Contacts, Screen.Me)

    // Route the hardware / gesture back button through in-app navigation for sub-screens;
    // on the top-level tabs let the system handle it (background the app).
    BackHandler(enabled = !viewModel.isTopLevel) { viewModel.handleBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BgGradientStart, BgGradientEnd)
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomBar) {
                    BottomNavBar(viewModel)
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = viewModel.state.screen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith
                            fadeOut(animationSpec = tween(200))
                    },
                    label = "screen"
                ) { screen ->
                    when (screen) {
                        Screen.Home -> HomeScreen(viewModel)
                        Screen.Contacts -> ContactsListScreen(viewModel)
                        Screen.People -> PeopleScreen(viewModel)
                        Screen.Me -> MeScreen(viewModel)
                        Screen.Detail -> ContactDetailScreen(viewModel)
                        Screen.Record -> RecordScreen(viewModel)
                        Screen.Done -> DoneScreen(viewModel)
                        Screen.Flow -> FocusFlowScreen(viewModel)
                        Screen.About -> AboutScreen(viewModel)
                        Screen.Notifications -> NotificationsScreen(viewModel)
                        Screen.Groups -> GroupsScreen(viewModel)
                        Screen.ImportContacts -> ImportContactsScreen(viewModel)
                        Screen.AddContact -> AddContactScreen(viewModel)
                        Screen.CityArrival -> CityArrivalScreen(viewModel)
                        Screen.Bless -> BlessListScreen(viewModel)
                        Screen.GiftList -> GiftListScreen(viewModel)
                        Screen.AccountSecurity -> AccountSecurityScreen(viewModel)
                        Screen.DeleteAccount -> DeleteAccountScreen(viewModel)
                        Screen.Grade -> GradeAssistantScreen(viewModel)
                    }
                }
            }
        }

        // 退出登录确认弹层（覆盖底部导航）
        LogoutSheet(viewModel)
    }
}

@Composable
private fun BottomNavBar(viewModel: PeopleNetViewModel) {
    val currentScreen = viewModel.state.screen

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.74f))
            .padding(horizontal = 22.dp, vertical = 11.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(
            label = "今天",
            isActive = currentScreen == Screen.Home,
            onClick = { viewModel.switchTab(Screen.Home) }
        )
        NavItem(
            label = "联系人",
            isActive = currentScreen == Screen.Contacts,
            onClick = { viewModel.switchTab(Screen.Contacts) }
        )
        NavItem(
            label = "我",
            isActive = currentScreen == Screen.Me,
            onClick = { viewModel.switchTab(Screen.Me) }
        )
    }
}

@Composable
private fun NavItem(label: String, isActive: Boolean, onClick: () -> Unit, badge: Int = 0) {
    Box(contentAlignment = Alignment.TopEnd) {
        if (isActive) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(999.dp), ambientColor = PurplePrimary.copy(alpha = 0.3f))
                    .clip(RoundedCornerShape(999.dp))
                    .background(PurplePrimary)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = label,
                color = NavInactiveText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(onClick = onClick)
                    .padding(horizontal = 8.dp, vertical = 7.dp)
            )
        }
        if (badge > 0) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = 3.dp, y = (-3).dp)
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(PinkAccent)
            ) {
                Text(
                    text = badge.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
