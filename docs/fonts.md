# 字体

应用默认内置 Noto Serif SC Regular，来自 Noto CJK 官方仓库的简体中文静态 OpenType 版本，使用 SIL Open Font License 1.1。字体文件保持原样，以避免旧安卓依赖可变字体支持。

- [字体来源](https://github.com/notofonts/noto-cjk/tree/main/Serif/SubsetOTF/SC)
- [字体原文件](https://raw.githubusercontent.com/notofonts/noto-cjk/main/Serif/SubsetOTF/SC/NotoSerifSC-Regular.otf)
- [许可证来源](https://raw.githubusercontent.com/notofonts/noto-cjk/main/Serif/LICENSE)
- 本地字体：`app/src/main/assets/fonts/NotoSerifSC-Regular.otf`
- 随包许可证：`app/src/main/assets/fonts/OFL.txt`
- 获取日期：2026-11-05
- SHA-256：`e8f396decc1f0963a016a989c3d8852e863d1350996f573860a80767c83a1cd3`

字体在应用进程内复用，不在每个日期格重复加载。浏览器预览使用同名衬线字体，无法加载时回退到宋体类系统字体。安卓真实设备的加载、字重与墨水屏可读性仍需实机验证。

## 0.3.0 英文排版

- 标题、日期和控件：来自 [Google Fonts 的 Source Serif 4](https://github.com/google/fonts/tree/main/ofl/sourceserif4)，生成 400 / 650 / 900 三种静态实例，分别固定光学尺寸 20 / 20 / 60。
- 日程正文：来自 [Google Fonts 的 EB Garamond](https://github.com/google/fonts/tree/main/ofl/ebgaramond)，生成 400 字重静态实例。
- 派生字体更名为 NewsprintDisplay / NewsprintReading，保留版权和许可元数据；两个原始 OFL 许可证随字体放在 assets/fonts。
- `tools/prepare-fonts.py` 使用 fonttools 4.60.1 生成实例，输出文件不包含 fvar 表。工具仅用于开发，不进入应用运行时。
- 中文文本继续使用原始 Noto Serif SC Regular；中文粗标题由平台合成粗体。英文字体不覆盖 CJK，因此混合中文的日程整行采用 Noto，避免旧安卓缺字。
- 字体均在进程内缓存；应用不新增网络权限。浏览器预览从 Google Fonts 加载原字体，字形与安卓静态实例的光学尺寸、中文粗体可能略有差异。

## 主题分支：0.5.0-theme.1

新增字体均随 APK 离线打包，不依赖系统下载字体。英文静态实例由 `tools/prepare-theme-fonts.py` 生成，中文黑体直接使用官方静态 OTF。`tools/theme-font-sources.json` 固定来源提交及下载文件 SHA-256；缓存键包含完整上游路径，并核对元数据和字体内部家族名称，防止不同字体缓存重名。

| 界面名称 | APK 内部家族 | 普通 / 标题字重 | 中文搭配 | 上游目录与许可 |
| --- | --- | --- | --- | --- |
| Source Serif 4 / EB Garamond | NewsprintDisplay / NewsprintReading | 沿用原版 | Noto Serif SC | 上述原版来源，OFL 1.1 |
| Geist | InkGeist | 400 / 700 | Noto Sans SC | [google/fonts/ofl/geist](https://github.com/google/fonts/tree/main/ofl/geist)，OFL 1.1 |
| Libre Baskerville | InkBaskerville | 400 / 700 | Noto Serif SC | [google/fonts/ofl/librebaskerville](https://github.com/google/fonts/tree/main/ofl/librebaskerville)，OFL 1.1 |
| Fraunces | InkFraunces | 400 / 600 | Noto Serif SC | [google/fonts/ofl/fraunces](https://github.com/google/fonts/tree/main/ofl/fraunces)，OFL 1.1 |
| Roboto Slab | InkSlab | 400 / 600 | Noto Serif SC | [google/fonts/apache/robotoslab](https://github.com/google/fonts/tree/main/apache/robotoslab)，Apache 2.0 |
| Space Grotesk | InkSpace | 400 / 500 | Noto Sans SC | [google/fonts/ofl/spacegrotesk](https://github.com/google/fonts/tree/main/ofl/spacegrotesk)，OFL 1.1 |
| Atkinson Hyperlegible Next | InkAtkinson | 400 / 700 | Noto Sans SC | [google/fonts/ofl/atkinsonhyperlegiblenext](https://github.com/google/fonts/tree/main/ofl/atkinsonhyperlegiblenext)，OFL 1.1 |

中文黑体来源：[Noto CJK Sans / SC 静态 OTF](https://github.com/notofonts/noto-cjk/tree/main/Sans/SubsetOTF/SC)，许可证随包保存为 `notosanssc-OFL.txt`。字体原文件 SHA-256 为 `faa6c9df652116dde789d351359f3d7e5d2285a2b2a1f04a2d7244df706d5ea9`。

所有派生字体保留原版权和许可元数据，改为 Ink 前缀家族名；内部描述附静态实例生成及变更说明。原始许可证按家族随包附带；Roboto Slab 使用 Apache 许可证文件，未归为 OFL。生成结果均无 `fvar` 表。

主题版使用 `MetricAffectingSpan` 按中英文片段选择字体，混排事项保留拉丁字体与对应中文字体；输入框的字体 span 不改变内容或光标。中文粗标题使用合成粗体。英文标题字距在 API 21+ 使用平台属性，API 19 使用逐词 span，保留原文字和按词换行。所有字体按资产名称在进程内缓存，不在每个日期格重复加载。

电脑端 API 19/28 测试不能替代真实 Android 4.4 与墨水屏字形、内存占用和刷新表现验收。
