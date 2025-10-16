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

### 🎨 视觉反馈机制

#### 选中状态高亮

```kotlin
// NumberPickerAdapter 中的选中状态处理
fun bind(number: Int, isSelected: Boolean, onItemSelected: (Int) -> Unit, onSelectedChange: (Int) -> Unit) {
    if (isSelected) {
        // 选中项：白色文字、36sp大小、完全透明、粗体、紫色背景
        numberText.setTextColor(itemView.context.getColor(R.color.white))
        numberText.textSize = 36f
        numberText.alpha = 1.0f
        numberText.setTypeface(null, android.graphics.Typeface.BOLD)
        itemView.setBackgroundColor(itemView.context.getColor(R.color.purple_primary))
    } else {
        // 非选中项：次要色文字、32sp大小、60%透明、普通字体、透明背景
        numberText.setTextColor(itemView.context.getColor(R.color.text_secondary))
        numberText.textSize = 32f
        numberText.alpha = 0.6f
        numberText.setTypeface(null, android.graphics.Typeface.NORMAL)
        itemView.setBackgroundColor(Color.TRANSPARENT)
    }
}
```

#### 居中对齐机制

```xml
<!-- RecyclerView 布局配置 -->
<androidx.recyclerview.widget.RecyclerView
    android:layout_width="56dp"
    android:layout_height="56dp"
    android:clipToPadding="false"
    android:paddingTop="24dp"
    android:paddingBottom="24dp" />
```

**作用**：
- `clipToPadding="false"`：允许内容在 padding 区域显示
- `paddingTop/Bottom="24dp"`：为上下方留出空间，实现居中效果

### 💡 设计优势

#### 1. 操作容错性
- **防止误操作**：轻微滑动不会改变数值，避免意外修改
- **明确意图**：只有明确的滑动操作才会改变数值

#### 2. 视觉连贯性
- **平滑动画**：所有滚动都有平滑的过渡效果
- **即时反馈**：选中状态立即更新，用户清楚知道当前选择

#### 3. 操作效率
- **快速切换**：熟练用户可以快速滑动到目标数值
- **精确定位**：自动吸附确保数值准确，无需手动微调

### 🎯 实际应用场景

#### 场景1：设置闹钟时间
**用户目标**：设置明天早上 7:30 的闹钟

**操作步骤**：
1. 打开添加任务界面，开启闹钟开关
2. 月份选择器：当前"10月"，无需修改
3. 日期选择器：从"16日"滑动到"17日"（充分滑动）
4. 小时选择器：从"15时"滑动到"7时"（大幅滑动，经过多次数值）
5. 分钟选择器：从"30分"轻微滑动后回弹，确认"30分"合适

**交互体验**：
- 每次滑动都有明确的视觉反馈
- 误操作（轻微滑动）不会影响设置
- 大幅滑动可以快速到达目标数值

#### 场景2：快速浏览可选时间
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

## 应用特性

### 核心功能
- ✅ **任务管理**：创建、编辑、删除待办任务
- ✅ **任务状态**：标记任务为已完成或进行中
- ✅ **任务筛选**：查看全部任务、进行中任务、已完成任务
- ✅ **任务详情**：包含标题和描述信息

### 闹钟功能
- ⏰ **可选闹钟**：为每个任务设置闹钟提醒
- 🕐 **自定义时间**：年月日小时分钟精确设置
- 🔔 **智能提醒**：时间到达时发送通知
- 📱 **权限管理**：自动处理Android 12+精确闹钟权限

### 用户界面
- 🎨 **Material Design**：现代化Material Design界面
- 📱 **响应式布局**：适配不同屏幕尺寸
- 🌙 **卡片式设计**：清晰的任务展示
- 🎯 **智能滑动交互**：自动吸附式时间选择器

## 技术架构

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
- `AlarmManager`：闹钟管理系统

## 应用界面

### 主要功能区域
1. **顶部筛选标签**：全部任务、进行中、已完成
2. **任务列表区域**：卡片式任务展示
3. **浮动添加按钮**：创建新任务
4. **空状态提示**：友好的空列表提示

### 任务项功能
- ✅ **复选框**：标记任务完成状态
- ✏️ **编辑按钮**：修改任务内容和闹钟
- 🗑️ **删除按钮**：删除不需要的任务
- ⏰ **闹钟图标**：显示闹钟提醒时间

### 对话框界面
- **添加/编辑任务**：输入标题和描述
- **闹钟设置**：可选的闹钟功能开关
- **时间选择器**：自定义滑动选择年月日时分

## 权限说明

应用需要以下权限：

### Android 13+ (API 33+)
- `POST_NOTIFICATIONS`：发送通知权限

### Android 12+ (API 31+)
- `SCHEDULE_EXACT_ALARM`：精确闹钟权限

### 权限处理
- 应用启动时自动请求必要权限
- 权限被拒绝时提供友好提示
- 不获取ROOT权限，确保应用安全

## 数据存储

### 数据库结构
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

### 数据操作
- 插入新任务
- 更新任务信息
- 删除任务
- 查询任务（全部/进行中/已完成/有闹钟）

## 闹钟系统

### 闹钟类型
- **精确闹钟**：Android 12+使用setExactAndAllowWhileIdle
- **系统闹钟**：Android 6.0-11使用setExactAndAllowWhileIdle
- **兼容闹钟**：Android 6.0以下使用setExact

### 闹钟管理
- **设置闹钟**：任务创建或编辑时设置
- **取消闹钟**：任务删除或完成时取消
- **更新闹钟**：任务编辑时重新设置
- **重启恢复**：设备重启后自动恢复闹钟

## 编译和运行

### 环境要求
- Android Studio Arctic Fox 或更高版本
- JDK 17
- Android SDK API 34

### 编译步骤
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

## 项目结构

```
app/
├── src/main/
│   ├── java/com/example/todolist/
│   │   ├── data/
│   │   │   ├── database/          # 数据库相关
│   │   │   └── repository/        # 数据仓库
│   │   ├── ui/
│   │   │   ├── adapter/          # RecyclerView适配器
│   │   │   ├── viewmodel/        # ViewModel
│   │   │   └── widget/           # 自定义控件
│   │   ├── alarm/                # 闹钟功能
│   │   └── MainActivity.kt       # 主活动
│   ├── res/
│   │   ├── layout/               # 布局文件
│   │   ├── drawable/             # 图标资源
│   │   ├── values/               # 字符串和颜色
│   │   └── mipmap/               # 应用图标
│   └── AndroidManifest.xml       # 应用清单
├── build.gradle                  # 应用构建配置
└── README.md                     # 项目说明
```

## 版本信息

- **版本号**：1.0
- **版本代码**：1
- **最后更新**：2025年10月

## 开发者说明

本应用遵循以下开发原则：
- 不涉及手机ROOT权限
- 使用官方推荐的架构模式
- 注重用户体验和界面美观
- 保证数据安全和隐私保护

## GitHub推送

项目已准备就绪，可以直接推送到GitHub仓库。

## 许可证

本项目仅用于学习和演示目的。