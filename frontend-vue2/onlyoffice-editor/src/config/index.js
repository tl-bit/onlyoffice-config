/**
 * 应用配置
 * 
 * 从环境变量读取配置，支持开发和生产环境
 */

// 后端 API 地址
export const API_BASE_URL = process.env.VUE_APP_API_BASE_URL || 'http://localhost:3000';

// ONLYOFFICE 文档服务器地址
export const DOCUMENT_SERVER_URL = process.env.VUE_APP_DOCUMENT_SERVER_URL || 'http://localhost:8080';

// ONLYOFFICE API 脚本地址
export const DOCUMENT_SERVER_API_URL = `${DOCUMENT_SERVER_URL}/web-apps/apps/api/documents/api.js`;

/**
 * 获取完整的 API URL
 * @param {string} path API 路径
 * @returns {string} 完整 URL
 */
export function getApiUrl(path) {
  return `${API_BASE_URL}${path}`;
}

/**
 * 导出配置对象
 */
export default {
  API_BASE_URL,
  DOCUMENT_SERVER_URL,
  DOCUMENT_SERVER_API_URL,
  getApiUrl,
};
