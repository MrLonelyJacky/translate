import { createRouter, createWebHistory } from 'vue-router'

import UploadView from '../views/UploadView.vue'
import OcrEditView from '../views/OcrEditView.vue'

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
      path: '/ocr',
      name: 'ocr',
      component: OcrEditView,
    },
    {
      path: '/result',
      redirect: '/ocr',
    },
  ],
})
