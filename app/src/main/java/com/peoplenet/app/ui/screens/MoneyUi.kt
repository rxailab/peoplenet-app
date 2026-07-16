package com.peoplenet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peoplenet.app.data.GiftReturn
import com.peoplenet.app.data.LoanStatus
import com.peoplenet.app.data.MoneyRecord
import com.peoplenet.app.data.MoneyType

// ---- 金钱往来配色（2a 融合方案）----

/** 类型胶囊配色：(背景, 文字)。借出=紫 / 借入=蓝 / 送礼=橙 / 收礼=绿。 */
fun moneyTypeColors(type: MoneyType): Pair<Color, Color> = when (type) {
    MoneyType.Lend -> Color(0xFFEEEAFE) to Color(0xFF6C5CE7)
    MoneyType.Borrow -> Color(0xFFE3F0FC) to Color(0xFF2E77C5)
    MoneyType.GiveGift -> Color(0xFFFFE9CE) to Color(0xFFD96F1B)
    MoneyType.ReceiveGift -> Color(0xFFE1F8F0) to Color(0xFF0FA47C)
}

/** 还款状态配色：未还=粉 / 部分还=橙 / 已还=绿。 */
fun loanStatusColors(status: LoanStatus): Pair<Color, Color> = when (status) {
    LoanStatus.Unpaid -> Color(0xFFFFE1E9) to Color(0xFFF0567E)
    LoanStatus.Partial -> Color(0xFFFFE9CE) to Color(0xFFD96F1B)
    LoanStatus.Paid -> Color(0xFFE1F8F0) to Color(0xFF0FA47C)
}

/** 回礼状态配色：待回礼=橙 / 已回礼=绿。 */
fun giftReturnColors(status: GiftReturn): Pair<Color, Color> = when (status) {
    GiftReturn.Pending -> Color(0xFFFFE9CE) to Color(0xFFD96F1B)
    GiftReturn.Returned -> Color(0xFFE1F8F0) to Color(0xFF0FA47C)
}

/** ¥2,000 千分位。 */
fun formatYuan(n: Int): String = "¥" + "%,d".format(n)

/** 一笔记录的主标题：金额型「¥2,000」；实物型「茅台一瓶 估值 ¥1,500」（估值部分由调用方另渲染更淡）。 */
fun moneyMainAmount(record: MoneyRecord): String =
    if (record.isPhysical) record.itemName else formatYuan(record.amount)

@Composable
fun SmallPill(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.ExtraBold,
        color = fg,
        modifier = modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    )
}

@Composable
fun TypePill(type: MoneyType, modifier: Modifier = Modifier) {
    val (bg, fg) = moneyTypeColors(type)
    Text(
        text = type.label,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = fg,
        modifier = modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
