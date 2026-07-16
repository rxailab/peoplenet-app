package com.peoplenet.app.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.PhotoCamera
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peoplenet.app.data.SampleData
import com.peoplenet.app.ui.theme.*
import com.peoplenet.app.viewmodel.PeopleNetViewModel
import com.peoplenet.app.viewmodel.Screen

/** Decode a picked image Uri into a downsampled ImageBitmap suitable for an avatar. */
private fun decodeAvatar(context: Context, uri: Uri): ImageBitmap? {
    return try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = maxOf(info.size.width, info.size.height)
                decoder.setTargetSampleSize((longest / 256).coerceAtLeast(1))
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }
        bitmap?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AddContactScreen(viewModel: PeopleNetViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var name by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("朋友") }
    var freq by remember { mutableStateOf("每月") }
    var note by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf<ImageBitmap?>(null) }
    var addingGroup by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) avatar = decodeAvatar(context, uri)
    }
    fun launchPicker() =
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    val canSave = name.trim().isNotEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 20.dp, top = 6.dp, bottom = 2.dp)
        ) {
            Text(
                text = "‹",
                fontSize = 26.sp,
                color = PurplePrimary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { viewModel.switchTab(Screen.Contacts) }
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(text = "联系人", fontSize = 12.sp, color = LightText, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
        ) {
            Text(
                text = "添加联系人",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Live avatar preview (tap to pick a photo) + name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = PurplePrimary.copy(alpha = 0.35f))
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(PurplePrimary, PurpleLight)))
                            .clickable { launchPicker() }
                    ) {
                        val pic = avatar
                        val initial = name.trim().take(1)
                        when {
                            pic != null -> Image(
                                bitmap = pic,
                                contentDescription = "头像",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            initial.isNotEmpty() -> Text(
                                text = initial,
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                            else -> Icon(
                                imageVector = Icons.Rounded.AddAPhoto,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    // camera badge to signal it's tappable
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .offset(x = 3.dp, y = 3.dp)
                            .size(24.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { launchPicker() }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = null,
                            tint = PurplePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text(text = "TA 的名字或昵称", color = LightText, fontSize = 15.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = PurplePrimary,
                        focusedTextColor = DarkText,
                        unfocusedTextColor = DarkText
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = PurplePrimary.copy(alpha = 0.15f))
                )
            }

            // Relationship / group  —— 可添加自定义分组；长按空分组删除
            FieldLabel(text = "关系分组（长按可删除自定义分组）")
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.state.groupOrder.forEach { g ->
                    GroupChoiceChip(
                        label = g,
                        active = group == g,
                        onClick = { group = g },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val count = viewModel.contactsInGroup(g)
                            val deleted = viewModel.deleteGroup(g)
                            when {
                                deleted -> {
                                    if (group == g) group = viewModel.state.groupOrder.firstOrNull() ?: ""
                                    toast(context, "已删除分组「$g」")
                                }
                                count > 0 -> toast(context, "「$g」下还有 $count 位联系人，不能删除")
                                else -> toast(context, "至少保留一个分组")
                            }
                        }
                    )
                }
                AddGroupChip(onClick = {
                    addingGroup = true
                    newGroupName = ""
                })
            }
            if (addingGroup) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    TextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        singleLine = true,
                        placeholder = { Text(text = "新分组名称", color = LightText, fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = PurplePrimary,
                            focusedTextColor = DarkText,
                            unfocusedTextColor = DarkText
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .shadow(6.dp, RoundedCornerShape(12.dp), ambientColor = PurplePrimary.copy(alpha = 0.15f))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val canAdd = newGroupName.trim().isNotEmpty()
                    Text(
                        text = "添加",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (canAdd) PurplePrimary else FadedText.copy(alpha = 0.5f))
                            .clickable(enabled = canAdd) {
                                val added = viewModel.addGroup(newGroupName)
                                if (added != null) group = added
                                newGroupName = ""
                                addingGroup = false
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            // Contact frequency
            FieldLabel(text = "期望多久联系一次")
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SampleData.freqOptions.forEach { f ->
                    ChipButton(
                        label = f,
                        active = freq == f,
                        activeColor = PurplePrimary,
                        onClick = { freq = f }
                    )
                }
            }

            // City / location tag
            FieldLabel(text = "所在城市（可选）")
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SampleData.cityOptions.forEach { ct ->
                    ChipButton(
                        label = ct,
                        active = city == ct,
                        activeColor = PurplePrimary,
                        onClick = { city = if (city == ct) "" else ct }
                    )
                }
            }

            // Note / tagline
            FieldLabel(text = "一句话备注（可选）")
            TextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text(text = "比如：大学室友，爱打球", color = LightText, fontSize = 14.sp) },
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
                    .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = PurplePrimary.copy(alpha = 0.15f))
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Save button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (canSave) Modifier.shadow(14.dp, RoundedCornerShape(16.dp), ambientColor = PurplePrimary.copy(alpha = 0.4f))
                        else Modifier
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (canSave) Brush.linearGradient(listOf(PurplePrimary, PurpleLight))
                        else Brush.linearGradient(listOf(FadedText.copy(alpha = 0.5f), FadedText.copy(alpha = 0.5f)))
                    )
                    .clickable(enabled = canSave) {
                        viewModel.addContact(name = name, group = group, freq = freq, note = note, city = city, avatar = avatar)
                    }
                    .padding(vertical = 15.dp)
            ) {
                Text(
                    text = if (canSave) "保存联系人" else "先填个名字吧",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MediumText,
        modifier = Modifier.padding(top = 22.dp, bottom = 10.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupChoiceChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val bg = if (active) PurplePrimary else Color.Transparent
    val fg = if (active) Color.White else Color(0xFF8A8378)
    val borderColor = if (active) PurplePrimary else Color(0xFFE1DCD2)
    Text(
        text = label,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.5.dp, borderColor, RoundedCornerShape(999.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun AddGroupChip(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.5.dp, Color(0xFFD9C9F5), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = "新分组", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PurplePrimary)
    }
}

private fun toast(context: android.content.Context, msg: String) {
    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
}
