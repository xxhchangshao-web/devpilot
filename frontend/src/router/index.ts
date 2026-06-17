import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import Login from '@/views/Login.vue'
import Home from '@/views/Home.vue'
import { useUserStore } from '@/store/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/components/AppLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: Home,
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      {
        path: 'notes',
        name: 'Notes',
        component: () => import('@/views/notes/NoteList.vue'),
        meta: { title: '问题笔记管理', icon: 'Document' }
      },
      {
        path: 'notes/:id',
        name: 'NoteDetail',
        component: () => import('@/views/notes/NoteDetail.vue'),
        meta: { title: '笔记详情' }
      },
      {
        path: 'tags',
        name: 'Tags',
        component: () => import('@/views/tags/index.vue'),
        meta: { title: '标签管理', icon: 'PriceTag' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：检查登录状态，控制页面访问权限
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')

  if (token && to.path === '/login') {
    // 已登录用户访问登录页，跳转到笔记列表
    // 从 localStorage 恢复角色信息到 store
    const userStr = localStorage.getItem('user')
    if (userStr) {
      try {
        const user = JSON.parse(userStr)
        const userStore = useUserStore()
        userStore.setRole(user.role || '')
        userStore.setUsername(user.username || '')
      } catch {
        // JSON 解析失败，忽略
      }
    }
    next('/notes')
  } else if (!token && to.path !== '/login') {
    // 未登录用户访问受保护页面，跳转到登录页
    next('/login')
  } else {
    // 已登录访问非登录页 或 未登录访问登录页，放行
    if (token) {
      // 已登录时恢复角色信息到 store
      const userStr = localStorage.getItem('user')
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          const userStore = useUserStore()
          userStore.setRole(user.role || '')
          userStore.setUsername(user.username || '')
        } catch {
          // JSON 解析失败，忽略
        }
      }
    }
    next()
  }
})

export default router
