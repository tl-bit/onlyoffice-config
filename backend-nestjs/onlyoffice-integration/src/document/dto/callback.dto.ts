import { IsNumber, IsOptional, IsString, IsArray, IsBoolean } from 'class-validator';

/**
 * ONLYOFFICE 回调请求 DTO
 * 
 * 当文档状态变化时，ONLYOFFICE 会向 callbackUrl 发送此数据
 * 
 * 状态码说明:
 * - 0: 文档正在编辑中
 * - 1: 文档准备保存
 * - 2: 文档已保存，需要下载并保存到存储
 * - 3: 保存文档时发生错误
 * - 4: 文档关闭，无修改
 * - 6: 正在编辑，但当前状态已保存（强制保存）
 * - 7: 强制保存时发生错误
 */
export class CallbackDto {
  /** 文档状态码 */
  @IsNumber()
  status: number;

  /** 文档唯一标识 */
  @IsString()
  key: string;

  /** 编辑后的文档下载地址 */
  @IsOptional()
  @IsString()
  url?: string;

  /** 变更历史下载地址 */
  @IsOptional()
  @IsString()
  changesurl?: string;

  /** JWT Token */
  @IsOptional()
  @IsString()
  token?: string;

  /** 当前编辑用户列表 */
  @IsOptional()
  @IsArray()
  users?: string[];

  /** 最后保存时间 */
  @IsOptional()
  @IsString()
  lastsave?: string;

  /** 文档是否未修改 */
  @IsOptional()
  @IsBoolean()
  notmodified?: boolean;

  /** 文件类型 */
  @IsOptional()
  @IsString()
  filetype?: string;

  /** 强制保存类型 */
  @IsOptional()
  @IsNumber()
  forcesavetype?: number;

  /**
   * 判断是否需要保存文档
   */
  needSave(): boolean {
    return this.status === 2 || this.status === 6;
  }

  /**
   * 判断文档是否正在编辑
   */
  isEditing(): boolean {
    return this.status === 0;
  }

  /**
   * 判断是否发生错误
   */
  hasError(): boolean {
    return this.status === 3 || this.status === 7;
  }
}

/**
 * 回调响应 DTO
 */
export class CallbackResponseDto {
  /** 错误码: 0 成功，非0 失败 */
  error: number;
  
  /** 错误消息 */
  message?: string;

  static success(): CallbackResponseDto {
    return { error: 0 };
  }

  static fail(message: string): CallbackResponseDto {
    return { error: 1, message };
  }
}
