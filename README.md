# TodoList Android应用

一个简洁优雅的TodoList Android应用，采用现代Material Design设计风格。

## 功能特性

### 📝 任务管理
- ✅ 添加新任务（标题和描述）
- ✅ 标记任务完成/未完成
- ✅ 任务完成后自动显示删除线
- ✅ 实时任务统计（进行中/已完成）
- ✅ 任务数据本地持久化存储

### 🎨 界面设计
- 🌙 现代暗色主题
- 🎨 紫色渐变配色方案
- 📱 Material Design组件
- 🔄 流畅的动画效果
- 📊 实时数据展示

### 💾 技术特性
- 🗄️ Room数据库本地存储
- 🔄 MVVM架构模式
- ⚡ Kotlin协程异步处理
- 📱 响应式UI设计
- 🎯 ViewBinding类型安全

## 技术栈

- **开发语言**: Kotlin
- **JDK版本**: JDK 17
- **架构模式**: MVVM + Repository
- **数据库**: Room Database
- **UI框架**: Material Design Components
- **异步处理**: Kotlin Coroutines
- **数据绑定**: ViewBinding
- **最低SDK**: API 31 (Android 12)
- **目标SDK**: API 36 (Android 14)

## 项目结构

```
app/src/main/java/com/example/todolist/
├── data/
│   ├── database/
│   │   ├── Task.kt                 # 任务实体类
│   │   ├── TaskDao.kt             # 数据访问对象
│   │   └── TodoDatabase.kt        # Room数据库
│   └── repository/
│       └── TaskRepository.kt      # 数据仓库层
├── ui/
│   ├── adapter/
│   │   └── TaskAdapter.kt         # RecyclerView适配器
│   └── viewmodel/
│       └── TaskViewModel.kt       # 视图模型
└── MainActivity.kt                # 主活动
```

## 主要功能说明

### 1. 添加任务
- 点击右下角悬浮按钮
- 弹出添加任务对话框
- 输入任务标题（必填）和描述（可选）
- 点击"添加"按钮保存任务

### 2. 完成任务
- 点击任务左侧的复选框
- 已完成任务显示删除线效果
- 文字透明度降低，表示完成状态
- 实时更新任务统计数据

### 3. 界面展示
- 顶部显示"My Tasks"标题和当前日期
- 右上角显示进行中和已完成任务数量
- 任务列表按创建时间倒序排列
- 空状态提示界面

## 构建要求

### 环境要求
- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 17
- Android SDK API 36
- Kotlin 2.0.21

### 构建步骤
1. 克隆项目到本地
2. 确保JDK 17为全局默认版本：
   ```bash
   export JAVA_HOME=/path/to/jdk17
   export PATH=$JAVA_HOME/bin:$PATH
   ```
3. 在Android Studio中打开项目
4. 等待Gradle同步完成
5. 点击运行按钮构建并安装应用

## 主要依赖

- **Room Database**: 数据持久化
- **Lifecycle ViewModel**: MVVM架构支持
- **Material Components**: UI组件库
- **RecyclerView**: 列表展示
- **Coroutines**: 异步任务处理

## 应用截图

应用界面采用紫色渐变主题，包含：
- 渐变色标题栏
- 任务统计展示
- 圆角任务卡片
- 悬浮添加按钮

## 版本信息

- **版本号**: 1.0.0
- **版本代码**: 1
- **最后更新**: 2024年

## 开发者

按照要求，本项目使用JDK 17开发，不涉及任何需要手机root权限的功能，适合在GitHub上开源分享。

## 许可证

本项目采用MIT许可证开源。