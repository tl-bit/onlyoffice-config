# ONLYOFFICE Document Server 生产环境部署方案

## 📁 项目结构

```
production-project/
├── docker/                          # Docker 配置
│   ├── windows/                     # Windows 环境
│   │   ├── docker-compose.yml       # 基础配置
│   │   ├── docker-compose.intranet.yml  # 内网环境
│   │   └── docker-compose.internet.yml  # 外网环境
│   ├── linux/                       # Linux 环境
│   │   ├── docker-compose.yml
│   │   ├── docker-compose.intranet.yml
│   │   └── docker-compose.internet.yml
│   ├── nginx/                       # Nginx 配置
│   │   ├── nginx.http.conf          # HTTP 配置
│   │   └── nginx.https.conf         # HTTPS 配置
│   └── .env.example                 # 环境变量模板
├── backend-java/                    # Java 后端
│   └── onlyoffice-integration/
├── backend-nestjs/                  # NestJS 后端
│   └── onlyoffice-integration/
├── frontend-vue2/                   # Vue2 前端
│   └── onlyoffice-editor/
└── docs/                            # 部署文档
    ├── DEPLOYMENT.md                # 部署指南
    ├── CONFIGURATION.md             # 配置说明
    └── TROUBLESHOOTING.md           # 故障排查
```

## 🚀 快速开始

### 1. 选择部署环境

| 环境 | 配置文件 |
|------|----------|
| Windows + 内网 | `docker/windows/docker-compose.yml` + `docker-compose.intranet.yml` |
| Windows + 外网 | `docker/windows/docker-compose.yml` + `docker-compose.internet.yml` |
| Linux + 内网 | `docker/linux/docker-compose.yml` + `docker-compose.intranet.yml` |
| Linux + 外网 | `docker/linux/docker-compose.yml` + `docker-compose.internet.yml` |

### 2. 配置环境变量

```bash
cp docker/.env.example docker/.env
# 编辑 .env 文件，配置必要参数
```

### 3. 启动服务

```bash
# Linux 内网 HTTP
cd docker/linux
docker-compose -f docker-compose.yml -f docker-compose.intranet.yml up -d

# Linux 外网 HTTPS
cd docker/linux
docker-compose -f docker-compose.yml -f docker-compose.internet.yml up -d
```

### 4. 选择后端

- **Java 后端**: 参考 `backend-java/README.md`
- **NestJS 后端**: 参考 `backend-nestjs/README.md`

### 5. 部署前端

参考 `frontend-vue2/README.md`

## 📖 详细文档

- [部署指南](docs/DEPLOYMENT.md)
- [配置说明](docs/CONFIGURATION.md)
- [故障排查](docs/TROUBLESHOOTING.md)

## ⚠️ 重要配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `ONLYOFFICE_PORT` | ONLYOFFICE 服务端口 | 8080 |
| `BACKEND_PORT` | 后端服务端口 | 3000 |
| `JWT_SECRET` | JWT 密钥 | 必须修改 |
| `DOCUMENT_SERVER_URL` | ONLYOFFICE 服务地址 | 根据环境配置 |
| `BACKEND_CALLBACK_URL` | 回调地址 | 根据环境配置 |
