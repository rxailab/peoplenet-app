package com.peoplenet.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peoplenet.app.ui.theme.*
import com.peoplenet.app.viewmodel.PeopleNetViewModel

@Composable
fun DoneScreen(viewModel: PeopleNetViewModel) {
    val contact = viewModel.selectedContact ?: return
    val nextLabel = viewModel.state.selectedFreq ?: "每月"

    // Pop animation
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Checkmark icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .scale(scale.value)
                .size(88.dp)
                .shadow(18.dp, RoundedCornerShape(28.dp), ambientColor = PurplePrimary.copy(alpha = 0.4f))
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(PurplePrimary, PurpleLight)
                    )
                )
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(46.dp)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "搞定啦",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = DarkText
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = buildAnnotatedString {
                append("下次会在 ")
                withStyle(SpanStyle(color = PurplePrimary, fontWeight = FontWeight.Bold)) {
                    append(nextLabel)
                }
                append(" 提醒你\n找 ${contact.name} 聊聊")
            },
            fontSize = 13.5.sp,
            color = Color(0xFF8A7FB3),
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 220.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Review timeline link
        Text(
            text = "看看和 ${contact.name} 的时间线 ›",
            color = PurplePrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
            modifier = Modifier.clickable { viewModel.review() }
        )

        Spacer(modifier = Modifier.height(11.dp))

        // Return home button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = PurplePrimary.copy(alpha = 0.2f))
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .clickable { viewModel.toHome() }
                .padding(horizontal = 30.dp, vertical = 12.dp)
        ) {
            Text(
                text = "返回首页",
                color = PurplePrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
