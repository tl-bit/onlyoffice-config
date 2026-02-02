import { Module } from '@nestjs/common';
import { MulterModule } from '@nestjs/platform-express';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { diskStorage } from 'multer';
import { extname, join } from 'path';
import { existsSync, mkdirSync } from 'fs';
import { DocumentController } from './document.controller';
import { DocumentService } from './document.service';
import { JwtService } from './jwt.service';
import { FileStorageService } from './file-storage.service';

/**
 * 文档模块
 * 
 * 包含文档管理和 ONLYOFFICE 集成的所有功能
 */
@Module({
  imports: [
    // 文件上传配置
    MulterModule.registerAsync({
      imports: [ConfigModule],
      useFactory: async (configService: ConfigService) => {
        const uploadDir = configService.get<string>('UPLOAD_DIR', './uploads');
        const absolutePath = join(process.cwd(), uploadDir);
        
        // 确保上传目录存在
        if (!existsSync(absolutePath)) {
          mkdirSync(absolutePath, { recursive: true });
        }
        
        return {
          storage: diskStorage({
            destination: absolutePath,
            filename: (req, file, callback) => {
              // 保留原始文件名
              const originalName = Buffer.from(file.originalname, 'latin1').toString('utf8');
              callback(null, originalName);
            },
          }),
          limits: {
            fileSize: configService.get<number>('MAX_FILE_SIZE', 104857600),
          },
          fileFilter: (req, file, callback) => {
            const allowedTypes = configService.get<string>('ALLOWED_FILE_TYPES', 'docx,xlsx,pptx')
              .split(',')
              .map(t => t.trim().toLowerCase());
            
            const ext = extname(file.originalname).toLowerCase().slice(1);
            
            if (allowedTypes.includes(ext)) {
              callback(null, true);
            } else {
              callback(new Error(`不支持的文件类型: ${ext}`), false);
            }
          },
        };
      },
      inject: [ConfigService],
    }),
  ],
  controllers: [DocumentController],
  providers: [DocumentService, JwtService, FileStorageService],
  exports: [DocumentService, JwtService, FileStorageService],
})
export class DocumentModule {}
