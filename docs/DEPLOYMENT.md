# ONLYOFFICE 部署指南

## 目录

1. [环境准备](#环境准备)
2. [Docker 部署](#docker-部署)
3. [后端部署](#后端部署)
4. [前端部署](#前端部署)
5. [Nginx 配置](#nginx-配置)
6. [SSL 证书配置](#ssl-证书配置)

---

## 环境准备

### 系统要求

| 组件 | 最低配置 | 推荐配置 |
|------|----------|----------|
| CPU | 2 核 | 4 核+ |
| 内存 | 4 GB | 8 GB+ |
| 磁盘 | 20 GB | 50 GB+ |
| Docker | 20.10+ | 最新版 |

### 安装 Docker

**Linux (Ubuntu/Debian):**
```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
# 重新登录后生效
```

**Linux (CentOS/RHEL):**
```bash
sudo yum install -y yum-utils
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo systemctl start docker
sudo systemctl enable docker
```

**Windows:**
1. 下载并安装 [Docker Desktop](https://www.docker.com/products/docker-desktop)
2. 启用 WSL2 后端（推荐）
3. 重启电脑

### 安装 Docker Compose

**Linux:**
```bash
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

**Windows:**
Docker Desktop 已包含 Docker Compose。

---

## Docker 部署

### 1. 配置环境变量

```bash
cd production-project/docker
cp .env.example .env
```

编辑 `.env` 文件，根据实际环境修改配置：

```bash
# 内网环境示例
ONLYOFFICE_PORT=8080
BACKEND_PORT=3000
JWT_SECRET=your-strong-secret-key-here
SERVER_HOST=192.168.1.100
DOCUMENT_SERVER_URL=http://192.168.1.100:8080
BACKEND_CALLBACK_URL=http://192.168.1.100:3000

# 外网环境示例
ONLYOFFICE_PORT=8080
BACKEND_PORT=3000
JWT_SECRET=your-strong-secret-key-here
SERVER_HOST=docs.example.com
PROTOCOL=https
DOCUMENT_SERVER_URL=https://docs.example.com
BACKEND_CALLBACK_URL=https://api.example.com
```

### 2. 启动 ONLYOFFICE

**Linux 内网 HTTP:**
```bash
cd docker/linux
docker-compose -f docker-compose.yml -f docker-compose.intranet.yml up -d
```

**Linux 外网 HTTPS:**
```bash
cd docker/linux
docker-compose -f docker-compose.yml -f docker-compose.internet.yml --profile https up -d
```

**Windows 内网:**
```powershell
cd docker\windows
docker-compose -f docker-compose.yml -f docker-compose.intranet.yml up -d
```

**Windows 外网:**
```powershell
cd docker\windows
docker-compose -f docker-compose.yml -f docker-compose.internet.yml --profile https up -d
```

### 3. 验证部署

```bash
# 检查容器状态
docker ps

# 检查健康状态
curl http://localhost:8080/healthcheck

# 查看日志
docker logs onlyoffice-document-server
```

### 4. 常用命令

```bash
# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 查看日志
docker-compose logs -f

# 更新镜像
docker-compose pull
docker-compose up -d
```

---

## 后端部署

### Java 后端

**1. 构建:**
```bash
cd backend-java/onlyoffice-integration
mvn clean package -DskipTests
```

**2. 运行:**
```bash
# 使用环境变量
export JWT_SECRET=your-secret
export DOCUMENT_SERVER_URL=http://localhost:8080
export BACKEND_CALLBACK_URL=http://host.docker.internal:3000

java -jar target/onlyoffice-integration-1.0.0.jar
```

**3. Docker 部署（可选）:**
```dockerfile
FROM openjdk:11-jre-slim
COPY target/onlyoffice-integration-1.0.0.jar app.jar
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### NestJS 后端

**1. 安装依赖:**
```bash
cd backend-nestjs/onlyoffice-integration
npm install
```

**2. 构建:**
```bash
npm run build
```

**3. 运行:**
```bash
# 配置环境变量
cp .env.example .env
# 编辑 .env

npm run start:prod
```

**4. PM2 部署（推荐）:**
```bash
npm install -g pm2
pm2 start dist/main.js --name onlyoffice-backend
pm2 save
pm2 startup
```

---

## 前端部署

### 1. 构建

```bash
cd frontend-vue2/onlyoffice-editor

# 配置环境变量
cp .env.example .env.local
# 编辑 .env.local，设置正确的 API 地址

npm install
npm run build
```

### 2. 部署到 Nginx

```bash
# 复制构建产物
sudo cp -r dist/* /var/www/onlyoffice-editor/
```

### 3. Nginx 配置

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    root /var/www/onlyoffice-editor;
    index index.html;
    
    # Vue Router history 模式
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # 静态资源缓存
    location /static {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
    
    # Gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript;
}
```

---

## Nginx 配置

### HTTP 反向代理

```nginx
# /etc/nginx/conf.d/onlyoffice.conf

upstream onlyoffice {
    server 127.0.0.1:8080;
}

upstream backend {
    server 127.0.0.1:3000;
}

server {
    listen 80;
    server_name docs.example.com;
    
    client_max_body_size 100M;
    
    # ONLYOFFICE
    location / {
        proxy_pass http://onlyoffice;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {
    listen 80;
    server_name api.example.com;
    
    client_max_body_size 100M;
    
    # 后端 API
    location / {
        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## SSL 证书配置

### 使用 Let's Encrypt（推荐）

```bash
# 安装 Certbot
sudo apt install certbot python3-certbot-nginx

# 获取证书
sudo certbot --nginx -d docs.example.com -d api.example.com

# 自动续期
sudo certbot renew --dry-run
```

### 使用自签名证书（内网）

```bash
# 生成自签名证书
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout /etc/nginx/ssl/privkey.pem \
  -out /etc/nginx/ssl/fullchain.pem \
  -subj "/CN=localhost"
```

### HTTPS Nginx 配置

```nginx
server {
    listen 80;
    server_name docs.example.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name docs.example.com;
    
    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
    ssl_prefer_server_ciphers off;
    
    add_header Strict-Transport-Security "max-age=31536000" always;
    
    # ... 其他配置
}
```

---

## 部署检查清单

- [ ] Docker 和 Docker Compose 已安装
- [ ] 环境变量已正确配置
- [ ] JWT 密钥已设置且与 ONLYOFFICE 一致
- [ ] 端口未被占用（8080, 3000）
- [ ] 防火墙已开放必要端口
- [ ] ONLYOFFICE 健康检查通过
- [ ] 后端 API 可访问
- [ ] 前端可正常加载
- [ ] 文档编辑和保存功能正常
- [ ] SSL 证书已配置（外网环境）
