import { Injectable, Logger, BadRequestException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import axios from 'axios';
import { JwtService } from './jwt.service';
import { FileStorageService } from './file-storage.service';
import { DocumentConfigDto } from './dto/document-config.dto';
import { CallbackDto } from './dto/callback.dto';

/**
 * 文档服务
 * 
 * 负责生成 ONLYOFFICE 编辑器配置和处理回调
 */
@Injectable()
export class DocumentService {
  private readonly logger = new Logger(DocumentService.name);
  private readonly documentServerUrl: string;
  private readonly callbackUrl: string;

  constructor(
    private configService: ConfigService,
    private jwtService: JwtService,
    private fileStorageService: FileStorageService,
  ) {
    this.documentServerUrl = this.configService.get<string>(
      'DOCUMENT_SERVER_URL',
      'http://localhost:8080'
    );
    this.callbackUrl = this.configService.get<string>(
      'BACKEND_CALLBACK_URL',
      'http://host.docker.internal:3000'
    );
  }

  /**
   * 获取文档编辑器配置
   * 
   * @param documentId 文档 ID
   * @param fileType 文件类型
   * @param userId 用户 ID
   * @param userName 用户名称
   * @param mode 编辑模式
   * @returns 编辑器配置
   */
  getDocumentConfig(
    documentId: string,
    fileType: string,
    userId?: string,
    userName?: string,
    mode: string = 'edit',
  ): DocumentConfigDto {
    // 验证文件是否存在
    if (!this.fileStorageService.fileExists(documentId, fileType)) {
      throw new BadRequestException(`文档不存在: ${documentId}`);
    }

    // 获取文件最后修改时间
    const lastModified = this.fileStorageService.getLastModifiedTime(documentId, fileType);

    // 生成文档唯一 key
    const documentKey = this.generateDocumentKey(documentId, lastModified);

    // 生成文档下载 URL
    const documentUrl = this.fileStorageService.generateDownloadUrl(documentId, fileType);

    // 构建配置对象
    const config: DocumentConfigDto = {
      document: {
        fileType,
        key: documentKey,
        title: `${documentId}.${fileType}`,
        url: documentUrl,
        permissions: {
          download: true,
          edit: mode === 'edit',
          print: true,
          review: true,
          comment: true,
        },
      },
      editorConfig: {
        callbackUrl: `${this.callbackUrl}/api/office/callback`,
        lang: 'zh-CN',
        mode,
        user: {
          id: userId || 'anonymous',
          name: userName || '匿名用户',
        },
        customization: {
          autosave: true,
          forcesave: true,
          chat: false,
          comments: true,
        },
      },
      documentType: this.getDocumentType(fileType),
      width: '100%',
      height: '100%',
    };

    // 生成 JWT Token
    config.token = this.jwtService.createToken(config);

    this.logger.log(`生成文档配置: documentId=${documentId}, key=${documentKey}`);

    return config;
  }

  /**
   * 处理 ONLYOFFICE 回调
   * 
   * @param callback 回调数据
   */
  async handleCallback(callback: CallbackDto): Promise<void> {
    this.logger.log(`收到回调: status=${callback.status}, key=${callback.key}`);

    // 验证 JWT Token
    if (callback.token) {
      if (!this.jwtService.isTokenValid(callback.token)) {
        throw new BadRequestException('JWT Token 验证失败');
      }
      this.logger.debug('JWT Token 验证成功');
    }

    // 根据状态处理
    if (callback.needSave()) {
      await this.saveDocument(callback);
    } else if (callback.isEditing()) {
      this.logger.debug(`文档正在编辑中: key=${callback.key}`);
    } else if (callback.hasError()) {
      this.logger.error(`文档保存错误: key=${callback.key}, status=${callback.status}`);
    } else {
      this.logger.debug(`文档状态变更: key=${callback.key}, status=${callback.status}`);
    }
  }

  /**
   * 保存文档
   */
  private async saveDocument(callback: CallbackDto): Promise<void> {
    const { url, key, filetype } = callback;

    if (!url) {
      throw new BadRequestException('回调中缺少文档 URL');
    }

    // 从 key 中提取文档 ID
    const documentId = this.extractDocumentIdFromKey(key);
    if (!documentId) {
      throw new BadRequestException(`无法从 key 中提取文档 ID: ${key}`);
    }

    const fileType = filetype || 'docx';

    this.logger.log(`开始保存文档: documentId=${documentId}, url=${url}`);

    try {
      // 下载文档
      const response = await axios.get(url, {
        responseType: 'arraybuffer',
        timeout: 30000,
      });

      // 保存文档
      this.fileStorageService.saveFile(
        Buffer.from(response.data),
        documentId,
        fileType,
      );

      this.logger.log(`文档保存成功: documentId=${documentId}`);
    } catch (error) {
      this.logger.error(`保存文档失败: ${error.message}`);
      throw new BadRequestException(`保存文档失败: ${error.message}`);
    }
  }

  /**
   * 生成文档唯一 key
   */
  private generateDocumentKey(documentId: string, lastModified: number): string {
    // 对文件名进行 Base64 编码
    const encodedId = Buffer.from(documentId, 'utf8')
      .toString('base64')
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=/g, '');
    
    return `${encodedId}_${Math.floor(lastModified)}`;
  }

  /**
   * 从 key 中提取文档 ID
   */
  private extractDocumentIdFromKey(key: string): string | null {
    if (!key) {
      return null;
    }

    // key 格式: base64(documentId)_timestamp
    const lastUnderscore = key.lastIndexOf('_');
    if (lastUnderscore <= 0) {
      return null;
    }

    const encodedId = key.substring(0, lastUnderscore);

    try {
      // 还原 Base64 字符
      const base64 = encodedId
        .replace(/-/g, '+')
        .replace(/_/g, '/');
      
      return Buffer.from(base64, 'base64').toString('utf8');
    } catch {
      this.logger.warn(`解码文档 ID 失败: ${encodedId}`);
      return null;
    }
  }

  /**
   * 根据文件类型获取文档类型
   */
  private getDocumentType(fileType: string): string {
    switch (fileType?.toLowerCase()) {
      case 'xlsx':
      case 'xls':
      case 'ods':
      case 'csv':
        return 'cell';
      case 'pptx':
      case 'ppt':
      case 'odp':
        return 'slide';
      default:
        return 'word';
    }
  }

  /**
   * 获取 ONLYOFFICE 文档服务器地址
   */
  getDocumentServerUrl(): string {
    return this.documentServerUrl;
  }
}
