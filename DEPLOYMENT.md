# DocBridge 生产部署指南

本文说明在 Linux 生产服务器上部署 DocBridge：Vue 静态前端由 Docker 中的
Nginx 提供，Spring Boot 后端也运行在 Docker 中，MySQL 使用 Docker 外部的
独立实例。下文假定仓库已部署到服务器，且 Docker Engine 与 Docker Compose
Plugin 已安装。

## 1. 部署拓扑与端口

```text
浏览器
  │ HTTPS（建议由云负载均衡/反向代理终止 TLS）
  ▼
宿主机:80 ── Docker Nginx（Vue 静态资源，/api 转发）
                    │ Docker 内部网络
                    └── Spring Boot:8090
                              │ TCP 3306
                              ▼
                         外部 MySQL
```

- 仅 Nginx 发布宿主机端口 `80`；后端 `8090` 仅在 Compose 网络内可访问。
- MySQL 不属于 Compose 服务，不会随应用启动、停止或销毁。
- 数据库地址必须从后端容器可达。优先使用数据库私网 IP 或私有 DNS 名称，
  不要使用容器内的 `127.0.0.1` 或 Compose 服务名 `mysql`。
- 当前 Nginx 配置只监听 HTTP/80。生产环境应在前置负载均衡/反向代理配置
  HTTPS，并将请求安全转发到此 Nginx；不要将未加密 HTTP 当作公网生产入口。

## 2. 准备 MySQL

在外部 MySQL 实例上创建专用数据库和最小权限应用账号；不要使用 root 账号
连接应用。以下 SQL 由 DBA 按实例策略执行，并替换密码及主机限制：

```sql
CREATE DATABASE docbridge
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER 'docbridge'@'应用服务器或私网网段'
  IDENTIFIED BY '替换为强密码';
GRANT SELECT, INSERT, UPDATE, DELETE
  ON docbridge.* TO 'docbridge'@'应用服务器或私网网段';
```

根据数据库账号的宿主限制语法和实际网络环境调整 `应用服务器或私网网段`。
仅允许应用服务器访问 MySQL 的 TCP `3306`；不要将 `3306` 暴露到公网。
示例 `.env.example` 使用 `sslMode=REQUIRED` 加密数据库连接。正式环境优先使用
`sslMode=VERIFY_IDENTITY` 并配置受信任的 CA 证书；若数据库不支持 TLS，只能在
隔离的可信私网内连接，并评估链路风险。

应用不会自动建表（`spring.sql.init.mode=never`）。首次部署或新增表前，
备份数据库，并由 DBA/迁移账号执行 schema 脚本，例如：

```sh
mysql --host=数据库私网地址 --user=迁移账号 --password docbridge \
  < backend/src/main/resources/schema.sql
```

该迁移账号应与应用运行账号分离。执行前检查 SQL 与当前数据库状态；脚本只包含
`CREATE TABLE IF NOT EXISTS`，不会替代升级迁移，也不会自动处理既有结构变化。

## 3. 配置生产环境变量

在仓库根目录创建 `.env`，内容以 `.env.example` 为模板。示例文件中的值均为
占位值，不能直接用于生产：

```sh
cp .env.example .env
chmod 600 .env
```

至少替换并检查以下配置：

| 变量 | 说明 |
| --- | --- |
| `DB_URL` | 外部 MySQL JDBC 地址，例如 `jdbc:mysql://10.0.0.10:3306/docbridge?...`；地址需从后端容器可达 |
| `DB_USERNAME` / `DB_PASSWORD` | 上一步创建的专用数据库账号及强密码 |
| `APP_JWT_SECRET` | 至少 32 字符的随机密钥，所有后端实例保持一致 |
| `APP_OCR_TENCENT_SECRET_ID` / `APP_OCR_TENCENT_SECRET_KEY` | 腾讯云 OCR 凭证 |
| `APP_TRANSLATE_TENCENT_SECRET_ID` / `APP_TRANSLATE_TENCENT_SECRET_KEY` | 腾讯云翻译凭证 |
| `APP_WECHAT_APP_ID` / `APP_WECHAT_APP_SECRET` | 微信开放平台网站应用凭证 |
| `APP_WECHAT_REDIRECT_URI` | 微信开放平台已登记的 HTTPS 回调地址 |
| `APP_WECHAT_FRONTEND_ORIGIN` | 对外 HTTPS 网站源，需与实际域名完全一致 |

Compose 强制开启 `APP_AUTH_ENABLED` 与 `APP_WECHAT_ENABLED`，并要求配置有效的
微信开放平台应用。不要改为关闭认证的共享开发用户模式对公网提供服务。不要将
真实 `.env` 提交到版本库或放入镜像。

如果数据库密码中含有 JDBC URL 特殊字符，密码应配置在 `DB_PASSWORD`，不要
拼入 URL。若密码被用于 URL 参数，则需按 JDBC URL 规则正确编码。

## 4. 构建并启动

在仓库根目录执行：

```sh
docker compose config
docker compose up -d --build
```

第一条命令用于检查 Compose 展开结果；确认数据库地址、镜像配置及环境变量
没有误填后再执行启动命令。Nginx 对外提供宿主机 `80` 端口；如该端口已被
占用，先调整 `docker-compose.yml` 中 `frontend.ports` 并同步更新防火墙规则。

检查容器和启动日志：

```sh
docker compose ps
docker compose logs --tail=200 backend frontend
```

浏览器访问绑定的域名。健康检查地址为
`https://your-domain.example/api/actuator/health`（若通过 HTTPS 前置代理）；
也可以在服务器本机使用 `curl http://127.0.0.1/api/actuator/health` 检查
Nginx 到后端的路由。

## 5. 网络与防火墙

- 公网仅开放 HTTPS 入口（以及必要时用于证书签发的 HTTP/80）。
- 如果采用外部 TLS 终止，限制宿主机 `80` 仅允许负载均衡器/反向代理访问，
  或按实际网关架构开放。
- MySQL 防火墙只允许应用服务器私网地址访问 `3306`。
- 不对公网发布后端 `8090`，当前 Compose 仅通过 `expose` 在 Docker 网络内部
  暴露该端口。

## 6. 更新、停止与持久化

发布新版本时先备份数据库，再在新代码目录执行：

```sh
docker compose up -d --build
docker compose ps
docker compose logs --tail=200 backend frontend
```

停止应用但保留持久化数据：

```sh
docker compose down
```

后端图片存储写入命名卷 `backend-storage`，`docker compose down` 不会删除该卷。
MySQL 数据完全由外部数据库负责。定期备份数据库和 Docker 卷；恢复演练应在
非生产环境完成。不要使用 `docker compose down -v`，否则会删除后端存储卷。

## 7. 上线前检查

- `.env` 已使用生产凭证，权限受限，且不在 Git 跟踪范围内。
- 数据库已初始化；运行账号仅有应用所需权限，迁移由独立 DBA/迁移账号执行。
- 从后端容器网络可连接数据库。
- 微信回调域名、HTTPS 与 `APP_WECHAT_FRONTEND_ORIGIN` 一致。
- OCR/翻译云服务已开通，地域和凭证匹配。
- 应用仍强制仅接受 JPG/PNG 且图片最大 10 MB；Nginx/Servlet 限制不能被误当作业务校验。
- HTTPS、日志、数据库备份与恢复流程已验证。

## 8. 常见故障定位

- **后端无法启动或数据库连接失败**：核对 `DB_URL` 中的数据库私网地址、端口、
  数据库名、账号授权范围、MySQL 防火墙，以及容器到数据库的网络连通性。
- **浏览器返回 502**：查看 `docker compose logs backend frontend`，确认后端已
  正常启动；Compose 内 Nginx 使用 `backend:8090` 访问后端。
- **微信登录失败**：检查开放平台授权回调域名、回调 URL、App ID/Secret 和
  HTTPS 前置代理设置。
- **OCR/翻译失败**：确认云服务已开通、凭证有效、地域正确，并检查后端日志中的
  稳定错误码；不要将密钥或图片内容写入公开日志。
