import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { ServeStaticModule } from '@nestjs/serve-static';
import { join } from 'path';
import { DocumentModule } from './document/document.module';

/**
 * 应用主模块
 */
@Module({
  imports: [
    // 配置模块 - 加载环境变量
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: ['.env.local', '.env'],
    }),
    
    // 静态文件服务 - 提供文档下载
    ServeStaticModule.forRoot({
      rootPath: join(process.cwd(), process.env.UPLOAD_DIR || './uploads'),
      serveRoot: '/uploads',
    }),
    
    // 文档模块
    DocumentModule,
  ],
})
export class AppModule {}
