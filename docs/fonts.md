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
