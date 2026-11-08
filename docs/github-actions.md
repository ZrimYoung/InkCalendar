# GitHub Actions

工作流：`.github/workflows/android-ci.yml`。在面向 main 的 PR、main 推送和手动触发时执行。工作流需合入 main 后，才成为主线配置并提供默认分支的手动运行入口。

使用 Ubuntu 24.04、Temurin JDK 17、Android Platform 35 / Build Tools 34.0.0，以及仓库固定的 Gradle 8.9 Wrapper。执行 `testDebugUnitTest lintDebug assembleDebug`；测试和 lint 未通过时，不交付成功 APK。

## 下载结果

在 Actions 运行页的 Artifacts 下载：

- `Android-reports-运行编号`：JUnit、测试页面、lint 报告和可生成的电脑渲染图；失败时仍尽量上传。
- `InkCalendar-签名模式-运行编号`：APK、SHA-256、构建元数据和公开签名报告。保存 14 天，不自动创建 Release。

分支与提交、签名模式和 APK 校验值可在运行摘要或 `signing.txt` 中查看。CI 通过不代表低版本设备或墨水屏真机验收。

## 当前 APK 的签名

项目目前采用 Android 默认 debug 签名，未配置正式 release 签名。原有调试 keystore 在开发环境复用；私钥和 keystore 没有进入源码仓库。GitHub 发布的 0.3.4 与 0.4.0 APK 的公开证书已在线核对为同一张，证书 SHA-256 为：

```text
6abe0b1fb9a54c235449098445e5a63053f67e8606cf309816d38313fb6fa72f
```

[0.4.1 验收记录](validation-0.4.1.md)记载 v1/v2 签名验证与覆盖升级通过；本次在线证书核对范围为上述两个已发布版本，不能把它说成对 0.4.1 APK 的重新验证。

## CI 签名模式

### ephemeral-debug

PR 始终采用临时调试密钥；未配置原 keystore Secret 的其他运行也采用这种模式。GitHub 临时运行器会生成自己的 debug.keystore，跨运行不保证使用同一张证书，因此不能假定这些包可以覆盖现有安装。不要为安装 CI 包而卸载含重要事项数据的现有应用。

临时包用于构建和新安装测试，不自动发布。私钥不会被缓存或上传到 Artifact。

### existing-debug

需要沿用现有签名时，将原始 debug.keystore 的 Base64 内容保存到仓库 Actions Secret：

`INK_CALENDAR_DEBUG_KEYSTORE_BASE64`

Secret 应从原 keystore 创建，不能从 APK 的公开证书恢复私钥。现有项目使用 Android 默认调试别名 `androiddebugkey` 和默认调试口令 `android`。工作流会先核对 keystore 证书与上述已发布证书一致，再核对生成 APK 确实使用了它；不一致即失败。

原私钥只在 main 推送或手动触发的可信运行中导入临时目录，PR 不能获取该 Secret。Secret 配置完成前，CI 不会声称沿用了原签名。本次只提供配置入口，不生成替代原私钥的新长期密钥，也不变更正式发布签名策略。

## 2026-11-06 接入验证

配置通过 YAML 解析、触发范围/权限断言、官方 Actions 固定提交核对和 Bash 语法检查。作业级环境变量不使用 runner 上下文，Android 用户目录在步骤启动后通过 GITHUB_ENV 设置。

首次在线验证遇到 GitHub `startup_failure`：即使独立分支仅保留一个无依赖、只执行 echo 的最小工作流，仍在启动时失败，没有产生 job 或日志。诊断运行见 [42173822933](https://github.com/ZrimYoung/InkCalendar/actions/runs/42173822933)。API 未给出进一步错误原因，不能据此声称测试、lint 或 APK 构建已经通过。

临时诊断配置已清理，草稿 PR 保留完整预期工作流。main 和主题分支未修改；需先解决 GitHub 启动问题并重新验证，再评估合并。
