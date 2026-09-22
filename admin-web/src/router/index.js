import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue')
  },
  {
    path: '/',
    component: () => import('@/views/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '工作台' } },
      { path: 'employee', name: 'Employee', component: () => import('@/views/employee/index.vue'), meta: { title: '员工管理' } },
      { path: 'category', name: 'Category', component: () => import('@/views/category/index.vue'), meta: { title: '分类管理' } },
      { path: 'dish', name: 'Dish', component: () => import('@/views/dish/index.vue'), meta: { title: '菜品管理' } },
      { path: 'setmeal', name: 'Setmeal', component: () => import('@/views/setmeal/index.vue'), meta: { title: '套餐管理' } },
      { path: 'report', name: 'Report', component: () => import('@/views/report/index.vue'), meta: { title: '数据统计' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录跳转登录页
router.beforeEach((to, from, next) => {
  if (to.path === '/login') {
    next()
  } else {
    if (!getToken()) {
      next('/login')
    } else {
      next()
    }
  }
})

export default router