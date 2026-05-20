import { useUserStore } from '@/store/user'

/**
 * 字符权限校验
 * @param {Array} value 校验值
 * @returns {Boolean}
 */
export function checkPermi(value: string[]): boolean {
  if (value && value instanceof Array && value.length > 0) {
    const userStore = useUserStore()
    const permissions = userStore.permissions
    const all_permission = '*:*:*'
    const permissionPermissions = value

    const hasPermissions = permissions.some(permission => {
      return all_permission === permission || permissionPermissions.includes(permission)
    })

    if (!hasPermissions) {
      return false
    }
    return true
  } else {
    console.error(`need roles! Like v-hasPermi="['system:user:add']"`)
    return false
  }
}
