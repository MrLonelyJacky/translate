CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  wechat_unionid VARCHAR(128) NOT NULL,
  wechat_openid VARCHAR(128),
  role VARCHAR(32) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_users_unionid (wechat_unionid)
);

CREATE TABLE IF NOT EXISTS quota_account (
  user_id BIGINT PRIMARY KEY,
  free_total INT NOT NULL,
  free_used INT NOT NULL,
  free_refunded INT NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  CONSTRAINT fk_quota_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS translation_task (
  task_id VARCHAR(32) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  source_filename VARCHAR(255) NOT NULL,
  source_type VARCHAR(16) NOT NULL,
  source_size BIGINT NOT NULL,
  source_path VARCHAR(512),
  status VARCHAR(32) NOT NULL,
  stage VARCHAR(32) NOT NULL,
  retry_count INT NOT NULL,
  error_code VARCHAR(64),
  error_message VARCHAR(255),
  created_at DATETIME(3) NOT NULL,
  started_at DATETIME(3),
  finished_at DATETIME(3),
  updated_at DATETIME(3) NOT NULL,
  KEY idx_task_user_created (user_id, created_at),
  KEY idx_task_status_created (status, created_at),
  CONSTRAINT fk_task_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS task_artifact (
  task_id VARCHAR(32) PRIMARY KEY,
  source_path VARCHAR(512),
  preview_path VARCHAR(512),
  output_path VARCHAR(512),
  expires_at DATETIME(3) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  CONSTRAINT fk_artifact_task FOREIGN KEY (task_id) REFERENCES translation_task(task_id)
);

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  task_id VARCHAR(32),
  action VARCHAR(64) NOT NULL,
  ip VARCHAR(64),
  user_agent VARCHAR(255),
  detail VARCHAR(255),
  created_at DATETIME(3) NOT NULL,
  KEY idx_audit_user_created (user_id, created_at),
  KEY idx_audit_task_created (task_id, created_at)
);

