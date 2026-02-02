# ONLYOFFICE 配置说明

## 目录

1. [环境变量配置](#环境变量配置)
2. [网络环境配置](#网络环境配置)
3. [安全配置](#安全配置)
4. [性能优化](#性能优化)

---

## 环境变量配置

### Docker 环境变量

| 变量名 | 说明 | 默认值 | 必填 |
|--------|------|--------|------|
| `ONLYOFFICE_PORT` | ONLYOFFICE 服务端口 | 8080 | 否 |
| `BACKEND_PORT` | 后端服务端口 | 3000 | 否 |
| `JWT_SECRET` | JWT 密钥 | - | **是** |
| `JWT_EXPIRES_IN` | JWT 过期时间（秒） | 3600 | 否 |
| `DOCUMENT_SERVER_URL` | ONLYOFFICE 对外地址 | http://localhost:8080 | 是 |
| `BACKEND_CALLBACK_URL` | 回调地址 | http://host.docker.internal:3000 | 是 |
| `UPLOAD_DIR` | 文件存储目录 | ./uploads | 否 |
| `MAX_FILE_SIZE` | 最大文件大小（字节） | 104857600 | 否 |
| `LOG_LEVEL` | 日志级别 | info | 否 |

### 后端环境变量

**Java (application.yml):**
```yaml
onlyoffice:
  document-server:
    url: ${DOCUMENT_SERVER_URL:http://localhost:8080}
  backend:
    callback-url: ${BACKEND_CALLBACK_URL:http://host.docker.internal:3000}
  jwt:
    secret: ${JWT_SECRET:your-secret}
  storage:
    upload-dir: ${UPLOAD_DIR:./uploads}
```

**NestJS (.env):**
```env
PORT=3000
DOCUMENT_SERVER_URL=http://localhost:8080
BACKEND_CALLBACK_URL=http://host.docker.internal:3000
JWT_SECRET=your-secret
UPLOAD_DIR=./uploads
```

### 前端环境变量

```env
VUE_APP_API_BASE_URL=http://localhost:3000
VUE_APP_DOCUMENT_SERVER_URL=http://localhost:8080
```

---

## 网络环境配置

### 内网环境

**特点：**
- 使用内网 IP 访问
- 无需域名和 SSL 证书
- 需要允许私有 IP 访问

**配置示例：**
```env
# 假设服务器 IP 为 192.168.1.100
SERVER_HOST=192.168.1.100
PROTOCOL=http
DOCUMENT_SERVER_URL=http://192.168.1.100:8080
BACKEND_URL=http://192.168.1.100:3000
BACKEND_CALLBACK_URL=http://192.168.1.100:3000
```

**Docker 配置：**
```yaml
environment:
  - ALLOW_PRIVATE_IP_ADDRESS=true
  - ALLOW_META_IP_ADDRESS=true
```

**回调地址说明：**

| 操作系统 | Docker 网络模式 | 回调地址 |
|----------|----------------|----------|
| Windows | bridge（默认） | http://host.docker.internal:3000 |
| Linux | bridge | http://宿主机IP:3000 |
| Linux | host | http://localhost:3000 |

### 外网环境

**特点：**
- 使用域名访问
- 建议启用 HTTPS
- 需要配置 SSL 证书

**配置示例：**
```env
SERVER_HOST=docs.example.com
PROTOCOL=https
DOCUMENT_SERVER_URL=https://docs.example.com
BACKEND_URL=https://api.example.com
BACKEND_CALLBACK_URL=https://api.example.com
```

**Docker 配置：**
```yaml
environment:
  - ALLOW_PRIVATE_IP_ADDRESS=false
  - ALLOW_META_IP_ADDRESS=false
```

---

## 安全配置

### JWT 配置

**生成强密钥：**
```bash
# Linux/Mac
openssl rand -hex 32

# Windows PowerShell
[System.Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

**配置要点：**
1. JWT 密钥必须与 ONLYOFFICE 配置一致
2. 生产环境使用至少 32 字符的随机密钥
3. 不要在代码中硬编码密钥

### CORS 配置

**开发环境：**
```env
CORS_ORIGINS=*
```

**生产环境：**
```env
CORS_ORIGINS=https://your-frontend-domain.com,https://another-domain.com
```

### 文件安全

**允许的文件类型：**
```env
ALLOWED_FILE_TYPES=docx,xlsx,pptx,doc,xls,ppt,odt,ods,odp,pdf
```

**文件大小限制：**
```env
MAX_FILE_SIZE=104857600  # 100MB
```

### HTTPS 配置

**Nginx SSL 配置：**
```nginx
ssl_protocols TLSv1.2 TLSv1.3;
ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
ssl_prefer_server_ciphers off;
ssl_session_cache shared:SSL:10m;
ssl_session_timeout 10m;

# HSTS
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

# 安全头
add_header X-Frame-Options "SAMEORIGIN" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-XSS-Protection "1; mode=block" always;
```

---

## 性能优化

### ONLYOFFICE 优化

**Docker 资源限制：**
```yaml
services:
  onlyoffice:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 4G
        reservations:
          cpus: '1'
          memory: 2G
```

**日志级别：**
```env
LOG_LEVEL=warn  # 生产环境使用 warn 或 error
```

### Nginx 优化

```nginx
# 启用 Gzip
gzip on;
gzip_vary on;
gzip_proxied any;
gzip_comp_level 6;
gzip_types text/plain text/css application/json application/javascript;

# 静态资源缓存
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
    expires 30d;
    add_header Cache-Control "public, immutable";
}

# 连接优化
keepalive_timeout 65;
client_max_body_size 100M;
proxy_buffering off;
```

### 后端优化

**Java:**
```yaml
server:
  tomcat:
    max-threads: 200
    min-spare-threads: 10
```

**NestJS:**
```typescript
// 启用压缩
import * as compression from 'compression';
app.use(compression());
```

---

## 端口冲突处理

如果默认端口被占用，修改以下配置：

**ONLYOFFICE 端口（默认 8080）：**
```env
ONLYOFFICE_PORT=8081
DOCUMENT_SERVER_URL=http://localhost:8081
```

**后端端口（默认 3000）：**
```env
BACKEND_PORT=3001
BACKEND_URL=http://localhost:3001
BACKEND_CALLBACK_URL=http://host.docker.internal:3001
```

**前端端口（默认 8081）：**
```javascript
// vue.config.js
devServer: {
  port: 8082
}
```

---

## 配置验证

### 检查 ONLYOFFICE

```bash
# 健康检查
curl http://localhost:8080/healthcheck

# API 可用性
curl -I http://localhost:8080/web-apps/apps/api/documents/api.js
```

### 检查后端

```bash
# 健康检查
curl http://localhost:3000/api/health

# 文档列表
curl http://localhost:3000/api/docs
```

### 检查回调连通性

```bash
# 从 ONLYOFFICE 容器测试回调
docker exec onlyoffice-document-server curl http://host.docker.internal:3000/api/health
```
