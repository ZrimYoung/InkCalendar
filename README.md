# 墨日历 · InkCalendar

面向 Android 墨水屏和浏览器的离线日历，采用黑白高对比界面，支持简体中文和 English。Android 与 Web 同步维护功能和界面，数据各自保存在本地。

当前版本：**0.5.0-theme.6 测试版**。最低支持 Android 4.4 / API 19，需要触控操作。

## 功能

- 月视图、周视图、滑动翻页、日期跳转与本地事项，支持全天或指定时间。周视图仅替换日历区域，保留日期和事项区的位置。
- 事项完成 / 撤销完成、隐藏已完成，以及未来 7 天 / 30 天列表。
- 农历与二十四节气仅在中文界面显示，可分别关闭；中国节日名称支持中英文。已收录 **2026 年**官方放假和补班安排，其他年份不推算调休。
- 纪念日和倒计时，首页最多置顶两项；中文界面可选择按农历纪念。
- 桌面展示模式：突出今日日期、置顶纪念日和最多三条近期未完成事项。
- 自动跟随系统、强制横屏、强制竖屏；横屏可选主副排版或对半双栏。
- 图标分组设置、开关、分段按钮及字号调节（滑块与 − / ＋ 按钮），支持全屏和前台保持唤醒。

日期范围为 1900—2100 年。历法来源、周年计算与调休范围见[日历数据说明](docs/calendar-data.md)。


## 外观与主题

在设置 → 外观与主题中选择报刊编辑、极简留白、手账便笺、票据清单或今日看板；选项网格随宽度自适应，预览跟随横竖屏布局。七组英文字体以 Aa 卡片预览，名称位于下方，中文自动搭配宋体或黑体。

可微调日期、月历、事项和按钮布局。设置、外观和组件页使用右上角 × / ✓ 取消或保存；子页保存回上级，设置页保存后应用。详见[主题说明](docs/themes.md)。

## 安装与数据

测试包使用调试签名。升级请使用相同应用 ID 和签名覆盖安装，以保留事项和偏好。

Android 数据存于应用私有数据库；Web 数据存于当前浏览器，初次打开为空日历。卸载应用、清除应用或站点数据会删除对应记录。当前没有数据导出、账号同步、重复事项、提醒和系统桌面小组件。桌面展示模式是应用内页面。

应用运行不需要网络或 Google Play 服务。保持唤醒仅在前台生效；休眠更新、残影与耗电受设备系统和刷新策略影响。

## Web 本地运行

需要 Node.js 22 或更高版本，运行时使用支持 ES2022 和 IndexedDB 的现代浏览器。

```sh
cd web
npm ci
npm run dev
```

打开终端显示的本地地址。正式入口使用真实日期并保存数据；在地址后加 `?demo=1` 可查看独立的原型示例。

运行 `npm test` 验证规则，`npm run build` 生成 `web/dist/` 并更新可直接打开的 [HTML 原型](preview/ink-calendar-preview.html)。`npm run preview` 可在本地检查构建结果及离线缓存；首次完整加载后，构建版可离线使用。

Web 的方向设置控制页面排版，全屏和唤醒取决于浏览器支持。两端维护与验证方式见[双端说明](docs/platform-parity.md)。

## Android 构建

需要 JDK 17、Android SDK Platform 35 和 Build Tools 34.0.0；Gradle 使用仓库自带的 Wrapper。

1. 配置 `JAVA_HOME`，并通过 `ANDROID_HOME` 或本地 `local.properties` 的 `sdk.dir` 指定 Android SDK。
2. 执行测试、静态检查和打包：

   ```sh
   ./gradlew testDebugUnitTest lintDebug assembleDebug
   ```

   Windows 使用 `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug`。

3. APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

首次构建及测试需要下载开发依赖；测试范围和平台差异见验收记录。

## 验证与许可

- [当前版本验证记录](docs/validation-0.5.0-theme.6.md)与[设备验收清单](docs/device-checklist.md)
- [字体来源与许可证](docs/fonts.md)
- [离线历法数据与许可证](docs/calendar-data.md)

[浏览器交互原型](preview/ink-calendar-preview.html)与 Web 共用界面代码，使用独立示例数据；修改说明见 [preview/README.md](preview/README.md)。
