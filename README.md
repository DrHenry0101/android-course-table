# 📅 CourseTable - 原生 Android 极简周课表

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.java.com/)
[![Release](https://img.shields.io/badge/Release-v0.1.0-blue.svg)](https://github.com/DrHenry0101/android-course-table/releases)

> 一款清爽、轻量、高信息密度的 Android 原生极简课表应用。参考 WakeUp 课程表的经典结构设计，专注个人课程管理，无需登录、无后台常驻、无隐私收集，打开即用。

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
  - 完美适配 20:9 及 1224×2700 等现代全面屏设备，滑动顺畅，底部 9–12 节课完整可见。

- 📥 **智能 CSV 导入**
  - 支持 **7 列中文课表格式**：`课程名称,星期,开始节数,结束节数,老师,地点,周数`。
  - 支持 **9 列英文示例格式**：`name,weekday,start_period,periods,week_start,week_end,teacher,room,note`。
  - 灵活周数规则解析：支持单周（`11`）、区间（`3-7`）、顿号分隔不连续周（`3-5、7-16`）以及末尾单双周标记（`4-16双`）。

- ✏️ **课程管理与离线存储**
  - 支持手动新增、编辑与删除课程。
  - 数据完全存储于本地（`SharedPreferences`），纯离线运行，零网络请求。

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
- JDK 17+
- Android SDK (API 35, Min SDK 23)
- Gradle 8.7+

### 构建指令
```powershell
# 编译 Debug APK
./gradlew assembleDebug

# 安装到连接的 Android 设备
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
