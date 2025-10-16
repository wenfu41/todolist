# TodoList 待办事项应用

一个功能完整的 Android 待办事项管理应用，支持任务管理、闹钟提醒和**创新的自动吸附式时间选择器**。

## 🌟 核心特色：自动吸附式时间选择器

本应用的核心亮点是实现了**智能滑动交互的时间选择器**，提供了流畅自然的用户体验。

### 🎯 交互设计理念

时间选择器采用**自动吸附机制**，确保用户操作精确且直观：
- **轻量滑动**：自动回弹到原位置
- **充分滑动**：自动切换到下一个数字
- **视觉反馈**：选中项高亮显示并居中对齐

### 📱 详细交互场景示例

#### 场景1：轻微滑动（自动回弹）

**用户操作**：
- 用户在月份选择器上从"3月"轻轻向下滑动到"3月"和"4月"之间的中间位置
- 滑动距离约为半个数字的高度

**系统响应**：
1. **滚动过程**：RecyclerView 显示"3月"和"4月"部分重叠的中间状态
2. **用户松手**：`LinearSnapHelper` 自动计算距离最近的项
3. **自动吸附**：因为距离"3月"更近，自动回弹到"3月"的完整位置
4. **状态更新**：滚动状态变为 `SCROLL_STATE_IDLE`，触发回调
5. **数值保持**：`currentMonth` 仍然是 3，没有变化

**用户体验**：感觉像"橡皮筋"效果，轻微的误操作不会导致数值改变。

#### 场景2：充分滑动（切换数字）

**用户操作**：
- 用户在小时选择器上从"15时"向下滑动，超过"15时"和"16时"之间的中点
- 滑动距离超过半个数字的高度

**系统响应**：
1. **滚动过程**：RecyclerView 显示"15时"和"16时"，"16时"占据更多显示区域
2. **用户松手**：`LinearSnapHelper` 检测到"16时"距离更近
3. **自动吸附**：平滑滚动到"16时"的完整居中位置
4. **状态更新**：滚动停止，触发 `SCROLL_STATE_IDLE` 回调
5. **数值更新**：
   ```kotlin
   val snappedView = hourSnapHelper.findSnapView(layoutManager) // 获取吸附到的视图
   val position = layoutManager?.getPosition(snappedView) ?: 0  // 获取位置
   currentHour = position  // 更新当前小时（如：15 → 16）
   notifyDateTimeChanged() // 通知时间变化
   ```

**用户体验**：流畅的数字切换，明确的操作反馈。

#### 场景3：连续滑动操作

**用户操作**：
- 用户在分钟选择器上快速连续滑动：从"30分" → "31分" → "32分" → "33分"
- 每次滑动都超过了切换阈值

**系统响应**：
1. **第一次滑动**：
   - "30分" → "31分"（吸附成功）
   - 触发 `currentMinute = 31` 更新

2. **第二次滑动**：
   - "31分" → "32分"（吸附成功）
   - 触发 `currentMinute = 32` 更新

3. **第三次滑动**：
   - "32分" → "33分"（吸附成功）
   - 触发 `currentMinute = 33` 更新

**关键机制**：每次滚动停止都会独立处理，确保数值准确性。

### 🔧 技术实现原理

#### 1. LinearSnapHelper 自动吸附机制

```kotlin
// 每个选择器都有独立的 SnapHelper 实例
val monthSnapHelper = LinearSnapHelper()
monthSnapHelper.attachToRecyclerView(monthPicker)
```

**作用**：`LinearSnapHelper` 是 Android 提供的辅助类，能够自动将 RecyclerView 吸附到最近的视图项。

#### 2. 滚动状态监听

```kotlin
addOnScrollListener(object : RecyclerView.OnScrollListener() {
    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        // 只在滚动停止时处理
        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            val snappedView = monthSnapHelper.findSnapView(layoutManager)
            val position = snappedView?.let { layoutManager?.getPosition(it) } ?: 0
            // 更新选中状态和数值
            monthAdapter.setSelectedPosition(position)
            currentMonth = position + 1
            notifyDateTimeChanged()
        }
    }
})
```

**关键点**：只在 `SCROLL_STATE_IDLE` 状态（滚动完全停止）时才处理数值更新。

#### 3. 初始化顺序控制

```kotlin
fun setDateTime(timeInMillis: Long) {
    isInitializing = true  // 防止初始化时触发监听器
    try {
        // 设置时间值
        currentYear = calendar.get(Calendar.YEAR)
        currentMonth = calendar.get(Calendar.MONTH) + 1
        // ... 设置其他值

        // 先滚动到目标位置
        monthPicker.scrollToPosition(currentMonth - 1)

        post {
            // 后设置选中状态和监听器
            monthAdapter.setSelectedPosition(currentMonth - 1)
        }
    } finally {
        // 延迟重置标志
        postDelayed({ isInitializing = false }, 500)
    }
}
```

**关键机制**：`isInitializing` 标志防止初始化过程中意外触发数值变化。

#### 4. 选中状态高亮显示

```kotlin
// Adapter 中的绑定逻辑
fun bind(number: Int, isSelected: Boolean, onItemSelected: (Int) -> Unit) {
    if (isSelected) {
        // 选中项样式：白色文字、36sp、粗体、紫色背景
        numberText.setTextColor(Color.WHITE)
        numberText.textSize = 36f
        numberText.setTypeface(null, Typeface.BOLD)
        itemView.setBackgroundColor(Color.parseColor("#6200EE"))
    } else {
        // 非选中项样式：灰色文字、32sp、普通字体、透明背景
        numberText.setTextColor(Color.parseColor("#757575"))
        numberText.textSize = 32f
        numberText.setTypeface(null, Typeface.NORMAL)
        itemView.setBackgroundColor(Color.TRANSPARENT)
    }
}
```

#### 5. 居中对齐布局

```xml
<androidx.recyclerview.widget.RecyclerView
    android:layout_width="56dp"
    android:layout_height="56dp"
    android:clipToPadding="false"
    android:paddingTop="24dp"
    android:paddingBottom="24dp" />
```

**设计原理**：
- `clipToPadding="false"`：允许内容在 padding 区域显示
- 上下各 24dp padding：为选中项提供居中空间

### 🎨 视觉反馈机制

#### 选中项高亮效果
- **背景颜色**：紫色 (`#6200EE`)
- **文字颜色**：白色 (`#FFFFFF`)
- **字体大小**：36sp（比非选中项大 4sp）
- **字体样式**：粗体

#### 非选中项样式
- **背景颜色**：透明
- **文字颜色**：灰色 (`#757575`)
- **字体大小**：32sp
- **字体样式**：普通

#### 滑动动画
- **吸附动画**：使用 `smoothScrollToPosition()` 实现平滑过渡
- **状态切换**：选中项变化时立即更新视觉状态
- **居中效果**：确保选中项始终在视图中心

### 💡 设计优势

#### 1. 操作容错性
- **防止误操作**：轻微滑动不会改变数值
- **明确意图**：只有充分的滑动操作才会改变数值
- **橡皮筋效果**：小幅度滑动自动回弹

#### 2. 视觉连贯性
- **平滑动画**：所有滚动都有平滑的过渡效果
- **即时反馈**：选中状态立即更新，用户清楚知道当前选择
- **视觉层次**：选中项突出显示，非选中项相对淡化

#### 3. 操作效率
- **快速切换**：熟练用户可以快速滑动到目标数值
- **精确定位**：自动吸附确保数值准确，无需手动微调
- **批量操作**：支持连续滑动操作

### 🎯 实际应用场景

#### 场景1：设置明天早上7:30的闹钟
**操作步骤**：
1. 打开添加任务界面，开启闹钟开关
2. 月份选择器：当前"10月"，无需修改
3. 日期选择器：从"16日"滑动到"17日"（充分滑动）
4. 小时选择器：从"15时"滑动到"7时"（大幅滑动）
5. 分钟选择器：轻微滑动后回弹，确认"30分"合适

**交互体验**：
- 每次滑动都有明确的视觉反馈
- 误操作（轻微滑动）不会影响设置
- 大幅滑动可以快速到达目标数值

#### 场景2：快速浏览时间选项
**用户目标**：查看可用的分钟选项
**操作步骤**：
1. 在分钟选择器上连续轻滑
2. 观察"00分"、"05分"、"10分"等选项
3. 每次轻微滑动都自动回弹，不会改变当前设置
4. 找到合适的数值后进行充分滑动确认选择

**交互体验**：
- 浏览时不会意外修改当前设置
- 确认选择时有明确的操作区分

---

## 📱 应用特性

### 核心功能
- ✅ **任务管理**：创建、编辑、删除待办任务
- ✅ **任务状态**：标记任务为已完成或进行中
- ✅ **任务筛选**：查看全部任务、进行中、已完成任务
- ✅ **任务详情**：包含标题和描述信息

### 闹钟功能
- ⏰ **可选闹钟**：为每个任务设置闹钟提醒
- 🕐 **自定义时间**：年月日小时分钟精确设置
- 🔔 **智能提醒**：时间到达时发送通知
- 🎵 **内置音频**：使用10秒音频文件确保可靠响铃
- 📳 **振动提醒**：振动1秒，停止0.3秒，循环模式
- 🎛️ **自动停止**：10秒后自动停止，避免过度干扰

### 用户界面
- 🎨 **Material Design**：现代化Material Design界面
- 📱 **响应式布局**：适配不同屏幕尺寸
- 🌙 **卡片式设计**：清晰的任务展示
- 🎯 **智能滑动交互**：自动吸附式时间选择器

## 🛠️ 技术架构

### 开发环境
- **语言**：Kotlin
- **JDK版本**：OpenJDK 17
- **最低SDK**：API 21 (Android 5.0)
- **目标SDK**：API 34 (Android 14)

### 架构模式
- **MVVM架构**：Model-View-ViewModel设计模式
- **Room数据库**：本地数据持久化存储
- **LiveData**：响应式数据观察
- **ViewBinding**：类型安全的视图绑定
- **协程**：异步任务处理

### 主要组件
- `MainActivity`：主活动界面
- `TaskViewModel`：任务数据管理
- `TaskRepository`：数据仓库层
- `TaskDao`：数据库访问对象
- `TaskAdapter`：RecyclerView适配器
- `DateTimePicker`：自定义时间选择器
- `AlarmSoundManager`：闹钟音频管理
- `AlarmReceiver`：闹钟广播接收器

## 🔧 构建和运行

### 环境要求
- Android Studio Arctic Fox 或更高版本
- JDK 17
- Android SDK API 34

### 构建步骤
1. 克隆项目到本地
2. 设置全局JDK环境变量：
   ```bash
   export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
   ```
3. 在项目根目录执行：
   ```bash
   ./gradlew assembleDebug
   ```
4. 安装到设备或模拟器：
   ```bash
   ./gradlew installDebug
   ```

## 📁 项目结构

```
app/
├── src/main/
│   ├── java/com/example/todolist/
│   │   ├── data/
│   │   │   ├── database/          # 数据库相关
│   │   │   │   ├── AppDatabase.kt
│   │   │   │   ├── TaskDao.kt
│   │   │   │   └── Converters.kt
│   │   │   └── repository/        # 数据仓库
│   │   │       └── TaskRepository.kt
│   │   ├── ui/
│   │   │   ├── adapter/          # RecyclerView适配器
│   │   │   │   └── TaskAdapter.kt
│   │   │   └── widget/           # 自定义控件
│   │   │       ├── DateTimePicker.kt
│   │   │       └── adapter/
│   │   │           └── NumberPickerAdapter.kt
│   │   ├── alarm/                # 闹钟功能
│   │   │   ├── AlarmReceiver.kt
│   │   │   ├── AlarmSoundManager.kt
│   │   │   ├── AlarmDismissReceiver.kt
│   │   │   └── AlarmSnoozeReceiver.kt
│   │   └── MainActivity.kt       # 主活动
│   ├── res/
│   │   ├── layout/               # 布局文件
│   │   │   ├── activity_main.xml
│   │   │   ├── item_task.xml
│   │   │   └── dialog_add_task.xml
│   │   ├── values/               # 字符串和颜色
│   │   │   ├── colors.xml
│   │   │   ├── strings.xml
│   │   │   └── themes.xml
│   │   ├── drawable/             # 图标资源
│   │   └── raw/                  # 原始资源
│   │       └── alarm_sound.wav   # 闹钟音频文件
│   └── AndroidManifest.xml       # 应用清单
├── build.gradle                  # 应用构建配置
└── README.md                     # 项目说明
```

## 📊 数据库设计

### 任务表结构
```kotlin
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,                    // 任务标题
    val description: String,              // 任务描述
    val isCompleted: Boolean = false,     // 完成状态
    val createdAt: Long,                  // 创建时间
    val completedAt: Long? = null,        // 完成时间
    val alarmTime: Long? = null,          // 闹钟时间
    val hasAlarm: Boolean = false         // 是否有闹钟
)
```

## 🔐 权限说明

应用需要以下权限：

### Android 13+ (API 33+)
- `POST_NOTIFICATIONS`：发送通知权限

### Android 12+ (API 31+)
- `SCHEDULE_EXACT_ALARM`：精确闹钟权限

### 其他权限
- `VIBRATE`：振动权限
- `WAKE_LOCK`：防止设备休眠
- `RECEIVE_BOOT_COMPLETED`：开机自启动

**注意**：应用已优化权限请求，只在需要时才请求权限，避免干扰用户体验。

## 🎵 闹钟音频

### 音频文件信息
- **文件名**：alarm_sound.wav
- **时长**：10秒
- **文件大小**：1.7MB
- **格式**：WAV (PCM 16-bit, 44.1kHz, 立体声)

### 音频特性
- **循环播放**：音频结束后自动重新开始
- **自动停止**：10秒后通过定时器自动停止
- **振动同步**：音频播放时同步振动提醒
- **兼容性好**：使用标准WAV格式，支持所有Android设备

## 🎯 用户操作指南

### 添加任务
1. 点击右下角的浮动按钮
2. 输入任务标题（必填）
3. 输入任务描述（可选）
4. 如需闹钟提醒，开启"设置闹钟提醒"开关
5. 在时间选择器中滑动选择闹钟时间
6. 点击"保存"按钮

### 时间选择器操作
- **轻微滑动**：滑动距离小于半个数字高度时会自动回弹
- **充分滑动**：滑动距离超过半个数字高度时会切换到相邻数字
- **快速滑动**：可以快速滑动到目标数值
- **精确选择**：松手后自动吸附到最近的数字

### 管理任务
- **标记完成**：点击任务左侧的复选框
- **编辑任务**：点击任务卡片的编辑按钮
- **删除任务**：点击任务卡片的删除按钮
- **查看详情**：点击任务卡片查看完整信息

### 闹钟操作
- **关闭闹钟**：点击通知中的"关闭"按钮
- **稍后提醒**：点击通知中的"稍后提醒"按钮（5分钟后再次提醒）

## 🐛 常见问题

### Q: 为什么时间选择器会自动回弹？
A: 这是设计的防误操作机制。当滑动距离小于阈值时，系统认为这是误操作，会自动回弹到原位置。

### Q: 如何精确选择时间？
A: 进行充分滑动，确保滑动距离超过半个数字的高度，这样就会切换到下一个数字。

### Q: 闹钟为什么不响？
A: 请检查以下设置：
1. 确保闹钟开关已开启
2. 检查设备音量是否开启
3. 确认应用有通知权限
4. 检查设备是否处于静音模式

### Q: 可以自定义闹钟铃声吗？
A: 目前使用内置的10秒音频文件，如需自定义，请替换 `res/raw/alarm_sound.wav` 文件。

## 📈 性能优化

### 文件大小优化
- **音频文件**：从50MB优化到1.7MB（减少96.6%）
- **APK体积**：通过资源优化和代码混淆减小APK大小
- **内存使用**：及时释放音频和振动资源，避免内存泄漏

### 代码优化
- **架构简化**：移除复杂的前台服务，直接在广播接收器中处理
- **权限优化**：按需请求权限，减少用户干扰
- **布局优化**：使用ViewBinding替代findViewById，提高性能

## 🔄 版本历史

### v1.0 (当前版本)
- ✅ 完整的任务管理功能
- ✅ 创新的自动吸附式时间选择器
- ✅ 可靠的闹钟提醒系统
- ✅ 优化的音频文件大小
- ✅ 简化的权限管理
- ✅ Material Design界面

## 🤝 贡献指南

欢迎提交Issue和Pull Request来改进这个项目。

### 开发规范
- 遵循Kotlin编码规范
- 使用Material Design设计规范
- 添加适当的注释和文档
- 确保向后兼容性

## 📄 许可证

本项目采用 MIT 许可证。详见 LICENSE 文件。

## 👨‍💻 开发者

本项目由 Claude Code Assistant 开发维护。

---

## 🌟 特色功能总结

1. **智能时间选择器**：创新的自动吸附机制，提供流畅的用户体验
2. **可靠闹钟系统**：使用内置音频文件，确保闹钟正常工作
3. **精确时间控制**：10秒自动停止，既有效提醒又不过度干扰
4. **权限优化**：按需请求，减少用户干扰
5. **性能优化**：文件大小优化，代码结构简洁
6. **用户友好**：直观的界面设计，详细的操作反馈

**这是一个功能完整、设计精美的待办事项应用，特别适合需要时间管理和任务提醒的用户使用。**