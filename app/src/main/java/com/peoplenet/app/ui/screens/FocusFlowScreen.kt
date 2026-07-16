package com.peoplenet.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peoplenet.app.data.Contact
import com.peoplenet.app.data.SampleData
import com.peoplenet.app.ui.theme.*
import com.peoplenet.app.viewmodel.PeopleNetViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

// Result-color semantics (3c): 左滑=已联系(绿) / 右滑=明天再提醒(橙)
private val DoneGrad = listOf(GreenAccent, Color(0xFF43D6AF))
private val TmrGrad = listOf(Color(0xFFF09A4E), Color(0xFFF6B36B))
private val TmrDeep = Color(0xFFE8892F)
private val DotIdle = Color(0xFFE4DDF6)
private val DotTmr = Color(0xFFF0B27C)
private val ScriptBg = Color(0xFFF4F0FF)
private const val QUICK = true   // swipeRightAction default: 直接标记已联系

@Composable
fun FocusFlowScreen(viewModel: PeopleNetViewModel) {
    val pending = viewModel.flowPending()

    Box(modifier = Modifier.fillMaxSize()) {
        if (pending.isEmpty()) {
            CelebrateContent(viewModel)
        } else {
            StackFlow(viewModel, pending)
        }

        viewModel.state.flowRecordFor?.let { id ->
            RecordSheet(viewModel, id)
        }

        FlowToast(viewModel.state.toastText, viewModel.state.toastSeq)
    }
}

@Composable
private fun StackFlow(viewModel: PeopleNetViewModel, pending: List<Contact>) {
    val today = viewModel.todayContacts
    val total = today.size
    val handled = viewModel.handledToday
    val posN = (handled + 1).coerceAtMost(total)
    val cur = pending.first()
    val next = pending.getOrNull(1)

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .shadow(6.dp, CircleShape, ambientColor = PurplePrimary.copy(alpha = 0.18f))
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { viewModel.exitFlow() }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "关闭",
                    tint = MediumText,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "今日联系", fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
                Text(
                    text = "第 $posN 位 · 共 $total 位",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MediumText,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(modifier = Modifier.size(36.dp))
        }

        // Progress dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .padding(top = 12.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            today.forEach { c ->
                val color = when {
                    viewModel.isContacted(c.id) -> GreenAccent
                    viewModel.isTomorrow(c.id) -> DotTmr
                    c.id == cur.id -> PurplePrimary
                    else -> DotIdle
                }
                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = 6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(color)
                )
            }
        }

        // Card stack
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            key(cur.id) {
                val scope = rememberCoroutineScope()
                val density = LocalDensity.current
                val thresholdPx = with(density) { 110.dp.toPx() }
                val maxDragPx = with(density) { 220.dp.toPx() }
                val leavePx = with(density) { 560.dp.toPx() }

                // Live drag offset is plain state (updated synchronously in onDrag);
                // the Animatable drives the settle/fling once the finger lifts.
                var dragX by remember { mutableStateOf(0f) }
                var dragY by remember { mutableStateOf(0f) }
                var settling by remember { mutableStateOf(false) }
                var snapBacking by remember { mutableStateOf(false) }
                val settleX = remember { Animatable(0f) }
                val settleY = remember { Animatable(0f) }
                var settleJob by remember { mutableStateOf<Job?>(null) }

                // Read the offset ONLY inside the draw phase (graphicsLayer) or a
                // derivedStateOf — never at composition scope — so a drag repaints the
                // card without recomposing it. That per-frame recomposition was the
                // first-swipe jank / "卡住" the user hit.
                val offsetXNow = { if (settling) settleX.value else dragX }
                val offsetYNow = { if (settling) settleY.value else dragY }

                // Behind: next card preview
                if (next != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 0.93f; scaleY = 0.93f
                                translationY = with(density) { 22.dp.toPx() }
                                alpha = 0.55f
                            }
                            .clip(RoundedCornerShape(30.dp))
                            .background(Color.White)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Brush.linearGradient(listOf(next.color, next.colorSecondary)))
                                    .graphicsLayer { alpha = 0.5f }
                            ) {
                                Text(text = next.av, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = next.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FadedText)
                        }
                    }
                }

                // Behind: reveal result layer (its own small recomposition scope)
                RevealLayer(offsetXNow = offsetXNow, thresholdPx = thresholdPx)

                // Front: current card
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val x = offsetXNow()
                            translationX = x
                            translationY = offsetYNow()
                            rotationZ = (x / maxDragPx) * 12f
                        }
                        .shadow(20.dp, RoundedCornerShape(30.dp), ambientColor = PurplePrimary.copy(alpha = 0.24f))
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color.White)
                        .pointerInput(cur.id) {
                            val onEnd: () -> Unit = {
                                // A commit fling (settling && !snapBacking) is left alone; only
                                // a snap-back-in-progress can be here with settling still true.
                                val x = dragX
                                when {
                                    x <= -thresholdPx -> {        // left → done
                                        settling = true; snapBacking = false
                                        settleJob = scope.launch {
                                            settleX.snapTo(dragX); settleY.snapTo(dragY)
                                            settleX.animateTo(-leavePx, tween(260))
                                            if (QUICK) {
                                                viewModel.flowMarkDone(cur.id, useRecordInputs = false)
                                            } else {
                                                viewModel.flowOpenRecord(cur.id)
                                                dragX = 0f; dragY = 0f; settling = false
                                            }
                                        }
                                    }
                                    x >= thresholdPx -> {         // right → tomorrow
                                        settling = true; snapBacking = false
                                        settleJob = scope.launch {
                                            settleX.snapTo(dragX); settleY.snapTo(dragY)
                                            settleX.animateTo(leavePx, tween(260))
                                            viewModel.flowMarkTomorrow(cur.id)
                                        }
                                    }
                                    else -> {                     // snap back (interruptible)
                                        settling = true; snapBacking = true
                                        settleJob = scope.launch {
                                            settleX.snapTo(dragX); settleY.snapTo(dragY)
                                            val jx = launch { settleX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                                            val jy = launch { settleY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                                            jx.join(); jy.join()
                                            dragX = 0f; dragY = 0f; settling = false; snapBacking = false
                                        }
                                    }
                                }
                            }
                            detectDragGestures(
                                onDragStart = {
                                    // Grabbing the card mid snap-back hands the position back to
                                    // the finger instead of swallowing the gesture.
                                    if (snapBacking) {
                                        settleJob?.cancel()
                                        dragX = settleX.value; dragY = settleY.value
                                        settling = false; snapBacking = false
                                    }
                                },
                                onDragEnd = onEnd,
                                onDragCancel = onEnd,
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (settling) return@detectDragGestures   // ignore drags on a committing card
                                    dragX = (dragX + dragAmount.x).coerceIn(-maxDragPx, maxDragPx)
                                    dragY = (dragY + dragAmount.y * 0.3f).coerceIn(-40f, 80f)
                                }
                            )
                        }
                        .padding(20.dp)
                ) {
                    CardBody(viewModel, cur, next != null)
                }
            }
        }

        // Gesture hints
        Row(
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            modifier = Modifier
                .padding(top = 4.dp, bottom = 6.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Text(text = "← 左滑 · 标记已联系", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = LightText)
            Text(text = "右滑 · 明天再说 →", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = LightText)
        }
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

/**
 * Under-card result layer for the swipe. The discrete look (which side / past threshold)
 * recomposes only when it actually changes via [derivedStateOf]; the continuous opacity and
 * icon scale are computed inside graphicsLayer (draw phase), so a drag never recomposes this.
 */
@Composable
private fun RevealLayer(offsetXNow: () -> Float, thresholdPx: Float) {
    val mode by remember(thresholdPx) {
        derivedStateOf {
            val x = offsetXNow()
            when {
                x < -6f -> if (-x >= thresholdPx) 2 else 1   // done (left)
                x > 6f -> if (x >= thresholdPx) 4 else 3      // tomorrow (right)
                else -> 0
            }
        }
    }
    if (mode != 0) {
        val done = mode == 1 || mode == 2
        val past = mode == 2 || mode == 4
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(if (done) DoneGrad else TmrGrad))
                .graphicsLayer {
                    val p = (abs(offsetXNow()) / thresholdPx).coerceIn(0f, 1f)
                    alpha = 0.3f + 0.7f * p
                }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(78.dp)
                        .graphicsLayer {
                            val p = (abs(offsetXNow()) / thresholdPx).coerceIn(0f, 1f)
                            val s = if (p >= 1f) 1.14f else (0.82f + 0.3f * p)
                            scaleX = s; scaleY = s
                        }
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.24f))
                ) {
                    if (done) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    } else {
                        Text(
                            text = "明",
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = (if (past) "松手 · " else "") + (if (done) "标记已联系" else "明天再提醒"),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.CardBody(
    viewModel: PeopleNetViewModel,
    cur: Contact,
    hasNext: Boolean
) {
    val clipboard = LocalClipboardManager.current
    val pill = viewModel.pillText(cur)
    val hint = cur.avoid?.topics?.joinToString(" · ").orEmpty()

    // Top pills
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (pill.isNotEmpty()) {
            PillTag(text = pill, bg = OverduePinkBg.takeIf { cur.overdue } ?: PurplePillBg,
                color = if (cur.overdue) PinkAccentLight else PurplePrimary)
        }
        Spacer(modifier = Modifier.weight(1f))
        PillTag(text = cur.rel, bg = cur.color.copy(alpha = 0.12f), color = cur.color)
    }

    // Avatar + name + context + hint
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(84.dp)
                .shadow(12.dp, RoundedCornerShape(26.dp), ambientColor = DarkText.copy(alpha = 0.22f))
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(cur.color, cur.colorSecondary)))
        ) {
            Text(text = cur.av, color = Color.White, fontSize = 33.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = cur.name, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
        Text(
            text = cur.context.ifEmpty { cur.tagline },
            fontSize = 12.5.sp,
            color = MediumText,
            modifier = Modifier.padding(top = 2.dp)
        )
        if (hint.isNotEmpty()) {
            Text(
                text = "雷区 · $hint",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AvoidText,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(AvoidBg)
                    .padding(horizontal = 13.dp, vertical = 5.dp)
            )
        }
    }

    // AI 开场白（1a）
    AiOpener(viewModel, cur, clipboard)

    Spacer(modifier = Modifier.weight(1f))

    // "换一位" swap button — hidden on the last remaining card
    if (hasNext) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(bottom = 10.dp)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White)
                .border(1.5.dp, Color(0xFFE4DDF6), RoundedCornerShape(999.dp))
                .clickable { viewModel.flowRequeue(cur.id) }
                .padding(horizontal = 16.dp, vertical = 7.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SwapHoriz,
                contentDescription = null,
                tint = MediumText,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "这位先跳过 · 换一位", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MediumText)
        }
    }

    // Bottom actions
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF1EDFB))
                .clickable { viewModel.flowMarkTomorrow(cur.id) }
                .padding(vertical = 13.dp)
        ) {
            Text(text = "明天再说", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = MediumText)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1.5f)
                .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = PurplePrimary.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(PurplePrimary, PurpleLight)))
                .clickable { viewModel.flowOpenRecord(cur.id) }
                .padding(vertical = 13.dp)
        ) {
            Text(text = "记一笔 · 已联系", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiOpener(
    viewModel: PeopleNetViewModel,
    cur: Contact,
    clipboard: androidx.compose.ui.platform.ClipboardManager
) {
    val tone = viewModel.state.flowTone
    Column(
        modifier = Modifier
            .padding(top = 13.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFF4F0FF), Color(0xFFEDE7FF))))
            .border(1.dp, PurplePrimary.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
            .padding(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AiBadge()
            Spacer(Modifier.width(6.dp))
            Text(text = "开场白 · 懂他，也懂时机", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MediumText)
            Spacer(Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { viewModel.nextOpener(cur.id) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = "换一个", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = PurplePrimary)
                Spacer(Modifier.width(3.dp))
                Icon(Icons.Rounded.Refresh, null, tint = PurplePrimary, modifier = Modifier.size(13.dp))
            }
        }
        // 语气切换
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 9.dp)) {
            viewModel.toneLabels.forEachIndexed { i, label ->
                ToneChip(label = label, selected = tone == i) { viewModel.setFlowTone(i, cur.id) }
            }
        }
        Text(
            text = "「${viewModel.openerFor(cur)}」",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkText,
            lineHeight = 22.sp,
            modifier = Modifier
                .padding(vertical = 9.dp)
                .heightIn(min = 44.dp)
        )
        // 依据
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "依据", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FadedText, modifier = Modifier.align(Alignment.CenterVertically))
            viewModel.openerBasis(cur).forEach { b -> BasisChip(b) }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(Color.White)
                .border(1.5.dp, Color(0xFFE4DDF6), RoundedCornerShape(13.dp))
                .clickable {
                    clipboard.setText(AnnotatedString(viewModel.openerFor(cur)))
                    viewModel.copyScriptToast()
                }
                .padding(vertical = 10.dp)
        ) {
            Text(text = "复制话术", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = PurplePrimary)
        }
    }
}

@Composable
fun AiBadge() {
    Text(
        text = "✦ AI",
        fontSize = 9.5.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Brush.linearGradient(listOf(PurplePrimary, PurpleLight)))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

@Composable
private fun ToneChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (selected) Color.White else MediumText,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .then(
                if (selected) Modifier.background(PurplePrimary)
                else Modifier.background(Color.White).border(1.5.dp, Color(0xFFE4DDF6), RoundedCornerShape(999.dp))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    )
}

@Composable
private fun BasisChip(basis: com.peoplenet.app.viewmodel.OpenerBasis) {
    Text(
        text = basis.text,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (basis.avoid) PinkAccentLight else MediumText,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE4DDF6), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
private fun PillTag(text: String, bg: Color, color: Color) {
    Text(
        text = text,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.ExtraBold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    )
}

@Composable
private fun CelebrateContent(viewModel: PeopleNetViewModel) {
    val doneN = viewModel.todayContacts.count { viewModel.isContacted(it.id) }
    val tmrN = viewModel.tomorrowCountToday

    val scale = remember { Animatable(0.8f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(92.dp)
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                .shadow(20.dp, RoundedCornerShape(30.dp), ambientColor = PurplePrimary.copy(alpha = 0.4f))
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(listOf(PurplePrimary, PurpleLight)))
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(46.dp)
            )
        }
        Text(
            text = "今天都联系完啦",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DarkText,
            modifier = Modifier.padding(top = 22.dp)
        )
        Text(
            text = "每一句问候，都会记进时间线",
            fontSize = 13.sp,
            color = Color(0xFF8A7FB3),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 20.dp)
        ) {
            StatPill(dot = GreenAccent, label = "已联系 $doneN")
            StatPill(dot = Color(0xFFF09A4E), label = "约了明天 $tmrN")
            StatPill(dot = PurplePrimary, label = "连续 7 天")
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(top = 28.dp)
                .shadow(14.dp, RoundedCornerShape(16.dp), ambientColor = PurplePrimary.copy(alpha = 0.4f))
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(PurplePrimary, PurpleLight)))
                .clickable { viewModel.exitFlow() }
                .padding(horizontal = 46.dp, vertical = 13.dp)
        ) {
            Text(text = "返回首页", fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
    }
}

@Composable
private fun StatPill(dot: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .shadow(8.dp, RoundedCornerShape(999.dp), ambientColor = PurplePrimary.copy(alpha = 0.12f))
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dot))
        Spacer(modifier = Modifier.width(7.dp))
        Text(text = label, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = DarkText)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordSheet(viewModel: PeopleNetViewModel, contactId: String) {
    val contact = viewModel.contacts.find { it.id == contactId } ?: return

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x73241A3D))
                .clickable { viewModel.flowCloseRecord() }
        )
        // Sheet
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(Color(0xFFFDFCFF))
                .clickable(enabled = false) {}
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = "记一笔", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(TagBg)
                        .clickable { viewModel.flowCloseRecord() }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭",
                        tint = MediumText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(modifier = Modifier.padding(top = 12.dp)) {
                Text(text = "和 ", fontSize = 13.5.sp, color = Color(0xFF6B6390))
                Text(text = contact.name, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
                Text(text = " 做了什么？", fontSize = 13.5.sp, color = Color(0xFF6B6390))
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                SampleData.chipOptions.forEach { label ->
                    ChipButton(
                        label = label,
                        active = viewModel.state.selectedChips.contains(label),
                        activeColor = PurplePrimary,
                        onClick = { viewModel.toggleChip(label) }
                    )
                }
            }

            TextField(
                value = viewModel.state.note,
                onValueChange = { viewModel.setNote(it) },
                placeholder = { Text(text = "想记点什么…（可选）", color = LightText, fontSize = 13.5.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF4F1FB),
                    unfocusedContainerColor = Color(0xFFF4F1FB),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = PurplePrimary,
                    focusedTextColor = DarkText,
                    unfocusedTextColor = DarkText
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            Text(
                text = "下次多久联系一次？",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6B6390),
                modifier = Modifier.padding(top = 16.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 10.dp)
            ) {
                SampleData.freqOptions.forEach { label ->
                    ChipButton(
                        label = label,
                        active = viewModel.state.selectedFreq == label,
                        activeColor = PurplePrimary,
                        onClick = { viewModel.pickFreq(label) }
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = PurplePrimary.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(PurplePrimary, PurpleLight)))
                    .clickable { viewModel.flowSaveRecord() }
                    .padding(vertical = 14.dp)
            ) {
                Text(text = "保存这一笔", fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun BoxScope.FlowToast(text: String, seq: Int) {
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
                .padding(bottom = 120.dp)
                .navigationBarsPadding()
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xEB241A3D))
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(text = text, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}
