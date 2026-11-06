# 浏览器交互原型

直接打开 [ink-calendar-preview.html](ink-calendar-preview.html)，无需安装依赖或启动服务。

原型与 Android 主题分支共用五套风格配方、七组字体及离线历法数据，可预览设置、主题、字号、屏幕方向、周/月视图、横屏布局和桌面展示模式。支持示例事项及纪念日的新增、修改、完成与删除，未来事项支持 7 天 / 30 天范围。

默认保留原有月视图框架；周视图仅替换日历区域。“对半双栏”需在设置中主动选择，仅横屏生效。字体卡片和主题缩略图随方向自适应。

预览日期固定为 2026-11-07。示例数据仅保存在当前浏览器，不连接设备数据库；全屏、唤醒和屏幕方向在此仅模拟设置状态，实际设备行为以 Android 应用为准。

## 修改

- `ink-calendar-preview.html`：页面入口与预览工具栏。
- `ink-calendar-preview.css`：布局与控件样式。
- `ink-calendar-preview.js`：风格、草稿保存和示例交互。
- `browser-state.js`：浏览器本地存储。

更新原生历法数据后运行 `python preview/generate_calendar_data.py`。修改字体或新增界面文字后运行 `python preview/build_fonts.py`（需要 `fonttools` 和 `brotli`）。`export_inline.py` 可将相同源码组合为单文件预览片段。

字体许可证保留在 `fonts.css`；历法数据许可见 `LICENSE-calendar-data.txt`；离线图标为 Lucide 1.17.0，许可见 `vendor/lucide-LICENSE`。
