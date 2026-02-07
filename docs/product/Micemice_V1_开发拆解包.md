# Micemice V1 开发拆解包（App-only）

- 文档版本：V1.0
- 文档日期：2026-02-07
- 关联文档：`docs/product/Micemice_App_PRD_v1.0.md`

## 1. V1目标与边界

### 1.1 V1业务目标

1. 打通笼位-个体-繁育-分型-cohort-任务-审计主链路。
2. 实现移动端现场高频操作闭环（扫码、转笼、断奶、采样）。
3. 提供可用于内部检查的基础合规导出能力。

### 1.2 V1范围内

1. 账号与权限（RBAC）
2. 主数据管理（品系/房间/机架/笼位/协议）
3. 笼位与个体管理
4. 繁育流程（配种、查栓、产仔、断奶、分笼）
5. 分型流程（采样、批次、回填）
6. cohort 条件筛选与锁定
7. 任务中心与提醒
8. 报表（基础）
9. 导入导出（CSV）
10. 审计日志

### 1.3 V1范围外

1. 多机构计费与结算引擎
2. 深度第三方硬件集成
3. 高级统计建模

## 2. 产品与交互拆解

## 2.1 核心流程图（文本）

1. 创建主数据 -> 配种 -> 产仔 -> 断奶分笼 -> 采样分型 -> cohort 构建 -> 实验记录 -> 报表导出。
2. 每个节点自动生成任务并进入任务中心。
3. 所有关键写操作进入审计日志。

## 2.2 页面清单（V1）

1. 登录页
2. 工作台
3. 笼位列表页
4. 笼卡详情页
5. 个体列表页
6. 个体详情页
7. 配种向导页
8. 断奶向导页
9. 分型批次页
10. 分型回填页
11. cohort 构建页
12. 任务中心页
13. 报表页
14. 管理中心页
15. 审计日志页

## 2.3 页面级字段清单（核心）

## 工作台

1. 今日待办总数
2. 高风险任务数
3. 协议到期提醒
4. 快捷入口（配笼/断奶/采样/转笼）

## 笼卡详情

1. cage_id
2. room/rack/slot
3. current_animals
4. capacity_limit
5. breeding_status
6. recent_events
7. 快捷操作按钮

## 个体详情

1. animal_id
2. identifier
3. sex
4. dob
5. strain
6. genotype
7. status
8. current_cage
9. lineage
10. experiment_flags

## 配种向导

1. male_id
2. female_id
3. start_date
4. expected_birth
5. protocol_id
6. risk_prompts

## 分型回填

1. batch_id
2. sample_id
3. marker
4. call
5. confidence
6. reviewer
7. version

## cohort 构建

1. 条件：strain/genotype/sex/age/weight/status
2. 候选数量
3. 入组名单
4. 盲法编码
5. 锁定时间

## 3. Epic 拆解

## Epic 1：账号权限与组织能力

### 用户故事

1. 作为管理员，我希望配置角色权限，以控制不同角色可见和可操作的模块。
2. 作为审计人员，我希望查看关键操作日志，以便检查可追溯性。

### 验收标准

1. 不同角色登录后导航与操作按钮按权限动态展示。
2. 写操作均有审计记录，含 before/after。

## Epic 2：主数据与字典

### 用户故事

1. 作为管理员，我希望维护品系和等位基因模板，以统一命名。
2. 作为实验员，我希望快速选择标准化主数据，减少手输错误。

### 验收标准

1. 品系命名必须通过模板校验。
2. 主数据变更进入审计日志。

## Epic 3：笼位与个体管理

### 用户故事

1. 作为实验员，我希望扫码进入笼卡并快速执行转笼。
2. 作为 PI，我希望查看当前笼位占用与拥挤风险。

### 验收标准

1. 扫码进笼卡 < 2 秒。
2. 超容量有醒目告警。

## Epic 4：繁育流程

### 用户故事

1. 作为实验员，我希望通过向导完成配种并自动生成后续任务。
2. 作为实验员，我希望在断奶时批量分笼，减少重复操作。

### 验收标准

1. 配种和断奶均支持批量处理。
2. 查栓/断奶任务按时间窗自动创建。

## Epic 5：分型流程

### 用户故事

1. 作为实验员，我希望登记采样并形成送检批次。
2. 作为实验员，我希望批量回填分型结果并处理冲突。

### 验收标准

1. 分型结果版本化。
2. 冲突结果必须人工确认。

## Epic 6：实验cohort

### 用户故事

1. 作为博士生，我希望按多条件筛选并锁定 cohort。
2. 作为 PI，我希望复现某次筛选条件。

### 验收标准

1. 筛选条件快照保存。
2. 锁定后成员变更需审计。

## Epic 7：任务与提醒

### 用户故事

1. 作为实验员，我希望在工作台看到今日必须完成任务。
2. 作为管理员，我希望配置提醒策略。

### 验收标准

1. 任务支持逾期升级提醒。
2. 支持模板任务。

## Epic 8：报表与导出

### 用户故事

1. 作为 PI，我希望看见繁殖效率和笼位占用趋势。
2. 作为审计人员，我希望导出检查所需记录。

### 验收标准

1. 报表按时间与课题过滤。
2. 导出 CSV/PDF 成功并可复核。

## 4. 技术架构拆解（Android）

## 4.1 分层架构

1. `app`：路由、主题、全局容器
2. `core-common`：基础工具、Result封装、时间与格式
3. `core-network`：API、拦截器、序列化
4. `core-database`：Room、DAO、迁移
5. `core-sync`：WorkManager 同步任务
6. `core-auth`：会话与权限
7. `feature-workbench`
8. `feature-cage`
9. `feature-animal`
10. `feature-breeding`
11. `feature-genotyping`
12. `feature-cohort`
13. `feature-task`
14. `feature-report`
15. `feature-admin`

## 4.2 关键技术选型

1. Kotlin + Coroutines + Flow
2. Jetpack Compose + Navigation
3. Room + SQLCipher（可选）
4. Retrofit + OkHttp
5. Hilt DI
6. WorkManager（离线同步）
7. CameraX（扫码）

## 4.3 离线与同步策略

1. 所有写操作先写本地事件表。
2. 同步器按队列上送，成功后标记。
3. 冲突策略：同字段冲突进入冲突中心待人工处理。

## 5. API 拆解（V1建议）

## 5.1 Auth

1. `POST /api/v1/auth/login`
2. `POST /api/v1/auth/refresh`
3. `GET /api/v1/me`

## 5.2 Master Data

1. `GET /api/v1/strains`
2. `POST /api/v1/strains`
3. `GET /api/v1/rooms`
4. `GET /api/v1/racks`
5. `GET /api/v1/protocols`

## 5.3 Cage/Animal

1. `GET /api/v1/cages`
2. `GET /api/v1/cages/{id}`
3. `POST /api/v1/cages/{id}/move`
4. `GET /api/v1/animals`
5. `GET /api/v1/animals/{id}`
6. `POST /api/v1/animals/{id}/events`

## 5.4 Breeding

1. `POST /api/v1/breedings`
2. `POST /api/v1/breedings/{id}/plug-check`
3. `POST /api/v1/litters`
4. `POST /api/v1/litters/{id}/wean`

## 5.5 Genotyping

1. `POST /api/v1/samples`
2. `POST /api/v1/genotyping-batches`
3. `POST /api/v1/genotyping-results/import`
4. `POST /api/v1/genotyping-results/confirm`

## 5.6 Cohort

1. `POST /api/v1/cohorts/query`
2. `POST /api/v1/cohorts`
3. `POST /api/v1/cohorts/{id}/lock`

## 5.7 Task/Report/Audit

1. `GET /api/v1/tasks`
2. `POST /api/v1/tasks/{id}/complete`
3. `GET /api/v1/reports/summary`
4. `GET /api/v1/audit-logs`
5. `GET /api/v1/exports/compliance`

## 6. 数据库实体（V1最小集）

1. users
2. roles
3. permissions
4. user_roles
5. protocols
6. strains
7. cages
8. animals
9. breeding_pairs
10. litters
11. samples
12. genotyping_batches
13. genotyping_results
14. cohorts
15. cohort_members
16. tasks
17. audit_logs
18. sync_events

## 7. 测试拆解

## 7.1 单元测试

1. 繁育时间窗计算
2. cohort 筛选条件引擎
3. 分型冲突判定
4. 权限判定

## 7.2 集成测试

1. 配种到断奶链路
2. 采样到分型回填链路
3. 离线写入到同步上送链路

## 7.3 UI测试

1. 扫码进笼卡
2. 断奶批量分笼流程
3. 今日任务完成流程

## 7.4 验收测试用例（示例）

1. 用例：超笼告警
- 前置：笼容量5只，当前5只
- 操作：新增第6只
- 预期：阻断或强提醒（按配置）+审计日志

2. 用例：协议过期阻断
- 前置：协议状态已过期
- 操作：执行关键操作（配种）
- 预期：阻断并提示续期

3. 用例：分型冲突
- 前置：同 sample 两条互斥结果
- 操作：导入结果
- 预期：标记冲突，要求人工确认

## 8. 设计规范拆解

## 8.1 视觉与组件

1. 组件优先：按钮、输入框、卡片、标签、时间轴统一组件化。
2. 状态颜色：
- 正常：绿色
- 风险：橙色
- 阻断：红色
3. 任务优先级视觉层级明确。

## 8.2 交互规范

1. 高风险动作必须二次确认。
2. 批量操作必须支持撤销。
3. 长列表提供固定筛选条。
4. 空状态提供下一步引导。

## 9. Sprint 规划（8周）

## Sprint 1（周1-2）

1. 基础框架、导航、登录、权限、审计骨架
2. 主数据（品系/房间/笼位）

## Sprint 2（周3-4）

1. 笼位与个体模块
2. 扫码与笼卡详情
3. 任务中心基础

## Sprint 3（周5-6）

1. 繁育流程（配种/查栓/断奶）
2. 分型流程（采样/批次/回填）

## Sprint 4（周7-8）

1. cohort 构建
2. 报表与导出
3. 性能优化与稳定性收敛

## 10. 资源与职责

1. 产品经理：需求控制、验收标准、优先级。
2. 交互设计师：任务流与关键页面原型。
3. Android工程师：端架构、页面、同步、扫码。
4. 后端工程师：API、规则引擎、审计与导出。
5. QA：链路测试与回归。
6. 生物学专家：规则与字段口径审校。

## 11. 里程碑验收（Go/No-Go）

1. M1：核心链路跑通（配种->断奶->分型->cohort）。
2. M2：审计和导出可用于模拟检查。
3. M3：关键性能达标，崩溃率达标。

## 12. 发布清单

1. 发布说明与变更日志。
2. 数据字典与接口文档。
3. 管理员手册和实验员速查手册。
4. 回滚策略与故障应急预案。
