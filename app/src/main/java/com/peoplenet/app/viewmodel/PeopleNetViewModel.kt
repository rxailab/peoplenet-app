package com.peoplenet.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.peoplenet.app.net.DashScopeAsr
import com.peoplenet.app.net.QwenConfig
import com.peoplenet.app.net.QwenParse
import com.peoplenet.app.net.QwenService
import com.peoplenet.app.data.AvoidInfo
import com.peoplenet.app.data.Contact
import com.peoplenet.app.data.DateUtil
import com.peoplenet.app.data.GiftReturn
import com.peoplenet.app.data.HistoryEntry
import com.peoplenet.app.data.LoanStatus
import com.peoplenet.app.data.MoneyRecord
import com.peoplenet.app.data.MoneyType
import com.peoplenet.app.data.SampleData

enum class Screen {
    // 底部四 tab（1d 四 tab 信息架构：给「人情」一个家）
    Home, Contacts, People, Me,
    Detail, Record, Done, Flow,
    // "我" tab sub-screens
    About, Notifications, Groups, ImportContacts,
    AddContact,
    // 5a 融合方案：城市落地页
    CityArrival,
    // 7a 节日融合方案：祝福清单页 + 礼物清单页(3a)
    Bless, GiftList,
    // 登录账号系统：账号与安全 / 注销账号
    AccountSecurity, DeleteAccount,
    // 亲密度分级助手
    Grade
}

/** 来自真实日历的即将出行。 */
data class TripInfo(val city: String, val dateLabel: String, val friendIds: List<String>)

/** 5a 融合方案：一次「到达朋友的城市」事件驱动的三个触点共享状态。 */
data class GeoState(
    val arrived: Boolean = false,                    // 是否已到某位朋友所在的城市
    val notifShown: Boolean = false,                 // 到达推送是否显示
    val cityDismissed: Boolean = false,              // 首页横幅本次到访是否已收起（不再提醒）
    val cityExpanded: Boolean = false,               // 首页横幅是否展开成员列表
    val remind: Map<String, Boolean> = emptyMap(),   // 每人「到达提醒」开关（缺省=开，仅存显式改动）
    val pregreeted: Set<String> = emptySet(),        // 出发前已「提前问好」
    val acted: Map<String, String> = emptyMap(),     // id -> greeted | met | skip
    // 真实数据
    val currentCity: String? = null,                 // 真实定位城市
    val arrivedCity: String? = null,                 // 已到达的朋友城市（展示名，如「杭州」「上海」）
    val realTrip: TripInfo? = null,                  // 真实日历行程
    val geoLoaded: Boolean = false                   // 是否已尝试读取真实定位/日历
)

/** 在某座城市的朋友：距离 + 在城开场白 + 城市（用于匹配真实定位/日历）。 */
data class GeoFriend(
    val id: String,
    val dist: String,
    val cityScript: String,
    val city: String,
    val aliases: List<String>
)

/** 7a 节日融合方案：两段式「先准备，再祝福」的共享状态。 */
data class FestState(
    val cdStep: Int = 0,                          // 0=距节日还远, 1=前7天(准备), 2=当天(祝福)
    val notifShown: Boolean = false,              // 当天的到达推送
    val cart: Set<String> = emptySet(),           // 已「加清单」的礼物 key："friendId:index"
    val giftSearch: String = "",                  // 礼物清单页搜索词（名字或关系）
    val giftOpen: Set<String> = setOf("mom", "zhou"), // 手风琴展开的人（默认前两位）
    val acted: Map<String, String> = emptyMap()   // friendId -> sent | skip
)

/** 节日里要祝福的朋友：复用联系人头像/名字 + 一句定制祝福。 */
data class FestFriend(val id: String, val script: String)

/** 一件推荐礼物（商品占位色块 + 名称 + 价格）。 */
data class Gift(
    val char: String,
    val name: String,
    val price: Int,
    val c1: androidx.compose.ui.graphics.Color,
    val c2: androidx.compose.ui.graphics.Color
)

/** 按人推荐的礼物：谁 + 推荐理由 + 3 件商品。 */
data class GiftRec(val friendId: String, val reason: String, val items: List<Gift>)

data class NotifSettings(
    val dailyReminder: Boolean = true,
    val reminderTime: String = "09:00",
    val birthdayReminder: Boolean = true,
    val overdueReminder: Boolean = true,
    val dndEnabled: Boolean = false,
    val dndRange: String = "22:00 - 08:00"
)

/** 登录流程的步骤（未登录时）。 */
enum class AuthStage { Phone, Otp, Profile }

/** 登录账号系统状态（原型：模拟登录，内存态）。 */
data class AuthState(
    val loggedIn: Boolean = false,       // 已完成登录
    val guest: Boolean = false,          // 游客「先逛逛」
    val stage: AuthStage = AuthStage.Phone,
    val phone: String = "",              // 11 位手机号
    val otp: String = "",                // 6 位验证码
    val nickname: String = "阿哲",
    val avatarChar: String = "哲",
    val wechatBound: Boolean = false,    // 是否绑定微信
    val agreed: Boolean = false,         // 是否勾选协议
    val showLogoutSheet: Boolean = false // 退出登录确认弹层
) {
    /** 已进入 App（登录或游客）。 */
    val inApp: Boolean get() = loggedIn || guest
    /** 手机号打码显示，如 138****8888。 */
    val phoneMasked: String
        get() = if (phone.length == 11) "${phone.take(3)}****${phone.takeLast(4)}" else phone
}

enum class SortMode {
    Group,       // by relationship group: 家人、朋友、同事…
    Closeness,   // by 亲密度心值 high→low
    Alphabetical // by pinyin initial A-Z
}

/** 亲密度心值四档：0 点头之交 · 1 熟人 · 2 很亲近 · 3 至亲挚友。 */
object Closeness {
    val names = listOf("点头之交", "熟人", "很亲近", "至亲挚友")
    fun name(level: Int?): String = if (level == null) "未定" else names.getOrElse(level) { "未定" }
    /** 用 ♥ 表示等级（0 与 null 返回空串）。 */
    fun hearts(level: Int?): String = when (level) {
        1 -> "♥"; 2 -> "♥♥"; 3 -> "♥♥♥"; else -> ""
    }
}

/**
 * 金钱往来的编辑草稿 —— 记一笔内联段落与详情页弹层共用。
 * [expanded] 仅记一笔段落用（false=折叠虚线入口）；[manual] 切到手动表单（否则一句话识别）。
 */
data class MoneyForm(
    val expanded: Boolean = false,
    val manual: Boolean = false,
    val nlText: String = "",
    val type: MoneyType = MoneyType.Lend,
    val isPhysical: Boolean = false,
    val amountText: String = "",
    val itemName: String = "",
    val estValueText: String = "",
    val note: String = "",
    val reminderOn: Boolean = true,
    val reminderDate: String = "8月4日",
    val recognized: Boolean = false,   // 一句话是否识别出内容
    val typePinned: Boolean = false    // 用户手动改过类型后，不再被重新识别覆盖
) {
    /** 能否保存：金额型需有金额；实物型需有名称。 */
    val valid: Boolean
        get() = if (isPhysical) itemName.isNotBlank()
        else (amountText.filter { it.isDigit() }.toIntOrNull() ?: 0) > 0
}

/** 一句话识别出的要素（实物礼无金额，用 itemName）。 */
data class MoneyRecognition(
    val type: MoneyType,
    val amount: Int,
    val reminderDate: String,
    val isPhysical: Boolean = false,
    val itemName: String = ""
)

// ---- AI 融入方案（1a-1d）数据 ----

/** 1a 开场白的「依据」标签；avoid=true 用粉色表示避雷。 */
data class OpenerBasis(val text: String, val avoid: Boolean = false)

/** 1c 本周联络计划的一条。 */
data class PlanItem(val whenLabel: String, val contactId: String, val name: String, val reason: String)

/** 语音助手解析出的「提醒」记录（Turn 6）。 */
data class VoiceReminder(val title: String, val date: String, val person: String, val time: String)

/**
 * 语音助手解析出的「人情账」记录（Turn 6）。
 * kind: lend借出 / borrow借入 / give送礼 / receive收礼 / collect收回借出；
 * item 非空 = 实物礼（amount 当作估值）；linked = 命中已有借出。
 */
data class VoiceMoney(
    val label: String, val person: String, val amount: Int,
    val linked: Boolean, val linkNote: String,
    val collect: Boolean = false, val kind: String = "lend", val item: String = ""
)

/** 语音助手解析出的「新建联系人」。 */
data class VoiceNewContact(val name: String, val relation: String, val note: String)

/** 一句话解析的完整结果：提醒 / 人情账 / 新建联系人 / 标记已联系（可同时多个）。 */
data class VoiceParse(
    val reminder: VoiceReminder? = null,
    val money: VoiceMoney? = null,
    val newContact: VoiceNewContact? = null,
    val contactedPerson: String? = null
) {
    val isEmpty: Boolean get() = reminder == null && money == null && newContact == null && contactedPerson == null
    val count: Int get() = listOfNotNull(reminder, money, newContact, contactedPerson).size
}

/** 1d 礼物参谋的一条建议。 */
data class GiftSuggestion(
    val contactId: String,
    val name: String,
    val av: String,
    val color: Color,
    val color2: Color,
    val gift: String,
    val price: String,
    val reason: String,
    val warn: String
)

data class AppState(
    val screen: Screen = Screen.Home,
    val previousTab: Screen = Screen.Home,
    val selectedContactId: String? = null,
    val doneMap: Map<String, String> = emptyMap(),     // contactId -> next freq
    val logMap: Map<String, List<HistoryEntry>> = emptyMap(),
    val note: String = "",
    val selectedChips: Set<String> = emptySet(),
    val selectedFreq: String? = null,
    val searchQuery: String = "",
    val sortMode: SortMode = SortMode.Group,
    // Contacts 分组视图：可折叠 + 可调顺序
    val groupOrder: List<String> = SampleData.groupOrder,
    val collapsedGroups: Set<String> = emptySet(),
    val reorderGroup: String? = null,               // 长按折叠的分组后，显示其上下箭头
    // 亲密度分级助手
    val gradeSkipped: Set<String> = emptySet(),     // 本次「跳过」的联系人
    val gradeTier: Map<String, Int> = emptyMap(),   // 助手里「换一档」后当前选中的档位
    // Focus flow (3c "点击横幅卡片") state
    val tomorrowMap: Set<String> = emptySet(),      // ids snoozed to "明天再提醒"
    val flowOrder: List<String> = emptyList(),       // today queue order (mutated by "换一位")
    val flowRecordFor: String? = null,               // contact whose record sheet is open in-flow
    val scriptIndex: Map<String, Int> = emptyMap(),  // "换一个" opening-line cursor per contact
    val toastText: String = "",
    val toastSeq: Int = 0,                           // bump to re-trigger the toast animation
    // "我" tab settings
    val notif: NotifSettings = NotifSettings(),
    val importedCount: Int = 0,                      // running total of "imported" contacts
    // 5a 融合方案
    val geo: GeoState = GeoState(),
    val fest: FestState = FestState(),
    // 登录账号系统
    val auth: AuthState = AuthState(),
    // 金钱往来 / 人情往来（2a 融合方案）
    val moneyDraft: MoneyForm = MoneyForm(),         // 记一笔内联段落草稿
    val moneySheet: MoneyForm? = null,               // 详情页底部弹层（null=关闭）
    val moneySheetContactId: String? = null,         // 弹层归属的联系人
    val moneySheetEditId: String? = null,            // 非空=正在编辑这条记录（否则新增）
    val ledgerTab: Int = 1,                          // 人情账本：0=借还 1=礼簿（默认礼簿）
    val moneySnoozed: Set<String> = emptySet(),      // 今天页「再等一周 / 下次再说」本次隐藏
    // AI 融入方案（1a-1d）
    val flowTone: Int = 2,                            // 1a 开场白语气：0自然寒暄 1关心近况 2约见面
    val aiStage: Int = 0,                             // 1b 一句话：0空闲 1听中 2识别中 3完成
    val aiText: String = "",                          // 1b 正在识别的文字
    val aiExcerpt: String = "",                       // 1b 自动摘录
    val aiFreqSuggested: Boolean = false,             // 1b 频率是否为 AI 建议
    val aiMoneyLinked: Boolean = false,               // 1b 是否关联到已有借出
    val weekPlanAdopted: Boolean = false,             // 1c 本周计划已排入提醒
    val insightDismissed: Boolean = false,            // 1c 关系洞察已忽略
    val giftBatch: Int = 0,                           // 1d 礼物建议批次
    val giftAdded: Set<String> = emptySet(),          // 1d 已加入清单的礼物 key
    // 主界面优化（Turn 5/6）
    val authGuideDismissed: Boolean = false,          // 修正③ 授权引导已关闭（降级进行程卡）
    val homeActionFor: String? = null,                // 整行点开的底部操作单联系人 id（null=关闭）
    val voiceStage: Int = 0,                          // 语音助手：0关 1听写 2解析中 3结果
    val voiceOpenSeq: Int = 0,                        // 新开面板/重说 时自增：只有这两种情况自动开麦（解析失败返回不重启）
    val voiceText: String = "",                       // 语音识别的文字（可编辑=真实输入）
    val voiceReminder: VoiceReminder? = null,         // 解析出的提醒
    val voiceMoney: VoiceMoney? = null,               // 解析出的人情账
    val voiceNewContact: VoiceNewContact? = null,     // 解析出的新建联系人
    val voiceContactedPerson: String? = null,         // 解析出的「标记已联系」对象
    val voiceUndoContactId: String? = null,           // 本次保存新建的联系人 id（撤销时移除）
    val voiceConfirm: String = "",                    // 落库后顶部确认条（""=隐藏）
    val voiceConfirmSeq: Int = 0,                     // 触发确认条动画/自动消失
    val contactReminderNote: Map<String, String> = emptyMap(), // 语音落库后覆盖首页行副标题
    val voiceUndoPrevNotes: Map<String, String>? = null,   // 撤销用：保存前的行副标题快照
    val voiceUndoRecordIds: List<String> = emptyList()     // 撤销用：本次新增的记录 id
)

class PeopleNetViewModel : ViewModel() {

    var state by mutableStateOf(AppState())
        private set

    // Starts from the seed data; the "+" button appends new contacts at runtime.
    var contacts by mutableStateOf(SampleData.contacts)
        private set

    // 金钱往来 / 人情往来记录（2a 融合方案）；记一笔与详情页弹层都往这里写。
    var moneyRecords by mutableStateOf(SampleData.moneyRecords)
        private set
    private var moneySeq = 0

    // ---- 登录账号系统（原型：模拟登录，内存态）----

    /** 是否已进入 App（登录或游客）。未进入则显示登录流程。 */
    val inApp: Boolean get() = state.auth.inApp

    private fun updateAuth(t: (AuthState) -> AuthState) { state = state.copy(auth = t(state.auth)) }

    fun authSetPhone(p: String) = updateAuth { it.copy(phone = p.filter { c -> c.isDigit() }.take(11)) }
    fun authToggleAgree() = updateAuth { it.copy(agreed = !it.agreed) }

    /** 获取验证码 → 进入验证码页。 */
    fun authSendCode() {
        val a = state.auth
        if (a.phone.length != 11) { showToast("请输入 11 位手机号"); return }
        if (!a.agreed) { showToast("请先阅读并同意用户协议与隐私政策"); return }
        updateAuth { it.copy(stage = AuthStage.Otp, otp = "") }
    }

    fun authResendCode() { updateAuth { it.copy(otp = "") }; showToast("验证码已重新发送") }

    /** 输入验证码；满 6 位自动校验（模拟：任意 6 位通过）。 */
    fun authSetOtp(o: String) {
        val digits = o.filter { it.isDigit() }.take(6)
        updateAuth { it.copy(otp = digits) }
        if (digits.length == 6) updateAuth { it.copy(stage = AuthStage.Profile) }
    }

    /**
     * 微信授权回调结果（errCode, code）。errCode 0 = 成功。
     * 真实场景：把 code 发给你的后端，后端用 AppID+AppSecret 调微信接口换取
     * access_token+openid，再拉取昵称/头像返回给 App。原型无后端，成功即以微信身份登录。
     */
    fun onWechatResult(errCode: Int, code: String?) {
        when (errCode) {
            0 -> {
                updateAuth { it.copy(loggedIn = true, wechatBound = true, nickname = "微信用户", avatarChar = "微") }
                showToast("微信授权成功 · 已拿到 code（待后端换取资料）")
            }
            -2 -> showToast("已取消微信登录")
            -4 -> showToast("微信登录被拒绝")
            else -> showToast("微信登录失败（$errCode）")
        }
    }

    /** 先逛逛（游客）。 */
    fun authGuest() = updateAuth { it.copy(guest = true) }

    /** 游客点「登录/注册」→ 退出游客态，回到登录流程。 */
    fun authExitGuest() = updateAuth { it.copy(guest = false) }

    fun authSetNickname(n: String) = updateAuth { it.copy(nickname = n.take(12)) }

    /** 完善资料 / 先跳过 → 进入 App。 */
    fun authFinishProfile() {
        val nick = state.auth.nickname.trim().ifEmpty { "新朋友" }
        updateAuth { it.copy(loggedIn = true, nickname = nick, avatarChar = nick.takeLast(1)) }
    }

    /** 登录流程内返回上一步。 */
    fun authBackStep() = updateAuth {
        when (it.stage) {
            AuthStage.Otp -> it.copy(stage = AuthStage.Phone)
            AuthStage.Profile -> it.copy(stage = AuthStage.Otp)
            AuthStage.Phone -> it
        }
    }

    fun authRequestLogout() = updateAuth { it.copy(showLogoutSheet = true) }
    fun authCancelLogout() = updateAuth { it.copy(showLogoutSheet = false) }

    /** 退出登录：回到登录页（保留手机号方便重登）。 */
    fun authLogout() {
        state = state.copy(screen = Screen.Home, auth = AuthState(phone = state.auth.phone))
    }

    /** 注销账号：清空回到登录。 */
    fun authDeleteAccount() {
        state = state.copy(screen = Screen.Home, auth = AuthState())
        showToast("账号已进入 7 天冷静期")
    }

    fun openAccountSecurity() { state = state.copy(screen = Screen.AccountSecurity, previousTab = Screen.Me) }
    fun openDeleteAccount() { state = state.copy(screen = Screen.DeleteAccount) }

    val todayContacts: List<Contact>
        get() = contacts.filter { it.home == "today" }

    val weekContacts: List<Contact>
        get() = contacts.filter { it.home == "week" }

    // Pending = today's contacts neither contacted nor snoozed to tomorrow
    val remainingToday: Int
        get() = todayContacts.count { !isContacted(it.id) && !isTomorrow(it.id) }

    val handledToday: Int
        get() = todayContacts.count { isContacted(it.id) || isTomorrow(it.id) }

    // "已联系" takes precedence over "约在明天": once contacted, no longer counted as tomorrow
    val tomorrowCountToday: Int
        get() = todayContacts.count { isTomorrow(it.id) && !isContacted(it.id) }

    val doneCount: Int
        get() = state.doneMap.size

    val selectedContact: Contact?
        get() = state.selectedContactId?.let { id -> contacts.find { it.id == id } }

    // ---- 编辑联系人的喜好 / 禁忌 ----

    private fun updateContact(id: String, transform: (Contact) -> Contact) {
        contacts = contacts.map { if (it.id == id) transform(it) else it }
    }

    fun addTag(id: String, tag: String) {
        val t = tag.trim()
        if (t.isEmpty()) return
        updateContact(id) { c -> if (c.tags.any { it.equals(t, ignoreCase = true) }) c else c.copy(tags = c.tags + t) }
    }

    fun removeTag(id: String, tag: String) {
        updateContact(id) { c -> c.copy(tags = c.tags - tag) }
    }

    /** diet=true → 饮食禁忌；diet=false → 话题禁忌。 */
    fun addAvoid(id: String, diet: Boolean, item: String) {
        val t = item.trim()
        if (t.isEmpty()) return
        updateContact(id) { c ->
            val a = c.avoid ?: AvoidInfo()
            val na = if (diet) {
                if (a.diet.any { it.equals(t, ignoreCase = true) }) a else a.copy(diet = a.diet + t)
            } else {
                if (a.topics.any { it.equals(t, ignoreCase = true) }) a else a.copy(topics = a.topics + t)
            }
            c.copy(avoid = na)
        }
    }

    fun removeAvoid(id: String, diet: Boolean, item: String) {
        updateContact(id) { c ->
            val a = c.avoid ?: return@updateContact c
            c.copy(avoid = if (diet) a.copy(diet = a.diet - item) else a.copy(topics = a.topics - item))
        }
    }

    fun isContacted(contactId: String): Boolean = state.doneMap.containsKey(contactId)
    fun isTomorrow(contactId: String): Boolean = state.tomorrowMap.contains(contactId)

    fun pillText(contact: Contact): String {
        return when {
            isContacted(contact.id) -> "已联系·下次${state.doneMap[contact.id]}"
            isTomorrow(contact.id) -> "明天提醒"
            else -> contact.due
        }
    }

    fun historyFor(contact: Contact): List<HistoryEntry> {
        val logged = state.logMap[contact.id] ?: emptyList()
        val seed = contact.history.map { HistoryEntry(date = it.first, text = it.second) }
        return logged + seed
    }

    fun groupedContacts(): List<Pair<String, List<Contact>>> {
        val filtered = filteredContacts()
        return when (state.sortMode) {
            SortMode.Group -> {
                state.groupOrder.mapNotNull { group ->
                    val items = filtered.filter { it.group == group }
                    if (items.isNotEmpty()) group to items else null
                }
            }
            SortMode.Alphabetical -> {
                filtered
                    .groupBy { it.pinyin }
                    .toSortedMap()
                    .map { (letter, items) -> letter to items }
            }
            SortMode.Closeness -> {
                // 单一无名分组：按心值高→低（未定排最后），同档保持原顺序（近似「最近联系」）
                listOf("" to filtered.sortedByDescending { it.closeness ?: -1 })
            }
        }
    }

    private fun filteredContacts(): List<Contact> {
        val query = state.searchQuery.trim()
        if (query.isEmpty()) return contacts
        return contacts.filter { contact ->
            contact.name.contains(query, ignoreCase = true) ||
                contact.tags.any { it.contains(query, ignoreCase = true) } ||
                contact.rel.contains(query, ignoreCase = true) ||
                contact.pinyin.equals(query, ignoreCase = true)
        }
    }

    fun setSearchQuery(query: String) {
        state = state.copy(searchQuery = query)
    }

    fun toggleSortMode() {
        val next = if (state.sortMode == SortMode.Group) SortMode.Alphabetical else SortMode.Group
        state = state.copy(sortMode = next)
    }

    fun setSortMode(mode: SortMode) {
        state = state.copy(sortMode = mode)
    }

    // ---- 亲密度心值 + 分级助手（1b × 1d 融合方案）----

    /** 手动设置某人的亲密度心值（详情页/助手都写这个字段）。 */
    fun setCloseness(id: String, level: Int) {
        updateContact(id) { it.copy(closeness = level) }
    }

    /** 已定亲密度的人数 / 未定的人数。 */
    val closenessSetCount: Int get() = contacts.count { it.closeness != null }
    val closenessUnsetCount: Int get() = contacts.count { it.closeness == null }

    /** 按「期望频率」建议心值档位（0-3）。 */
    fun suggestCloseness(contact: Contact): Int = when (contact.freq) {
        "每周" -> 3
        "每月" -> 2
        "每季" -> 1
        else -> 0   // 每半年 / 每年 / 空
    }

    /** 建议理由文案。 */
    fun suggestReason(contact: Contact): String = when (contact.freq) {
        "每周" -> "高频期望 · 来往密切"
        "每月" -> "每月期望 · 来往中等"
        "每季" -> "每季期望 · 保持往来"
        else -> "低频期望 · 来往较少"
    }

    // ---- 分级助手 ----

    fun openGrade() { state = state.copy(screen = Screen.Grade, previousTab = Screen.Contacts) }

    /** 待整理队列：未定心值、且本次没跳过的联系人。 */
    fun gradeQueue(): List<Contact> =
        contacts.filter { it.closeness == null && it.id !in state.gradeSkipped }

    /** 助手里当前选中的档位（换一档后覆盖，否则用建议值）。 */
    fun gradeTierFor(contact: Contact): Int =
        state.gradeTier[contact.id] ?: suggestCloseness(contact)

    fun gradeAdopt(id: String) {
        val c = contacts.find { it.id == id } ?: return
        setCloseness(id, gradeTierFor(c))
    }

    /** 换一档：在 0-3 之间循环切换当前展示的档位。 */
    fun gradeChangeTier(id: String) {
        val c = contacts.find { it.id == id } ?: return
        val next = (gradeTierFor(c) + 1) % 4
        state = state.copy(gradeTier = state.gradeTier + (id to next))
    }

    fun gradeSkip(id: String) {
        state = state.copy(gradeSkipped = state.gradeSkipped + id)
    }

    /** 一键采纳全部当前建议。 */
    fun gradeAdoptAll() {
        val queue = gradeQueue()
        contacts = contacts.map { c -> if (c in queue) c.copy(closeness = gradeTierFor(c)) else c }
    }

    // ==== 金钱往来 / 人情往来（2a 融合方案）====

    fun contactById(id: String): Contact? = contacts.find { it.id == id }

    /** 某人的所有往来记录（最新在前，按插入顺序倒序）。 */
    fun moneyFor(contactId: String): List<MoneyRecord> =
        moneyRecords.filter { it.contactId == contactId }.reversed()

    fun moneyCountFor(contactId: String): Int = moneyRecords.count { it.contactId == contactId }

    private fun contactNameFor(id: String): String = contacts.find { it.id == id }?.name ?: ""

    // ---- 一句话智能识别（记一笔内联段落）----

    /** 从一句话里拆出「类型 / 金额 / 提醒」。无金额但是送礼/收礼 → 识别为实物礼；还款不当作新的一笔。 */
    fun recognizeMoney(text: String): MoneyRecognition? {
        if (text.isBlank()) return null
        // 还款不是新的一笔往来（用详情页胶囊标记已还），避免误记成新借出
        if (Regex("还了我|还我了|还我|还款|还清|还上|归还").containsMatchIn(text) && !text.contains("借")) return null
        // 没有明确的借/送/收/垫等往来信号（如「请他吃饭花了三百」纯消费）→ 不当作金钱往来
        val type = classifyMoneyType(text) ?: return null
        val amount = parseAmount(text)
        val reminder = when {
            Regex("下个月|下月|一个月").containsMatchIn(text) -> "8月4日"
            text.contains("月底") -> "7月31日"
            Regex("下周|下星期|一周|一星期").containsMatchIn(text) -> "7月11日"
            Regex("年底|过年").containsMatchIn(text) -> "12月31日"
            else -> "8月4日"
        }
        if (amount == null) {
            // 无金额：仅当判断为礼时按实物礼识别，否则识别失败
            if (type.isGift) return MoneyRecognition(type, 0, reminder, isPhysical = true, itemName = extractItemName(text))
            return null
        }
        return MoneyRecognition(type, amount, reminder)
    }

    /** 方向感知的类型判定；无明确往来信号时返回 null（避免把消费/闲聊误记成借出）。 */
    private fun classifyMoneyType(text: String): MoneyType? = when {
        Regex("借给|借出|垫了|垫付|借出去").containsMatchIn(text) -> MoneyType.Lend
        Regex("借了?.{0,6}给").containsMatchIn(text) -> MoneyType.Lend        // 借(了)…给某人
        Regex("向.{0,4}借|找.{0,4}借|问.{0,4}借|借我|借了我|借入").containsMatchIn(text) -> MoneyType.Borrow
        Regex("送我|送了我|随我|随了我|收礼|收到|收了|回礼|给我.*红包").containsMatchIn(text) -> MoneyType.ReceiveGift
        Regex("随礼|送礼|送了|随了|封了|包了|随份子|给.*红包").containsMatchIn(text) -> MoneyType.GiveGift
        Regex("借了|借的").containsMatchIn(text) -> MoneyType.Lend
        else -> null
    }

    /** 从「收了林夕的茶具」这类句子里粗略取出物品名。 */
    private fun extractItemName(text: String): String {
        val after = text.substringAfterLast("的", "")
        val raw = after.ifBlank { text }
            .replace(Regex("[收送了随礼给我他她的一件个份子]"), "")
            .trim()
        return raw.ifBlank { "礼物" }.take(8)
    }

    /** 抓金额：先去掉日期数字，支持「万/千/k」与中文数字（两千/五百/一万）。 */
    private fun parseAmount(text: String): Int? {
        // 去掉日期里的数字，避免把「5月20日」的 5 当成金额
        val cleaned = text
            .replace(Regex("\\d+月(\\d+[日号])?"), "·")
            .replace(Regex("\\d+[日号点]"), "·")
            .replace(Regex("\\d+年"), "·")
        Regex("(\\d+(?:\\.\\d+)?)\\s*(万|千|k|K)?").find(cleaned)?.let { m ->
            val num = m.groupValues[1].toDoubleOrNull()
            if (num != null) {
                val mult = when (m.groupValues[2]) { "万" -> 10000; "千", "k", "K" -> 1000; else -> 1 }
                val v = (num * mult).toInt()
                if (v > 0) return v
            }
        }
        return parseChineseAmount(cleaned)
    }

    private val cnDigit = mapOf(
        '零' to 0, '一' to 1, '二' to 2, '两' to 2, '三' to 3, '四' to 4,
        '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9
    )
    private val cnUnit = mapOf('十' to 10, '百' to 100, '千' to 1000)

    /** 解析中文数字金额（两千=2000、五百=500、一万=10000）。 */
    private fun parseChineseAmount(text: String): Int? {
        // 先去掉星期/月份里的中文数字，避免把「周六」的六、「八月」的八当成金额
        val cleaned = text
            .replace(Regex("(周|星期|礼拜)[一二三四五六日天]"), " ")
            .replace(Regex("[一二三四五六七八九十]+月[初中底]?"), " ")
        val runs = Regex("[零一二两三四五六七八九十百千万]+").findAll(cleaned).map { it.value }.toList()
        // 金额通常带量级（十/百/千/万）；优先取带量级的一段，避免误取零散单字
        val run = runs.firstOrNull { r -> r.any { it in "十百千万" } } ?: runs.firstOrNull() ?: return null
        var total = 0
        var section = 0
        var current = 0
        var lastUnit = 0        // 最近一个量级（十/百/千/万），用于口语省略末位单位
        var zeroSeen = false    // 「零」表示直接跳到个位，不做省略推断
        for (c in run) {
            when {
                c == '零' -> { current = 0; zeroSeen = true }
                cnDigit.containsKey(c) -> current = cnDigit.getValue(c)
                c == '万' -> { total += (section + current) * 10000; section = 0; current = 0; lastUnit = 10000; zeroSeen = false }
                cnUnit.containsKey(c) -> {
                    if (current == 0) current = 1
                    section += current * cnUnit.getValue(c)
                    current = 0
                    lastUnit = cnUnit.getValue(c)
                    zeroSeen = false
                }
            }
        }
        // 口语省略：八百八=880、一万五=15000、两千五=2500（末位数字补上低一档量级）
        val tail = if (current != 0 && lastUnit >= 10 && !zeroSeen) current * (lastUnit / 10) else current
        val result = total + section + tail
        return if (result > 0) result else null
    }

    // ---- 记一笔内联段落草稿 ----

    private fun updateDraft(t: (MoneyForm) -> MoneyForm) { state = state.copy(moneyDraft = t(state.moneyDraft)) }

    /** 展开 / 收起「金钱往来」段落。 */
    fun moneyToggleExpand() = updateDraft { it.copy(expanded = !it.expanded) }

    /** 输入一句话 → 实时识别，回填金额/提醒/物品；类型若被用户手动改过（typePinned）则不覆盖。 */
    fun moneySetNl(text: String) {
        val rec = recognizeMoney(text)
        updateDraft { d ->
            if (rec == null) {
                d.copy(nlText = text, amountText = "", itemName = "", isPhysical = false, recognized = false)
            } else {
                val newType = if (d.typePinned) d.type else rec.type
                d.copy(
                    nlText = text,
                    type = newType,
                    isPhysical = rec.isPhysical,
                    amountText = if (rec.isPhysical) "" else rec.amount.toString(),
                    itemName = if (rec.isPhysical) rec.itemName else "",
                    reminderDate = rec.reminderDate,
                    reminderOn = newType.isLoan,
                    recognized = true
                )
            }
        }
    }

    /** 进入借还类型时恢复提醒默认开；离开借还则关。 */
    private fun reminderForType(next: MoneyType, prev: MoneyType, prevOn: Boolean): Boolean =
        if (next.isLoan) (if (prev.isLoan) prevOn else true) else false

    fun moneyCycleType() = updateDraft {
        val next = MoneyType.values()[(it.type.ordinal + 1) % MoneyType.values().size]
        it.copy(type = next, typePinned = true, reminderOn = reminderForType(next, it.type, it.reminderOn))
    }

    fun moneySetType(type: MoneyType) = updateDraft {
        it.copy(type = type, typePinned = true, reminderOn = reminderForType(type, it.type, it.reminderOn))
    }

    fun moneySetAmount(text: String) = updateDraft { it.copy(amountText = text.filter { c -> c.isDigit() }.take(9)) }
    fun moneySetItem(text: String) = updateDraft { it.copy(itemName = text) }
    fun moneySetEstValue(text: String) = updateDraft { it.copy(estValueText = text.filter { c -> c.isDigit() }.take(9)) }
    fun moneySetPhysical(physical: Boolean) = updateDraft { it.copy(isPhysical = physical) }
    fun moneySetNote(text: String) = updateDraft { it.copy(note = text) }
    fun moneyToggleReminder() = updateDraft { it.copy(reminderOn = !it.reminderOn) }
    fun moneyToggleManual() = updateDraft { it.copy(manual = !it.manual) }

    private fun resetDraft() { state = state.copy(moneyDraft = MoneyForm()) }

    /** 把一份草稿变成正式记录并写入。 */
    private fun commitForm(form: MoneyForm, contactId: String) {
        moneySeq += 1
        val amount = form.amountText.filter { it.isDigit() }.toIntOrNull() ?: 0
        val estValue = form.estValueText.filter { it.isDigit() }.toIntOrNull() ?: 0
        val rec = MoneyRecord(
            id = "nu${moneySeq}",
            contactId = contactId,
            type = form.type,
            isPhysical = form.isPhysical,
            amount = if (form.isPhysical) 0 else amount,
            itemName = if (form.isPhysical) form.itemName.trim() else "",
            estValue = if (form.isPhysical) estValue else 0,
            date = "今天",
            note = form.note.trim(),
            reminderDate = if (form.type.isLoan && form.reminderOn) form.reminderDate else "",
            loanStatus = if (form.type.isLoan) LoanStatus.Unpaid else null,
            giftReturn = if (form.type == MoneyType.ReceiveGift) GiftReturn.Pending else null
        )
        moneyRecords = moneyRecords + rec
    }

    /** 记一笔保存时，若段落展开且有效则一并落库。返回是否记了金钱往来。 */
    fun commitMoneyDraftIfAny(contactId: String): Boolean {
        val d = state.moneyDraft
        if (!d.expanded || !d.valid) return false
        commitForm(d, contactId)
        return true
    }

    // ---- 详情页底部弹层表单 ----

    fun openMoneySheet(contactId: String) {
        state = state.copy(moneySheet = MoneyForm(expanded = true), moneySheetContactId = contactId, moneySheetEditId = null)
    }

    /** 编辑已有的一笔：把记录回填进弹层表单。 */
    fun openMoneySheetForEdit(r: MoneyRecord) {
        val form = MoneyForm(
            expanded = true,
            type = r.type,
            isPhysical = r.isPhysical,
            amountText = if (!r.isPhysical && r.amount > 0) r.amount.toString() else "",
            itemName = r.itemName,
            estValueText = if (r.estValue > 0) r.estValue.toString() else "",
            note = r.note,
            reminderOn = r.reminderDate.isNotBlank(),
            reminderDate = r.reminderDate.ifBlank { "8月4日" }
        )
        state = state.copy(moneySheet = form, moneySheetContactId = r.contactId, moneySheetEditId = r.id)
    }

    fun closeMoneySheet() { state = state.copy(moneySheet = null, moneySheetContactId = null, moneySheetEditId = null) }

    /** 删除一笔往来。 */
    fun deleteMoneyRecord(id: String) {
        moneyRecords = moneyRecords.filterNot { it.id == id }
        showToast("已删除")
    }

    private fun updateSheet(t: (MoneyForm) -> MoneyForm) {
        val s = state.moneySheet ?: return
        state = state.copy(moneySheet = t(s))
    }

    fun sheetSetType(type: MoneyType) = updateSheet {
        it.copy(type = type, reminderOn = reminderForType(type, it.type, it.reminderOn))
    }
    fun sheetSetPhysical(physical: Boolean) = updateSheet { it.copy(isPhysical = physical) }
    fun sheetSetAmount(text: String) = updateSheet { it.copy(amountText = text.filter { c -> c.isDigit() }.take(9)) }
    fun sheetSetItem(text: String) = updateSheet { it.copy(itemName = text) }
    fun sheetSetEstValue(text: String) = updateSheet { it.copy(estValueText = text.filter { c -> c.isDigit() }.take(9)) }
    fun sheetSetNote(text: String) = updateSheet { it.copy(note = text) }
    fun sheetToggleReminder() = updateSheet { it.copy(reminderOn = !it.reminderOn) }

    fun saveMoneySheet() {
        val form = state.moneySheet ?: return
        val cid = state.moneySheetContactId ?: return
        if (!form.valid) { showToast("填一下金额或名称"); return }
        val editId = state.moneySheetEditId
        if (editId != null) {
            val amount = form.amountText.filter { it.isDigit() }.toIntOrNull() ?: 0
            val estValue = form.estValueText.filter { it.isDigit() }.toIntOrNull() ?: 0
            moneyRecords = moneyRecords.map { r ->
                if (r.id != editId) r
                else r.copy(
                    type = form.type,
                    isPhysical = form.isPhysical,
                    amount = if (form.isPhysical) 0 else amount,
                    itemName = if (form.isPhysical) form.itemName.trim() else "",
                    estValue = if (form.isPhysical) estValue else 0,
                    note = form.note.trim(),
                    reminderDate = if (form.type.isLoan && form.reminderOn) form.reminderDate else "",
                    // 类型没变则保留原状态；换了类型则重置为该类型的默认状态
                    loanStatus = if (form.type.isLoan) (r.loanStatus ?: LoanStatus.Unpaid) else null,
                    giftReturn = if (form.type == MoneyType.ReceiveGift) (r.giftReturn ?: GiftReturn.Pending) else null
                )
            }
            showToast("改好啦")
        } else {
            commitForm(form, cid)
            showToast("记好啦")
        }
        state = state.copy(moneySheet = null, moneySheetContactId = null, moneySheetEditId = null)
    }

    // ---- 详情页行内状态切换 ----

    /** 点借出/借入的状态胶囊：未还 → 部分还 → 已还 → 未还 循环。 */
    fun cycleLoanStatus(recordId: String) {
        moneyRecords = moneyRecords.map { r ->
            if (r.id != recordId || r.loanStatus == null) r
            else {
                val next = LoanStatus.values()[(r.loanStatus.ordinal + 1) % LoanStatus.values().size]
                r.copy(loanStatus = next)
            }
        }
    }

    // ---- 人情账本（People：从「我」进入的二级页）----

    fun openPeople() { state = state.copy(screen = Screen.People, previousTab = Screen.Me) }
    fun setLedgerTab(tab: Int) { state = state.copy(ledgerTab = tab) }

    /** 「＋ 记一笔」：先去联系人列表挑一个人，再进他的档案记一笔（往来始终挂在某个人名下）。 */
    fun ledgerAddEntry() {
        state = state.copy(screen = Screen.Contacts, previousTab = Screen.Contacts)
        showToast("挑一位联系人 · 在他档案里记一笔")
    }

    /** 礼簿：按事件归档的送礼/收礼记录。事件顺序按其展示日期倒序（近的在前）。 */
    fun giftEvents(): List<Triple<String, Pair<String, String>, List<MoneyRecord>>> {
        val gifts = moneyRecords.filter { it.type.isGift && it.event.isNotBlank() }
        val byEvent = gifts.groupBy { it.event }
        return byEvent.entries
            .map { (event, records) ->
                val meta = SampleData.eventMeta[event] ?: ("" to "")
                Triple(event, meta, records)
            }
            .sortedByDescending { monthDayKey(it.second.first) }   // 按「月*100+日」倒序，近的在前
    }

    /** 把「5月20日」解析成可比较的 月*100+日；解析不出返回 -1。 */
    private fun monthDayKey(date: String): Int {
        val m = Regex("(\\d+)月(\\d+)").find(date) ?: return -1
        return m.groupValues[1].toInt() * 100 + m.groupValues[2].toInt()
    }

    /** 无事件的散记礼（若有），归到「其他」。 */
    fun looseGifts(): List<MoneyRecord> = moneyRecords.filter { it.type.isGift && it.event.isBlank() }

    /** 借还：待收回（借出未清）/ 待还（借入未清）/ 已结清。 */
    fun loansByStatus(): Triple<List<MoneyRecord>, List<MoneyRecord>, List<MoneyRecord>> {
        val loans = moneyRecords.filter { it.type.isLoan }
        val toCollect = loans.filter { it.type == MoneyType.Lend && it.loanStatus != LoanStatus.Paid }
        val toRepay = loans.filter { it.type == MoneyType.Borrow && it.loanStatus != LoanStatus.Paid }
        val settled = loans.filter { it.loanStatus == LoanStatus.Paid }
        return Triple(toCollect, toRepay, settled)
    }

    // ---- 今天页人情提醒 ----

    /** 借出到期提醒：未还清、设了提醒、未被本次隐藏。 */
    fun loanReminders(): List<MoneyRecord> = moneyRecords.filter {
        it.type == MoneyType.Lend && it.loanStatus != LoanStatus.Paid &&
            it.reminderDate.isNotBlank() && it.id !in state.moneySnoozed
    }

    /** 回礼提醒：收礼待回礼、未被本次隐藏。 */
    fun giftReminders(): List<MoneyRecord> = moneyRecords.filter {
        it.type == MoneyType.ReceiveGift && it.giftReturn == GiftReturn.Pending && it.id !in state.moneySnoozed
    }

    val moneyReminderCount: Int get() = loanReminders().size + giftReminders().size

    fun markLoanPaid(recordId: String) {
        moneyRecords = moneyRecords.map { if (it.id == recordId) it.copy(loanStatus = LoanStatus.Paid) else it }
        showToast("已标记还清")
    }

    fun markLoanPartial(recordId: String) {
        moneyRecords = moneyRecords.map { if (it.id == recordId) it.copy(loanStatus = LoanStatus.Partial) else it }
        showToast("已标记部分还")
    }

    fun markGiftReturned(recordId: String) {
        moneyRecords = moneyRecords.map { if (it.id == recordId) it.copy(giftReturn = GiftReturn.Returned) else it }
        showToast("回礼记好啦")
    }

    fun snoozeMoneyReminder(recordId: String) {
        state = state.copy(moneySnoozed = state.moneySnoozed + recordId)
        showToast("好，过阵子再提醒你")
    }

    // ==== AI 融入方案（1a-1d）====

    // ---- 1a 会接话的开场白（专注流）----

    val toneLabels = listOf("自然寒暄", "关心近况", "约见面")

    fun setFlowTone(tone: Int, contactId: String) {
        // 切换语气后，仅重置当前联系人的候选游标（不影响其他人）
        state = state.copy(flowTone = tone, scriptIndex = state.scriptIndex - contactId)
    }

    /** 按语气取开场白候选：老周为设计稿定制，其余按语气套模板。 */
    fun toneScriptsFor(contact: Contact, tone: Int): List<String> {
        if (contact.id == "zhou") return when (tone) {
            0 -> listOf(
                "老周，好久没跟你坐下喝茶了，最近厂里忙不忙？",
                "刷到一组老照片，想起你说要整理老相册，弄得怎么样了？"
            )
            1 -> listOf(
                "周哥，入伏了，血糖还稳吗？冰的少碰点。",
                "听说儿子装修快收尾了，你这阵子别太操心，睡好点。"
            )
            else -> listOf(
                "周哥，说好的钓鱼呢？周六杭州多云 26℃，正适合下竿，走一个？",
                "上次你帮我看的那套房子，周六钓完鱼顺路再去转转？"
            )
        }
        val n = contact.name
        return when (tone) {
            0 -> listOf("$n，好久没联系了，最近怎么样？", "$n，突然想起你，最近一切都好吗？")
            1 -> listOf("$n，最近还好吗？注意身体，别太累。", "$n，天气忽冷忽热，记得照顾好自己。")
            else -> listOf("$n，找个时间聚聚呀，${contact.tagline}。", "$n，这周末有空吗？出来坐坐？")
        }
    }

    /** 当前语气下的开场白（供专注流展示）。 */
    fun openerFor(contact: Contact): String {
        val list = toneScriptsFor(contact, state.flowTone)
        val idx = (state.scriptIndex[contact.id] ?: 0) % list.size
        return list[idx]
    }

    /** 换一个：在当前语气的候选里循环。 */
    fun nextOpener(contactId: String) {
        state = state.copy(scriptIndex = state.scriptIndex + (contactId to ((state.scriptIndex[contactId] ?: 0) + 1)))
    }

    /** 开场白的「依据」标签：上次话题 + 天气 + 已避开的雷区。 */
    fun openerBasis(contact: Contact): List<OpenerBasis> {
        val list = mutableListOf<OpenerBasis>()
        val topic = contact.tagline.removePrefix("答应了").removePrefix("答应").ifBlank { contact.tagline }
        if (contact.tagline.isNotBlank()) list.add(OpenerBasis(if (contact.id == "zhou") "答应一起钓鱼" else contact.tagline))
        val city = contact.city.ifBlank { "本地" }
        list.add(OpenerBasis("周六$city 多云 26℃"))
        contact.avoid?.topics?.firstOrNull()?.let { t ->
            list.add(OpenerBasis("已避开：${t.removePrefix("别").removePrefix("少")}", avoid = true))
        }
        return list
    }

    // ---- 1b 一句话记一笔（AI 解析填表）----

    private val aiDemoSentence = "跟老周打了个电话，约了周六去钓鱼，他借的两千说八月初还"

    // 演示协程期间用户可能离开记一笔页；离开后不再写状态，避免填到别的联系人身上
    private fun aiStillActive(target: String?): Boolean =
        state.screen == Screen.Record && state.selectedContactId == target

    /** 试试语音示例：模拟「听 → 识别 → 填好」。 */
    fun aiRunVoiceDemo() {
        if (state.aiStage == 1 || state.aiStage == 2) return
        val target = state.selectedContactId
        viewModelScope.launch {
            state = state.copy(aiStage = 1, aiText = "")
            val s = aiDemoSentence
            for (i in 1..s.length) {
                if (!aiStillActive(target)) return@launch
                state = state.copy(aiText = s.substring(0, i))
                delay(26)
            }
            if (!aiStillActive(target)) return@launch
            delay(300)
            state = state.copy(aiStage = 2)
            delay(750)
            if (!aiStillActive(target)) return@launch
            applyAiParse(s)
        }
    }

    /** 用户自己打完一句话后点识别。 */
    fun aiParseTyped(text: String) {
        if (text.isBlank()) { showToast("先说一句试试～"); return }
        val target = state.selectedContactId
        viewModelScope.launch {
            state = state.copy(aiStage = 2, aiText = text)
            delay(650)
            if (!aiStillActive(target)) return@launch
            applyAiParse(text)
        }
    }

    fun aiSetText(text: String) { state = state.copy(aiText = text) }

    private fun applyAiParse(sentence: String) {
        val cid = state.selectedContactId ?: "zhou"
        // 方式
        val chips = mutableSetOf<String>()
        if (Regex("电话|打了个电话|通话").containsMatchIn(sentence)) chips.add("打了电话")
        if (Regex("见面|见了面|碰面").containsMatchIn(sentence)) chips.add("见了面")
        if (Regex("消息|微信|短信").containsMatchIn(sentence)) chips.add("发了消息")
        if (Regex("吃饭|聚餐|饭").containsMatchIn(sentence)) chips.add("一起吃饭")
        if (chips.isEmpty()) chips.add("打了电话")
        // 摘录：挑含「约 / 钓鱼 / 说好」的从句
        val excerpt = sentence.split("，", "。", ",").map { it.trim() }
            .firstOrNull { Regex("约|钓鱼|说好|一起").containsMatchIn(it) }
            ?.let { if (it.startsWith("约")) it else it }
            ?: sentence
        // 金钱往来
        val rec = recognizeMoney(sentence)
        val draft = if (rec != null && !rec.isPhysical) MoneyForm(
            expanded = true, type = rec.type,
            amountText = rec.amount.toString(),
            reminderDate = rec.reminderDate, reminderOn = rec.type.isLoan, recognized = true
        ) else state.moneyDraft
        // 是否与已有借出重复（老周已有一笔 n1 借出¥2000）
        val linked = rec != null && rec.type == MoneyType.Lend &&
            moneyRecords.any { it.contactId == cid && it.type == MoneyType.Lend && it.amount == rec.amount }
        state = state.copy(
            aiStage = 3,
            aiExcerpt = excerpt,
            selectedChips = state.selectedChips + chips,          // 合并，保留用户已选的方式
            note = if (state.note.isBlank()) excerpt else state.note,  // 不覆盖用户已写的备注
            moneyDraft = draft,
            selectedFreq = "每月",
            aiFreqSuggested = true,
            aiMoneyLinked = linked
        )
    }

    fun aiReset() {
        state = state.copy(
            aiStage = 0, aiText = "", aiExcerpt = "",
            aiFreqSuggested = false, aiMoneyLinked = false,
            selectedChips = emptySet(), note = "",
            moneyDraft = MoneyForm(),
            selectedFreq = selectedContact?.freq   // 还原成该联系人的默认频率
        )
    }

    // ---- 1c 关系洞察与本周计划（首页）----

    fun weekPlanItems(): List<PlanItem> = listOf(
        PlanItem("今天", "mom", "妈妈", "视频 · 体检报告该出了"),
        PlanItem("周六", "zhou", "老周", "钓鱼约起 · 周六多云正合适"),
        PlanItem("周日", "lin", "林夕", "约咖啡 · 内推 JD 已备好")
    )

    fun adoptWeekPlan() {
        if (!state.weekPlanAdopted) {
            state = state.copy(weekPlanAdopted = true)
            showToast("已排入 3 条提醒，可随时调整")
        }
    }

    fun dismissInsight() { state = state.copy(insightDismissed = true) }

    /** 关系洞察「现在去联系」：打开老周的专注流式记录（这里直接进详情）。 */
    fun insightContactNow() {
        openContact("zhou")
    }

    // ---- 1d 节日礼物参谋（首页）----

    private fun giftBatches(): List<List<GiftSuggestion>> = listOf(
        listOf(
            GiftSuggestion("mom", "妈妈", "妈", Color(0xFFE07A5F), Color(0xFFF2A07C), "羊绒披肩", "¥289", "怕冷 · 去年送的围巾她很喜欢，披肩正好接上", ""),
            GiftSuggestion("zhou", "老周", "周", Color(0xFF3D5A80), Color(0xFF5E83B3), "鱼竿收纳包", "¥168", "爱钓鱼 · 你们周六正好要去，当面给", "已避开酒类 · 血糖高"),
            GiftSuggestion("min", "王敏", "敏", Color(0xFF9B7BD4), Color(0xFFB49BE0), "花草茶礼盒", "¥129", "喜欢瑜伽和看展，清淡不出错", "已避开坚果 · 花生过敏")
        ),
        listOf(
            GiftSuggestion("mom", "妈妈", "妈", Color(0xFFE07A5F), Color(0xFFF2A07C), "足浴按摩仪", "¥399", "跳广场舞 · 晚上泡脚放松腿脚", ""),
            GiftSuggestion("zhou", "老周", "周", Color(0xFF3D5A80), Color(0xFF5E83B3), "老照片修复册", "¥219", "他一直念叨想整理老相册", "已避开酒类 · 血糖高"),
            GiftSuggestion("min", "王敏", "敏", Color(0xFF9B7BD4), Color(0xFFB49BE0), "红酒杯一对", "¥199", "喜欢红酒 · 配她上次提的家宴", "已避开坚果 · 花生过敏")
        )
    )

    fun giftSuggestions(): List<GiftSuggestion> = giftBatches()[state.giftBatch % giftBatches().size]

    fun swapGiftBatch() { state = state.copy(giftBatch = state.giftBatch + 1) }

    fun giftAddedKey(index: Int): String = "${state.giftBatch}-$index"
    fun isGiftAdded(index: Int): Boolean = giftAddedKey(index) in state.giftAdded
    fun toggleGiftAdded(index: Int) {
        val key = giftAddedKey(index)
        val added = key in state.giftAdded
        state = state.copy(giftAdded = if (added) state.giftAdded - key else state.giftAdded + key)
        if (!added) showToast("已加入中秋清单 · 共 ${state.giftAdded.count()} 件")
    }

    // ==== 主界面优化（Turn 5 整行可点/操作单/完成态 · Turn 6 语音助手）====

    private fun yuan(n: Int) = "¥" + "%,d".format(n)

    // ---- 修正③ 授权引导可关闭（降级进行程卡）----
    val authGuideOn: Boolean get() = !state.authGuideDismissed
    fun dismissAuthGuide() { state = state.copy(authGuideDismissed = true) }

    // ---- 首页整行副标题（语音落库覆盖优先；老周折叠「关系洞察」）----
    fun homeRowSubtitle(contact: Contact): String {
        state.contactReminderNote[contact.id]?.let { return it }
        return when (contact.id) {
            "mom" -> "上次聊到体检报告 · 计划今天视频"
            "zhou" -> "钓鱼约定还在 · 间隔在悄悄拉长"
            "lin" -> "说好帮她内推 · 计划周日约咖啡"
            else -> contact.tagline
        }
    }

    // ---- 5b 整行点开的底部操作单 ----
    fun openHomeAction(id: String) { state = state.copy(homeActionFor = id) }
    fun closeHomeAction() { state = state.copy(homeActionFor = null) }
    val homeActionContact: Contact? get() = state.homeActionFor?.let { contactById(it) }

    fun homeMarkContacted(id: String) { flowMarkDone(id, useRecordInputs = false); state = state.copy(homeActionFor = null) }
    fun homeMarkTomorrow(id: String) { flowMarkTomorrow(id); state = state.copy(homeActionFor = null) }

    // ---- 修正④ 完成态（与首页计数口径一致：已联系或已挪到明天都算处理完）----
    val homeAllDone: Boolean get() = todayContacts.isNotEmpty() && todayContacts.all { isContacted(it.id) || isTomorrow(it.id) }

    // ---- Turn 6 语音助手：听写 → 解析 → 落库 ----
    private val voiceDemo = "周六提醒我和老周去钓鱼，顺便把借他的两千收一下"

    // Turn 6 语音助手 · Paraformer 实时语音识别
    private val asr = DashScopeAsr()
    private var asrRestarts = 0          // 服务器侧结束后的无缝重连次数（听到新内容即清零）
    private var lastTextChange = 0L      // 识别文字最近一次变化的时间：2 秒没新字 → 自动结束（比音量判断抗噪）
    private var lastAutoFinished = ""    // 上次自动结束时的文本；相同文本不再重复触发（防解析失败后死循环）

    /** 是否正在收音（听写面板的 ⏹停止/🎙继续 开关据此渲染）。 */
    var voiceListening by mutableStateOf(false)
        private set

    /** 麦克风实时音量 0..1（不放进 AppState，避免 10Hz 全页重组）。 */
    var voiceLevel by mutableStateOf(0f)
        private set

    /** 进入听写：开始实时识别（需已授权 RECORD_AUDIO）。增量文字直接写入 voiceText。 */
    fun voiceStartAsr() {
        if (voiceListening || !QwenConfig.enabled) return
        voiceListening = true
        lastTextChange = System.currentTimeMillis()
        val prefix = state.voiceText   // 重连时保留此前已识别的文字
        asr.start(
            onText = { t, _ ->
                asrRestarts = 0
                val newText = prefix + t
                if (newText != state.voiceText) lastTextChange = System.currentTimeMillis()
                state = state.copy(voiceText = newText)
            },
            onLevel = { lv ->
                voiceLevel = lv
                // 「2 秒没识别出新字就自动结束」：环境噪音只要没转成文字就不算说话。
                // 同一段文本只触发一次——解析失败回到听写页后不再拿旧文本反复解析。
                val txt = state.voiceText
                if (voiceListening && state.voiceStage == 1 && txt.isNotBlank() &&
                    txt != lastAutoFinished && System.currentTimeMillis() - lastTextChange > 2000) {
                    lastAutoFinished = txt
                    voiceFinish()
                }
            },
            onFinished = {
                // 服务器侧结束（长时间静音超时等）：还停在听写页就无缝重连，麦克风不能悄悄变聋
                if (voiceListening && state.voiceStage == 1 && asrRestarts < 3) {
                    voiceListening = false; asrRestarts++
                    android.util.Log.d("QwenAsr", "server finished → auto restart #$asrRestarts")
                    voiceStartAsr()
                } else if (voiceListening) {
                    voiceListening = false; voiceLevel = 0f
                }
            },
            onError = { msg ->
                voiceListening = false; voiceLevel = 0f
                android.util.Log.w("QwenAsr", "err: $msg"); showToast("语音识别不可用，可手动打字")
            }
        )
    }
    fun voiceStopAsr() { if (!voiceListening) return; voiceListening = false; voiceLevel = 0f; asr.stop() }
    fun voiceAsrDenied() { showToast("麦克风未授权，可手动打字") }

    /** App 退后台：停止录音并收起语音面板（后台不该占用麦克风）。 */
    fun onAppBackground() { if (state.voiceStage != 0) voiceClose() }

    fun voiceOpen() { asrRestarts = 0; lastAutoFinished = ""; state = state.copy(voiceStage = 1, voiceOpenSeq = state.voiceOpenSeq + 1, voiceText = "", voiceReminder = null, voiceMoney = null, voiceNewContact = null, voiceContactedPerson = null) }
    fun voiceClose() { voiceStopAsr(); state = state.copy(voiceStage = 0) }
    /** 只有内容真的变了才当作「手动打字」停掉识别；输入框获得焦点等触发的
     *  同值 onValueChange（三星键盘常见）不再误杀识别会话。 */
    fun voiceSetText(t: String) {
        if (t != state.voiceText) voiceStopAsr()
        state = state.copy(voiceText = t)
    }

    /** 播放示例：边说边出字。 */
    fun voiceRunDemo() {
        if (state.voiceStage != 1) return
        voiceStopAsr()
        viewModelScope.launch {
            state = state.copy(voiceText = "")
            for (i in 1..voiceDemo.length) {
                if (state.voiceStage != 1) return@launch
                state = state.copy(voiceText = voiceDemo.substring(0, i)); delay(30)
            }
        }
    }

    /** 说完了 → 交给 Qwen 解析成结构化记录（失败或未配置 KEY 时回退本地规则）。 */
    fun voiceFinish() {
        voiceStopAsr()
        val text = state.voiceText.trim()
        if (text.isBlank()) { showToast("先说一句试试～"); return }
        viewModelScope.launch {
            state = state.copy(voiceStage = 2)
            val t0 = System.currentTimeMillis()
            val p = resolveVoiceParse(text)
            // 保证「解析中」至少停留一会儿，避免闪一下
            val elapsed = System.currentTimeMillis() - t0
            if (elapsed < 600) delay(600 - elapsed)
            if (state.voiceStage != 2) return@launch
            // 没解析出内容：回到听写页但保持「已暂停」，文字保留 —— 用户自己决定改字还是点「继续」再说
            if (p.isEmpty) { state = state.copy(voiceStage = 1); showToast("要带上联系人名字才能记：如「周六提醒我和老周钓鱼」「新建联系人王强」"); return@launch }
            state = state.copy(
                voiceStage = 3,
                voiceReminder = p.reminder, voiceMoney = p.money,
                voiceNewContact = p.newContact, voiceContactedPerson = p.contactedPerson
            )
        }
    }

    /** 优先 Qwen；未配置 KEY / 网络异常 / 空结果时回退本地规则解析。 */
    private suspend fun resolveVoiceParse(text: String): VoiceParse {
        if (QwenConfig.enabled) {
            val q = withTimeoutOrNull(15000) {
                try { QwenService.parse(text, contacts.map { it.name }, QwenConfig.TODAY) }
                catch (e: Exception) { android.util.Log.w("QwenVoice", "call failed: $e"); null }
            }
            if (q != null && !q.isEmpty) {
                val mapped = mapQwenParse(q)
                if (!mapped.isEmpty) {
                    android.util.Log.d("QwenVoice", "USING QWEN result")
                    return mapped
                }
            }
            android.util.Log.d("QwenVoice", "Qwen empty/failed → local fallback")
        }
        return parseVoiceInput(text)
    }

    /** 把 Qwen 抽取结果映射成 App 结构，「关联已有借出」仍用本地数据判断。 */
    private fun mapQwenParse(q: QwenParse): VoiceParse {
        // 新建联系人：同名已存在时不重复建
        val newContact = if (q.hasNewContact && contacts.none { it.name == q.newContactName })
            VoiceNewContact(q.newContactName!!, q.newContactRel ?: "朋友", q.newContactNote ?: "")
        else null

        // 操作对象：已知联系人，或本句要新建的联系人
        val existing = q.person?.let { p -> contacts.firstOrNull { it.name == p } }
        val name = existing?.name ?: newContact?.name?.takeIf { it == q.person || q.person == null }
        if (name == null && newContact == null) return VoiceParse()

        val target = name ?: newContact!!.name
        val reminder = if (q.hasReminder)
            VoiceReminder(
                q.reminderTitle!!,
                q.reminderDate?.let { DateUtil.resolveDateWord(it) } ?: "本周",   // 日期本地确定性换算，不信模型的算术
                target, q.reminderTime ?: "上午 9:00"
            )
        else null

        val money = if (q.hasMoney) {
            val kind = q.kind!!
            val amount = q.amount ?: 0
            val collect = kind == "collect"
            val existingLoan = if (collect && existing != null)
                moneyRecords.firstOrNull { it.contactId == existing.id && it.type == MoneyType.Lend && it.amount == amount }
            else null
            VoiceMoney(
                label = when (kind) {
                    "collect" -> "收回借出"; "borrow" -> "记一笔借入"
                    "give" -> "记一笔送礼"; "receive" -> "记一笔收礼"
                    else -> "记一笔借出"
                },
                person = target, amount = amount, linked = existingLoan != null, collect = collect,
                kind = kind, item = q.item ?: "",
                linkNote = if (existingLoan != null) "已关联到「${target}借的 ${yuan(amount)}到期」，见面时提醒你当面收"
                else if (collect) "记为收回，不重复新增借出" else ""
            )
        } else null

        val contactedPerson = if (q.contacted && existing != null) existing.name else null
        return VoiceParse(reminder, money, newContact, contactedPerson)
    }

    fun voiceRedo() { voiceStopAsr(); asrRestarts = 0; lastAutoFinished = ""; state = state.copy(voiceStage = 1, voiceOpenSeq = state.voiceOpenSeq + 1, voiceText = "", voiceReminder = null, voiceMoney = null, voiceNewContact = null, voiceContactedPerson = null) }

    override fun onCleared() { asr.release(); super.onCleared() }
    fun voiceDropReminder() { state = state.copy(voiceReminder = null) }
    fun voiceDropMoney() { state = state.copy(voiceMoney = null) }
    fun voiceDropNewContact() { state = state.copy(voiceNewContact = null) }
    fun voiceDropContacted() { state = state.copy(voiceContactedPerson = null) }

    /** 本地规则回退：只认「提醒 + 借出/收回」。必须点到某位联系人，否则无从落库。 */
    private fun parseVoiceInput(text: String): VoiceParse {
        val person = contacts.firstOrNull { text.contains(it.name) } ?: return VoiceParse()
        val name = person.name
        val hasReminder = Regex("提醒|约|钓鱼|见面|喝茶|吃饭|聚|咖啡|周[一二三四五六日天]|明天|后天|下周").containsMatchIn(text) && !text.startsWith("借")
        val reminder = if (hasReminder) {
            val activity = when {
                text.contains("钓鱼") -> "钓鱼"; text.contains("喝茶") -> "喝茶"
                text.contains("吃饭") -> "吃饭"; text.contains("咖啡") -> "喝咖啡"
                else -> "见面"
            }
            val date = when {
                Regex("周六|星期六|礼拜六").containsMatchIn(text) -> "周六 ${DateUtil.nextWeekday(java.util.Calendar.SATURDAY)}"
                Regex("周日|星期日|礼拜天").containsMatchIn(text) -> "周日 ${DateUtil.nextWeekday(java.util.Calendar.SUNDAY)}"
                text.contains("明天") -> "明天 ${DateUtil.monthDay(1)}"
                text.contains("后天") -> "后天 ${DateUtil.monthDay(2)}"
                text.contains("今天") -> "今天 ${DateUtil.monthDay(0)}"
                else -> "本周"
            }
            VoiceReminder("和${name}${activity}", date, name, "上午 9:00")
        } else null
        val amount = parseAmount(text)
        val money = if (amount != null && Regex("借|收|还|钱|块|元|万|千|红包|礼").containsMatchIn(text)) {
            val collect = Regex("收|收回|要回|拿回|还我|还了").containsMatchIn(text)
            val existing = moneyRecords.firstOrNull { it.contactId == person.id && it.type == MoneyType.Lend && it.amount == amount }
            VoiceMoney(
                label = if (collect) "收回借出" else "记一笔借出",
                person = name, amount = amount, linked = existing != null, collect = collect,
                kind = if (collect) "collect" else "lend",
                linkNote = if (existing != null) "已关联到「${name}借的 ${yuan(amount)}到期」，见面时提醒你当面收"
                else if (collect) "记为收回，不重复新增借出" else ""
            )
        } else null
        return VoiceParse(reminder, money)
    }

    /** 都对，保存 → 落库 + 首页立即反映；快照支持撤销。 */
    fun voiceSave() {
        val r = state.voiceReminder; val m = state.voiceMoney
        val nc = state.voiceNewContact; val cp = state.voiceContactedPerson
        val confirmParts = mutableListOf<String>()

        // ① 新建联系人（其余记录可挂在这个新人名下）；语音的分组白名单校验，非法值归入「朋友」
        var createdId: String? = null
        if (nc != null) {
            val group = nc.relation.takeIf { it in listOf("家人", "朋友", "同事", "同学") } ?: "朋友"
            val c = buildContact(nc.name, group, note = nc.note.ifBlank { "语音新建 · 还没记录过" })
            contacts = contacts + c
            createdId = c.id
            confirmParts.add("新建 ${nc.name}")
        }

        // 操作对象：已知联系人 或 刚新建的联系人
        val personName = r?.person ?: m?.person ?: cp ?: nc?.name
        val cid = personName?.let { p -> contacts.firstOrNull { it.name == p }?.id }
        if (cid == null && confirmParts.isEmpty()) return

        val notes = mutableListOf<String>()
        val insertedIds = mutableListOf<String>()
        r?.let {
            val act = it.title.substringAfter(it.person).ifBlank { "见面" }
            notes.add("${it.date.substringBefore(" ")}$act 提醒已设")
            confirmParts.add("${it.date.substringBefore(" ")}提醒")
        }
        if (m != null && cid != null) {
            when (m.kind) {
                "collect" -> {   // 收回：只记提醒，不新增借出
                    notes.add("见面时收 ${yuan(m.amount)}")
                    confirmParts.add("收回 ${yuan(m.amount)}")
                }
                else -> {
                    moneySeq += 1
                    val rid = "nv${moneySeq}"
                    val physical = m.item.isNotBlank()
                    val record = when (m.kind) {
                        "borrow" -> MoneyRecord(id = rid, contactId = cid, type = MoneyType.Borrow,
                            amount = m.amount, date = "今天", loanStatus = LoanStatus.Unpaid)
                        "give" -> MoneyRecord(id = rid, contactId = cid, type = MoneyType.GiveGift,
                            amount = if (physical) 0 else m.amount, isPhysical = physical,
                            itemName = m.item, estValue = if (physical) m.amount else 0, date = "今天")
                        "receive" -> MoneyRecord(id = rid, contactId = cid, type = MoneyType.ReceiveGift,
                            amount = if (physical) 0 else m.amount, isPhysical = physical,
                            itemName = m.item, estValue = if (physical) m.amount else 0,
                            date = "今天", giftReturn = GiftReturn.Pending)
                        else -> MoneyRecord(id = rid, contactId = cid, type = MoneyType.Lend,
                            amount = m.amount, date = "今天", loanStatus = LoanStatus.Unpaid)
                    }
                    moneyRecords = moneyRecords + record
                    insertedIds.add(rid)
                    val what = if (physical) m.item else yuan(m.amount)
                    val verb = when (m.kind) { "borrow" -> "借入"; "give" -> "送礼"; "receive" -> "收礼"; else -> "借出" }
                    notes.add("$verb $what")
                    confirmParts.add("$verb $what")
                }
            }
        }
        // ② 标记已联系（走与首页操作单相同的路径）
        if (cp != null && cid != null) {
            flowMarkDone(cid, useRecordInputs = false)
            confirmParts.add("已联系 $cp")
        }

        val prevNotes = state.contactReminderNote
        val newNotes = if (notes.isNotEmpty() && cid != null) prevNotes + (cid to ("🔔 " + notes.joinToString(" · "))) else prevNotes
        val count = listOfNotNull(r, m, nc, cp).size
        state = state.copy(
            voiceStage = 0, voiceReminder = null, voiceMoney = null, voiceText = "",
            voiceNewContact = null, voiceContactedPerson = null,
            contactReminderNote = newNotes,
            voiceConfirm = "已记下 $count 条：" + confirmParts.joinToString(" · "),
            voiceConfirmSeq = state.voiceConfirmSeq + 1,
            voiceUndoPrevNotes = prevNotes, voiceUndoRecordIds = insertedIds,
            voiceUndoContactId = createdId
        )
    }

    /** 撤销：只回退本次保存（行副标题快照 + 本次新增的记录 + 本次新建的联系人）。 */
    fun voiceUndo() {
        val prev = state.voiceUndoPrevNotes
        if (prev != null) {
            moneyRecords = moneyRecords.filterNot { it.id in state.voiceUndoRecordIds }
            state.voiceUndoContactId?.let { cid ->
                moneyRecords = moneyRecords.filterNot { it.contactId == cid }
                contacts = contacts.filterNot { it.id == cid }
            }
            state = state.copy(
                contactReminderNote = prev, voiceConfirm = "",
                voiceUndoPrevNotes = null, voiceUndoRecordIds = emptyList(), voiceUndoContactId = null
            )
        } else {
            state = state.copy(voiceConfirm = "")
        }
    }
    fun voiceDismissConfirm() { state = state.copy(voiceConfirm = "") }

    // ---- 分组折叠 / 调整顺序 ----

    fun isGroupCollapsed(group: String): Boolean = state.collapsedGroups.contains(group)

    fun toggleGroupCollapsed(group: String) {
        val set = state.collapsedGroups.toMutableSet()
        if (!set.add(group)) set.remove(group)
        // Collapsing/expanding always dismisses the reorder arrows
        state = state.copy(collapsedGroups = set, reorderGroup = null)
    }

    /** Long-press a collapsed group to reveal/hide its move arrows. */
    fun toggleGroupReorder(group: String) {
        state = state.copy(reorderGroup = if (state.reorderGroup == group) null else group)
    }

    /**
     * Swap a group with its nearest *visible* (non-empty) neighbour in the given
     * direction. Empty custom groups are invisible on the Contacts list, so we skip
     * over them — otherwise an enabled arrow could swap with an invisible slot and
     * appear to do nothing on the first tap.
     */
    fun moveGroup(group: String, up: Boolean) {
        val order = state.groupOrder.toMutableList()
        val i = order.indexOf(group)
        if (i < 0) return
        val step = if (up) -1 else 1
        var j = i + step
        while (j in order.indices && contactsInGroup(order[j]) == 0) j += step
        if (j !in order.indices) return
        val tmp = order[i]; order[i] = order[j]; order[j] = tmp
        state = state.copy(groupOrder = order)
    }

    /** 新增自定义分组；已存在则复用。返回规范化后的分组名（空白输入返回 null）。 */
    fun addGroup(name: String): String? {
        val n = name.trim()
        if (n.isEmpty()) return null
        val existing = state.groupOrder.firstOrNull { it.equals(n, ignoreCase = true) }
        if (existing != null) return existing
        state = state.copy(groupOrder = state.groupOrder + n)
        return n
    }

    /** 该分组下的联系人数量（删除守卫用）。 */
    fun contactsInGroup(group: String): Int = contacts.count { it.group == group }

    /** 删除一个分组：仅当其下没有联系人、且不是最后一个分组时才允许。返回是否已删除。 */
    fun deleteGroup(name: String): Boolean {
        if (contactsInGroup(name) > 0) return false
        if (state.groupOrder.size <= 1) return false
        if (name !in state.groupOrder) return false
        state = state.copy(
            groupOrder = state.groupOrder.filter { it != name },
            collapsedGroups = state.collapsedGroups - name,
            reorderGroup = if (state.reorderGroup == name) null else state.reorderGroup
        )
        return true
    }

    fun availableLetters(): List<String> {
        val filtered = filteredContacts()
        return filtered.map { it.pinyin }.distinct().sorted()
    }

    // ---- Focus flow (tap the home banner → full-screen card stack) ----

    private val todayIds: List<String>
        get() = todayContacts.map { it.id }

    /** Today's still-pending contacts, in the current (possibly re-queued) order. */
    fun flowPending(): List<Contact> {
        val order = state.flowOrder.ifEmpty { todayIds }
        return order.mapNotNull { id -> todayContacts.find { it.id == id } }
            .filter { !isContacted(it.id) && !isTomorrow(it.id) }
    }

    /** 打开「专注流」整页卡片滑动，按今日队列顺序逐个处理。 */
    fun openFlow() {
        if (remainingToday == 0) return
        state = state.copy(
            screen = Screen.Flow,
            previousTab = Screen.Home,
            flowOrder = todayIds,
            flowRecordFor = null
        )
    }

    fun exitFlow() {
        state = state.copy(screen = Screen.Home, flowRecordFor = null, selectedContactId = null)
    }

    /** Left-swipe / "记一笔·已联系". [useRecordInputs] pulls chips+note+freq from the record sheet. */
    fun flowMarkDone(contactId: String, useRecordInputs: Boolean) {
        val contact = contacts.find { it.id == contactId } ?: return
        val methods = if (useRecordInputs)
            SampleData.chipOptions.filter { state.selectedChips.contains(it) }
        else emptyList()
        val note = if (useRecordInputs) state.note.trim() else ""
        val label = if (methods.isEmpty()) listOf("标记已联系") else methods
        val text = label.joinToString(" · ") + (if (note.isNotEmpty()) " — $note" else "")

        val logs = state.logMap.toMutableMap()
        val entry = HistoryEntry(date = "刚刚", text = text, methods = label, fresh = true)
        logs[contactId] = listOf(entry) + (logs[contactId] ?: emptyList())

        val done = state.doneMap.toMutableMap()
        done[contactId] = if (useRecordInputs) (state.selectedFreq ?: contact.freq) else contact.freq

        state = state.copy(
            doneMap = done,
            logMap = logs,
            tomorrowMap = state.tomorrowMap - contactId,
            flowRecordFor = null
        )
        showToast(if (useRecordInputs) "已记一笔" else "已标记已联系")
    }

    fun flowMarkTomorrow(contactId: String) {
        state = state.copy(tomorrowMap = state.tomorrowMap + contactId)
        showToast("好，明天再提醒你")
    }

    /** "这位先跳过 · 换一位" — send current card to the tail of the queue. */
    fun flowRequeue(contactId: String) {
        if (flowPending().size < 2) {
            showToast("今天只剩这一位啦")
            return
        }
        val order = state.flowOrder.ifEmpty { todayIds }
        state = state.copy(flowOrder = order.filter { it != contactId } + contactId)
        showToast("先放一放，待会儿再来")
    }

    fun flowOpenRecord(contactId: String) {
        val c = contacts.find { it.id == contactId } ?: return
        state = state.copy(
            flowRecordFor = contactId,
            selectedChips = emptySet(),
            selectedFreq = c.freq,
            note = ""
        )
    }

    fun flowCloseRecord() {
        state = state.copy(flowRecordFor = null)
    }

    fun flowSaveRecord() {
        val id = state.flowRecordFor ?: return
        flowMarkDone(id, useRecordInputs = true)
    }

    fun scriptFor(contact: Contact): String {
        if (contact.scripts.isEmpty()) return "最近怎么样？找个时间聚聚呀。"
        val idx = (state.scriptIndex[contact.id] ?: 0) % contact.scripts.size
        return contact.scripts[idx]
    }

    fun nextScript(contactId: String) {
        val idx = (state.scriptIndex[contactId] ?: 0) + 1
        state = state.copy(scriptIndex = state.scriptIndex + (contactId to idx))
    }

    fun copyScriptToast() {
        showToast("开场白已复制，去微信贴上")
    }

    private fun showToast(msg: String) {
        state = state.copy(toastText = msg, toastSeq = state.toastSeq + 1)
    }

    /** 供 UI 层触发的 toast（如账号与安全的「示意」提示）。 */
    fun showToastPublic(msg: String) = showToast(msg)

    // ---- "我" tab sub-screens ----

    fun openSettings(target: Screen) {
        state = state.copy(screen = target, previousTab = Screen.Me)
    }

    fun backToMe() {
        state = state.copy(screen = Screen.Me)
    }

    private val reminderTimeOptions = listOf("08:00", "09:00", "12:00", "20:00", "21:00")
    private val dndRangeOptions = listOf("22:00 - 08:00", "23:00 - 07:00", "00:00 - 09:00", "全天不打扰")

    fun toggleDailyReminder() {
        state = state.copy(notif = state.notif.copy(dailyReminder = !state.notif.dailyReminder))
    }

    fun cycleReminderTime() {
        val i = reminderTimeOptions.indexOf(state.notif.reminderTime)
        val next = reminderTimeOptions[(i + 1) % reminderTimeOptions.size]
        state = state.copy(notif = state.notif.copy(reminderTime = next))
    }

    fun toggleBirthdayReminder() {
        state = state.copy(notif = state.notif.copy(birthdayReminder = !state.notif.birthdayReminder))
    }

    fun toggleOverdueReminder() {
        state = state.copy(notif = state.notif.copy(overdueReminder = !state.notif.overdueReminder))
    }

    fun toggleDnd() {
        state = state.copy(notif = state.notif.copy(dndEnabled = !state.notif.dndEnabled))
    }

    fun cycleDndRange() {
        val i = dndRangeOptions.indexOf(state.notif.dndRange)
        val next = dndRangeOptions[(i + 1) % dndRangeOptions.size]
        state = state.copy(notif = state.notif.copy(dndRange = next))
    }

    /** Groups derived from contacts, in the current display order, non-empty only. */
    fun groups(): List<Pair<String, List<Contact>>> =
        state.groupOrder.mapNotNull { g ->
            val items = contacts.filter { it.group == g }
            if (items.isNotEmpty()) g to items else null
        }

    fun confirmImport(count: Int) {
        state = state.copy(importedCount = state.importedCount + count)
    }

    // ---- 5a 融合方案：行程预告 + 到达推送 + 首页横幅 ----

    // 少数几位好友的在城开场白手写；其余按城市自动生成
    private val geoScriptOverrides = mapOf(
        "zhou" to "周哥，我这两天在杭州！说好的钓鱼是不是能安排上了？",
        "lin" to "林夕，我在杭州出差，约杯手冲咖啡？顺便聊聊内推的事。",
        "lei" to "张磊，我 5 号到上海，老同学出来撮一顿？顺便看看你的新工作室。"
    )

    /** 稳定的伪距离（没有真实坐标，仅用于展示）。 */
    private fun geoPseudoDist(id: String): String {
        val km = 1.0 + kotlin.math.abs(id.hashCode()) % 120 / 10.0   // 1.0 .. 12.9
        return String.format(java.util.Locale.US, "%.1f km", km)
    }

    private fun geoCityScript(c: Contact): String =
        geoScriptOverrides[c.id]
            ?: c.scripts.firstOrNull()
            ?: "${c.name}，我这两天在${c.city}，顺路见一面？"

    /** geoFriends 从「有城市 tag 的联系人」派生（含后来新增的联系人）。 */
    val geoFriends: List<GeoFriend>
        get() = contacts.filter { it.city.isNotBlank() }.map { c ->
            GeoFriend(
                id = c.id,
                dist = geoPseudoDist(c.id),
                cityScript = geoCityScript(c),
                city = c.city,
                aliases = SampleData.aliasesFor(c.city)
            )
        }

    val geoCity = "杭州"   // 默认城市（无到达/无行程时的回退）

    private val geoMainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    fun geoContact(id: String): Contact? = contacts.find { it.id == id }

    /** 某座城市里的朋友。 */
    private fun friendsIn(city: String): List<GeoFriend> = geoFriends.filter { it.city == city }

    // 到达提醒默认开启：只有被显式关掉（== false）才算关
    private fun anyRemindIn(city: String): Boolean =
        friendsIn(city).any { state.geo.remind[it.id] != false }

    /** 日历检索用的全部城市关键词（含别名，长度≥2 避免单字误匹配）。 */
    private fun geoCityTokens(): List<String> =
        geoFriends.flatMap { listOf(it.city) + it.aliases }.filter { it.length >= 2 }.distinct()

    /** 关键词（如「hangzhou」「上海」）→ 城市展示名。 */
    private fun cityForToken(token: String): String? =
        geoFriends.firstOrNull { f -> (listOf(f.city) + f.aliases).any { it.equals(token, ignoreCase = true) } }?.city

    /** 真实定位城市名里若含某位朋友城市的别名（长度≥2），返回那座城市展示名，否则 null。 */
    private fun matchedFriendCity(gpsCity: String): String? =
        geoFriends.firstOrNull { f ->
            (listOf(f.city) + f.aliases).any { it.length >= 2 && gpsCity.contains(it, ignoreCase = true) }
        }?.city

    /** 当前聚焦的城市展示名：优先已到达城市，其次日历行程城市，最后回退默认。 */
    val arrivedCityName: String get() = state.geo.arrivedCity ?: state.geo.realTrip?.city ?: geoCity

    /** 已到达城市里的朋友（横幅 / 推送 / 落地页用）。 */
    val geoArrivedFriends: List<GeoFriend> get() = friendsIn(arrivedCityName)

    /** 日历行程城市里的朋友（行前卡片用）。 */
    val geoTripFriends: List<GeoFriend> get() = state.geo.realTrip?.let { friendsIn(it.city) } ?: emptyList()

    /** 读取真实定位 + 真实日历（后台线程），回主线程写 state。权限需已授予。 */
    fun refreshGeoData(context: android.content.Context) {
        val app = context.applicationContext
        val tokens = geoCityTokens()
        kotlin.concurrent.thread {
            val gpsCity = com.peoplenet.app.data.GeoLocator.currentCity(app)
            val calTrip = com.peoplenet.app.data.CalendarReader.findUpcomingTrip(app, tokens)
            val trip = calTrip?.let {
                val tripCity = cityForToken(it.matchedToken) ?: geoCity
                TripInfo(city = tripCity, dateLabel = it.dateLabel, friendIds = friendsIn(tripCity).map { f -> f.id })
            }
            geoMainHandler.post {
                state = state.copy(geo = state.geo.copy(currentCity = gpsCity, realTrip = trip, geoLoaded = true))
                // 只在「确切匹配到某座朋友城市」时才改写到达状态。
                // 刻意不因「定位暂时不含城市名」自动复位——反向地理编码常抖动到
                // 区名(黄浦区)/省名(江苏省)/国家级粗定位，那会误判「离开」并清空 acted。
                // 横幅可手动 ✕ 收起；进程重启也会清空（内存态）。
                if (gpsCity != null) {
                    val ac = matchedFriendCity(gpsCity)
                    when {
                        ac != null && !state.geo.arrived -> geoRealArrival(ac)             // 首次到达朋友的城市
                        ac != null && state.geo.arrived && ac != state.geo.arrivedCity -> { // 直接换到另一座朋友城市
                            resetArrival()
                            geoRealArrival(ac)
                        }
                    }
                }
            }
        }
    }

    /** 真实定位判定「到了某位朋友的城市」时触发：一次事件驱动到达推送 + 首页横幅。 */
    fun geoRealArrival(city: String) {
        if (state.geo.arrived) return
        val anyOn = anyRemindIn(city)
        state = state.copy(geo = state.geo.copy(arrived = true, arrivedCity = city, cityDismissed = false, notifShown = anyOn))
        if (!anyOn) showToast("到达提醒全关了 · 只显示首页横幅")
    }

    /** 换城时清掉上一座城市的本次到访状态（保留 remind 开关），随后对新城重新触发。 */
    private fun resetArrival() {
        state = state.copy(
            geo = state.geo.copy(
                arrived = false, arrivedCity = null, notifShown = false,
                cityDismissed = false, cityExpanded = false, acted = emptyMap()
            )
        )
    }

    /** 行前的「即将出行」卡片：日历里有匹配行程、且还没到达时显示。 */
    val showTripCard: Boolean get() = state.geo.realTrip != null && !state.geo.arrived

    /** 在城期间的蓝色横幅：到达后、且本次没被收起时显示。 */
    val showCityBanner: Boolean get() = state.geo.arrived && !state.geo.cityDismissed

    /** 已到达城市里、到达提醒开着的朋友（推送/落地页 kicker 用）。 */
    fun geoRemindNames(): String =
        geoArrivedFriends.filter { state.geo.remind[it.id] != false }
            .mapNotNull { geoContact(it.id)?.name }.joinToString("、")

    /** 已到达城市里的全部朋友名字。 */
    fun geoAllNames(): String =
        geoArrivedFriends.mapNotNull { geoContact(it.id)?.name }.joinToString("、")

    fun geoScript(id: String): String = geoFriends.find { it.id == id }?.cityScript ?: ""

    fun geoToggleRemind(id: String) {
        val m = state.geo.remind.toMutableMap()
        m[id] = !(m[id] ?: true)   // 缺省视为「开」
        state = state.copy(geo = state.geo.copy(remind = m))
    }

    /** 出发前「提前问好」（clipboard 复制在 UI 层做）。 */
    fun geoPregreet(id: String) {
        if (state.geo.pregreeted.contains(id)) return
        state = state.copy(geo = state.geo.copy(pregreeted = state.geo.pregreeted + id))
        showToast("话术已复制 · 出发前打个招呼")
    }

    /** 点开到达推送 → 城市落地页。 */
    fun geoOpenArrival() {
        state = state.copy(
            geo = state.geo.copy(notifShown = false),
            screen = Screen.CityArrival,
            previousTab = Screen.Home
        )
    }

    fun geoDismissNotif() {
        state = state.copy(geo = state.geo.copy(notifShown = false))
    }

    /** 落地页里对某位朋友的操作：greeted / met / skip。 */
    fun geoAct(id: String, kind: String) {
        val m = state.geo.acted.toMutableMap()
        m[id] = kind
        state = state.copy(geo = state.geo.copy(acted = m))
        val name = geoContact(id)?.name ?: ""
        when (kind) {
            "greeted" -> showToast("话术已复制，去微信贴上")
            "met" -> showToast("已记一笔 · 「在${arrivedCityName}约了$name」")
        }
    }

    /** 横幅右上角 ✕ / 卡片收起：本次到访不再显示横幅。 */
    fun geoDismissCity() {
        state = state.copy(geo = state.geo.copy(cityDismissed = true, cityExpanded = false))
        showToast("好，这次到访不再提示")
    }

    fun geoToggleCityExpand() {
        state = state.copy(geo = state.geo.copy(cityExpanded = !state.geo.cityExpanded))
    }

    /** 落地页底部「本次到访不再提醒」：关落地页 + 收起横幅。 */
    fun geoMute() {
        state = state.copy(geo = state.geo.copy(cityDismissed = true), screen = Screen.Home)
        showToast("好，这次到访不再提醒")
    }

    // ---- 7a 节日融合方案：两段式「先准备，再祝福」（示例节日：中秋节） ----

    val festName = "中秋节"

    // 中秋要祝福的 4 位朋友（复用 mom/zhou/lin/yu 的头像/名字）
    val festFriends = listOf(
        FestFriend("mom", "妈，中秋快乐！月饼别多吃哈，周末我回家陪你过。"),
        FestFriend("zhou", "周哥，中秋快乐！啥时候一起赏月喝两杯？"),
        FestFriend("lin", "中秋快乐！假期好好休息，别太卷了～"),
        FestFriend("yu", "宇哥中秋快乐！好久没聚，节后约顿饭？")
    )

    // 倒计时卡展开 · 按人推荐礼物（3a 手风琴 + 搜索）。商品图为占位色块，接商品库后换真图。
    val festGifts = listOf(
        GiftRec("mom", "上次聊到体检 · 低糖优先", listOf(
            Gift("月", "低糖月饼礼盒", 128, androidx.compose.ui.graphics.Color(0xFFC96F3B), androidx.compose.ui.graphics.Color(0xFFE39A63)),
            Gift("披", "羊绒披肩", 299, androidx.compose.ui.graphics.Color(0xFFB0567A), androidx.compose.ui.graphics.Color(0xFFD283A2)),
            Gift("足", "足浴按摩仪", 399, androidx.compose.ui.graphics.Color(0xFF5E7F5A), androidx.compose.ui.graphics.Color(0xFF8BAB86))
        )),
        GiftRec("zhou", "钓鱼佬 · 喜欢户外", listOf(
            Gift("渔", "渔具收纳包", 259, androidx.compose.ui.graphics.Color(0xFF3D5A80), androidx.compose.ui.graphics.Color(0xFF5E83B3)),
            Gift("茶", "老白茶礼盒", 168, androidx.compose.ui.graphics.Color(0xFF5E7F5A), androidx.compose.ui.graphics.Color(0xFF8BAB86)),
            Gift("灯", "户外露营灯", 139, androidx.compose.ui.graphics.Color(0xFF8B6F47), androidx.compose.ui.graphics.Color(0xFFB3946A))
        )),
        GiftRec("lin", "咖啡重度爱好者", listOf(
            Gift("咖", "手冲咖啡礼盒", 139, androidx.compose.ui.graphics.Color(0xFF7A5230), androidx.compose.ui.graphics.Color(0xFFA67B52)),
            Gift("杯", "保温随行杯", 109, androidx.compose.ui.graphics.Color(0xFF8367C7), androidx.compose.ui.graphics.Color(0xFFA98BE0)),
            Gift("香", "香薰蜡烛", 99, androidx.compose.ui.graphics.Color(0xFFB0567A), androidx.compose.ui.graphics.Color(0xFFD283A2))
        )),
        GiftRec("yu", "老同学 · 一起看过球", listOf(
            Gift("酿", "精酿组合装", 119, androidx.compose.ui.graphics.Color(0xFFB3823B), androidx.compose.ui.graphics.Color(0xFFD6A75F)),
            Gift("游", "聚会桌游", 149, androidx.compose.ui.graphics.Color(0xFF2A9D8F), androidx.compose.ui.graphics.Color(0xFF5BC4B5)),
            Gift("袋", "球场随身包", 99, androidx.compose.ui.graphics.Color(0xFF3D5A80), androidx.compose.ui.graphics.Color(0xFF5E83B3))
        )),
        GiftRec("yi", "上月寄了腊肠 · 回个礼", listOf(
            Gift("锅", "珐琅铸铁锅", 329, androidx.compose.ui.graphics.Color(0xFFC96F3B), androidx.compose.ui.graphics.Color(0xFFE39A63)),
            Gift("巾", "旅行真丝丝巾", 159, androidx.compose.ui.graphics.Color(0xFFB0567A), androidx.compose.ui.graphics.Color(0xFFD283A2)),
            Gift("音", "便携蓝牙音箱", 199, androidx.compose.ui.graphics.Color(0xFF8367C7), androidx.compose.ui.graphics.Color(0xFFA98BE0))
        )),
        GiftRec("lei", "篮球加摄影 · 装备控", listOf(
            Gift("摄", "相机清洁套装", 129, androidx.compose.ui.graphics.Color(0xFF3D5A80), androidx.compose.ui.graphics.Color(0xFF5E83B3)),
            Gift("充", "磁吸充电宝", 199, androidx.compose.ui.graphics.Color(0xFF4C9A8E), androidx.compose.ui.graphics.Color(0xFF6FB8AD)),
            Gift("篮", "室内训练篮球", 159, androidx.compose.ui.graphics.Color(0xFFB3823B), androidx.compose.ui.graphics.Color(0xFFD6A75F))
        )),
        GiftRec("min", "红酒和看展 · 忌花生", listOf(
            Gift("醒", "醒酒器套装", 239, androidx.compose.ui.graphics.Color(0xFFB0567A), androidx.compose.ui.graphics.Color(0xFFD283A2)),
            Gift("垫", "专业瑜伽垫", 189, androidx.compose.ui.graphics.Color(0xFF9B7BD4), androidx.compose.ui.graphics.Color(0xFFB49BE0)),
            Gift("展", "美术馆双人票", 299, androidx.compose.ui.graphics.Color(0xFF3D5A80), androidx.compose.ui.graphics.Color(0xFF5E83B3))
        )),
        GiftRec("dad", "爱钓鱼下棋 · 戒烟中", listOf(
            Gift("棋", "实木象棋礼盒", 169, androidx.compose.ui.graphics.Color(0xFF8B6F47), androidx.compose.ui.graphics.Color(0xFFB3946A)),
            Gift("架", "折叠鱼竿支架", 129, androidx.compose.ui.graphics.Color(0xFF3D5A80), androidx.compose.ui.graphics.Color(0xFF5E83B3)),
            Gift("菊", "胎菊茶礼盒", 89, androidx.compose.ui.graphics.Color(0xFF5E7F5A), androidx.compose.ui.graphics.Color(0xFF8BAB86))
        )),
        GiftRec("chen", "爱书法养生 · 高中班主任", listOf(
            Gift("笔", "善琏湖笔套装", 199, androidx.compose.ui.graphics.Color(0xFF8B6F47), androidx.compose.ui.graphics.Color(0xFFB3946A)),
            Gift("史", "历史丛书礼盒", 139, androidx.compose.ui.graphics.Color(0xFF3D5A80), androidx.compose.ui.graphics.Color(0xFF5E83B3)),
            Gift("壶", "多功能养生壶", 179, androidx.compose.ui.graphics.Color(0xFF5E7F5A), androidx.compose.ui.graphics.Color(0xFF8BAB86))
        ))
    )

    fun festContact(id: String): Contact? = contacts.find { it.id == id }
    fun festScript(id: String): String = festFriends.find { it.id == id }?.script ?: ""

    // 第一段：前 7 天倒计时准备卡；第二段：当天节日祝福卡
    val festPrepBannerOn: Boolean get() = state.fest.cdStep == 1
    val festDayBannerOn: Boolean get() = state.fest.cdStep == 2
    val festSentCount: Int get() = festFriends.count { state.fest.acted[it.id] == "sent" }
    val festHandledCount: Int get() = festFriends.count { state.fest.acted.containsKey(it.id) }
    val festAllDone: Boolean get() = festHandledCount >= festFriends.size

    /** 「▶ 模拟」两段式推进：距节日 20 天 → 前 7 天(准备) → 当天(祝福+推送)。 */
    fun festSimulate() {
        if (state.fest.cdStep >= 2) { showToast("已经是${festName}当天了，先「重置」"); return }
        val next = state.fest.cdStep + 1
        state = state.copy(fest = state.fest.copy(cdStep = next, notifShown = next == 2))
    }

    fun festReset() {
        val backHome = if (state.screen == Screen.Bless) Screen.Home else state.screen
        state = state.copy(screen = backHome, fest = FestState())
    }

    /** 打开礼物清单页（3a：手风琴 + 搜索）。返回时回到进入前的 tab（首页或「人情」）。 */
    fun festOpenGiftList() {
        val fromTab = if (state.screen in listOf(Screen.Home, Screen.People, Screen.Contacts, Screen.Me))
            state.screen else Screen.Home
        state = state.copy(screen = Screen.GiftList, previousTab = fromTab)
    }

    fun festSetGiftSearch(q: String) {
        state = state.copy(fest = state.fest.copy(giftSearch = q))
    }

    fun festToggleGiftOpen(friendId: String) {
        val s = state.fest.giftOpen
        val next = if (s.contains(friendId)) s - friendId else s + friendId
        state = state.copy(fest = state.fest.copy(giftOpen = next))
    }

    fun festIsGiftOpen(friendId: String): Boolean = state.fest.giftOpen.contains(friendId)

    /** 已加入礼物清单的总件数（页头 / 当天推送用）。 */
    val festCartCount: Int get() = state.fest.cart.size

    fun festInCart(friendId: String, index: Int): Boolean =
        state.fest.cart.contains("$friendId:$index")

    /** 某人已备的礼物件数。 */
    fun festPersonCartCount(friendId: String): Int =
        state.fest.cart.count { it.substringBefore(':') == friendId }

    fun festPersonDone(friendId: String): Boolean = festPersonCartCount(friendId) > 0

    /** 已备好（至少备了一件）的人数。 */
    val festGiftDoneCount: Int get() = festGifts.count { festPersonDone(it.friendId) }

    /** 参与礼物推荐的总人数。 */
    val festGiftPeopleCount: Int get() = festGifts.size

    /** 清单里礼物的合计金额（¥）。 */
    val festCartTotal: Int
        get() = state.fest.cart.sumOf { key ->
            val fid = key.substringBefore(':')
            val idx = key.substringAfter(':').toIntOrNull() ?: return@sumOf 0
            festGifts.firstOrNull { it.friendId == fid }?.items?.getOrNull(idx)?.price ?: 0
        }

    /** 搜索匹配：名字或关系包含关键词（空词全匹配）。 */
    fun festGiftMatches(friendId: String, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        val c = festContact(friendId) ?: return false
        return c.name.contains(q, ignoreCase = true) || c.rel.contains(q, ignoreCase = true)
    }

    /** 「加清单」：多选把礼物加入清单。 */
    fun festToggleCart(friendId: String, index: Int) {
        val key = "$friendId:$index"
        val has = state.fest.cart.contains(key)
        val next = if (has) state.fest.cart - key else state.fest.cart + key
        state = state.copy(fest = state.fest.copy(cart = next))
        if (!has) showToast("已加入礼物清单")
    }

    /** 打开祝福清单页（推送/节日卡同源入口）。 */
    fun festOpenBless() {
        state = state.copy(
            fest = state.fest.copy(notifShown = false),
            screen = Screen.Bless,
            previousTab = Screen.Home
        )
    }

    fun festDismissNotif() {
        state = state.copy(fest = state.fest.copy(notifShown = false))
    }

    /** 逐位祝福：sent（复制并发送）或 skip（跳过）。剪贴板复制在 UI 层做。 */
    fun festAct(id: String, kind: String) {
        val m = state.fest.acted.toMutableMap()
        m[id] = kind
        state = state.copy(fest = state.fest.copy(acted = m))
        if (kind == "sent") showToast("祝福已复制，去微信发给${festContact(id)?.name ?: ""}")
    }

    /** 一键群发：把还没处理的都标记为已送出（全部话术已在 UI 层复制）。 */
    fun festBatchSend() {
        val m = state.fest.acted.toMutableMap()
        festFriends.forEach { if (m[it.id] == null) m[it.id] = "sent" }
        state = state.copy(fest = state.fest.copy(acted = m))
        showToast("${festFriends.size} 条祝福都复制好了")
    }

    // ---- Add contact (联系人页右上角 +) ----

    fun openAddContact() {
        state = state.copy(screen = Screen.AddContact, previousTab = Screen.Contacts)
    }

    private var addSeq = 0

    /** 构造一个新联系人（添加页与语音「新建联系人」共用）。 */
    internal fun buildContact(name: String, group: String, note: String, freq: String = "每月", city: String = "", avatar: ImageBitmap? = null): Contact {
        val trimmed = name.trim()
        val palette = NEW_PALETTES[addSeq % NEW_PALETTES.size]
        addSeq++
        return Contact(
            id = "new_$addSeq",
            name = trimmed,
            pinyin = pinyinInitial(trimmed),
            rel = group,
            group = group,
            home = null,
            av = trimmed.take(1),
            color = palette[0],
            colorSecondary = palette[1],
            softColor = palette[2],
            due = "",
            overdue = false,
            freq = freq,
            tagline = note.trim().ifEmpty { "刚加入 · 还没记录过" },
            tags = emptyList(),
            dates = emptyList(),
            history = emptyList(),
            job = null,
            avoid = null,
            city = city.trim(),
            avatar = avatar
        )
    }

    /** Appends a new contact built from the add form and jumps back to the list. */
    fun addContact(name: String, group: String, freq: String, note: String, city: String = "", avatar: ImageBitmap? = null) {
        if (name.trim().isEmpty()) return
        contacts = contacts + buildContact(name, group, note, freq, city, avatar)
        state = state.copy(screen = Screen.Contacts, previousTab = Screen.Contacts, sortMode = SortMode.Group)
    }

    // Navigation
    fun openContact(contactId: String) {
        val currentTab = when (state.screen) {
            Screen.Home, Screen.Contacts, Screen.Me -> state.screen
            else -> state.previousTab
        }
        state = state.copy(screen = Screen.Detail, selectedContactId = contactId, previousTab = currentTab)
    }

    fun goBack() {
        state = when (state.screen) {
            Screen.Record -> state.copy(screen = Screen.Detail)
            Screen.Detail -> state.copy(screen = state.previousTab, selectedContactId = null)
            Screen.GiftList -> state.copy(screen = state.previousTab)
            Screen.People -> state.copy(screen = Screen.Me)
            Screen.Grade -> state.copy(screen = state.previousTab)   // 与 handleBack 一致，回「联系人」而非首页
            else -> state.copy(screen = Screen.Home)
        }
    }

    /** True if the top-level (tab) screen is showing — system back should NOT be intercepted. */
    val isTopLevel: Boolean
        get() = state.screen in listOf(Screen.Home, Screen.Contacts, Screen.Me)

    /** Handles the hardware / gesture back button for in-app sub-screens. */
    fun handleBack() {
        state = when (state.screen) {
            Screen.Record -> state.copy(screen = Screen.Detail)
            Screen.Detail -> state.copy(screen = state.previousTab, selectedContactId = null)
            Screen.Done, Screen.Flow -> state.copy(screen = Screen.Home, selectedContactId = null, flowRecordFor = null)
            Screen.About, Screen.Notifications, Screen.Groups, Screen.ImportContacts -> state.copy(screen = Screen.Me)
            Screen.AddContact -> state.copy(screen = Screen.Contacts)
            Screen.CityArrival -> state.copy(screen = Screen.Home)
            Screen.Bless -> state.copy(screen = Screen.Home)
            Screen.GiftList -> state.copy(screen = state.previousTab)
            Screen.People -> state.copy(screen = Screen.Me)
            Screen.AccountSecurity -> state.copy(screen = Screen.Me)
            Screen.DeleteAccount -> state.copy(screen = Screen.AccountSecurity)
            Screen.Grade -> state.copy(screen = Screen.Contacts)
            else -> state
        }
    }

    fun switchTab(tab: Screen) {
        // 切 tab 时收起语音面板/操作单，避免离开后残留或写回旧状态
        voiceStopAsr()
        state = state.copy(screen = tab, selectedContactId = null, previousTab = tab, voiceStage = 0, homeActionFor = null)
    }

    fun toRecord() {
        val contact = selectedContact ?: return
        state = state.copy(
            screen = Screen.Record,
            note = "",
            selectedChips = emptySet(),
            selectedFreq = contact.freq,
            moneyDraft = MoneyForm(),
            aiStage = 0, aiText = "", aiExcerpt = "",
            aiFreqSuggested = false, aiMoneyLinked = false
        )
    }

    fun toggleChip(label: String) {
        val chips = state.selectedChips.toMutableSet()
        if (chips.contains(label)) chips.remove(label) else chips.add(label)
        state = state.copy(selectedChips = chips)
    }

    fun pickFreq(freq: String) {
        state = state.copy(selectedFreq = freq)
    }

    fun setNote(value: String) {
        state = state.copy(note = value)
    }

    fun save() {
        val contactId = state.selectedContactId ?: return
        val methods = SampleData.chipOptions.filter { state.selectedChips.contains(it) }
        val noteText = state.note.trim()
        val text = noteText.ifEmpty { if (methods.isEmpty()) "联系了一下" else "" }

        val entry = HistoryEntry(
            date = "刚刚",
            text = text,
            methods = methods,
            fresh = true
        )
        val logs = state.logMap.toMutableMap()
        logs[contactId] = listOf(entry) + (logs[contactId] ?: emptyList())

        val done = state.doneMap.toMutableMap()
        done[contactId] = state.selectedFreq ?: "每月"

        // 记一笔里若填了金钱往来，一并落库
        commitMoneyDraftIfAny(contactId)

        // Contacting clears any "明天提醒" snooze so the two states never coexist
        state = state.copy(
            screen = Screen.Done,
            doneMap = done,
            logMap = logs,
            tomorrowMap = state.tomorrowMap - contactId,
            moneyDraft = MoneyForm()
        )
    }

    fun review() {
        state = state.copy(screen = Screen.Detail)
    }

    fun toHome() {
        state = state.copy(screen = Screen.Home, selectedContactId = null, previousTab = Screen.Home)
    }

    companion object {
        // Avatar gradients cycled through for newly-added contacts
        private val NEW_PALETTES = listOf(
            listOf(Color(0xFF6C7BE0), Color(0xFF8E9BF0), Color(0xFFE4E7FA)),
            listOf(Color(0xFFE0699A), Color(0xFFF08FB5), Color(0xFFF9E1EC)),
            listOf(Color(0xFF3FA98C), Color(0xFF5FC7A8), Color(0xFFDDF2EB)),
            listOf(Color(0xFFD98A5B), Color(0xFFE8A878), Color(0xFFF6E6D6)),
            listOf(Color(0xFF8367C7), Color(0xFFA98BE0), Color(0xFFE7DEF7)),
            listOf(Color(0xFF5A7BB5), Color(0xFF7D9BCB), Color(0xFFDCE5F2))
        )

        // Best-effort pinyin initial for the A-Z index, keyed by common Chinese surnames.
        // Latin names use their own first letter; anything unknown falls into "#".
        private val SURNAME_INITIALS: Map<Char, Char> = buildMap {
            fun put(letter: Char, chars: String) = chars.forEach { put(it, letter) }
            put('A', "安敖")
            put('B', "白包鲍毕卞别薄")
            put('C', "蔡曹岑常柴陈成程池褚崔淳")
            put('D', "戴邓狄刁丁董窦杜段")
            put('F', "范方房费冯凤封傅付樊")
            put('G', "甘高葛耿龚宫巩勾顾关郭管桂")
            put('H', "韩杭郝何贺洪侯胡花华黄霍")
            put('J', "姬嵇吉纪季贾简江姜蒋焦金荆靳景鞠")
            put('K', "康柯孔寇匡邝")
            put('L', "赖蓝郎劳乐雷冷黎李连廉梁廖林蔺凌刘柳龙娄卢鲁陆路吕罗骆")
            put('M', "马麦满毛梅孟米苗缪闵明莫牟穆")
            put('N', "倪聂宁牛钮聂")
            put('O', "欧")
            put('P', "潘庞裴彭皮平蒲濮")
            put('Q', "戚齐钱强乔秦邱丘秋裘屈曲瞿")
            put('R', "冉饶任阮")
            put('S', "沙单尚邵佘申沈盛施石史舒司宋苏孙")
            put('T', "谈谭汤唐陶滕田童涂屠")
            put('W', "万汪王危韦卫魏温文翁巫吴伍武")
            put('X', "奚习席夏冼向项萧谢辛邢熊徐许宣薛荀")
            put('Y', "严阎颜晏杨姚叶易殷尹应尤游于余俞虞郁喻袁元岳云")
            put('Z', "臧曾翟詹张章赵郑钟周朱诸祝庄卓邹祖左")
        }

        fun pinyinInitial(name: String): String {
            val c = name.trim().firstOrNull() ?: return "#"
            if (c in 'A'..'Z') return c.toString()
            if (c in 'a'..'z') return c.uppercaseChar().toString()
            return SURNAME_INITIALS[c]?.toString() ?: "#"
        }
    }
}
