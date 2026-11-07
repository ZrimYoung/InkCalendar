# 浏览器交互原型

直接打开 [ink-calendar-preview.html](ink-calendar-preview.html)，无需安装依赖或启动服务。也可运行 Web 后访问 `/?demo=1`。

原型与 Web 共用 `web/src/` 的界面、主题和业务规则，支持设置草稿、五套风格、七组字体、周/月视图、横屏布局、示例事项及纪念日操作。日期固定为 2026-11-07，示例数据与正式 Web 日历分开保存。

默认保留原有月视图框架；周视图仅替换日历区域。“对半双栏”是单独的横屏选项。字体采用 Aa 大卡片，主题缩略图随方向调整比例。

全屏、唤醒及屏幕方向在原型中仅模拟设置状态。原型采用界面文字的字体子集；正式 Web 使用完整字体，可显示用户输入的其他文字。

## 修改与生成

修改 `web/src/ui/` 的界面、`web/src/domain/` 的规则或 `shared/calendar-contract.json` 的主题配置，再运行：

```sh
npm --prefix web ci
npm --prefix web run build
```

`ink-calendar-preview.js` 和 `.css` 是生成文件，请勿直接编辑。Android、Web 与原型的共同维护方式见[双端说明](../docs/platform-parity.md)。

新增界面文字或字体后，运行 `python preview/build_fonts.py` 更新原型字体子集（需要 `fonttools` 和 `brotli`）。`python preview/export_inline.py OUTPUT.html` 可生成单文件预览片段。

字体许可保留在 `fonts.css`；历法数据许可见 `LICENSE-calendar-data.txt`；图标许可见 `vendor/lucide-LICENSE`。
