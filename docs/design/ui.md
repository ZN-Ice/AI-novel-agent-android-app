# UI设计规范

> **文档用途**：定义AI小说安卓App的界面设计标准，作为前端开发的唯一设计依据。
>
> **维护规则**：本文档为固化文档，变更需人工审批并记录版本。

---

## 一、界面原型

### 1.1 首页（小说列表）

```
┌────────────────────────────────────────┐
│  ≡  AI小说创作                    [+]  │  <- 标题栏
├────────────────────────────────────────┤
│                                        │
│  ┌──────────────────────────────────┐  │
│  │  📖 玄幻大作                     │  │  <- 小说卡片
│  │  最近编辑：2026-03-21            │  │
│  │  章节：12章 | 字数：3.2万        │  │
│  │                         [删除]   │  │
│  └──────────────────────────────────┘  │
│                                        │
│  ┌──────────────────────────────────┐  │
│  │  📖 都市传说                     │  │
│  │  最近编辑：2026-03-20            │  │
│  │  章节：5章 | 字数：1.1万         │  │
│  │                         [删除]   │  │
│  └──────────────────────────────────┘  │
│                                        │
│            (空状态提示)                 │
│         点击 + 开始创作                 │
│                                        │
├────────────────────────────────────────┤
│          🎤 语音指令提示                │  <- 底部提示栏
│     "新建小说" 开始你的创作之旅         │
└────────────────────────────────────────┘
```

**功能说明**：
- 展示所有小说列表（按最近编辑排序）
- 点击卡片进入创作空间
- 右滑或点击删除按钮触发删除确认
- 底部显示语音指令提示
- 右上角 + 按钮触发新建

### 1.2 新建小说弹窗

```
┌────────────────────────────────────────┐
│           新建小说                     │
├────────────────────────────────────────┤
│                                        │
│  小说名称                              │
│  ┌──────────────────────────────────┐  │
│  │ 请输入小说名称...                │  │  <- 输入框
│  │                           🎤     │  │  <- 语音输入按钮
│  └──────────────────────────────────┘  │
│                                        │
│  小说类型（可选）                       │
│  ┌────────┐ ┌────────┐ ┌────────┐    │
│  │  玄幻  │ │  都市  │ │  科幻  │    │
│  └────────┘ └────────┘ └────────┘    │
│                                        │
│                                        │
│     [取消]              [创建]         │
│                                        │
└────────────────────────────────────────┘
```

**功能说明**：
- 支持键盘输入和语音输入
- 小说名称必填，不超过50字
- 类型可选，用于AI生成参考
- 创建成功后自动跳转创作空间

### 1.3 删除确认弹窗

```
┌────────────────────────────────────────┐
│           ⚠️ 确认删除                  │
├────────────────────────────────────────┤
│                                        │
│  确定要删除《玄幻大作》吗？             │
│                                        │
│  删除后将保留7天，期间可恢复。          │
│  7天后将永久删除所有内容。              │
│                                        │
│     [取消]              [删除]         │
│                                        │
└────────────────────────────────────────┘
```

**功能说明**：
- 二次确认防止误删
- 明确告知软删除规则
- 语音指令"确认删除"/"取消"可操作

### 1.4 创作空间（编辑器）

```
┌────────────────────────────────────────┐
│  ←  玄幻大作 - 第三章          💾 ✓    │  <- 标题栏
├────────────────────────────────────────┤
│  第一章 天地初开                       │  <- 章节导航
│  第二章 觉醒                          │
│  第三章 突破 ●                        │  <- 当前章节
│  + 新建章节                           │
├────────────────────────────────────────┤
│                                        │
│  李明站在山顶，望着远方...             │  <- 编辑区域
│                                        │
│  "我终于突破了！"他激动地...           │
│                                        │
│  （光标闪烁）                          │
│                                        │
│                                        │
│                                        │
│                                        │
├────────────────────────────────────────┤
│  🎤              │  ✨ AI续写          │  <- 底部工具栏
│  语音指令         │  智能生成           │
└────────────────────────────────────────┘
```

**功能说明**：
- 左侧章节导航，右侧编辑区域
- 自动保存（每30秒/离开时）
- 底部语音输入和AI续写按钮
- 支持语音指令控制

---

## 二、页面流程图

```
┌─────────────────────────────────────────────────────────────────┐
│                         页面流程图                               │
└─────────────────────────────────────────────────────────────────┘

                        ┌─────────────┐
                        │   启动App   │
                        └──────┬──────┘
                               │
                               ▼
                        ┌─────────────┐
                        │    首页     │◄──────────────────────┐
                        │ (小说列表)  │                       │
                        └──────┬──────┘                       │
                               │                              │
              ┌────────────────┼────────────────┐            │
              │                │                │            │
              ▼                ▼                │            │
       ┌───────────┐    ┌───────────┐          │            │
       │ 新建小说  │    │ 点击卡片  │          │            │
       │   弹窗    │    │           │          │            │
       └─────┬─────┘    └─────┬─────┘          │            │
             │                │                │            │
             │ 输入名称       │                │            │
             │ 校验通过       │                │            │
             ▼                │                │            │
       ┌───────────┐          │                │            │
       │ 创建记录  │          │                │            │
       │ + 跳转    │──────────┤                │            │
       └───────────┘          │                │            │
                              │                │            │
                              ▼                │            │
                        ┌───────────┐          │            │
                        │ 创作空间  │          │            │
                        │  (编辑器) │          │            │
                        └─────┬─────┘          │            │
                              │                │            │
                              │ 点击返回       │            │
                              │                │            │
                              ├────────────────┼────────────┘
                              │                │
                              │ 滑动删除       │
                              ▼                │
                        ┌───────────┐          │
                        │ 删除确认  │          │
                        │   弹窗    │          │
                        └─────┬─────┘          │
                              │                │
                    ┌─────────┼─────────┐      │
                    │                   │      │
                    ▼                   ▼      │
              ┌──────────┐        ┌──────────┐ │
              │ 确认删除 │        │   取消   │─┘
              │ 软删除   │        │  返回    │
              └──────────┘        └──────────┘
```

---

## 三、组件库规范

### 3.1 小说卡片组件

```xml
<!-- list_item_novel.xml -->
<androidx.cardview.widget.CardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="@dimen/spacing_md"
    app:cardCornerRadius="@dimen/corner_md"
    app:cardElevation="2dp">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="@dimen/spacing_md">

        <!-- 小说图标 -->
        <ImageView
            android:id="@+id/iv_novel_icon"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:src="@drawable/ic_novel"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

        <!-- 小说标题 -->
        <TextView
            android:id="@+id/tv_novel_title"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginStart="@dimen/spacing_md"
            android:textAppearance="@style/TextAppearance.AppCompat.Subhead"
            android:textColor="@color/text_primary"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toEndOf="@id/iv_novel_icon"
            app:layout_constraintTop_toTopOf="parent"
            tools:text="玄幻大作" />

        <!-- 最近编辑时间 -->
        <TextView
            android:id="@+id/tv_edit_time"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginStart="@dimen/spacing_md"
            android:textAppearance="@style/TextAppearance.AppCompat.Caption"
            android:textColor="@color/text_secondary"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toEndOf="@id/iv_novel_icon"
            app:layout_constraintTop_toBottomOf="@id/tv_novel_title"
            tools:text="最近编辑：2026-03-21" />

        <!-- 统计信息 -->
        <TextView
            android:id="@+id/tv_stats"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginStart="@dimen/spacing_md"
            android:textAppearance="@style/TextAppearance.AppCompat.Caption"
            android:textColor="@color/text_secondary"
            app:layout_constraintEnd_toStartOf="@id/btn_delete"
            app:layout_constraintStart_toEndOf="@id/iv_novel_icon"
            app:layout_constraintTop_toBottomOf="@id/tv_edit_time"
            tools:text="章节：12章 | 字数：3.2万" />

        <!-- 删除按钮 -->
        <ImageButton
            android:id="@+id/btn_delete"
            android:layout_width="36dp"
            android:layout_height="36dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:src="@drawable/ic_delete"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            android:contentDescription="@string/delete_novel" />

    </androidx.constraintlayout.widget.ConstraintLayout>

</androidx.cardview.widget.CardView>
```

### 3.2 语音指示器组件

```xml
<!-- view_voice_indicator.xml -->
<androidx.constraintlayout.widget.ConstraintLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bg_voice_indicator"
    android:padding="@dimen/spacing_md">

    <!-- 语音波形动画 -->
    <com.novelapp.aiagent.voice.VoiceWaveView
        android:id="@+id/wave_view"
        android:layout_width="48dp"
        android:layout_height="48dp"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <!-- 状态文本 -->
    <TextView
        android:id="@+id/tv_status"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="@dimen/spacing_md"
        android:textAppearance="@style/TextAppearance.AppCompat.Body1"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toEndOf="@id/wave_view"
        app:layout_constraintTop_toTopOf="parent"
        tools:text="正在聆听..." />

    <!-- 提示文本 -->
    <TextView
        android:id="@+id/tv_hint"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="@dimen/spacing_md"
        android:textAppearance="@style/TextAppearance.AppCompat.Caption"
        android:textColor="@color/text_secondary"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toEndOf="@id/wave_view"
        app:layout_constraintTop_toBottomOf="@id/tv_status"
        tools:text="说出"新建小说"开始创作" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### 3.3 按钮样式

```xml
<!-- styles.xml -->
<!-- 主要按钮 -->
<style name="Widget.App.Button.Primary" parent="Widget.MaterialComponents.Button">
    <item name="android:textColor">@color/white</item>
    <item name="backgroundTint">@color/primary</item>
    <item name="cornerRadius">8dp</item>
    <item name="android:minHeight">48dp</item>
    <item name="android:textAppearance">@style/TextAppearance.AppCompat.Body1</item>
</style>

<!-- 次要按钮 -->
<style name="Widget.App.Button.Secondary" parent="Widget.MaterialComponents.Button.OutlinedButton">
    <item name="android:textColor">@color/primary</item>
    <item name="strokeColor">@color/primary</item>
    <item name="strokeWidth">1dp</item>
    <item name="cornerRadius">8dp</item>
    <item name="android:minHeight">48dp</item>
</style>

<!-- 文本按钮 -->
<style name="Widget.App.Button.Text" parent="Widget.MaterialComponents.Button.TextButton">
    <item name="android:textColor">@color/primary</item>
    <item name="android:minHeight">48dp</item>
</style>

<!-- 危险按钮 -->
<style name="Widget.App.Button.Danger" parent="Widget.MaterialComponents.Button">
    <item name="android:textColor">@color/white</item>
    <item name="backgroundTint">@color/error</item>
    <item name="cornerRadius">8dp</item>
    <item name="android:minHeight">48dp</item>
</style>
```

---

## 四、交互设计说明

### 4.1 手势交互

| 手势 | 页面 | 行为 |
|------|------|------|
| 点击 | 小说卡片 | 进入创作空间 |
| 长按 | 小说卡片 | 显示快捷菜单（删除/重命名） |
| 左滑 | 小说卡片 | 显示删除按钮 |
| 点击 | + 按钮 | 显示新建弹窗 |
| 点击 | 语音按钮 | 开始语音识别 |
| 长按 | 语音按钮 | 持续录音 |
| 下拉 | 编辑器 | 刷新章节内容 |
| 双击 | 编辑器文本 | 选中文本 |

### 4.2 动画规范

```kotlin
// 标准动画时长
const val ANIM_DURATION_SHORT = 200L    // 短动画
const val ANIM_DURATION_MEDIUM = 400L   // 中等动画
const val ANIM_DURATION_LONG = 600L     // 长动画

// 标准插值器
val INTERPOLATOR_STANDARD = FastOutSlowInInterpolator()
val INTERPOLATOR_ENTER = DecelerateInterpolator()
val INTERPOLATOR_EXIT = AccelerateInterpolator()

// 页面转场
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // 淡入淡出转场
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
}
```

### 4.3 反馈机制

| 场景 | 反馈类型 | 内容 |
|------|----------|------|
| 创建成功 | Toast | "小说创建成功" |
| 删除成功 | Snackbar | "已删除，可在此恢复" + 撤销按钮 |
| 保存成功 | 图标变化 | 标题栏保存图标变绿 |
| 语音识别中 | 动画 | 波形动画 + 状态文本 |
| 网络错误 | Dialog | 错误提示 + 重试按钮 |
| AI生成中 | 进度条 | 加载动画 + 预计时间 |

---

## 五、适配规范

### 5.1 屏幕尺寸适配

```xml
<!-- values/dimens.xml (默认) -->
<dimen name="card_width">match_parent</dimen>
<dimen name="editor_font_size">16sp</dimen>

<!-- values-sw320dp/dimens.xml (小屏手机) -->
<dimen name="card_width">match_parent</dimen>
<dimen name="editor_font_size">14sp</dimen>

<!-- values-sw600dp/dimens.xml (平板) -->
<dimen name="card_width">400dp</dimen>
<dimen name="editor_font_size">18sp</dimen>
```

### 5.2 横竖屏适配

```xml
<!-- values-land/dimens.xml (横屏) -->
<dimen name="editor_padding">48dp</dimen>

<!-- layout-land/activity_editor.xml -->
<!-- 横屏使用双栏布局 -->
```

### 5.3 深色模式适配

```xml
<!-- values/colors.xml -->
<color name="background">#FAFAFA</color>
<color name="text_primary">#212121</color>

<!-- values-night/colors.xml -->
<color name="background">#121212</color>
<color name="text_primary">#FFFFFF</color>
```

---

## 六、版本记录

| 版本 | 日期 | 变更内容 | 审批人 |
|------|------|----------|--------|
| v1.0.0 | 2026-03-21 | 初始化UI设计规范 | - |

---

**维护者**：AI小说安卓App研发团队
**相关文档**：[AGENTS.md](../AGENTS.md) | [api.md](./api.md) | [voice.md](./voice.md)
