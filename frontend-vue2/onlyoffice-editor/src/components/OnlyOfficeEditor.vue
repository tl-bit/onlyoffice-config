<template>
  <div class="onlyoffice-editor">
    <!-- 编辑器容器 -->
    <div :id="editorId" class="editor-container" :style="containerStyle"></div>

    <!-- 加载状态 -->
    <div v-if="loading" class="editor-loading">
      <div class="loading-spinner"></div>
      <p>{{ loadingText }}</p>
    </div>

    <!-- 错误状态 -->
    <div v-if="error" class="editor-error">
      <p>{{ error }}</p>
      <button @click="retry" class="btn-retry">重试</button>
    </div>
  </div>
</template>

<script>
import { DOCUMENT_SERVER_API_URL } from "../config";

/**
 * ONLYOFFICE 编辑器组件
 *
 * 封装 ONLYOFFICE Document Editor 的 Vue2 组件
 * 支持文档编辑、查看、事件监听等功能
 *
 * @example
 * <OnlyOfficeEditor
 *   :config="editorConfig"
 *   :document-server-url="documentServerUrl"
 *   @ready="onEditorReady"
 *   @document-state-change="onDocumentStateChange"
 *   @error="onEditorError"
 * />
 */
export default {
  name: "OnlyOfficeEditor",

  props: {
    /**
     * 编辑器配置（从后端 API 获取）
     */
    config: {
      type: Object,
      required: true,
    },

    /**
     * ONLYOFFICE 文档服务器 URL
     * 如不提供，使用环境变量配置
     */
    documentServerUrl: {
      type: String,
      default: "",
    },

    /**
     * 编辑器容器 ID
     */
    editorId: {
      type: String,
      default: "onlyoffice-editor",
    },

    /**
     * 编辑器宽度
     */
    width: {
      type: String,
      default: "100%",
    },

    /**
     * 编辑器高度
     */
    height: {
      type: String,
      default: "100%",
    },
  },

  data() {
    return {
      editorInstance: null,
      loading: true,
      loadingText: "正在加载编辑器...",
      error: null,
      apiLoaded: false,
    };
  },

  computed: {
    containerStyle() {
      return {
        width: this.width,
        height: this.height,
      };
    },

    apiUrl() {
      if (this.documentServerUrl) {
        return `${this.documentServerUrl}/web-apps/apps/api/documents/api.js`;
      }
      return DOCUMENT_SERVER_API_URL;
    },
  },

  watch: {
    config: {
      handler(newConfig) {
        if (newConfig && this.apiLoaded) {
          this.initEditor();
        }
      },
      deep: true,
    },
  },

  mounted() {
    this.loadApi();
  },

  beforeDestroy() {
    this.destroyEditor();
  },

  methods: {
    /**
     * 加载 ONLYOFFICE API 脚本
     */
    async loadApi() {
      // 检查是否已加载
      if (window.DocsAPI) {
        this.apiLoaded = true;
        this.initEditor();
        return;
      }

      this.loadingText = "正在加载 ONLYOFFICE API...";

      try {
        await this.loadScript(this.apiUrl);
        this.apiLoaded = true;
        this.initEditor();
      } catch (err) {
        this.error = "ONLYOFFICE API 加载失败，请检查文档服务器是否正常运行";
        this.loading = false;
        this.$emit("error", { type: "api-load-error", message: err.message });
      }
    },

    /**
     * 动态加载脚本
     */
    loadScript(src) {
      return new Promise((resolve, reject) => {
        // 检查是否已存在
        const existing = document.querySelector(`script[src="${src}"]`);
        if (existing) {
          resolve();
          return;
        }

        const script = document.createElement("script");
        script.src = src;
        script.async = true;
        script.onload = resolve;
        script.onerror = () =>
          reject(new Error(`Failed to load script: ${src}`));
        document.head.appendChild(script);
      });
    },

    /**
     * 初始化编辑器
     */
    initEditor() {
      // 销毁已有实例
      this.destroyEditor();

      // 检查 API 是否可用
      if (!window.DocsAPI) {
        this.error = "ONLYOFFICE API 未加载";
        this.loading = false;
        return;
      }

      // 检查配置
      if (!this.config) {
        this.error = "缺少编辑器配置";
        this.loading = false;
        return;
      }

      this.loadingText = "正在初始化编辑器...";
      this.error = null;

      try {
        // 合并配置
        const editorConfig = {
          ...this.config,
          width: this.width,
          height: this.height,
          events: {
            onReady: this.onReady.bind(this),
            onDocumentStateChange: this.onDocumentStateChange.bind(this),
            onError: this.onError.bind(this),
            onWarning: this.onWarning.bind(this),
            onInfo: this.onInfo.bind(this),
            onRequestClose: this.onRequestClose.bind(this),
            onDocumentReady: this.onDocumentReady.bind(this),
          },
        };

        // 创建编辑器实例
        this.editorInstance = new window.DocsAPI.DocEditor(
          this.editorId,
          editorConfig
        );

        // 超时保护：5秒后自动关闭 loading
        setTimeout(() => {
          if (this.loading) {
            this.loading = false;
          }
        }, 5000);
      } catch (err) {
        this.error = `创建编辑器失败: ${err.message}`;
        this.loading = false;
        this.$emit("error", { type: "init-error", message: err.message });
      }
    },

    /**
     * 销毁编辑器实例
     */
    destroyEditor() {
      if (this.editorInstance) {
        try {
          this.editorInstance.destroyEditor();
        } catch (err) {
          console.warn("销毁编辑器时出错:", err);
        }
        this.editorInstance = null;
      }
    },

    /**
     * 重试加载
     */
    retry() {
      this.loading = true;
      this.error = null;

      if (!this.apiLoaded) {
        this.loadApi();
      } else {
        this.initEditor();
      }
    },

    // ==================== 事件处理 ====================

    /**
     * 编辑器就绪
     */
    onReady() {
      this.loading = false;
      this.$emit("ready");
    },

    /**
     * 文档就绪
     */
    onDocumentReady() {
      this.loading = false;
      this.$emit("document-ready");
    },

    /**
     * 文档状态变化
     */
    onDocumentStateChange(event) {
      // event.data: true 表示有未保存的更改，false 表示已保存
      console.log("文档状态变化:", event.data ? "有未保存更改" : "已保存");
      this.$emit("document-state-change", event.data);
    },

    /**
     * 编辑器错误
     */
    onError(event) {
      const errorCode = event.data?.errorCode;
      const errorDescription = event.data?.errorDescription || "未知错误";

      this.error = `编辑器错误: ${errorDescription}`;
      this.loading = false;

      this.$emit("error", {
        type: "editor-error",
        code: errorCode,
        message: errorDescription,
      });
    },

    /**
     * 编辑器警告
     */
    onWarning(event) {
      this.$emit("warning", event.data);
    },

    /**
     * 编辑器信息
     */
    onInfo(event) {
      this.$emit("info", event.data);
    },

    /**
     * 请求关闭
     */
    onRequestClose() {
      this.$emit("request-close");
    },

    // ==================== 公共方法 ====================

    /**
     * 获取编辑器实例
     */
    getEditor() {
      return this.editorInstance;
    },

    /**
     * 刷新编辑器
     */
    refresh() {
      this.initEditor();
    },
  },
};
</script>

<style scoped>
.onlyoffice-editor {
  position: relative;
  width: 100%;
  height: 100%;
}

.editor-container {
  width: 100%;
  height: 100%;
}

.editor-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.9);
  z-index: 10;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.editor-loading p {
  margin-top: 16px;
  color: #666;
}

.editor-error {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fff;
  z-index: 10;
}

.editor-error p {
  color: #c62828;
  margin-bottom: 16px;
  text-align: center;
  padding: 0 20px;
}

.btn-retry {
  padding: 8px 24px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-retry:hover {
  background: #5a6fd6;
}
</style>
