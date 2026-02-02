/**
 * ONLYOFFICE 文档配置 DTO
 * 
 * 用于返回给前端的编辑器初始化配置
 */
export interface DocumentConfigDto {
  /** 文档信息 */
  document: {
    /** 文件类型: docx, xlsx, pptx 等 */
    fileType: string;
    /** 文档唯一标识 */
    key: string;
    /** 文档标题 */
    title: string;
    /** 文档下载地址 */
    url: string;
    /** 权限配置 */
    permissions: {
      download: boolean;
      edit: boolean;
      print: boolean;
      review: boolean;
      comment: boolean;
    };
  };
  
  /** 编辑器配置 */
  editorConfig: {
    /** 回调地址 */
    callbackUrl: string;
    /** 界面语言 */
    lang: string;
    /** 编辑模式: edit, view */
    mode: string;
    /** 用户信息 */
    user: {
      id: string;
      name: string;
    };
    /** 自定义配置 */
    customization: {
      autosave: boolean;
      forcesave: boolean;
      chat: boolean;
      comments: boolean;
    };
  };
  
  /** 文档类型: word, cell, slide */
  documentType: string;
  
  /** JWT Token */
  token?: string;
  
  /** 编辑器宽度 */
  width?: string;
  
  /** 编辑器高度 */
  height?: string;
}
