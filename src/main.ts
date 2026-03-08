import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import App from './App.vue'
import MainLayout from '@/layouts/MainLayout.vue'
import HomePage from '@/pages/HomePage.vue'
import BrowsePage from '@/pages/BrowsePage.vue'
import FeaturedPage from '@/pages/FeaturedPage.vue'
import CommunityPage from '@/pages/CommunityPage.vue'
import ProfilePage from '@/pages/ProfilePage.vue'
import EditorPage from '@/pages/EditorPage.vue'
import NotFoundPage from '@/pages/NotFoundPage.vue'
import { authGuard, saveEditorState, scrollBehavior, trackPageView } from '@/router/guards'
import '@/assets/styles/global.css'

const browseRedirectPaths = [
  '/settings',
  '/notifications',
  '/blog',
  '/trending',
  '/collections',
  '/groups',
  '/contests',
  '/collaborations',
  '/tuner',
  '/metronome',
  '/chord-chart',
  '/transpose',
  '/festival/scores',
  '/festival/contest',
  '/contests/festival'
]

const featuredRedirectPaths = ['/features', '/pricing', '/festival/music']

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '', name: 'home', component: HomePage },
      { path: 'browse', name: 'browse', component: BrowsePage },
      { path: 'featured', name: 'featured', component: FeaturedPage },
      { path: 'forum', name: 'forum', component: CommunityPage },
      { path: 'profile', name: 'profile', component: ProfilePage, meta: { requiresAuth: true } },
      { path: 'my-scores', name: 'my-scores', component: ProfilePage, meta: { requiresAuth: true } },
      { path: 'score/:id', name: 'score-detail', redirect: to => `/editor/${to.params.id as string}` }
    ]
  },
  { path: '/forums', redirect: '/forum' },
  { path: '/editor', name: 'editor', component: EditorPage },
  { path: '/editor/:id', name: 'editor-score', component: EditorPage },
  { path: '/login', name: 'login', redirect: '/' },
  ...browseRedirectPaths.map((path) => ({
    path,
    redirect: '/browse'
  })),
  ...featuredRedirectPaths.map((path) => ({
    path,
    redirect: '/featured'
  })),
  { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundPage }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: scrollBehavior as any
})

router.beforeEach((to, from, next) => {
  authGuard(to, from, (authDecision?: unknown) => {
    if (authDecision !== undefined) {
      next(authDecision as any)
      return
    }
    saveEditorState(to, from, next)
  })
})

router.afterEach((to, from) => {
  trackPageView(to, from)
})

createApp(App).use(createPinia()).use(router).mount('#app')
