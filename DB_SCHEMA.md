# DB_SCHEMA.md

## 1. 目标与范围

当前数据库设计服务于图片 OCR + 翻译同步流程，重点覆盖：
- 用户与配额
- 配额扣减流水
- 审计日志

说明：
- 原 `translation_tasks` / `task_artifacts` 任务模型已下线，不作为当前主流程。

数据库建议：
- 生产：MySQL 8+
- 本地开发：Oracle（可选）

---

## 2. 领域实体（当前）

- `users`：用户信息（可选登录）
- `quota_account`：用户配额账户
- `quota_transactions`：配额扣减/返还流水
- `audit_logs`：审计日志

---

## 3. 业务规则

1. 每次调用图片翻译接口（`/api/v1/image/translate`）扣减 1 次。
2. 当前默认免费额度：`free_total = 1000`。
3. 无登录模式下使用开发用户 `user_id = 1001`。
4. 配额不足时返回 `QUOTA_EXHAUSTED`。

---

## 4. DDL（MySQL 示例）

```sql
CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY,
  wechat_unionid VARCHAR(128),
  wechat_openid VARCHAR(128),
  role VARCHAR(32) NOT NULL DEFAULT 'USER',
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL
);

CREATE TABLE IF NOT EXISTS quota_account (
  user_id BIGINT PRIMARY KEY,
  free_total INT NOT NULL DEFAULT 1000,
  free_used INT NOT NULL DEFAULT 0,
  free_refunded INT NOT NULL DEFAULT 0,
  updated_at DATETIME(3) NOT NULL,
  CONSTRAINT fk_quota_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS quota_transactions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  action VARCHAR(16) NOT NULL,
  amount INT NOT NULL,
  reason VARCHAR(64) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  CONSTRAINT fk_quota_tx_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  action VARCHAR(64) NOT NULL,
  detail TEXT,
  created_at DATETIME(3) NOT NULL
);
```

---

## 5. 索引建议

```sql
CREATE INDEX idx_quota_tx_user_created ON quota_transactions(user_id, created_at DESC);
CREATE INDEX idx_audit_user_created ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_action_created ON audit_logs(action, created_at DESC);
```

---

## 6. 配额扣减事务建议

```sql
BEGIN;

SELECT * FROM quota_account WHERE user_id = ? FOR UPDATE;

-- 计算 remaining = free_total + free_refunded - free_used
-- 若 <= 0 则回滚并返回 QUOTA_EXHAUSTED

UPDATE quota_account
SET free_used = free_used + 1,
    updated_at = NOW(3)
WHERE user_id = ?;

INSERT INTO quota_transactions(user_id, action, amount, reason, created_at)
VALUES (?, 'consume', 1, 'image_translate', NOW(3));

COMMIT;
```

---

## 7. 审计动作建议

- `image.translate.request`
- `image.translate.success`
- `image.translate.failed`
- `quota.consume`
- `quota.exhausted`

---

## 8. 已下线数据结构

以下结构保留历史兼容信息，但不再作为当前主流程依赖：
- `translation_tasks`
- `task_artifacts`

后续如确认不再回退异步任务模式，可执行正式迁移脚本删除。
