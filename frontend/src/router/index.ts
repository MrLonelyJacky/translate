import { createRouter, createWebHistory } from 'vue-router'

import UploadView from '../views/UploadView.vue'
import ResultView from '../views/ResultView.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/upload',
    },
    {
      path: '/upload',
      name: 'upload',
      component: UploadView,
    },
    {
      path: '/result',
      name: 'result',
      component: ResultView,
    },
  ],
})

