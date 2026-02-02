<template>
  <div id="app">
    <header class="app-header">
      <h1>📄 ONLYOFFICE 文档编辑器</h1>
    </header>

    <main class="app-main">
      <!-- 文档列表视图 -->
      <div v-if="!isEditing" class="doc-list-container">
        <div class="toolbar">
          <button
            @click="refreshDocs"
            :disabled="loading"
            class="btn btn-secondary"
          >
            🔄 刷新列表
          </button>
          <label class="btn btn-primary upload-btn">
            📤 上传文档
            <input
              type="file"
              accept=".docx,.xlsx,.pptx,.doc,.xls,.ppt"
              @change="handleFileUpload"
              hidden
            />
          </label>
        </div>

        <div v-if="loading" class="loading">加载中...</div>

        <div v-else-if="error" class="error-message">
          {{ error }}
          <button @click="refreshDocs" class="btn btn-link">重试</button>
        </div>

        <div v-else-if="documents.length === 0" class="empty-state">
          <p>📂 暂无文档</p>
          <p class="hint">点击"上传文档"添加文件</p>
        </div>

        <ul v-else class="doc-list">
          <li v-for="doc in documents" :key="doc.id" class="doc-item">
            <span class="doc-icon">{{ getDocIcon(doc.type || "docx") }}</span>
            <span class="doc-name">{{ doc.name }}</span>
            <div class="doc-actions">
              <button
                @click="openEditor(doc.id, doc.type || 'docx', 'edit')"
                class="btn btn-primary btn-sm"
              >
                ✏️ 编辑
              </button>
              <button
                @click="openEditor(doc.id, doc.type || 'docx', 'view')"
                class="btn btn-secondary btn-sm"
              >
                👁️ 查看
              </button>
              <button @click="confirmDelete(doc)" class="btn btn-danger btn-sm">
                🗑️
              </button>
            </div>
          </li>
        </ul>
      </div>

      <!-- 编辑器视图 -->
      <div v-else class="editor-view">
        <div class="editor-toolbar">
          <button @click="closeEditor" class="btn btn-secondary">
            ← 返回列表
          </button>
          <span class="current-doc">
            {{ currentMode === "edit" ? "编辑" : "查看" }}:
            {{ currentDocId }}.{{ currentFileType }}
          </span>
          <span v-if="hasUnsavedChanges" class="status warning">
            ⚠️ 有未保存的更改
          </span>
          <span v-else-if="showSavedTip" class="status success">
            ✓ 已保存
          </span>
        </div>

        <OnlyOfficeEditor
          v-if="editorConfig"
          :config="editorConfig"
          :document-server-url="documentServerUrl"
          class="editor-wrapper"
          @ready="onEditorReady"
          @document-state-change="onDocumentStateChange"
          @error="onEditorError"
          @request-close="closeEditor"
        />
      </div>
    </main>

    <!-- 删除确认对话框 -->
    <div
      v-if="deleteConfirm"
      class="modal-overlay"
      @click.self="deleteConfirm = null"
    >
      <div class="modal">
        <h3>确认删除</h3>
        <p>确定要删除文档 "{{ deleteConfirm.name }}" 吗？</p>
        <div class="modal-actions">
          <button @click="deleteConfirm = null" class="btn btn-secondary">
            取消
          </button>
          <button @click="doDelete" class="btn btn-danger">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import OnlyOfficeEditor from "./components/OnlyOfficeEditor.vue";
import {
  getDocumentConfig,
  getDocumentList,
  uploadDocument,
  deleteDocument,
} from "./api/document";
import { DOCUMENT_SERVER_URL } from "./config";

export default {
  name: "App",

  components: {
    OnlyOfficeEditor,
  },

  data() {
    return {
      // 文档列表
      documents: [],
      loading: false,
      error: null,

      // 编辑器状态
      isEditing: false,
      editorConfig: null,
      currentDocId: null,
      currentFileType: "docx",
      currentMode: "edit",
      editorReady: false,
      hasUnsavedChanges: false,
      showSavedTip: false,
      savedTipTimer: null,

      // 文档服务器地址
      documentServerUrl: DOCUMENT_SERVER_URL,

      // 删除确认
      deleteConfirm: null,

      // 当前用户（可从登录系统获取）
      currentUser: {
        id: "user-1",
        name: "当前用户",
      },
    };
  },

  mounted() {
    this.refreshDocs();
  },

  methods: {
    /**
     * 刷新文档列表
     */
    async refreshDocs() {
      this.loading = true;
      this.error = null;

      try {
        this.documents = await getDocumentList();
      } catch (err) {
        console.error("获取文档列表失败:", err);
        this.error = "获取文档列表失败，请检查后端服务是否正常运行";
      } finally {
        this.loading = false;
      }
    },

    /**
     * 打开编辑器
     */
    async openEditor(docId, fileType, mode) {
      this.loading = true;
      this.error = null;

      try {
        // 获取编辑器配置
        const config = await getDocumentConfig(docId, {
          fileType,
          userId: this.currentUser.id,
          userName: this.currentUser.name,
          mode,
        });

        this.editorConfig = config;
        this.currentDocId = docId;
        this.currentFileType = fileType;
        this.currentMode = mode;
        this.isEditing = true;
        this.editorReady = false;
        this.hasUnsavedChanges = false;
      } catch (err) {
        console.error("打开编辑器失败:", err);
        this.error = err.response?.data?.error || "打开编辑器失败";
      } finally {
        this.loading = false;
      }
    },

    /**
     * 关闭编辑器
     */
    closeEditor() {
      if (this.hasUnsavedChanges) {
        if (!confirm("有未保存的更改，确定要关闭吗？")) {
          return;
        }
      }

      this.isEditing = false;
      this.editorConfig = null;
      this.currentDocId = null;
      this.editorReady = false;
      this.hasUnsavedChanges = false;

      // 刷新列表
      this.refreshDocs();
    },

    /**
     * 编辑器就绪回调
     */
    onEditorReady() {
      console.log("编辑器就绪");
      this.editorReady = true;
    },

    /**
     * 文档状态变化回调
     */
    onDocumentStateChange(hasChanges) {
      console.log(
        "App 收到状态变化:",
        hasChanges,
        "editorReady:",
        this.editorReady
      );
      this.hasUnsavedChanges = hasChanges;
      // 确保编辑器已就绪
      if (!this.editorReady) {
        this.editorReady = true;
      }

      // 清除之前的定时器
      if (this.savedTipTimer) {
        clearTimeout(this.savedTipTimer);
        this.savedTipTimer = null;
      }

      // 保存成功时显示提示，2秒后自动隐藏
      if (!hasChanges) {
        this.showSavedTip = true;
        this.savedTipTimer = setTimeout(() => {
          this.showSavedTip = false;
        }, 2000);
      } else {
        this.showSavedTip = false;
      }
    },

    /**
     * 编辑器错误回调
     */
    onEditorError(error) {
      console.error("编辑器错误:", error);
      this.error = error.message;
    },

    /**
     * 处理文件上传
     */
    async handleFileUpload(event) {
      const file = event.target.files[0];
      if (!file) return;

      this.loading = true;
      this.error = null;

      try {
        await uploadDocument(file, (percent) => {
          console.log(`上传进度: ${percent}%`);
        });

        // 刷新列表
        await this.refreshDocs();
      } catch (err) {
        console.error("上传失败:", err);
        this.error = "上传失败: " + (err.response?.data?.error || err.message);
      } finally {
        this.loading = false;
        // 清空 input
        event.target.value = "";
      }
    },

    /**
     * 确认删除
     */
    confirmDelete(doc) {
      this.deleteConfirm = doc;
    },

    /**
     * 执行删除
     */
    async doDelete() {
      if (!this.deleteConfirm) return;

      const { id, type } = this.deleteConfirm;
      this.deleteConfirm = null;
      this.loading = true;

      try {
        await deleteDocument(id, type || "docx");
        await this.refreshDocs();
      } catch (err) {
        console.error("删除失败:", err);
        this.error = "删除失败: " + (err.response?.data?.error || err.message);
        this.loading = false;
      }
    },

    /**
     * 获取文档图标
     */
    getDocIcon(type) {
      const icons = {
        docx: "📝",
        doc: "📝",
        xlsx: "📊",
        xls: "📊",
        pptx: "📽️",
        ppt: "📽️",
        pdf: "📕",
      };
      return icons[type] || "📄";
    },
  },
};
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

#app {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen,
    Ubuntu, sans-serif;
  -webkit-font-smoothing: antialiased;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.app-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 16px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  flex-shrink: 0;
}

.app-header h1 {
  font-size: 1.5rem;
  font-weight: 600;
}

.app-main {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 文档列表 */
.doc-list-container {
  padding: 24px;
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
  overflow-y: auto;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.doc-list {
  list-style: none;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.doc-item {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  gap: 12px;
}

.doc-item:last-child {
  border-bottom: none;
}

.doc-item:hover {
  background: #f9f9f9;
}

.doc-icon {
  font-size: 1.5rem;
}

.doc-name {
  flex: 1;
  font-size: 1rem;
  color: #333;
  word-break: break-all;
}

.doc-actions {
  display: flex;
  gap: 8px;
}

/* 按钮 */
.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 0.85rem;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-secondary {
  background: #e0e0e0;
  color: #333;
}

.btn-secondary:hover:not(:disabled) {
  background: #d0d0d0;
}

.btn-danger {
  background: #ef5350;
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: #e53935;
}

.btn-link {
  background: none;
  color: #667eea;
  text-decoration: underline;
  padding: 4px 8px;
}

.upload-btn {
  cursor: pointer;
}

/* 状态 */
.loading {
  text-align: center;
  padding: 40px;
  color: #666;
}

.error-message {
  background: #ffebee;
  color: #c62828;
  padding: 16px;
  border-radius: 8px;
  text-align: center;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.empty-state .hint {
  margin-top: 8px;
  font-size: 0.9rem;
  color: #999;
}

/* 编辑器视图 */
.editor-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: white;
  border-bottom: 1px solid #e0e0e0;
  flex-shrink: 0;
}

.current-doc {
  font-weight: 500;
  color: #333;
}

.status {
  margin-left: auto;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 0.85rem;
}

.status.success {
  background: #e8f5e9;
  color: #2e7d32;
}

.status.warning {
  background: #fff3e0;
  color: #ef6c00;
}

.editor-wrapper {
  flex: 1;
  overflow: hidden;
}

/* 模态框 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: white;
  padding: 24px;
  border-radius: 8px;
  min-width: 300px;
  max-width: 90%;
}

.modal h3 {
  margin-bottom: 12px;
}

.modal p {
  margin-bottom: 20px;
  color: #666;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
