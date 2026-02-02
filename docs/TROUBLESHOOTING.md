# ONLYOFFICE 故障排查指南

## 目录

1. [常见问题](#常见问题)
2. [错误代码说明](#错误代码说明)
3. [日志查看](#日志查看)
4. [调试技巧](#调试技巧)

---

## 常见问题

### 1. 编辑器白屏 / API 未加载

**症状：**
- 页面显示空白
- 控制台报错 `DocsAPI is not defined`
- 提示 "ONLYOFFICE API 未加载"

**原因：**
- ONLYOFFICE 服务未启动或未完全启动
- API 脚本地址错误
- 网络不通

**解决方案：**

```bash
# 1. 检查 ONLYOFFICE 容器状态
docker ps | grep onlyoffice

# 2. 检查健康状态
curl http://localhost:8080/healthcheck

# 3. 检查 API 脚本是否可访问
curl -I http://localhost:8080/web-apps/apps/api/documents/api.js

# 4. 查看容器日志
docker logs onlyoffice-document-server --tail 100

# 5. 如果刚启动，等待 2-3 分钟让服务完全启动
```

### 2. 回调 404 错误

**症状：**
- 文档无法保存
- ONLYOFFICE 日志显示回调返回 404
- 编辑后关闭，更改丢失

**原因：**
- 回调地址配置错误
- 后端服务未运行
- 网络不通（Docker 容器无法访问宿主机）

**解决方案：**

```bash
# 1. 检查后端服务是否运行
curl http://localhost:3000/api/health

# 2. 检查回调地址配置
# 确保 BACKEND_CALLBACK_URL 是 ONLYOFFICE 容器能访问的地址

# 3. 从容器内测试回调连通性
docker exec onlyoffice-document-server curl http://host.docker.internal:3000/api/health

# 4. Linux 下如果 host.docker.internal 不可用
# 使用宿主机实际 IP 地址
docker exec onlyoffice-document-server curl http://192.168.1.100:3000/api/health

# 5. 检查防火墙
# Windows: 确保 3000 端口允许入站
# Linux: sudo ufw allow 3000
```

### 3. JWT 签名失败

**症状：**
- 编辑器显示 "Token validation failed"
- 控制台显示 JWT 相关错误
- 文档无法加载

**原因：**
- JWT 密钥不匹配
- Token 过期
- 签名算法不一致

**解决方案：**

```bash
# 1. 确认 ONLYOFFICE 的 JWT 配置
docker exec onlyoffice-document-server cat /etc/onlyoffice/documentserver/local.json | grep secret

# 2. 确保后端配置的 JWT_SECRET 与 ONLYOFFICE 一致

# 3. 重启 ONLYOFFICE 使配置生效
docker restart onlyoffice-document-server

# 4. 检查系统时间是否同步（Token 过期判断依赖时间）
date
```

### 4. 文档无法下载

**症状：**
- 编辑器显示 "Download failed"
- 文档加载失败

**原因：**
- 文档 URL 不可访问
- 文件不存在
- MIME 类型错误

**解决方案：**

```bash
# 1. 检查文档 URL 是否可访问
curl -I http://localhost:3000/uploads/test.docx

# 2. 从 ONLYOFFICE 容器测试
docker exec onlyoffice-document-server curl -I http://host.docker.internal:3000/uploads/test.docx

# 3. 检查文件是否存在
ls -la uploads/

# 4. 检查 MIME 类型配置
# 确保 .docx 返回正确的 Content-Type
```

### 5. 端口被占用

**症状：**
- 服务启动失败
- 报错 "port is already in use"

**解决方案：**

```bash
# 1. 查找占用端口的进程
# Windows
netstat -ano | findstr :8080

# Linux
lsof -i :8080

# 2. 修改端口配置
# 编辑 .env 文件
ONLYOFFICE_PORT=8081
BACKEND_PORT=3001

# 3. 更新相关配置
DOCUMENT_SERVER_URL=http://localhost:8081
BACKEND_CALLBACK_URL=http://host.docker.internal:3001
```

### 6. 中文文件名乱码

**症状：**
- 文件名显示乱码
- 无法打开中文名文件

**解决方案：**

后端已处理 URL 编码，确保：
1. 文件名使用 `encodeURIComponent` 编码
2. 后端正确解码文件名
3. 数据库/文件系统支持 UTF-8

### 7. 内存不足

**症状：**
- 容器频繁重启
- 服务响应缓慢
- OOM 错误

**解决方案：**

```bash
# 1. 检查内存使用
docker stats onlyoffice-document-server

# 2. 增加 Docker 内存限制
# docker-compose.yml
deploy:
  resources:
    limits:
      memory: 4G

# 3. 增加 Docker Desktop 内存（Windows/Mac）
# Settings -> Resources -> Memory
```

---

## 错误代码说明

### ONLYOFFICE 回调状态码

| 状态码 | 说明 | 处理方式 |
|--------|------|----------|
| 0 | 文档正在编辑 | 无需处理 |
| 1 | 文档准备保存 | 无需处理 |
| 2 | 文档已保存 | 下载并保存文档 |
| 3 | 保存错误 | 记录错误日志 |
| 4 | 关闭无修改 | 无需处理 |
| 6 | 强制保存 | 下载并保存文档 |
| 7 | 强制保存错误 | 记录错误日志 |

### ONLYOFFICE 错误码

| 错误码 | 说明 |
|--------|------|
| -1 | 未知错误 |
| -2 | 解析错误 |
| -3 | 下载错误 |
| -4 | 无法保存 |
| -5 | 命令错误 |
| -6 | 强制保存错误 |
| -7 | 回调错误 |
| -8 | Token 错误 |

---

## 日志查看

### ONLYOFFICE 日志

```bash
# 查看所有日志
docker logs onlyoffice-document-server

# 实时查看
docker logs -f onlyoffice-document-server

# 查看最近 100 行
docker logs --tail 100 onlyoffice-document-server

# 查看特定服务日志
docker exec onlyoffice-document-server cat /var/log/onlyoffice/documentserver/docservice/out.log
docker exec onlyoffice-document-server cat /var/log/onlyoffice/documentserver/converter/out.log
```

### 后端日志

**Java:**
```bash
# 查看日志文件
tail -f logs/onlyoffice-integration.log

# 调整日志级别
# application.yml
logging:
  level:
    com.example.onlyoffice: DEBUG
```

**NestJS:**
```bash
# PM2 日志
pm2 logs onlyoffice-backend

# 调整日志级别
# .env
LOG_LEVEL=debug
```

### Nginx 日志

```bash
# 访问日志
tail -f /var/log/nginx/access.log

# 错误日志
tail -f /var/log/nginx/error.log
```

---

## 调试技巧

### 1. 测试 API 连通性

```bash
# 测试 ONLYOFFICE
curl http://localhost:8080/healthcheck

# 测试后端
curl http://localhost:3000/api/health

# 测试文档配置
curl http://localhost:3000/api/doc/test?fileType=docx
```

### 2. 测试回调

```bash
# 模拟回调请求
curl -X POST http://localhost:3000/api/office/callback \
  -H "Content-Type: application/json" \
  -d '{"status": 0, "key": "test_123"}'
```

### 3. 检查 Docker 网络

```bash
# 查看网络
docker network ls

# 检查容器网络
docker inspect onlyoffice-document-server | grep -A 20 "Networks"

# 测试容器间通信
docker exec onlyoffice-document-server ping host.docker.internal
```

### 4. 浏览器调试

1. 打开开发者工具（F12）
2. 查看 Console 标签页的错误信息
3. 查看 Network 标签页的请求状态
4. 检查 API 请求的响应内容

### 5. 重置环境

```bash
# 停止并删除容器
docker-compose down -v

# 清理 Docker 缓存
docker system prune -a

# 重新启动
docker-compose up -d
```

---

## 获取帮助

如果以上方法都无法解决问题：

1. 收集相关日志
2. 记录错误信息和复现步骤
3. 检查 [ONLYOFFICE 官方文档](https://api.onlyoffice.com/)
4. 搜索 [ONLYOFFICE GitHub Issues](https://github.com/ONLYOFFICE/DocumentServer/issues)
