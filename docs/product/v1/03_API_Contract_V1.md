# Micemice V1 API 契约草案

- 版本：V1.0
- 日期：2026-02-07
- 协议：HTTPS + JSON
- 鉴权：Bearer Token

## 1. 通用规范

1. Base URL：`/api/v1`
2. 时间格式：ISO-8601（UTC）
3. 分页：`page`, `page_size`, `total`
4. 幂等键：`X-Idempotency-Key`（写操作建议）

### 1.1 通用响应

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

### 1.2 通用错误码

1. `40001` 参数校验失败
2. `40101` 未登录或token失效
3. `40301` 权限不足
4. `40401` 资源不存在
5. `40901` 数据冲突
6. `42201` 业务规则阻断
7. `50001` 服务器异常

## 2. Auth

## POST /auth/login

请求：

```json
{
  "username": "alice",
  "password": "******",
  "org_code": "LAB001"
}
```

响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "access_token": "token",
    "refresh_token": "token",
    "expires_in": 7200,
    "user": {
      "id": "u_1",
      "name": "Alice",
      "roles": ["researcher"]
    }
  }
}
```

## POST /auth/refresh

## GET /me

## 3. Cage

## GET /cages

查询参数：`keyword`, `room_id`, `status`, `page`, `page_size`

## GET /cages/{id}

返回笼信息、在笼个体、最近事件。

## POST /cages/{id}/move

请求：

```json
{
  "target_cage_id": "cage_2001",
  "animal_ids": ["a_1", "a_2"],
  "reason": "weaning_split"
}
```

业务规则：

1. 目标笼容量校验。
2. 协议状态校验。

## 4. Animal

## GET /animals

查询参数：`strain_id`, `sex`, `genotype`, `status`, `age_min`, `age_max`

## GET /animals/{id}

返回基础信息、分型、家系、实验事件。

## POST /animals

## POST /animals/{id}/events

请求示例：

```json
{
  "event_type": "weight_measurement",
  "event_time": "2026-02-07T09:00:00Z",
  "payload": {
    "weight_g": 24.3
  }
}
```

## 5. Breeding

## POST /breedings

```json
{
  "male_id": "a_m_1",
  "female_id": "a_f_1",
  "mating_date": "2026-02-07",
  "protocol_id": "p_1"
}
```

返回：创建结果 + 自动任务列表。

## POST /breedings/{id}/plug-check

## POST /litters

## POST /litters/{id}/wean

```json
{
  "wean_date": "2026-03-01",
  "pup_assignments": [
    {"animal_id": "a_p_1", "target_cage_id": "c_101"},
    {"animal_id": "a_p_2", "target_cage_id": "c_102"}
  ]
}
```

## 6. Genotyping

## POST /samples

## POST /genotyping-batches

## POST /genotyping-results/import

支持 CSV 上传，返回成功条数与冲突条数。

## POST /genotyping-results/confirm

用于人工确认冲突。

## 7. Cohort

## POST /cohorts/query

请求示例：

```json
{
  "strain_ids": ["s_1"],
  "genotypes": ["+/-"],
  "sex": ["female"],
  "age_weeks": {"min": 8, "max": 12}
}
```

## POST /cohorts

## POST /cohorts/{id}/lock

## 8. Task

## GET /tasks

查询参数：`status`, `priority`, `assignee_id`, `date`

## POST /tasks/{id}/complete

## POST /tasks/{id}/reassign

## 9. Report

## GET /reports/summary

返回：笼位占用率、繁殖成功率、存活率、任务完成率。

## GET /reports/breeding-trend

## 10. Audit & Export

## GET /audit-logs

查询参数：`entity_type`, `entity_id`, `operator_id`, `from`, `to`

## GET /exports/compliance

查询参数：`protocol_id`, `from`, `to`, `format`

## 11. Webhook/Event（预留）

1. `task.overdue`
2. `protocol.expiring`
3. `cage.over_capacity`

## 12. 接口验收要求

1. 所有写操作可追踪 request_id。
2. 错误码和错误信息稳定可测。
3. 核心查询接口 p95 < 800ms。
4. 同一请求重试不产生重复副作用（幂等）。
