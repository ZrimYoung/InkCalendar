# 墨日历 · InkCalendar

面向低配置安卓墨水屏的离线桌面日历。名称为暂定方案，当前主屏采用简洁的报纸式衬线排版。

## 第一版功能

当前版本：0.3.1。纯白底黑字、衬线标题、粗细分隔线与方形线条图标；横屏以细线分栏，竖屏上下排列。日期选中时反色，今天有额外边框和粗体标识。设置页采用统一的黑白排版与自绘选择标记，可切换简体中文 / English，保存后立即生效并保留选择。

内置 Noto Serif SC 中文字体、Source Serif 4 派生的英文标题字体和 EB Garamond 派生的英文正文字体，均为静态字体、离线显示。字体来源、生成方式与许可证见 `docs/fonts.md`。图标保留 48 dp 点击区域和双语无障碍说明，长按可查看含义。

- 大日期、星期、周一开始的月历；点击日期查看当日事项。
- 月历区域左滑进入下个月、右滑回到上个月；保留翻月按钮。上下滑动用于滚动，短距离或明显斜向拖动不翻月，无翻页动画。
- 选中今天时显示包含今天在内的未来七天事项，全天事项排在当日定时事项前面。
- 本地添加、编辑、删除事项；可选 24 小时制时间，留空表示全天。点击事项编辑，删除需要确认。
- 切换界面语言只改变界面、日期和提示文字，不翻译或修改用户填写的事项。
- 横竖屏布局；全屏、保持唤醒、大字模式可在设置中调整。
- 前台跨午夜换日；恢复前台或系统时间、时区变更时校正日期。浏览其他日期时保留所选日期。
- 纯黑白、高对比、无页面切换动画、无秒针、无网络权限、无第三方运行时依赖。界面不使用暖色背景、红色标记或系统蓝色设置控件。

## 当前边界

- 兼容目标 Android 4.4 / API 19 及以上，**目标设备尚未实测**。默认假设设备支持触控。
- 保持唤醒仅作用于本应用前台；不强制调整设备前光。它不等于设备休眠后仍能自动更新屏幕。
- 未接入厂商墨水屏刷新 SDK，不承诺局部刷新或消除残影。应用减少重绘触发，实际刷新由设备控制。
- 日期选择范围 1900—2100 年；日期和事项使用本地日历日，切换时区不会平移已有事项。
- 不含农历、节气、法定假期调休、重复日程、通知响铃、账号同步、桌面小组件和开机自启。这些需求尚未确认。
- 事项保存在应用私有 SQLite 数据库。此版无导出功能、系统备份关闭；卸载或清除应用数据会删除事项。后续升级应使用相同应用 ID 和签名覆盖安装。
- 测试包使用调试签名，只用于试用。正式发布前需要另行管理发布签名。

## 构建

使用 JDK 17、Gradle 8.9、Android Gradle Plugin 8.7.3、Android SDK Platform 35 / Build Tools 34.0.0。最低运行版本与构建 SDK 版本不同。

1. 安装 JDK 17 和 Android SDK，在本机 `local.properties` 设置 `sdk.dir`，或配置 `ANDROID_HOME`。
2. 执行 `./gradlew testDebugUnitTest lintDebug assembleDebug`。
3. 测试包位于 `app/build/outputs/apk/debug/app-debug.apk`。

项目不需要 Google Play 服务或运行时联网。开发依赖首次构建时需要联网下载。

可执行 `./build-local.sh` 完成测试、静态检查和打包；`./build-local.sh assembleDebug` 仅打包。

## 验收

自动测试覆盖闰年、无效日期、月历首列、跨月跨年、日期范围边界、时区稳定性、夏令时换日，以及事项输入校验。安卓静态检查用于发现低版本 API 使用问题。

目标设备到位后，按 `docs/device-checklist.md` 验证安装、布局、事项持久化、跨日更新、残影与耗电。最新检查结果见 `docs/validation-0.3.1.md`；浏览器预览不是 APK 实机截图。

## 机制参考

- [Android：保持屏幕唤醒](https://developer.android.com/develop/background-work/background-tasks/awake/screen-on)
- [Android：Doze 与应用待机](https://developer.android.com/training/monitoring-device-state/doze-standby)
- [Android 15：窗口边距与全屏边界](https://developer.android.com/about/versions/15/behavior-changes-15)

常亮与后台行为按官方机制实现；最终表现仍需结合设备厂商的休眠和刷新策略验证。
