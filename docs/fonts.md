# 字体

应用内置 Noto Serif SC Regular，来自 Noto CJK 官方仓库的简体中文静态 OpenType 版本，使用 SIL Open Font License 1.1。字体文件保持原样，以避免旧安卓依赖可变字体支持。

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
