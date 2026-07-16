package com.peoplenet.app.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

data class JobInfo(
    val title: String,
    val org: String,
    val industry: String
)

data class AvoidInfo(
    val diet: List<String> = emptyList(),
    val topics: List<String> = emptyList()
)

/** 金钱往来 / 人情往来（2a 融合方案）四类。 */
enum class MoneyType(val label: String, val hint: String) {
    Lend("借出", "我借钱给他"),
    Borrow("借入", "他借钱给我"),
    GiveGift("送礼", "我送出礼金或礼物"),
    ReceiveGift("收礼", "我收到礼金或礼物");

    val isLoan: Boolean get() = this == Lend || this == Borrow
    val isGift: Boolean get() = this == GiveGift || this == ReceiveGift
}

/** 借出 / 借入 的还款状态。 */
enum class LoanStatus(val label: String) { Unpaid("未还"), Partial("部分还"), Paid("已还") }

/** 收礼 / 送礼 的回礼状态（送礼一般为 null）。 */
enum class GiftReturn(val label: String) { Pending("待回礼"), Returned("已回礼") }

/**
 * 一笔金钱往来记录。金额型记 [amount]；实物型（[isPhysical]）记 [itemName] + [estValue]。
 * 借出/借入用 [loanStatus] + [reminderDate]；收礼/送礼用 [event] 归入礼簿、[giftReturn] 标回礼。
 * 不计人情净值——只记心意。
 */
data class MoneyRecord(
    val id: String,
    val contactId: String,
    val type: MoneyType,
    val isPhysical: Boolean = false,
    val amount: Int = 0,
    val itemName: String = "",
    val estValue: Int = 0,
    val date: String,
    val note: String = "",
    val event: String = "",
    val reminderDate: String = "",
    val loanStatus: LoanStatus? = null,
    val giftReturn: GiftReturn? = null
)

data class Contact(
    val id: String,
    val name: String,
    val pinyin: String,      // pinyin initial letter for A-Z index
    val rel: String,
    val group: String,
    val home: String?,       // "today", "week", or null
    val av: String,          // avatar initial
    val color: Color,
    val colorSecondary: Color,
    val softColor: Color,
    val due: String,
    val overdue: Boolean,
    val freq: String,
    val tagline: String,
    val tags: List<String>,
    val dates: List<Pair<String, String>>,
    val history: List<Pair<String, String>>,
    val job: JobInfo?,
    val avoid: AvoidInfo?,
    val context: String = "",           // richer "last touch" line for the focus flow card
    val city: String = "",              // 所在城市（地理位置 tag）— 驱动行程/到达匹配
    val closeness: Int? = null,         // 亲密度心值：null=未定, 0=点头之交, 1=熟人, 2=很亲近, 3=至亲挚友
    val scripts: List<String> = emptyList(),  // opening-line suggestions ("换一个" cycles them)
    val avatar: ImageBitmap? = null     // user-picked photo; falls back to the gradient + initial
)

data class HistoryEntry(
    val date: String,
    val text: String,
    val methods: List<String> = emptyList(),
    val fresh: Boolean = false
)

object SampleData {
    val contacts = listOf(
        Contact(
            id = "mom", name = "妈妈", pinyin = "M", rel = "家人", group = "家人", home = "today", city = "成都", closeness = 3,
            av = "妈", color = Color(0xFFE07A5F), colorSecondary = Color(0xFFF2A07C),
            softColor = Color(0xFFF6E1D8),
            due = "该联系了", overdue = false, freq = "每周",
            tagline = "上次聊到体检报告",
            tags = listOf("爱喝茶", "跳广场舞", "怕冷"),
            dates = listOf("生日" to "农历三月初八", "结婚纪念" to "5月20日"),
            history = listOf("10天前" to "视频聊了半小时，叮嘱她按时吃药", "上个月" to "寄了一条羊绒围巾"),
            job = JobInfo("退休教师", "原市第三小学", "教育"),
            avoid = AvoidInfo(diet = listOf("海鲜过敏", "忌生冷"), topics = listOf("别提体重", "少聊病痛")),
            context = "聊到体检报告 · 10天前视频过",
            scripts = listOf(
                "妈，体检报告出来了没？周末我回来陪你一起去拿。",
                "妈，最近降温了，那条羊绒围巾记得戴上。",
                "这周末我回家吃饭吧，想吃你做的菜了。"
            )
        ),
        Contact(
            id = "zhou", name = "老周", pinyin = "Z", rel = "老友", group = "朋友", home = "today", city = "杭州", closeness = 2,
            av = "周", color = Color(0xFF3D5A80), colorSecondary = Color(0xFF5E83B3),
            softColor = Color(0xFFDCE4EE),
            due = "逾期 5 天", overdue = true, freq = "每月",
            tagline = "答应了一起去钓鱼",
            tags = listOf("钓鱼", "老照片", "爱喝两杯"),
            dates = listOf("生日" to "6月12日"),
            history = listOf("2个月前" to "一起吃了顿饭，聊到老同事", "今年初" to "帮我看了套房子"),
            job = JobInfo("车间主任", "红星机械厂", "制造业"),
            avoid = AvoidInfo(diet = listOf("血糖高·少甜", "忌油腻"), topics = listOf("别问儿子工作", "别劝他戒酒")),
            context = "答应了一起去钓鱼 · 已逾期 5 天",
            scripts = listOf(
                "周哥，说好的钓鱼呢？这周六天气不错，走一个？",
                "老周，上次你帮我看的那套房子，找时间再聊聊？",
                "好久没跟你坐下喝茶了，约个晚上聚聚？"
            )
        ),
        Contact(
            id = "lin", name = "林夕", pinyin = "L", rel = "同事", group = "同事", home = "today", city = "杭州", closeness = 2,
            av = "林", color = Color(0xFF8367C7), colorSecondary = Color(0xFFA98BE0),
            softColor = Color(0xFFE7DEF7),
            due = "该联系了", overdue = false, freq = "每月",
            tagline = "说好帮她内推",
            tags = listOf("手冲咖啡", "养猫", "跑马拉松"),
            dates = listOf("生日" to "9月3日"),
            history = listOf("3周前" to "一起做了季度复盘"),
            job = JobInfo("产品经理", "蓝湖科技", "互联网"),
            avoid = AvoidInfo(diet = listOf("素食", "不吃辣"), topics = listOf("别催婚", "别打听薪资")),
            context = "说好帮她内推 · 3周前一起复盘",
            scripts = listOf(
                "内推的事我问过了，JD 发你，这周约杯咖啡细聊？",
                "马拉松报名开了，今年你还冲不冲？",
                "楼下新开了家手冲店，明天带你去测评一下？"
            )
        ),
        Contact(
            id = "yu", name = "小宇", pinyin = "X", rel = "同学", group = "同学", home = "week", city = "上海",
            av = "宇", color = Color(0xFF2A9D8F), colorSecondary = Color(0xFF5BC4B5),
            softColor = Color(0xFFD5EFEB),
            due = "本周", overdue = false, freq = "每季",
            tagline = "好久没聚了",
            tags = listOf("打游戏", "想创业", "爱辩论"),
            dates = listOf("生日" to "12月1日"),
            history = listOf("上个季度" to "来家里吃过饭"),
            job = JobInfo("创业筹备中", "自己在张罗", "电商"),
            avoid = AvoidInfo(diet = emptyList(), topics = listOf("别劝进大厂", "别催问进度")),
            context = "上个季度来家里吃过饭",
            scripts = listOf(
                "宇哥，最近项目张罗得怎么样？周末出来开黑顺便聊聊？"
            )
        ),
        Contact(
            id = "dad", name = "爸爸", pinyin = "B", rel = "家人", group = "家人", home = null, city = "成都", closeness = 3,
            av = "爸", color = Color(0xFFD98A5B), colorSecondary = Color(0xFFE8A878),
            softColor = Color(0xFFF6E6D6),
            due = "", overdue = false, freq = "每周",
            tagline = "爱看新闻，话不多",
            tags = listOf("钓鱼", "下棋", "戒烟中"),
            dates = listOf("生日" to "农历八月十五"),
            history = listOf("上周" to "一起看了场球赛"),
            job = JobInfo("退休", "原第二机床厂", "机械"),
            avoid = AvoidInfo(diet = listOf("忌油腻", "少饮酒"), topics = listOf("别递烟·戒烟中", "别催体检"))
        ),
        Contact(
            id = "yi", name = "小姨", pinyin = "X", rel = "家人", group = "家人", home = null, city = "成都",
            av = "姨", color = Color(0xFFC77BBF), colorSecondary = Color(0xFFD89BD2),
            softColor = Color(0xFFF3E3F1),
            due = "", overdue = false, freq = "每季",
            tagline = "做饭一绝，热心",
            tags = listOf("旅游", "做饭", "广场舞"),
            dates = listOf("生日" to "3月22日"),
            history = listOf("上个月" to "她寄了自制腊肠"),
            job = JobInfo("个体经营", "姨家小吃店", "餐饮"),
            avoid = AvoidInfo(diet = listOf("忌生冷"), topics = listOf("别提表妹相亲", "别比收入"))
        ),
        Contact(
            id = "lei", name = "张磊", pinyin = "Z", rel = "朋友", group = "朋友", home = null, city = "上海",
            av = "磊", color = Color(0xFF4C9A8E), colorSecondary = Color(0xFF6FB8AD),
            softColor = Color(0xFFD6ECE8),
            due = "", overdue = false, freq = "每月",
            tagline = "大学室友",
            tags = listOf("篮球", "数码", "摄影"),
            dates = listOf("生日" to "7月8日"),
            history = listOf("2周前" to "约了周末打球"),
            job = JobInfo("资深设计师", "合鸣广告", "广告设计"),
            avoid = AvoidInfo(diet = listOf("不吃香菜"), topics = listOf("别聊前任", "别问买房"))
        ),
        Contact(
            id = "min", name = "王敏", pinyin = "W", rel = "同事", group = "同事", home = null, city = "深圳",
            av = "敏", color = Color(0xFF9B7BD4), colorSecondary = Color(0xFFB49BE0),
            softColor = Color(0xFFE8E0F6),
            due = "", overdue = false, freq = "每月",
            tagline = "隔壁组负责人",
            tags = listOf("瑜伽", "红酒", "看展"),
            dates = listOf("生日" to "11月2日"),
            history = listOf("3周前" to "对接了项目"),
            job = JobInfo("团队负责人", "蓝湖科技", "互联网"),
            avoid = AvoidInfo(diet = listOf("花生过敏！", "不吃辣"), topics = listOf("别聊裁员", "别问年龄"))
        ),
        Contact(
            id = "chen", name = "陈老师", pinyin = "C", rel = "其他", group = "其他", home = null, city = "北京",
            av = "陈", color = Color(0xFF5A7BB5), colorSecondary = Color(0xFF7D9BCB),
            softColor = Color(0xFFDCE5F2),
            due = "", overdue = false, freq = "每半年",
            tagline = "高中班主任",
            tags = listOf("书法", "历史", "养生"),
            dates = listOf("教师节" to "9月10日"),
            history = listOf("今年初" to "发了拜年消息"),
            job = JobInfo("退休班主任", "原市第一中学", "教育"),
            avoid = AvoidInfo(diet = listOf("清淡为主", "忌辛辣"), topics = listOf("别提应酬喝酒"))
        )
    )

    val chipOptions = listOf("打了电话", "见了面", "发了消息", "一起吃饭", "送了礼物")
    val freqOptions = listOf("每周", "每月", "每季", "每半年", "每年")
    val groupOrder = listOf("家人", "朋友", "同事", "同学", "其他")

    // 常见城市可选项（添加联系人时的城市快选）
    val cityOptions = listOf("北京", "上海", "广州", "深圳", "杭州", "成都", "南京", "武汉", "西安", "重庆")

    /** 城市 → 匹配关键词（真实日历 / 定位匹配用）。未列出的城市回退到城市名本身。 */
    val cityAliases: Map<String, List<String>> = mapOf(
        "北京" to listOf("北京", "beijing"),
        "上海" to listOf("上海", "shanghai"),
        "广州" to listOf("广州", "guangzhou"),
        "深圳" to listOf("深圳", "shenzhen"),
        "杭州" to listOf("杭州", "hangzhou"),
        "成都" to listOf("成都", "chengdu"),
        "南京" to listOf("南京", "nanjing"),
        "武汉" to listOf("武汉", "wuhan"),
        "西安" to listOf("西安", "xi'an", "xian"),
        "重庆" to listOf("重庆", "chongqing")
    )

    /** 城市的匹配关键词；至少含城市名本身（长度≥2，避免单字误匹配）。 */
    fun aliasesFor(city: String): List<String> =
        cityAliases[city] ?: listOf(city).filter { it.length >= 2 }

    // 金钱往来 / 人情往来 种子数据（2a 融合方案）
    val moneyRecords = listOf(
        MoneyRecord(
            id = "n1", contactId = "zhou", type = MoneyType.Lend, amount = 2000,
            date = "5月30日", note = "他儿子装修", reminderDate = "8月4日",
            loanStatus = LoanStatus.Unpaid
        ),
        MoneyRecord(
            id = "n2", contactId = "zhou", type = MoneyType.GiveGift, isPhysical = true,
            itemName = "茅台一瓶", estValue = 1500, date = "1月18日", note = "他生日"
        ),
        MoneyRecord(
            id = "n3", contactId = "zhou", type = MoneyType.ReceiveGift, amount = 800,
            date = "3月2日", note = "我乔迁", event = "我乔迁", giftReturn = GiftReturn.Pending
        ),
        MoneyRecord(
            id = "n4", contactId = "zhou", type = MoneyType.GiveGift, amount = 1000,
            date = "5月20日", note = "微信转账 · 新婚大喜", event = "老周儿子婚礼"
        ),
        MoneyRecord(
            id = "n5", contactId = "lin", type = MoneyType.ReceiveGift, isPhysical = true,
            itemName = "茶具一套", estValue = 600, date = "3月2日", note = "我乔迁",
            event = "我乔迁", giftReturn = GiftReturn.Pending
        ),
        MoneyRecord(
            id = "n6", contactId = "yu", type = MoneyType.ReceiveGift, amount = 800,
            date = "3月2日", note = "现金红包", event = "我乔迁", giftReturn = GiftReturn.Returned
        ),
        MoneyRecord(
            id = "n7", contactId = "yu", type = MoneyType.Lend, amount = 500,
            date = "6月26日", loanStatus = LoanStatus.Unpaid
        ),
        MoneyRecord(
            id = "n8", contactId = "mom", type = MoneyType.GiveGift, isPhysical = true,
            itemName = "羊绒围巾", estValue = 600, date = "5月12日", note = "母亲节"
        )
    )

    /** 礼簿事件的展示日期 / 地点（按事件名索引）。 */
    val eventMeta: Map<String, Pair<String, String>> = mapOf(
        "老周儿子婚礼" to ("5月20日" to "杭州"),
        "我乔迁" to ("3月2日" to "")
    )
}
