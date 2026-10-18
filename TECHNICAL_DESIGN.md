# DocBridge MVP 技术方案（图片 OCR + 翻译）

## 1. 范围与目标

本文档定义当前 MVP 的可落地技术实现。

MVP 范围（当前）：
- 仅支持图片输入：`JPG`、`PNG`
- 识别链路：图片上传 -> OCR -> 翻译 -> 在线对照预览
- 结果形态：左侧原图，右侧原文/译文列表
- 配额控制：每次调用接口扣减 1 次免费额度

MVP 暂不包含：
- PDF/DOCX/PPTX/XLSX 处理
- 异步任务队列、任务状态页、下载文件
- 登录强依赖（当前可在无登录开发模式运行）

## 2. 系统架构

总体组件：
- 前端：Vue 3 + TypeScript + Vite + Element Plus
- 后端：Spring Boot 3（Java 17+）
- 数据库：本地开发 Oracle（可选），生产 MySQL
- OCR：腾讯云 OCR（GeneralBasicOCR）
- 翻译：腾讯云机器翻译 TMT（TextTranslate）
- 网关：Nginx（本地/生产均可统一入口）

调用链路（同步）：
1. 用户在上传页上传或粘贴截图。
2. 前端调用 `POST /api/v1/image/translate`。
3. 后端做图片校验与配额扣减。
4. 后端调用腾讯 OCR，获取文本行与位置信息。
5. 后端调用腾讯翻译，按 OCR 行逐条翻译。
6. 返回结构化结果，前端跳转结果页展示左图右文。

## 3. 输入边界与校验

- 单次仅 1 张图片
- 支持格式：`JPG` / `PNG`
- 单文件大小：`<= 10MB`
- 不支持非图片类型、空文件、损坏文件

错误码：
- `BAD_REQUEST`
- `UNSUPPORTED_FORMAT`
- `FILE_TOO_LARGE`
- `OCR_FAILED`
- `TRANSLATION_FAILED`
- `QUOTA_EXHAUSTED`

## 4. 核心数据模型（当前仍保留）

- `users`
- `quota_account`
- `audit_logs`

说明：
- `task` 相关模型与逻辑已下线，不再参与流程。
- 配额按用户维度扣减，每次图片翻译调用扣 1 次。

## 5. 后端模块说明

- `image.controller.ImageTranslateController`
  - `POST /api/v1/image/translate`
- `image.service.ImageTranslateService`
  - 图片校验、配额扣减、OCR+翻译编排
- `processing.ocr.TencentOcrService`
  - 腾讯 OCR TC3 签名调用
- `processing.translate.TencentTranslatorService`
  - 腾讯翻译 TC3 签名调用

## 6. 前端页面与路由

- `/upload`：中心化上传页（支持拖拽/点击/Ctrl+V）
- `/result`：结果页（左图右文对照，支持复制全部）

状态管理：
- `stores/translate.ts` 保存本次识别翻译结果，支持刷新恢复（sessionStorage）

## 7. 可观测性

建议记录：
- OCR 成功率、失败率、平均耗时
- 翻译成功率、失败率、平均耗时
- 接口总耗时 P95
- 配额消耗量与剩余额度
- 错误码分布（特别是云服务未开通、鉴权失败）

## 8. 安全与密钥管理

- 不在仓库写明文密钥
- 所有腾讯密钥使用环境变量注入
- 泄露后必须立即轮换 `SecretKey`

关键环境变量：
- `APP_OCR_TENCENT_SECRET_ID`
- `APP_OCR_TENCENT_SECRET_KEY`
- `APP_TRANSLATE_TENCENT_SECRET_ID`
- `APP_TRANSLATE_TENCENT_SECRET_KEY`
- `APP_JWT_SECRET`

## 9. 部署方案（更新）

### 9.1 本地无 Docker

- Nginx: `80`
- Frontend dev: `5173`
- Backend: `8090`

### 9.2 Docker 部署（推荐生产方式）

已提供文件：
- `backend/Dockerfile`
- `frontend/Dockerfile`
- `docker-compose.yml`
- `deploy/nginx/prod.conf`
- `.env.example`

容器拓扑：
- `frontend`（Nginx + dist）
- `backend`（Spring Boot）
- `mysql`（MySQL 8.4）

启动：
```powershell
cd C:\devlopEnv\project\translate
Copy-Item .env.example .env
docker compose up -d --build
```

访问：
- `http://localhost`

## 10. 测试策略

功能测试：
- JPG/PNG 上传与预览
- OCR 识别输出正确
- 翻译输出正确
- 配额扣减与不足拦截

异常测试：
- 腾讯 OCR 未开通
- 腾讯密钥错误
- 网络超时
- 超大文件、错误格式

## 11. 后续演进

- 增加 OCR 框与结果联动高亮
- 增加网页翻译能力
- 恢复登录后按真实用户维度配额统计
- 再评估文档类格式（PDF/DOCX/PPTX）回归计划
