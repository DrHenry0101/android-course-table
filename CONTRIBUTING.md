# 贡献指南

欢迎通过 Issue 或 Pull Request 改进 CourseTable。较大的功能变更建议先在 Issue 中讨论目标和使用场景。

## 开发与验证

按 [README](README.md) 准备 JDK 21 与 Android SDK，通过仓库自带 Wrapper 构建，不要提交本机 SDK 路径或缓存。

提交前运行：

```powershell
.\gradlew.bat --no-daemon assembleDebug lintDebug
```

macOS / Linux 使用 `bash ./gradlew --no-daemon assembleDebug lintDebug`。涉及界面、数据保存或导入行为的修改，还需在测试设备上验证对应流程；说明 Android 版本、步骤与结果。

PR 应说明解决的问题、修改后的行为和验证结果，尽量一次处理一个问题。截图、CSV 和日志必须脱敏，不要提交凭据、签名密钥或真实教务数据。

新增依赖、图标、字体或借用代码时，请提供来源、许可和所需署名。提交贡献前应确认有权按本项目 MIT 许可证提供相应贡献；第三方内容保留原有许可。

## 密钥检查

CI 使用 Gitleaks 8.30.1 扫描可读取的完整 Git 历史。已安装同版本 Gitleaks 时，可本地执行：

```sh
gitleaks git . --log-opts="--all --full-history" --ignore-gitleaks-allow --redact=100
```

如果凭据曾被提交，应先撤销或轮换凭据，再讨论历史处理；仅修改最新文件不能使旧凭据失效。不要在公开 Issue 中粘贴秘密值。
