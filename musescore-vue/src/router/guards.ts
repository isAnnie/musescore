import type { NavigationGuardNext, RouteLocationNormalized } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { useScoreStore } from '@/stores/scoreStore'

// 检查用户认证状态
export const authGuard = (
  to: RouteLocationNormalized,
  from: RouteLocationNormalized,
  next: NavigationGuardNext
) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    // 重定向到登录页面，并保存目标路由
    next({
      name: 'login',
      query: { redirect: to.fullPath }
    })
  } else if (to.meta.guestOnly && userStore.isLoggedIn) {
    // 已登录用户不能访问登录/注册页面
    next({ name: 'home' })
  } else {
    next()
  }
}

// 保存编辑状态
export const saveEditorState = (
  to: RouteLocationNormalized,
  from: RouteLocationNormalized,
  next: NavigationGuardNext
) => {
  const scoreStore = useScoreStore()
  
  // 如果从编辑器页面离开，检查是否有未保存的更改
  if (from.name === 'editor-score' || from.name === 'editor') {
    if (scoreStore.currentScore?.isDraft) {
      // 这里可以显示保存提示
      console.log('有未保存的更改')
    }
  }
  
  next()
}

// 滚动行为优化
export const scrollBehavior = (
  to: RouteLocationNormalized,
  from: RouteLocationNormalized,
  savedPosition: any
) => {
  if (savedPosition) {
    return savedPosition
  }
  
  // 如果路由有hash，滚动到对应元素
  if (to.hash) {
    return {
      el: to.hash,
      behavior: 'smooth'
    }
  }
  
  // 否则滚动到顶部
  return { top: 0 }
}

// 页面访问统计
export const trackPageView = (
  to: RouteLocationNormalized,
  from: RouteLocationNormalized
) => {
  // 这里可以集成Google Analytics或其他分析工具
  console.log(`页面访问: ${String(to.name)}`, to.path)
  
  // 记录页面访问时间
  const pageVisit = {
    path: to.path,
    name: to.name,
    timestamp: new Date().toISOString(),
    params: to.params,
    query: to.query
  }
  
  // 保存到localStorage用于分析
  const visits = JSON.parse(localStorage.getItem('pageVisits') || '[]')
  visits.push(pageVisit)
  localStorage.setItem('pageVisits', JSON.stringify(visits.slice(-100))) // 只保留最近100条
}
