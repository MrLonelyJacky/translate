# API_SPEC.md

## 1. 文档说明

- 版本：v2（图片 OCR + 翻译）
- 基础路径：`/api/v1`
- 数据格式：`application/json`（上传接口使用 `multipart/form-data`）
- 鉴权：当前可关闭（开发模式）；后续可接入 JWT

统一响应：

```json
{
  "code": "OK",
  "message": "success",
  "requestId": "...",
  "data": {}
}
```

错误响应：

```json
{
  "code": "OCR_FAILED",
  "message": "腾讯 OCR 失败: FailedOperation.UnOpenError - 服务未开通",
  "requestId": "...",
  "data": null
}
```

---

## 2. OCR 与翻译接口

### 2.1 图片 OCR 识别

- 方法：`POST`
- 路径：`/image/ocr`
- 鉴权：可选（当前开发模式可无鉴权）
- `Content-Type`：`multipart/form-data`

表单字段：
- `file`：图片文件（必填，`JPG/PNG`）

业务规则：
- 每次成功进入 OCR 处理流程扣减 1 次 OCR 配额
- 限制大小：`<= 10MB`

响应 `200`：

```json
{
  "code": "OK",
  "message": "success",
  "requestId": "...",
  "data": {
    "total": 2,
    "recognizedText": "Hello World\nSettings",
    "lines": [
      {
        "order": 1,
        "sourceText": "Hello World",
        "left": 100,
        "top": 120,
        "right": 260,
        "bottom": 150
      },
      {
        "order": 2,
        "sourceText": "Settings",
        "left": 98,
        "top": 200,
        "right": 210,
        "bottom": 228
      }
    ]
  }
}
```

### 2.2 文本翻译（翻译编辑后的识别文本）

- 方法：`POST`
- 路径：`/text/translate`
- 鉴权：可选（当前开发模式可无鉴权）
- `Content-Type`：`application/json`

请求体：

```json
{
  "text": "Hello World\nSettings"
}
```

业务规则：
- 每次调用翻译接口扣减 1 次翻译配额
- `text` 不能为空（空值/全空白返回 `BAD_REQUEST`）

响应 `200`：

```json
{
  "code": "OK",
  "message": "success",
  "requestId": "...",
  "data": {
    "total": 2,
    "lines": [
      {
        "order": 1,
        "sourceText": "Hello World",
        "translatedText": "你好，世界",
        "left": null,
        "top": null,
        "right": null,
        "bottom": null
      },
      {
        "order": 2,
        "sourceText": "Settings",
        "translatedText": "设置",
        "left": null,
        "top": null,
        "right": null,
        "bottom": null
      }
    ]
  }
}
```

---

## 3. 配额接口

### 3.1 查询当前用户配额

- 方法：`GET`
- 路径：`/quota`

响应 `200`：

```json
{
  "code": "OK",
  "message": "success",
  "requestId": "...",
  "data": {
    "ocrTotal": 1000,
    "ocrUsed": 12,
    "ocrRefunded": 0,
    "ocrRemaining": 988,
    "translateTotal": 1000,
    "translateUsed": 5,
    "translateRefunded": 0,
    "translateRemaining": 995
  }
}
```

---

## 4. 登录接口（保留扩展）

### 4.1 微信登录（可选）

- 方法：`POST`
- 路径：`/auth/wechat/login`

说明：
- 当前可在无登录模式运行；如启用鉴权，返回 JWT。

---

## 5. 错误码定义（当前）

| 错误码 | HTTP | 说明 |
|---|---:|---|
| `OK` | 200 | 成功 |
| `BAD_REQUEST` | 400 | 参数错误/空文件 |
| `UNSUPPORTED_FORMAT` | 400 | 仅支持 JPG/PNG |
| `FILE_TOO_LARGE` | 400 | 超过 10MB |
| `QUOTA_NOT_FOUND` | 403 | 配额信息不存在 |
| `QUOTA_EXHAUSTED` | 403 | 免费配额已用尽 |
| `OCR_FAILED` | 502 | OCR 调用失败 |
| `TRANSLATION_FAILED` | 502 | 翻译调用失败 |
| `UNAUTHORIZED` | 401 | 未登录或 token 无效（启用鉴权时） |
| `INTERNAL_ERROR` | 500 | 服务内部错误 |

---

## 6. 已下线接口（不再使用）

以下异步任务接口已下线：
- `POST /tasks/upload`
- `GET /tasks`
- `GET /tasks/{taskId}`
- `POST /tasks/{taskId}/cancel`
- `GET /tasks/{taskId}/preview`
- `GET /tasks/{taskId}/download`

---

## 7. 限流与性能建议

建议值（可配置）：
- 图片 OCR 接口：`5 req/min/用户`
- 文本翻译接口：`5 req/min/用户`
- 超时控制：`20s`~`30s`（OCR/翻译外部依赖）

---

## 8. 安全要求

- 禁止在配置文件写明文云密钥
- 统一通过环境变量注入：
  - `APP_OCR_TENCENT_SECRET_ID`
  - `APP_OCR_TENCENT_SECRET_KEY`
  - `APP_TRANSLATE_TENCENT_SECRET_ID`
  - `APP_TRANSLATE_TENCENT_SECRET_KEY`
