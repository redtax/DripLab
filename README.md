# 滴落间Lab（DripLab）

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.02-4285F4?logo=android)](https://developer.android.com/compose)
[![AGP](https://img.shields.io/badge/AGP-8.2.2-3DDC84?logo=android)](https://developer.android.com/studio)

> 专为咖啡爱好者打造的多方法咖啡冲煮辅助工具 — 精准掌控每一杯咖啡。

---

## 📱 项目简介

滴落间Lab（DripLab）是一款聚焦 **Android** 单平台的咖啡冲煮辅助应用。MVP 阶段以**手冲咖啡**为核心功能完整实现，同时预留法压壶、爱乐压、摩卡壶、冷萃、虹吸壶等扩展入口。

帮助用户精准控制冲煮参数、掌握注水/浸泡节奏、记录冲煮过程，让每一杯咖啡都能稳定呈现理想风味。

---

## ✨ 核心功能

### ☕ 手冲咖啡（MVP 核心）
- **参数设定** — 粉重、水粉比、水温、注水量，齿轮式滑块精准调节
- **分步注水** — 闷蒸 → 分段注水 → 等待滴滤，环形计时器实时显示进度
- **自动计时** — 按配方自动切换阶段，支持暂停 / 跳过 / 重来
- **振动/声音提示** — 4 种提示模式可选（静音 / 轻柔 / 标准 / 独享）

### 📋 配方管理
- **预设配方** — 内置一刀流、三段式、四段式、冠军方案四套经典配方
- **自定义配方** — 自由创建和编辑冲煮配方
- **JSON 导入/导出** — 分享和备份个人配方

### 📝 冲煮笔记
- 自动记录每次冲煮的参数、耗时和阶段数据
- 风味评分

### 🎨 主题系统
- 5 套主题配色：经典暖棕 / 深焙暗黑 / 薄荷冷萃 / 燕麦拿铁 / 摩卡渐变
- 支持深色模式切换

---

## 🏗️ 技术架构

本项目采用 **Clean Architecture** 三层架构 + **MVVM** 模式：

```
┌─────────────────────────────────────────────┐
│                   UI Layer                   │
│  Compose Screens + ViewModels + Navigation  │
├─────────────────────────────────────────────┤
│                 Domain Layer                 │
│    Models (Recipe, BrewPhase, AlertMode)    │
│    Repository Interfaces                    │
├─────────────────────────────────────────────┤
│                 Data Layer                   │
│  Room Database + DAO + Repository Impl      │
└─────────────────────────────────────────────┘
```

### 技术栈

| 类别 | 技术 | 版本 |
|---|---|---|
| 语言 | Kotlin | 1.9.22 |
| UI | Jetpack Compose | BOM 2024.02 |
| 构建 | Gradle + AGP | 8.2.2 |
| 数据库 | Room | 2.6.1 |
| DI | Hilt | 2.50 |
| 导航 | Navigation Compose | 2.7.7 |
| 序列化 | Gson | 2.10.1 |
| 编译 | KSP | 1.9.22-1.0.17 |

---

## 📂 项目结构

```
DripLab/
├── app/src/main/java/com/driplab/app/
│   ├── DripLabApplication.kt          # @HiltAndroidApp 入口
│   ├── MainActivity.kt                # 主 Activity + NavHost
│   │
│   ├── core/                          # 核心基础层
│   │   ├── theme/                     # 5 套配色主题
│   │   │   ├── Color.kt               # DripTheme 枚举 + DripColors
│   │   │   ├── Theme.kt               # DripLabTheme Composable
│   │   │   └── Type.kt                # Typography 定义
│   │   ├── calculator/
│   │   │   └── BrewCalculator.kt      # 粉水比/水温计算
│   │   └── timer/
│   │       └── BrewTimer.kt           # 冲煮状态机
│   │
│   ├── domain/                        # 领域层
│   │   ├── model/
│   │   │   ├── Enums.kt               # BrewMethod, BrewPhase, AlertMode
│   │   │   ├── Recipe.kt / RecipeStep.kt
│   │   │   └── BrewNote.kt
│   │   └── repository/                # 仓储接口
│   │
│   ├── data/                          # 数据层
│   │   ├── repository/                # 仓储实现
│   │   ├── PresetRecipeInitializer.kt # 预设配方初始化
│   │   └── core/database/             # Room 数据库
│   │       ├── DripLabDatabase.kt
│   │       ├── entity/                # RecipeEntity, RecipeStepEntity, BrewNoteEntity
│   │       └── dao/                   # RecipeDao, BrewNoteDao
│   │
│   ├── di/
│   │   └── AppModule.kt               # Hilt DI 模块
│   │
│   └── ui/                            # 表现层
│       ├── navigation/
│       │   └── DripLabNavigation.kt   # 5 Tab 底部导航
│       ├── components/
│       │   ├── GearSlider.kt          # 齿轮式圆弧滑块
│       │   └── CircularTimer.kt       # 环形渐变计时器
│       ├── home/HomeScreen.kt         # 首页 6 宫格方法选择
│       ├── pourover/PourOverScreen.kt # 手冲咖啡主页
│       ├── recipe/RecipeScreen.kt     # 配方管理
│       ├── note/NoteScreen.kt         # 冲煮笔记
│       └── settings/SettingsScreen.kt # 主题/提示设置
│
├── build.gradle.kts                   # 根构建配置
├── settings.gradle.kts
└── gradle.properties
```

---

## 🚀 构建与运行

### 环境要求
- **Android Studio** Hedgehog (2023.1.1) 或更新版本
- **JDK** 17
- **Android SDK** API 34

### 克隆项目

```bash
git clone https://github.com/redtax/DripLab.git
```

### 在 Android Studio 中运行

1. `File → Open` → 选择项目目录
2. 等待 Gradle 同步完成
3. 选择运行设备（模拟器或真机）
4. 点击 `Run` 或按 `Ctrl + F9`

### 命令行构建

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```

---

## 🧪 测试

```bash
# 单元测试
./gradlew test

# 插桩测试
./gradlew connectedAndroidTest
```

---

## 📐 手冲冲煮状态机

```
IDLE → BLOOM(闷蒸) → POUR(第一次注水) → POUR(第二次) → WAIT(滴滤) → COMPLETE
                                 ↓ 暂停/跳过/停止 随时可用
```

---

## 🎯 路线图

- [x] MVP：手冲咖啡完整流程
- [x] 配方管理 + 预设配方
- [x] 5 套配色主题
- [x] 4 种提示模式
- [ ] 冲煮笔记导出
- [ ] 法压壶冲煮方法
- [ ] 爱乐压冲煮方法
- [ ] 冷萃计时
- [ ] 数据备份与云同步
- [ ] 冲煮曲线可视化
- [ ] 社区配方分享
- [ ] iOS 版本

---

## 📄 License

MIT © DripLab Team