import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as jwt from 'jsonwebtoken';

/**
 * JWT 服务
 * 
 * 负责 JWT Token 的生成和验证
 * 用于 ONLYOFFICE 与后端之间的安全通信
 */
@Injectable()
export class JwtService {
  private readonly logger = new Logger(JwtService.name);
  private readonly secret: string;
  private readonly expiresIn: number;

  constructor(private configService: ConfigService) {
    this.secret = this.configService.get<string>('JWT_SECRET', 'your-super-secret-key');
    this.expiresIn = this.configService.get<number>('JWT_EXPIRES_IN', 3600);
    
    this.logger.log('JWT 服务初始化完成');
  }

  /**
   * 为对象生成 JWT Token
   * 
   * @param payload 要签名的数据
   * @returns JWT Token 字符串
   */
  createToken(payload: object): string {
    try {
      return jwt.sign(payload, this.secret, {
        expiresIn: this.expiresIn,
        algorithm: 'HS256',
      });
    } catch (error) {
      this.logger.error(`创建 JWT Token 失败: ${error.message}`);
      throw error;
    }
  }

  /**
   * 验证 JWT Token
   * 
   * @param token JWT Token 字符串
   * @returns 解析后的数据
   * @throws 如果验证失败
   */
  verifyToken(token: string): jwt.JwtPayload {
    try {
      return jwt.verify(token, this.secret) as jwt.JwtPayload;
    } catch (error) {
      this.logger.warn(`JWT Token 验证失败: ${error.message}`);
      throw error;
    }
  }

  /**
   * 验证 Token 是否有效
   * 
   * @param token JWT Token 字符串
   * @returns true 如果有效
   */
  isTokenValid(token: string): boolean {
    try {
      this.verifyToken(token);
      return true;
    } catch {
      return false;
    }
  }

  /**
   * 获取 JWT 密钥
   */
  getSecret(): string {
    return this.secret;
  }
}
