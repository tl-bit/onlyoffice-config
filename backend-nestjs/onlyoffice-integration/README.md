# ONLYOFFICE NestJS 后端集成

基于 NestJS 的 ONLYOFFICE Document Server 集成服务。

## 项目结构

```
src/
├── main.ts                         # 启动入口
├── app.module.ts                   # 主模块
└── document/
    ├── document.module.ts          # 文档模块
    ├── document.controller.ts      # REST API 控制器
    ├── document.service.ts         # 文档服务
    ├── jwt.service.ts              # JWT 服务
    ├── file-storage.service.ts     # 文件存储服务
    └── dto/
        ├── document-config.dto.ts  # 编辑器配置 DTO
        └── callback.dto.ts         # 回调 DTO
```

## 快速开始

### 1. 安装依赖

```bash
npm install
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 文件
```

关键配置项：

```env
PORT=3000
DOCUMENT_SERVER_URL=http://localhost:8080
BACKEND_CALLBACK_URL=http://host.docker.internal:3000
JWT_SECRET=your-super-secret-key
```

### 3. 运行

```bash
# 开发模式
npm run start:dev

# 生产模式
npm run build
npm run start:prod
```

## API 文档

### 获取文档配置

```http
GET /api/doc/{id}?fileType=docx&userId=user1&userName=张三&mode=edit
```

### ONLYOFFICE 回调

```http
POST /api/office/callback
Content-Type: application/json

{
  "status": 2,
  "key": "...",
  "url": "...",
  "token": "..."
}
```

### 获取文档列表

```http
GET /api/docs
```

### 上传文档

```http
POST /api/docs/upload
Content-Type: multipart/form-data

file: (binary)
```

### 删除文档

```http
DELETE /api/docs/{id}?fileType=docx
```

### 健康检查

```http
GET /api/health
```

## 配置说明

| 环境变量 | 说明 | 默认值 |
|----------|------|--------|
| `PORT` | 服务端口 | 3000 |
| `DOCUMENT_SERVER_URL` | ONLYOFFICE 地址 | http://localhost:8080 |
| `BACKEND_CALLBACK_URL` | 回调地址 | http://host.docker.internal:3000 |
| `JWT_SECRET` | JWT 密钥 | - |
| `UPLOAD_DIR` | 上传目录 | ./uploads |
| `ALLOWED_FILE_TYPES` | 允许的文件类型 | docx,xlsx,pptx |
| `MAX_FILE_SIZE` | 最大文件大小 | 104857600 |
| `CORS_ORIGINS` | CORS 来源 | * |
