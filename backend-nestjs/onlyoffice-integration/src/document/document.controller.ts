import {
  Controller,
  Get,
  Post,
  Delete,
  Param,
  Query,
  Body,
  UploadedFile,
  UseInterceptors,
  Logger,
  HttpCode,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { DocumentService } from './document.service';
import { FileStorageService } from './file-storage.service';
import { CallbackDto, CallbackResponseDto } from './dto/callback.dto';

/**
 * 文档控制器
 * 
 * 提供文档管理和 ONLYOFFICE 集成的 REST API
 */
@Controller('api')
export class DocumentController {
  private readonly logger = new Logger(DocumentController.name);

  constructor(
    private documentService: DocumentService,
    private fileStorageService: FileStorageService,
  ) {}

  /**
   * 获取文档编辑器配置
   * 
   * @example GET /api/doc/test?fileType=docx&userId=user1&userName=张三&mode=edit
   * 
   * @param id 文档 ID（不含扩展名）
   * @param fileType 文件类型，默认 docx
   * @param userId 用户 ID（可选）
   * @param userName 用户名称（可选）
   * @param mode 编辑模式: edit/view，默认 edit
   */
  @Get('doc/:id')
  getDocumentConfig(
    @Param('id') id: string,
    @Query('fileType') fileType: string = 'docx',
    @Query('userId') userId?: string,
    @Query('userName') userName?: string,
    @Query('mode') mode: string = 'edit',
  ) {
    this.logger.log(`获取文档配置: id=${id}, fileType=${fileType}, userId=${userId}, mode=${mode}`);
    
    return this.documentService.getDocumentConfig(id, fileType, userId, userName, mode);
  }

  /**
   * ONLYOFFICE 回调接口
   * 
   * 当文档状态变化时（如保存、关闭），ONLYOFFICE 会调用此接口
   * 此接口由 ONLYOFFICE 自动调用，无需手动调用
   * 
   * 注意：必须返回 HTTP 200，ONLYOFFICE 不接受其他状态码
   */
  @Post('office/callback')
  @HttpCode(200)
  async handleCallback(@Body() callback: CallbackDto): Promise<CallbackResponseDto> {
    this.logger.log(`收到 ONLYOFFICE 回调: status=${callback.status}, key=${callback.key}`);
    
    try {
      // 将普通对象转换为 CallbackDto 实例以使用方法
      const dto = Object.assign(new CallbackDto(), callback);
      await this.documentService.handleCallback(dto);
      return CallbackResponseDto.success();
    } catch (error) {
      this.logger.error(`处理回调失败: ${error.message}`);
      return CallbackResponseDto.fail(error.message);
    }
  }

  /**
   * 获取文档列表
   * 
   * @example GET /api/docs
   */
  @Get('docs')
  listDocuments() {
    this.logger.log('获取文档列表');
    return this.fileStorageService.listDocuments();
  }

  /**
   * 上传文档
   * 
   * @example POST /api/docs/upload
   *          Content-Type: multipart/form-data
   *          file: (binary)
   */
  @Post('docs/upload')
  @UseInterceptors(FileInterceptor('file'))
  uploadDocument(@UploadedFile() file: Express.Multer.File) {
    this.logger.log(`上传文档: ${file.originalname}`);
    
    // 文件已由 Multer 保存，返回文档信息
    const originalName = Buffer.from(file.originalname, 'latin1').toString('utf8');
    const ext = originalName.split('.').pop() || 'docx';
    const id = originalName.slice(0, -ext.length - 1);
    
    return {
      success: true,
      documentId: id,
      filename: originalName,
      message: '上传成功',
    };
  }

  /**
   * 删除文档
   * 
   * @example DELETE /api/docs/test?fileType=docx
   */
  @Delete('docs/:id')
  deleteDocument(
    @Param('id') id: string,
    @Query('fileType') fileType: string = 'docx',
  ) {
    this.logger.log(`删除文档: id=${id}, fileType=${fileType}`);
    
    this.fileStorageService.deleteFile(id, fileType);
    
    return {
      success: true,
      message: '删除成功',
    };
  }

  /**
   * 获取 ONLYOFFICE 服务器信息
   */
  @Get('office/info')
  getOfficeInfo() {
    const serverUrl = this.documentService.getDocumentServerUrl();
    return {
      documentServerUrl: serverUrl,
      apiUrl: `${serverUrl}/web-apps/apps/api/documents/api.js`,
    };
  }

  /**
   * 健康检查接口
   */
  @Get('health')
  healthCheck() {
    return {
      status: 'ok',
      timestamp: Date.now(),
      service: 'onlyoffice-integration',
    };
  }
}
