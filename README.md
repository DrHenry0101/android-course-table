# 📅 CourseTable - 原生 Android 极简周课表

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.java.com/)
[![Release](https://img.shields.io/badge/Release-v0.1.0-blue.svg)](https://github.com/DrHenry0101/android-course-table/releases)

> 一款清爽、轻量、高信息密度的 Android 原生极简课表应用。参考 WakeUp 课程表的经典结构设计，专注个人课程管理，无需登录，应用不主动联网，打开即用。

---

## ✨ 核心特性

- 🎨 **WakeUp 风格视觉设计**
  - 清新蓝紫主色（`#4F6EF7`），护眼浅灰蓝背景（`#F6F8FC`）与 10dp 圆角卡片。
  - 顶部固定星期表头，包含当月标识（如“9月”）与当天高亮显示。
  - 10 组柔和莫兰迪色池，基于课程名称稳定哈希映射，周次切换颜色恒定。

- 📆 **动态日历自然周次**
  - **日期驱动**：根据当前系统日历与开学基准日自动计算自然周，每周无需手动修改、自然过周自动递增。
  - **每次打开自动对齐**：启动应用或从后台切回，默认直接定位并停留在符合当前日期的周次。
  - **一键“回到本周”**：浏览其他周次时，点击右上角一键快速切回当前周。
  - **便捷校准**：点击周次标题可唤起 1–25 周选择器，并可一键“设置第 X 周为当前周”，反推并保存基准日。

- 🛡️ **文本防溢出与排版优化**
  - 单元格物理级边界裁剪（`clipRect`），从底层杜绝课程文本向相邻星期列穿透与拥挤。
  - 课程名称自适应折行（最多 2 行），长教室名称自适应提取后置简码（如 `@K-602`），卡片整洁美观。
  - 点击卡片弹出详情弹窗，可查看完整课程名、地点全称、时间、教师及周数规则。

- 📜 **纵向平滑滚动（1–12 节）**
  - 面向手机竖屏布局，支持纵向滚动查看底部 9–12 节课。

- 📥 **智能 CSV 导入**
  - 支持 **7 列中文课表格式**：`课程名称,星期,开始节数,结束节数,老师,地点,周数`。
  - 支持 **9 列英文示例格式**：`name,weekday,start_period,periods,week_start,week_end,teacher,room,note`。
  - 灵活周数规则解析：支持单周（`11`）、区间（`3-7`）、顿号分隔不连续周（`3-5、7-16`）以及末尾单双周标记（`4-16双`）。

- ✏️ **课程管理与离线存储**
  - 支持手动新增、编辑与删除课程。
  - 课程和学期设置保存在本机应用私有存储（`SharedPreferences`），应用运行无需联网。系统备份策略见下方隐私说明。

---

## 📱 安装与下载

进入 [**Releases 页面**](https://github.com/DrHenry0101/android-course-table/releases) 下载最新发布的 `.apk` 安装包（如 `CourseTable-v0.1.0.apk`），下载至手机后直接点击即可安装运行。

---

## 📄 CSV 导入格式说明

应用支持通过系统文件选择器导入 UTF-8 编码的 CSV 课表文件。

### 格式 A：7 列中文课表格式（推荐）
```csv
课程名称,星期,开始节数,结束节数,老师,地点,周数
操作系统,1,1,2,张老师,教学楼 A-101,3-16
数据结构,3,3,4,李老师,实验楼 302,4-16双
计算机网络,4,5,6,王老师,公教楼 B-201,3-5、7-16
```

### 格式 B：9 列标准格式
```csv
name,weekday,start_period,periods,week_start,week_end,teacher,room,note
高等数学,1,1,2,1,16,王老师,A101,
大学英语,3,3,2,1,16,李老师,B202,
程序设计,5,5,2,1,8,赵老师,实验楼3,
```

---

## 🛠️ 本地编译构建

### 环境要求
- JDK 21，配置 `JAVA_HOME`。
- Android SDK Platform 35 和 Build Tools 34.0.0，配置 `ANDROID_HOME` 或 `ANDROID_SDK_ROOT`；也可以在不提交的 `local.properties` 中设置 `sdk.dir`。
- 仓库自带 Gradle Wrapper 8.7，Android Gradle Plugin 固定为 8.6.1，无需另装 Gradle。首次构建需要联网下载工具和依赖，应用运行无需联网。

### 构建指令

在仓库根目录执行。Windows PowerShell：

```powershell
.\gradlew.bat --no-daemon assembleDebug lintDebug
```

macOS / Linux：

```sh
bash ./gradlew --no-daemon assembleDebug lintDebug
```

Debug APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。若已将 Android SDK 的 `platform-tools` 加入 `PATH`，可安装到已连接并授权的测试设备：

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Debug APK 供开发验证使用。正式发行版需单独管理发布签名，签名文件和密码不应进入 Git 仓库。

## 已知限制与数据保存

- 当前面向 Android 6.0（API 23）及以上的手机竖屏使用；横屏、平板和折叠屏体验尚未完整验证。
- CSV 导入会追加课程，重复导入可能产生重复项；格式不合格的行会跳过。请先用 [脱敏示例](data/courses.example.csv) 验证格式。
- 当前没有课程导出功能。请保存原始 CSV；清除应用数据或卸载应用后，本机课程和学期设置可能丢失。
- 已关闭 Android 云备份，并配置规则排除标准设备迁移；厂商系统可能有差异。详见 [隐私说明](PRIVACY.md)。
- 当前不含教务系统登录、自动同步或课程提醒功能。

## 参与开发与反馈

普通问题和功能建议请提交到 [Issues](https://github.com/DrHenry0101/android-course-table/issues)，附 Android 版本、应用版本、复现步骤及脱敏示例。

贡献代码前请阅读 [贡献指南](CONTRIBUTING.md)。涉及凭据或个人数据的安全问题请按 [安全说明](SECURITY.md) 私下报告。

仓库提供自动构建、Android Lint 和 Git 历史密钥扫描工作流。配置位于 `.github/workflows/ci.yml`。

## 许可证与来源

项目作者与维护者：[DrHenry0101](https://github.com/DrHenry0101)。

本项目采用 [MIT License](LICENSE)，版权声明为 `Copyright (c) 2026 DrHenry0101`。第三方组件保留原有许可，见 [第三方说明](THIRD_PARTY_NOTICES.md)。

界面布局参考 WakeUp 课程表的使用方式；本项目是独立项目，与其官方没有隶属关系。
