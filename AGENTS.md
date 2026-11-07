# Android 与 Web 同步维护

- 本项目有 Android（`app/`）和 Web（`web/`）两个客户端；用户要求后续功能和界面同步维护。数据目前各端独立，不能将此要求解释为自动上传或云同步。
- 修改日历、事项、纪念日、主题或设置的共同功能时，同时检查两个客户端；平台专属行为在 `docs/platform-parity.md` 记录差异。
- 主题、字体、显示选项和约束以 `shared/calendar-contract.json` 为准；运行 `node tools/generate-shared.mjs` 更新生成代码，不手改生成文件。
- 共用业务规则用 `shared/parity-cases.json` 验证。修改规则时同时更新 Android 和 Web 的实现与测试。
- Web 与浏览器原型复用 `web/src/` 的界面和业务代码；原型使用独立示例数据。不要另复制一套界面实现。
- 默认保留既有月视图框架及当日日期位置；周视图只替换日历区域。对半双栏是单独的横屏选项。
- 共同功能改动运行 Android 的 `testDebugUnitTest lintDebug assembleDebug`、Web 的 `npm test` 和 `npm run build`；只改文档或平台专属代码时按影响范围验证并说明。
- README 和提交内容只保留项目必要信息；设备标识、个人目录、备份、签名及测试截图放在本地忽略目录，不进入 Git。
