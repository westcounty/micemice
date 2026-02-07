# Micemice

一个面向生物实验室的小鼠全生命周期管理 App（Android，离线优先）。

- 仓库：`https://github.com/westcounty/micemice`
- 当前版本：`v1.0.0`
- 产品范围：App-only（网络后端能力预留）
- PRD 完成度（除网络）：`74/74`

## 项目介绍

Micemice 用于覆盖实验室小鼠管理的核心流程：
1. 笼位管理：建笼、转笼、并笼、拆笼、容量预警、扫码进笼卡
2. 个体管理：状态机校验、家系展示、事件记录、附件管理
3. 繁育流程：配种、查栓、产仔、断奶向导、断奶撤销、繁育日历
4. 分型流程：采样、建批、板位映射、导入、冲突确认
5. Cohort/实验：条件筛选、锁定、盲法编码、实验事件、归档
6. 任务与通知：逾期升级、模板任务、通知中心、系统通知
7. 合规审计：协议校验、培训资质、审计日志、CSV/PDF/ZIP 导出
8. 管理治理：主数据、细粒度 RBAC、权限矩阵、同步队列、冲突中心

## App 导航（功能导航）

底部主导航：
1. 工作台
2. 扫码
3. 笼位
4. 个体
5. 繁育
6. 分型
7. 实验
8. 任务
9. 管理（Admin）

扩展页面：
1. 通知中心
2. 冲突中心（Admin）
3. Cohort
4. 报表（PI/Admin）

## 下载指引

Release 页面：
- `https://github.com/westcounty/micemice/releases/tag/v1.0.0`

当前提供文件：
1. `micemice-v1.0.0-app-release-unsigned.apk`
2. `micemice-v1.0.0-app-release.aab`
3. `SHA256SUMS.txt`
4. `RELEASE_NOTES.md`

说明：
1. `app-release-unsigned.apk` 为未签名包，不能直接安装到手机。
2. `aab` 适合发布流程（应用商店/内部分发流水线）。

如需本地快速安装测试，建议从源码构建 Debug 包：
```powershell
./gradlew.bat assembleDebug
```
产物通常在：
- `app/build/outputs/apk/debug/app-debug.apk`

校验文件（可选）：
```powershell
Get-FileHash .\app\build\outputs\bundle\release\app-release.aab -Algorithm SHA256
```

## 使用指引（快速开始）

### 1) 启动与登录

1. 安装并打开 App
2. 登录输入：
   1. 账号：非空
   2. 密码：至少 4 位
   3. 组织编码：非空（例如 `LAB-A1`）
3. 选择角色：`实验员` / `PI` / `管理员`

### 2) 推荐首日流程

1. 管理员先完成基础治理：
   1. 主数据（品系/基因型）
   2. 协议状态
   3. 培训资质
   4. 权限矩阵（细粒度 RBAC）
2. 实验员执行现场流程：
   1. 扫码进笼卡
   2. 笼位操作
   3. 个体事件与附件补录
   4. 繁育/分型任务闭环
3. PI 查看报表复盘：
   1. 完成率、繁育成功率、存活率
   2. 分布图与趋势图

### 3) 关键文档

1. PRD：`docs/product/Micemice_App_PRD_v1.0.md`
2. 实现对照：`docs/product/Micemice_PRD_实现对照清单_v1.0.md`
3. 开发拆解包：`docs/product/Micemice_V1_开发拆解包.md`
4. 快速入门：`docs/product/Micemice_快速入门指南_v1.0.md`
5. 角色手册：`docs/product/Micemice_角色使用手册_v1.0.md`
6. 墙贴 SOP（完整版）：`docs/product/Micemice_实验室墙贴SOP_v1.0.md`
7. 墙贴 SOP（半页版）：`docs/product/Micemice_实验室墙贴SOP_半页版_v1.0.md`
8. 墙贴 SOP（打印美化版）：`docs/product/Micemice_实验室墙贴SOP_打印美化版_v1.0.md`

## 技术栈与架构

1. 语言：Kotlin
2. UI：Jetpack Compose
3. 架构：MVVM
4. 本地存储：Room
5. 后台任务：WorkManager
6. 条码扫码：CameraX + ML Kit
7. 网络层预留：Retrofit

## 开发与验证

常用命令：
```powershell
./gradlew.bat testDebugUnitTest
./gradlew.bat assembleDebug
./gradlew.bat assembleRelease
./gradlew.bat bundleRelease
```

## License

当前仓库未单独声明开源许可证；如需开源发布，请补充 `LICENSE` 文件并明确授权条款。
