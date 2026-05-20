import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/store/user'

/**
 * 动态数据权限隔离指令 (v-dataScope)
 * 用于列表行级按钮的显隐与置灰联动控制
 *
 * 绑定值格式: v-dataScope="{ ownerId: row.salesId, ownerDeptId: row.deptId, action: 'disable' }"
 */
const dataScope: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding
    if (!value || typeof value !== 'object') {
      throw new Error(`v-dataScope 指令必须绑定一个配置对象，例如: { ownerId: row.salesId }`)
    }

    const { ownerId, ownerDeptId, action = 'disable' } = value
    const userStore = useUserStore()
    const userInfo = userStore.userInfo

    if (!userInfo) {
      return
    }

    const currentUserId = userInfo.userId || userInfo.id
    const currentDeptId = userInfo.deptId
    const highestScope = userInfo.dataScope // 最高数据权限: 1=全部, 2=本部门, 3=本部门及下属, 4=仅本人

    // 1. 超级管理员 / 全部数据权限 (scope = 1) -> 直接放行，不作任何处理
    if (highestScope === 1) {
      return
    }

    // 2. 如果当前用户就是数据的直接负责人 -> 直接放行，不作任何处理
    if (currentUserId !== undefined && ownerId !== undefined && currentUserId === ownerId) {
      return
    }

    // 3. 如果是无负责人数据 (如公海客户 ownerId 为 null) -> 全局放行，不作任何处理
    if (ownerId === null || ownerId === undefined) {
      return
    }

    let isAuthorized = false

    // 4. 执行前端数据隔离逻辑规则比对
    switch (highestScope) {
      case 2: // 本部门数据
        if (currentDeptId !== undefined && ownerDeptId !== undefined) {
          if (currentDeptId === ownerDeptId) {
            isAuthorized = true
          }
        } else {
          // 未传入负责人部门 ID 时，后备容错判定：放行以在后端由 BolasGuard 进行 100% 精准拦截
          isAuthorized = true
        }
        break
      case 3: // 本部门及下属部门
        if (currentDeptId !== undefined && ownerDeptId !== undefined) {
          if (currentDeptId === ownerDeptId) {
            isAuthorized = true
          } else {
            // 后备容错判定：放行以在后端由 BolasGuard 进行 100% 精准拦截
            isAuthorized = true
          }
        } else {
          // 未传入负责人部门 ID 时，后备放行由后端精准拦截
          isAuthorized = true
        }
        break
      case 4: // 仅本人数据
        if (currentUserId !== undefined && currentUserId === ownerId) {
          isAuthorized = true
        }
        break
      default:
        break
    }

    // 4. 若无权限，执行相应的 UI 联动限制策略
    if (!isAuthorized) {
      if (action === 'hide') {
        // 隐藏策略
        el.style.display = 'none'
        setTimeout(() => {
          el.parentNode && el.parentNode.removeChild(el)
        }, 0)
      } else {
        // 置灰策略 (完美适配 Element Plus 按钮)
        el.setAttribute('disabled', 'true')
        el.classList.add('is-disabled')
        el.style.pointerEvents = 'none'
        el.style.cursor = 'not-allowed'
        el.style.opacity = '0.6'
        
        // 防御点击：如果元素上直接绑定了 click 事件，进行拦截
        el.addEventListener('click', (e) => {
          e.preventDefault()
          e.stopPropagation()
          return false
        }, true)
      }
    }
  }
}

export default dataScope
