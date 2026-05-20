import request from '@/utils/request'
import type { Result, PageResult } from '@/types/api'

/**
 * 团队用户（部门成员）数据类型
 */
export interface TeamMember {
  id: number;
  realName: string;
  username: string;
  employeeNo: string;
  phone: string;
  email?: string;
  avatar?: string;
  deptId?: number;
  deptName?: string;
  /** 角色列表（系统角色） */
  roles?: { id: number; name: string; roleCode: string }[];
  /** 前端展示用的角色名称（取第一个角色名） */
  roleName: string;
  /** 前端展示用的角色颜色 */
  roleColor: string;
  /** 状态：0=禁用，1=正常 */
  status: number;
  /** 在职状态：active=在职，away=休假 */
  workStatus: string;
  lastLoginTime?: string;
  createTime?: string;
}

/**
 * 查询参数
 */
export interface TeamMemberQuery {
  pageNum: number;
  pageSize: number;
  deptId?: number;
  realName?: string;
  phone?: string;
  status?: number;
}

/**
 * 角色名 → 标签颜色的映射
 */
const roleColorMap: Record<string, string> = {
  admin: 'danger',
  manager: 'danger',
  senior: 'warning',
  consultant: 'primary',
  default: 'info'
}

/**
 * 从系统角色列表取可读的角色标签和颜色。
 * 过滤掉 ID=6 的兜底 "user" 角色，取第一个有效角色。
 */
function resolveRoleInfo(roles?: { id?: number; name: string; roleCode: string }[]): { roleName: string; roleColor: string } {
  if (!roles || roles.length === 0) {
    return { roleName: '未分配', roleColor: 'info' }
  }
  const meaningful = roles.filter(r => r.roleCode !== 'user' && r.id !== 6)
  const target = meaningful.length > 0 ? meaningful[0] : roles[0]
  return {
    roleName: target.name,
    roleColor: roleColorMap[target.roleCode] || roleColorMap.default
  }
}

/**
 * 分页查询部门成员
 * 底层复用系统用户接口，增强返回前端展示字段
 */
export function listTeamMembers(query: TeamMemberQuery) {
  return request<PageResult<TeamMember>>({
    url: '/system/v1/users/page',
    method: 'get',
    params: query
  }).then(res => {
    // 增强 records：补充 roleName/roleColor/workStatus 等展示字段
    if (res.data?.records) {
      res.data.records = res.data.records.map((user: any) => ({
        ...user,
        realName: user.realName || user.username,
        employeeNo: 'EMP-' + String(user.id).padStart(4, '0'),
        ...resolveRoleInfo(user.roles),
        workStatus: user.status === 1 ? 'active' : 'away'
      }))
    }
    return res
  })
}
