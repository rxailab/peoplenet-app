package com.peoplenet.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peoplenet.app.data.Contact
import com.peoplenet.app.ui.theme.*
import com.peoplenet.app.viewmodel.Closeness
import com.peoplenet.app.viewmodel.PeopleNetViewModel
import com.peoplenet.app.viewmodel.SortMode
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsListScreen(viewModel: PeopleNetViewModel) {
    val searchQuery = viewModel.state.searchQuery
    val sortMode = viewModel.state.sortMode
    val isAlphabetical = sortMode == SortMode.Alphabetical
    val isCloseness = sortMode == SortMode.Closeness
    // Grouping/sorting reruns only when its inputs change, not on every recomposition
    val groupOrder = viewModel.state.groupOrder
    val groups = remember(searchQuery, sortMode, groupOrder) { viewModel.groupedContacts() }
    // The whole list is small enough to compose once, so it scrolls as a plain
    // Column: a letter jump only changes the scroll offset — nothing recomposes
    // or remeasures. Revisit if the contact count ever grows into the hundreds
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Group-header Y positions inside the scrolling column, captured at layout time
    val headerOffsets = remember(groups) { mutableMapOf<String, Float>() }

    // Passed down as State objects and read only inside AlphabetSidebar /
    // FloatingLetterIndicator, so a drag recomposes those two instead of the whole screen
    val activeLetterState = remember { mutableStateOf<String?>(null) }
    val draggingState = remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "联系人",
                        fontWeight = FontWeight.Bold,
                        fontSize = 23.sp,
                        color = DarkText,
                        letterSpacing = 0.01.sp
                    )
                    Text(
                        text = "共 ${viewModel.contacts.size} 位 · 用心维护中",
                        fontSize = 12.sp,
                        color = MediumText,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(8.dp, RoundedCornerShape(12.dp), ambientColor = PurplePrimary.copy(alpha = 0.3f))
                        .clip(RoundedCornerShape(12.dp))
                        .background(PurplePrimary)
                        .clickable { viewModel.openAddContact() }
                ) {
                    Text(
                        text = "+",
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 23.sp
                    )
                }
            }

            // Search bar
            TextField(
                value = viewModel.state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(text = "搜索名字、标签…", color = NavInactiveText, fontSize = 13.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = NavInactiveText,
                        modifier = Modifier.size(17.dp)
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = PurplePrimary,
                    focusedTextColor = DarkText,
                    unfocusedTextColor = DarkText
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = PurplePrimary.copy(alpha = 0.15f))
            )

            // Sort toggle row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sortLabels = listOf(
                    "分组" to SortMode.Group,
                    "亲密度" to SortMode.Closeness,
                    "A-Z" to SortMode.Alphabetical
                )
                sortLabels.forEach { (label, mode) ->
                    val selected = viewModel.state.sortMode == mode
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) PurplePrimary else NavInactiveText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) PurplePillBg else Color.Transparent)
                            .clickable { if (!selected) viewModel.setSortMode(mode) }
                            .padding(horizontal = 13.dp, vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }

            // Contact list
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = if (isAlphabetical) 24.dp else 0.dp)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    if (groups.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "没有找到联系人", fontSize = 14.sp, color = LightText)
                        }
                    }

                    // 亲密度视图：顶部整理横幅 + 排序说明
                    if (isCloseness && searchQuery.isBlank()) {
                        val unset = viewModel.closenessUnsetCount
                        if (unset > 0) {
                            ClosenessBanner(
                                unsetCount = unset,
                                onGrade = { viewModel.openGrade() }
                            )
                        }
                        Text(
                            text = "按亲密度从高到低 · 同档按最近联系",
                            fontSize = 11.sp,
                            color = FadedText,
                            modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 2.dp)
                        )
                    }

                    groups.forEachIndexed { index, (groupName, contacts) ->
                        // Collapse + reorder apply only to the 分组 view, not A-Z / 亲密度
                        val collapsible = !isAlphabetical && !isCloseness
                        val collapsed = collapsible && viewModel.isGroupCollapsed(groupName)

                        // 亲密度视图是单一列表（groupName 为空），不渲染分组头
                        if (!isCloseness) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned {
                                    headerOffsets[groupName] = it.positionInParent().y
                                }
                                .padding(top = 12.dp, bottom = 10.dp)
                        ) {
                            // Left cluster: tap to collapse/expand; long-press a collapsed
                            // group to reveal its move arrows (分组 only)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(
                                        if (collapsible)
                                            Modifier.combinedClickable(
                                                onClick = { viewModel.toggleGroupCollapsed(groupName) },
                                                onLongClick = {
                                                    if (viewModel.isGroupCollapsed(groupName)) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        viewModel.toggleGroupReorder(groupName)
                                                    }
                                                }
                                            )
                                        else Modifier
                                    )
                                    .padding(start = 6.dp, top = 3.dp, bottom = 3.dp, end = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(PurplePrimary)
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = groupName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText,
                                    letterSpacing = 0.03.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${contacts.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FadedText
                                )
                                if (collapsible) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = if (collapsed) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                                        contentDescription = if (collapsed) "展开" else "折叠",
                                        tint = FadedText,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            // Arrows only appear once a collapsed group is long-pressed
                            val showArrows = collapsed &&
                                viewModel.state.reorderGroup == groupName &&
                                groups.size > 1
                            if (showArrows) {
                                Spacer(modifier = Modifier.weight(1f))
                                MoveButton(
                                    icon = Icons.Rounded.ArrowUpward,
                                    enabled = index > 0,
                                    onClick = { viewModel.moveGroup(groupName, up = true) }
                                )
                                MoveButton(
                                    icon = Icons.Rounded.ArrowDownward,
                                    enabled = index < groups.lastIndex,
                                    onClick = { viewModel.moveGroup(groupName, up = false) }
                                )
                            }
                        }
                        } // end if (!isCloseness) header

                        androidx.compose.animation.AnimatedVisibility(visible = !collapsed) {
                            Column {
                                contacts.forEach { contact ->
                                    ContactListItem(
                                        contact = contact,
                                        showCloseness = isCloseness,
                                        onClick = { viewModel.openContact(contact.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // A-Z Sidebar
                if (isAlphabetical) {
                    val letters = remember(searchQuery) { viewModel.availableLetters() }
                    AlphabetSidebar(
                        letters = letters,
                        activeLetterState = activeLetterState,
                        draggingState = draggingState,
                        onLetterHit = { letter ->
                            headerOffsets[letter]?.let { y ->
                                coroutineScope.launch {
                                    // A short animated scroll spreads the work across frames;
                                    // an instant jump costs a whole viewport in a single frame
                                    scrollState.animateScrollTo(y.roundToInt(), tween(120))
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }

                // Floating letter indicator
                FloatingLetterIndicator(activeLetterState = activeLetterState)
            }
        }
    }
}

@Composable
private fun MoveButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) PurplePrimary else FadedText.copy(alpha = 0.35f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun FloatingLetterIndicator(activeLetterState: State<String?>) {
    val activeLetter = activeLetterState.value
    val visible = activeLetter != null

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "indicator_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (visible) 100 else 250),
        label = "indicator_alpha"
    )

    // Keeps showing the last letter while the box fades out after release
    var lastLetter by remember { mutableStateOf("") }
    SideEffect { if (activeLetter != null) lastLetter = activeLetter }

    val bubbleBrush = remember { Brush.linearGradient(colors = listOf(PurplePrimary, PurpleLight)) }

    // Always composed — hidden is just alpha 0 on the layer (animated values are
    // read only inside graphicsLayer), so pressing the index bar never pays a
    // mount/dispose spike, and an idle screen still renders nothing
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .size(68.dp)
                .shadow(16.dp, RoundedCornerShape(20.dp), ambientColor = PurplePrimary.copy(alpha = 0.5f))
                .clip(RoundedCornerShape(20.dp))
                .background(bubbleBrush)
        ) {
            Text(
                text = activeLetter ?: lastLetter,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AlphabetSidebar(
    letters: List<String>,
    activeLetterState: MutableState<String?>,
    draggingState: MutableState<Boolean>,
    onLetterHit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allLetters = remember { ('A'..'Z').map { it.toString() } }
    val lettersSet = remember(letters) { letters.toSet() }
    // The pointer coroutine outlives recompositions; this keeps the latest callback visible to it
    val currentOnLetterHit by rememberUpdatedState(onLetterHit)
    var columnHeight by remember { mutableStateOf(0f) }

    val isDragging = draggingState.value
    val activeLetter = activeLetterState.value

    // Only two animated values for the whole sidebar, both read at draw/layer
    // time only, so the animation frames never recompose anything
    val sidebarBgAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.6f else 0f,
        animationSpec = tween(150),
        label = "sidebar_bg"
    )
    val sidebarScale by animateFloatAsState(
        targetValue = if (isDragging) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "sidebar_scale"
    )

    fun hitLetterAt(y: Float) {
        if (columnHeight <= 0f) return
        val index = (y / columnHeight * allLetters.size)
            .toInt()
            .coerceIn(0, allLetters.size - 1)
        val letter = allLetters[index]
        if (letter !in lettersSet || letter == activeLetterState.value) return
        activeLetterState.value = letter
        currentOnLetterHit(letter)
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = sidebarScale
                scaleY = sidebarScale
            }
            .padding(end = 2.dp, top = 4.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .drawBehind { drawRect(PurplePillBg.copy(alpha = sidebarBgAlpha)) }
            .onGloballyPositioned { columnHeight = it.size.height.toFloat() }
            // Raw pointer input: zero touch-slop, fires immediately on contact
            .pointerInput(lettersSet) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        down.consume()
                        draggingState.value = true
                        hitLetterAt(down.position.y)

                        // Track drag until release
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (change.changedToUp()) {
                                change.consume()
                                break
                            }
                            change.consume()
                            hitLetterAt(change.position.y)
                        }

                        draggingState.value = false
                        activeLetterState.value = null
                    }
                }
            }
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        allLetters.forEach { letter ->
            val hasContacts = letter in lettersSet
            val isActive = letter == activeLetter
            // Fixed-size cell: highlighting scales the layer, never relayouts the
            // column, so letters stay put under the finger and hit bands stay uniform
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 16.dp, height = 14.dp)
                    .graphicsLayer {
                        val cellScale = if (isActive) 1.3f else 1f
                        scaleX = cellScale
                        scaleY = cellScale
                    }
                    .clip(CircleShape)
                    .background(if (isActive) PurplePrimary else Color.Transparent)
            ) {
                Text(
                    text = letter,
                    fontSize = 9.sp,
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = when {
                        isActive -> Color.White
                        hasContacts -> PurplePrimary
                        else -> NavInactiveText.copy(alpha = 0.45f)
                    }
                )
            }
        }
    }
}

@Composable
private fun ClosenessBanner(unsetCount: Int, onGrade: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = PurplePrimary.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(PurplePrimary, PurpleLight)))
            .clickable(onClick = onGrade)
            .padding(horizontal = 15.dp, vertical = 13.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "还有 $unsetCount 位联系人没定亲密度",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "助手按联系频率给出建议 · 一分钟整理完",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "去整理 →",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PurplePrimary,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White)
                .padding(horizontal = 13.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun ContactListItem(
    contact: Contact,
    onClick: () -> Unit,
    showCloseness: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.5.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(17.dp),
                ambientColor = contact.color.copy(alpha = 0.15f),
                spotColor = contact.color.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(17.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            GradientAvatar(contact = contact, size = 42)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = DarkText
                )
                Text(
                    text = contact.tagline,
                    fontSize = 11.5.sp,
                    color = MediumText,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            if (showCloseness) {
                // 亲密度视图：右侧展示心值
                val level = contact.closeness
                when {
                    level == null -> Text(
                        text = "未定",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FadedText
                    )
                    level == 0 -> Text(
                        text = Closeness.names[0],
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FadedText
                    )
                    else -> Text(
                        text = Closeness.hearts(level),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6F91)
                    )
                }
            } else {
                val lastText = contact.history.firstOrNull()?.first ?: ""
                if (lastText.isNotEmpty()) {
                    Text(
                        text = lastText,
                        fontSize = 11.sp,
                        color = FadedText
                    )
                }
            }
        }
    }
}
