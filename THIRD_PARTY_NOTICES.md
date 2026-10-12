# 第三方组件说明

本项目自身代码的 MIT 许可证不覆盖另有许可的第三方组件。

## Gradle Wrapper 8.7

- 文件：`gradlew`、`gradlew.bat`、`gradle/wrapper/gradle-wrapper.jar`。
- 来源：[Gradle 8.7](https://github.com/gradle/gradle/tree/v8.7.0)，由官方 Gradle `wrapper` 任务生成。
- 许可：[Apache License 2.0](licenses/Apache-2.0.txt)。生成脚本中的原始版权和许可声明保持不变。
- Wrapper JAR 与 Gradle 分发包均按 Gradle 官方公布的 SHA-256 校验；分发包校验值保存在 `gradle-wrapper.properties`。

Android Gradle Plugin、JDK、Android SDK 和 CI 工具在开发或构建时获取，使用时应遵守各自许可。当前应用模块未声明额外的第三方运行时依赖。

后续引入第三方代码或素材时，请在此记录组件名称、准确来源、许可证与署名要求。
