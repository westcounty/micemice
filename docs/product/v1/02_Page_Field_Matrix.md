# Micemice V1 页面与字段矩阵

- 版本：V1.0
- 日期：2026-02-07

## 说明

1. `M`=必填，`O`=可选，`R`=只读，`C`=计算字段。
2. 核心页面字段以移动端录入效率优先。

## 登录页

| 字段 | 类型 | 规则 | 必填 |
|---|---|---|---|
| username | string | 4-64字符 | M |
| password | string | 8-128字符 | M |
| org_code | string | 组织编码 | O |

## 工作台

| 字段 | 类型 | 规则 | 必填 |
|---|---|---|---|
| today_task_count | int | 今日任务总数 | R |
| overdue_count | int | 逾期任务数 | R |
| protocol_risk_count | int | 协议风险数 | R |
| quick_actions | list | 快捷操作列表 | R |

## 笼卡详情页

| 字段 | 类型 | 规则 | 必填 |
|---|---|---|---|
| cage_id | string | 唯一ID | R |
| room_code | string | 房间编码 | R |
| rack_code | string | 机架编码 | R |
| slot_code | string | 笼位编码 | R |
| capacity_limit | int | 1-99 | R |
| current_count | int | 实时数量 | C |
| status | enum | active/closed/quarantine | R |
| tag_list | list | 标签 | O |

## 个体详情页

| 字段 | 类型 | 规则 | 必填 |
|---|---|---|---|
| animal_id | string | 唯一ID | R |
| identifier | string | 耳号或RFID | M |
| sex | enum | male/female/unknown | M |
| date_of_birth | date | 出生日期 | M |
| strain_id | string | 品系ID | M |
| genotype | string | 受模板校验 | O |
| father_id | string | 父本ID | O |
| mother_id | string | 母本ID | O |
| current_cage_id | string | 当前笼ID | R |
| status | enum | active/experiment/retired/dead | M |
| protocol_id | string | 协议ID | O |

## 配种向导页

| 字段 | 类型 | 规则 | 必填 |
|---|---|---|---|
| male_id | string | 必须为雄性且可用 | M |
| female_id | string | 必须为雌性且可用 | M |
| mating_date | date | 默认当天 | M |
| expected_plug_check_date | date | 自动计算可编辑 | O |
| expected_wean_date | date | 自动计算可编辑 | O |
| protocol_id | string | 关键操作建议必选 | O |
| notes | string | <=500 | O |

## 断奶向导页

| 字段 | 类型 | 规则 | 必填 |
|---|---|---|---|
| litter_id | string | 当前窝ID | M |
| pup_ids | list | 幼鼠ID列表 | M |
| target_cage_ids | list | 目标笼位 | M |
| wean_date | date | 默认当天 | M |
| keep_for_breeding | bool | 是否留种 | O |

## 分型采样页

| 字段 | 类型 | 规则 | 必填 |
|---|---|---|---|
| sample_id | string | 唯一ID | R |
| animal_id | string | 个体ID | M |
| sample_type | enum | ear/tail/blood | M |
| sampled_at | datetime | 采样时间 | M |
| operator_id | string | 操作者 | M |
| batch_id | string | 可后绑定 | O |

## 分型回填页

| 字段 | 类型 | 规则 | 必填 |
|---|---|---|---|
| batch_id | string | 批次ID | M |
| sample_id | string | 样本ID | M |
| marker | string | 位点 | M |
| call_value | string | +/+、+/-、-/- 等 | M |
| confidence | decimal | 0-1 | O |
| version | int | 自动递增 | R |
| reviewer_id | string | 审核人 | O |

## Cohort构建页

| 字段 | 类型 | 规则 | 必填 |
|---|---|---|---|
| cohort_name | string | 1-100字符 | M |
| project_id | string | 课题ID | M |
| criteria_strain | list | 品系条件 | O |
| criteria_genotype | list | 基因型条件 | O |
| criteria_age_range | object | 起止周龄 | O |
| criteria_sex | enum/list | 性别条件 | O |
| candidate_count | int | 候选数量 | C |
| lock_flag | bool | 是否锁定 | M |

## 任务中心页

| 字段 | 类型 | 规则 | 必填 |
|---|---|---|---|
| task_id | string | 唯一ID | R |
| task_type | enum | breeding/weaning/move/compliance | R |
| due_time | datetime | 截止时间 | R |
| priority | enum | high/medium/low | R |
| assignee_id | string | 责任人 | R |
| status | enum | todo/doing/done/overdue | R |

## 报表页

| 字段 | 类型 | 规则 | 必填 |
|---|---|---|---|
| time_range | daterange | 时间范围 | M |
| project_filter | string | 项目过滤 | O |
| strain_filter | string | 品系过滤 | O |
| cage_occupancy_rate | decimal | 0-1 | C |
| breeding_success_rate | decimal | 0-1 | C |
| survival_rate | decimal | 0-1 | C |

## 管理中心页

| 字段 | 类型 | 规则 | 必填 |
|---|---|---|---|
| role_name | string | 角色名唯一 | M |
| permission_keys | list | 权限键集合 | M |
| dictionary_item | string | 字典项值 | M |
| is_active | bool | 启用状态 | M |

## 表单交互规则

1. 默认仅展示必填字段，更多选项折叠显示。
2. 校验失败就地提示，不中断整个表单。
3. 成功提交后显示可撤销入口（限时）。
4. 所有删除操作采用软删除。
