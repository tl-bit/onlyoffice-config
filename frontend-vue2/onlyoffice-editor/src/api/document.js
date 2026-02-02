/**
 * 文档 API 服务
 * 
 * 封装与后端的所有 API 交互
 * 支持 Java 和 NestJS 后端（API 接口一致）
 */

import axios from 'axios';
import { API_BASE_URL } from '../config';

// 创建 axios 实例
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    // 可在此添加认证 token
    // const token = localStorage.getItem('token');
    // if (token) {
    //   config.headers.Authorization = `Bearer ${token}`;
    // }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API 请求错误:', error);
    return Promise.reject(error);
  }
);

/**
 * 获取文档编辑器配置
 * 
 * @param {string} documentId 文档 ID（不含扩展名）
 * @param {Object} options 可选参数
 * @param {string} options.fileType 文件类型，默认 'docx'
 * @param {string} options.userId 用户 ID
 * @param {string} options.userName 用户名称
 * @param {string} options.mode 编辑模式: 'edit' | 'view'
 * @returns {Promise<Object>} 编辑器配置
 */
export async function getDocumentConfig(documentId, options = {}) {
  const { fileType = 'docx', userId, userName, mode = 'edit' } = options;
  
  const params = new URLSearchParams();
  params.append('fileType', fileType);
  if (userId) params.append('userId', userId);
  if (userName) params.append('userName', userName);
  params.append('mode', mode);
  
  const response = await apiClient.get(`/api/doc/${encodeURIComponent(documentId)}?${params}`);
  return response.data;
}

/**
 * 获取文档列表
 * 
 * @returns {Promise<Array>} 文档列表
 */
export async function getDocumentList() {
  const response = await apiClient.get('/api/docs');
  return response.data;
}

/**
 * 上传文档
 * 
 * @param {File} file 文件对象
 * @param {Function} onProgress 上传进度回调
 * @returns {Promise<Object>} 上传结果
 */
export async function uploadDocument(file, onProgress) {
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await apiClient.post('/api/docs/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
        onProgress(percent);
      }
    },
  });
  
  return response.data;
}

/**
 * 删除文档
 * 
 * @param {string} documentId 文档 ID
 * @param {string} fileType 文件类型
 * @returns {Promise<Object>} 删除结果
 */
export async function deleteDocument(documentId, fileType = 'docx') {
  const response = await apiClient.delete(`/api/docs/${encodeURIComponent(documentId)}`, {
    params: { fileType },
  });
  return response.data;
}

/**
 * 获取 ONLYOFFICE 服务器信息
 * 
 * @returns {Promise<Object>} 服务器信息
 */
export async function getOfficeInfo() {
  const response = await apiClient.get('/api/office/info');
  return response.data;
}

/**
 * 健康检查
 * 
 * @returns {Promise<Object>} 健康状态
 */
export async function healthCheck() {
  const response = await apiClient.get('/api/health');
  return response.data;
}

export default {
  getDocumentConfig,
  getDocumentList,
  uploadDocument,
  deleteDocument,
  getOfficeInfo,
  healthCheck,
};
