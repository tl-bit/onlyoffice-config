import { Injectable, Logger, BadRequestException, NotFoundException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { join, extname } from 'path';
import { existsSync, mkdirSync, readdirSync, statSync, unlinkSync, writeFileSync, renameSync } from 'fs';

/**
 * 文件存储服务
 * 
 * 负责文档的存储、读取、删除等操作
 * 包含安全校验，防止路径遍历攻击
 */
@Injectable()
export class FileStorageService {
  private readonly logger = new Logger(FileStorageService.name);
  private readonly uploadDir: string;
  private readonly allowedTypes: Set<string>;
  private readonly callbackUrl: string;

  constructor(private configService: ConfigService) {
    // 初始化上传目录
    this.uploadDir = join(
      process.cwd(),
      this.configService.get<string>('UPLOAD_DIR', './uploads')
    );
    
    // 确保目录存在
    if (!existsSync(this.uploadDir)) {
      mkdirSync(this.uploadDir, { recursive: true });
    }
    
    // 解析允许的文件类型
    const types = this.configService.get<string>('ALLOWED_FILE_TYPES', 'docx,xlsx,pptx');
    this.allowedTypes = new Set(types.split(',').map(t => t.trim().toLowerCase()));
    
    // 回调地址
    this.callbackUrl = this.configService.get<string>(
      'BACKEND_CALLBACK_URL',
      'http://host.docker.internal:3000'
    );
    
    this.logger.log(`文件存储服务初始化完成: ${this.uploadDir}`);
  }

  /**
   * 获取文件路径
   * 
   * @param documentId 文档 ID
   * @param fileType 文件类型
   * @returns 文件绝对路径
   */
  getFilePath(documentId: string, fileType: string): string {
    // 安全校验
    const safeId = this.sanitizeDocumentId(documentId);
    if (!safeId) {
      throw new BadRequestException(`无效的文档 ID: ${documentId}`);
    }

    const filename = `${safeId}.${fileType}`;
    const filePath = join(this.uploadDir, filename);

    // 确保文件在上传目录内
    if (!filePath.startsWith(this.uploadDir)) {
      throw new BadRequestException(`非法的文件路径: ${documentId}`);
    }

    return filePath;
  }

  /**
   * 检查文件是否存在
   */
  fileExists(documentId: string, fileType: string): boolean {
    try {
      const filePath = this.getFilePath(documentId, fileType);
      return existsSync(filePath);
    } catch {
      return false;
    }
  }

  /**
   * 获取文件最后修改时间
   */
  getLastModifiedTime(documentId: string, fileType: string): number {
    const filePath = this.getFilePath(documentId, fileType);
    
    if (!existsSync(filePath)) {
      throw new NotFoundException(`文件不存在: ${documentId}.${fileType}`);
    }
    
    const stats = statSync(filePath);
    return stats.mtimeMs;
  }

  /**
   * 保存文件内容
   * 
   * @param content 文件内容
   * @param documentId 文档 ID
   * @param fileType 文件类型
   */
  saveFile(content: Buffer, documentId: string, fileType: string): void {
    const safeId = this.sanitizeDocumentId(documentId);
    if (!safeId) {
      throw new BadRequestException(`无效的文档 ID: ${documentId}`);
    }

    const filename = `${safeId}.${fileType}`;
    const filePath = join(this.uploadDir, filename);
    const tempPath = join(this.uploadDir, `${safeId}_temp_${Date.now()}.${fileType}`);

    try {
      // 先写入临时文件
      writeFileSync(tempPath, content);
      
      // 原子操作：重命名覆盖原文件
      renameSync(tempPath, filePath);
      
      this.logger.log(`文件保存成功: ${filename}`);
    } catch (error) {
      // 清理临时文件
      if (existsSync(tempPath)) {
        unlinkSync(tempPath);
      }
      throw error;
    }
  }

  /**
   * 获取文档列表
   */
  listDocuments(): Array<{ id: string; name: string; type: string }> {
    const files = readdirSync(this.uploadDir);
    
    return files
      .filter(file => {
        // 排除临时文件
        if (file.includes('_temp_')) return false;
        
        // 检查文件类型
        const ext = extname(file).slice(1).toLowerCase();
        return this.allowedTypes.has(ext);
      })
      .map(file => {
        const ext = extname(file).slice(1).toLowerCase();
        const id = file.slice(0, -ext.length - 1);
        return {
          id,
          name: file,
          type: ext,
        };
      });
  }

  /**
   * 删除文件
   */
  deleteFile(documentId: string, fileType: string): void {
    const filePath = this.getFilePath(documentId, fileType);
    
    if (existsSync(filePath)) {
      unlinkSync(filePath);
      this.logger.log(`文件删除成功: ${documentId}.${fileType}`);
    }
  }

  /**
   * 生成文档下载 URL
   */
  generateDownloadUrl(documentId: string, fileType: string): string {
    const encodedFilename = encodeURIComponent(`${documentId}.${fileType}`);
    return `${this.callbackUrl}/uploads/${encodedFilename}`;
  }

  /**
   * 清理文档 ID，防止路径遍历攻击
   * 
   * @param documentId 原始文档 ID
   * @returns 安全的文档 ID，如果无效则返回 null
   */
  sanitizeDocumentId(documentId: string): string | null {
    if (!documentId) {
      return null;
    }

    // 检查危险字符
    if (
      documentId.includes('..') ||
      documentId.includes('/') ||
      documentId.includes('\\') ||
      documentId.includes('\0')
    ) {
      return null;
    }

    // URL 解码
    try {
      const decoded = decodeURIComponent(documentId);
      // 再次检查解码后的内容
      if (decoded.includes('..') || decoded.includes('/') || decoded.includes('\\')) {
        return null;
      }
      return decoded;
    } catch {
      return documentId;
    }
  }

  /**
   * 获取上传目录路径
   */
  getUploadDir(): string {
    return this.uploadDir;
  }

  /**
   * 获取回调 URL
   */
  getCallbackUrl(): string {
    return this.callbackUrl;
  }
}
