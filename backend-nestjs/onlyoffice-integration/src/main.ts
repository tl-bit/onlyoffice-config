import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { AppModule } from './app.module';

/**
 * 应用启动入口
 */
async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  
  const configService = app.get(ConfigService);
  const port = configService.get<number>('PORT', 3000);
  const corsOrigins = configService.get<string>('CORS_ORIGINS', '*');

  // 启用 CORS
  app.enableCors({
    origin: corsOrigins === '*' ? true : corsOrigins.split(','),
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization'],
    credentials: true,
  });

  // 启用全局验证管道
  app.useGlobalPipes(new ValidationPipe({
    whitelist: true,
    transform: true,
  }));

  await app.listen(port);
  
  console.log(`
========================================
  ONLYOFFICE 集成服务已启动
  地址: http://localhost:${port}
  
  API 端点:
  - GET  /api/doc/:id          获取文档配置
  - POST /api/office/callback  ONLYOFFICE 回调
  - GET  /api/docs             获取文档列表
  - POST /api/docs/upload      上传文档
  - GET  /api/health           健康检查
========================================
  `);
}

bootstrap();
