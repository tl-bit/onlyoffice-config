# ONLYOFFICE Vue2 前端

基于 Vue2 的 ONLYOFFICE 文档编辑器前端应用。

## 项目结构

```
src/
├── main.js                         # 入口文件
├── App.vue                         # 主组件
├── config/
│   └── index.js                    # 配置文件
├── api/
│   └── document.js                 # API 服务
└── components/
    └── OnlyOfficeEditor.vue        # 编辑器组件
```

## 快速开始

### 1. 安装依赖

```bash
npm install
```

### 2. 配置环境变量

```bash
cp .env.example .env.local
```

编辑 `.env.local`：

```env
VUE_APP_API_BASE_URL=http://localhost:3000
VUE_APP_DOCUMENT_SERVER_URL=http://localhost:8080
```

### 3. 运行

```bash
# 开发模式
npm run serve

# 构建生产版本
npm run build
```

## 组件使用

### OnlyOfficeEditor 组件

```vue
<template>
  <OnlyOfficeEditor
    :config="editorConfig"
    :document-server-url="documentServerUrl"
    width="100%"
    height="600px"
    @ready="onEditorReady"
    @document-state-change="onDocumentStateChange"
    @error="onEditorError"
  />
</template>

<script>
import OnlyOfficeEditor from '@/components/OnlyOfficeEditor.vue';
import { getDocumentConfig } from '@/api/document';

export default {
  components: { OnlyOfficeEditor },
  
  data() {
    return {
      editorConfig: null,
      documentServerUrl: 'http://localhost:8080',
    };
  },
  
  async mounted() {
    // 获取编辑器配置
    this.editorConfig = await getDocumentConfig('test', {
      fileType: 'docx',
      userId: 'user-1',
      userName: '张三',
      mode: 'edit',
    });
  },
  
  methods: {
    onEditorReady() {
      console.log('编辑器已就绪');
    },
    
    onDocumentStateChange(hasChanges) {
      console.log('文档状态:', hasChanges ? '有未保存更改' : '已保存');
    },
    
    onEditorError(error) {
      console.error('编辑器错误:', error);
    },
  },
};
</script>
```

### Props

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `config` | Object | 是 | 编辑器配置（从后端 API 获取） |
| `documentServerUrl` | String | 否 | ONLYOFFICE 服务器地址 |
| `editorId` | String | 否 | 编辑器容器 ID，默认 'onlyoffice-editor' |
| `width` | String | 否 | 编辑器宽度，默认 '100%' |
| `height` | String | 否 | 编辑器高度，默认 '100%' |

### Events

| 事件 | 参数 | 说明 |
|------|------|------|
| `ready` | - | 编辑器就绪 |
| `document-ready` | - | 文档加载完成 |
| `document-state-change` | hasChanges: boolean | 文档状态变化 |
| `error` | { type, code, message } | 发生错误 |
| `warning` | data | 警告信息 |
| `info` | data | 信息 |
| `request-close` | - | 请求关闭编辑器 |

### Methods

| 方法 | 说明 |
|------|------|
| `getEditor()` | 获取 ONLYOFFICE 编辑器实例 |
| `refresh()` | 刷新编辑器 |

## API 服务

```javascript
import { 
  getDocumentConfig,
  getDocumentList,
  uploadDocument,
  deleteDocument,
} from '@/api/document';

// 获取文档配置
const config = await getDocumentConfig('test', {
  fileType: 'docx',
  userId: 'user-1',
  userName: '张三',
  mode: 'edit',
});

// 获取文档列表
const docs = await getDocumentList();

// 上传文档
await uploadDocument(file, (percent) => {
  console.log(`上传进度: ${percent}%`);
});

// 删除文档
await deleteDocument('test', 'docx');
```

## 配置说明

| 环境变量 | 说明 | 默认值 |
|----------|------|--------|
| `VUE_APP_API_BASE_URL` | 后端 API 地址 | http://localhost:3000 |
| `VUE_APP_DOCUMENT_SERVER_URL` | ONLYOFFICE 地址 | http://localhost:8080 |

## 生产部署

1. 构建生产版本：
   ```bash
   npm run build
   ```

2. 将 `dist` 目录部署到 Web 服务器（Nginx、Apache 等）

3. 配置 Nginx 示例：
   ```nginx
   server {
       listen 80;
       server_name your-domain.com;
       
       root /var/www/onlyoffice-editor/dist;
       index index.html;
       
       location / {
           try_files $uri $uri/ /index.html;
       }
   }
   ```
