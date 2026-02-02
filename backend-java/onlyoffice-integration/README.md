# ONLYOFFICE Java 后端集成

基于 Spring Boot 的 ONLYOFFICE Document Server 集成服务。

## 项目结构

```
src/main/java/com/example/onlyoffice/
├── OnlyOfficeApplication.java      # 启动类
├── config/
│   ├── OnlyOfficeProperties.java   # 配置属性类
│   └── WebConfig.java              # Web 配置（跨域、静态资源）
├── controller/
│   └── DocumentController.java     # REST API 控制器
├── dto/
│   ├── DocumentConfigDTO.java      # 编辑器配置 DTO
│   ├── CallbackDTO.java            # 回调请求 DTO
│   └── CallbackResponseDTO.java    # 回调响应 DTO
├── service/
│   ├── DocumentService.java        # 文档服务
│   ├── FileStorageService.java     # 文件存储服务
│   └── JwtService.java             # JWT 服务
└── exception/
    ├── GlobalExceptionHandler.java # 全局异常处理
    ├── DocumentException.java
    ├── FileNotFoundException.java
    ├── FileStorageException.java
    └── InvalidFileException.java
```

## 快速开始

### 1. 配置环境变量

复制并修改配置文件：

```bash
cp src/main/resources/application.yml src/main/resources/application-local.yml
```

关键配置项：

```yaml
onlyoffice:
  document-server:
    url: http://localhost:8080          # ONLYOFFICE 地址
  backend:
    callback-url: http://host.docker.internal:3000  # 回调地址
  jwt:
    secret: your-super-secret-key       # JWT 密钥（必须与 ONLYOFFICE 一致）
```

### 2. 构建运行

```bash
# 构建
mvn clean package -DskipTests

# 运行
java -jar target/onlyoffice-integration-1.0.0.jar

# 或使用 Maven
mvn spring-boot:run
```

### 3. 使用环境变量

```bash
# Linux/Mac
export JWT_SECRET=my-production-secret
export DOCUMENT_SERVER_URL=https://docs.example.com
export BACKEND_CALLBACK_URL=https://api.example.com
java -jar target/onlyoffice-integration-1.0.0.jar

# Windows PowerShell
$env:JWT_SECRET="my-production-secret"
$env:DOCUMENT_SERVER_URL="https://docs.example.com"
$env:BACKEND_CALLBACK_URL="https://api.example.com"
java -jar target/onlyoffice-integration-1.0.0.jar
```

## API 文档

### 获取文档配置

```http
GET /api/doc/{id}?fileType=docx&userId=user1&userName=张三&mode=edit
```

**参数：**
- `id` - 文档 ID（不含扩展名）
- `fileType` - 文件类型，默认 docx
- `userId` - 用户 ID（可选）
- `userName` - 用户名称（可选）
- `mode` - 编辑模式: edit/view，默认 edit

**响应：**
```json
{
  "document": {
    "fileType": "docx",
    "key": "dGVzdA==_1704067200000",
    "title": "test.docx",
    "url": "http://host.docker.internal:3000/uploads/test.docx",
    "permissions": {
      "download": true,
      "edit": true,
      "print": true
    }
  },
  "editorConfig": {
    "callbackUrl": "http://host.docker.internal:3000/api/office/callback",
    "lang": "zh-CN",
    "mode": "edit",
    "user": {
      "id": "user1",
      "name": "张三"
    }
  },
  "documentType": "word",
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

### ONLYOFFICE 回调

```http
POST /api/office/callback
Content-Type: application/json

{
  "status": 2,
  "key": "dGVzdA==_1704067200000",
  "url": "http://...",
  "token": "..."
}
```

**响应：**
```json
{
  "error": 0
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

| 配置项 | 环境变量 | 说明 | 默认值 |
|--------|----------|------|--------|
| `server.port` | `BACKEND_PORT` | 服务端口 | 3000 |
| `onlyoffice.document-server.url` | `DOCUMENT_SERVER_URL` | ONLYOFFICE 地址 | http://localhost:8080 |
| `onlyoffice.backend.callback-url` | `BACKEND_CALLBACK_URL` | 回调地址 | http://host.docker.internal:3000 |
| `onlyoffice.jwt.secret` | `JWT_SECRET` | JWT 密钥 | - |
| `onlyoffice.storage.upload-dir` | `UPLOAD_DIR` | 上传目录 | ./uploads |

## 注意事项

1. **JWT 密钥**：必须与 ONLYOFFICE Document Server 配置的密钥一致
2. **回调地址**：必须是 ONLYOFFICE 容器能够访问的地址
3. **文件存储**：生产环境建议使用对象存储（如 S3、OSS）
4. **安全性**：生产环境请限制 CORS 来源
