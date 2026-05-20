/**
 * 全局响应结构
 */
export interface Result<T = any> {
  code: number;
  message: string;
  data: T;
}

/**
 * 分页数据结构
 */
export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
}
