<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http } from '../api/http'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const popupRef = ref<Window | null>(null)

interface LoginMessagePayload {
  type: 'DOCBRIDGE_WECHAT_LOGIN'
  success: boolean
  data: {
    token: string
    user: {
      id: number
      unionId?: string
      openId?: string
      role: 'USER' | 'ADMIN'
    }
  }
}

const handleLoginMessage = (event: MessageEvent<LoginMessagePayload>) => {
  if (!event.data || event.data.type !== 'DOCBRIDGE_WECHAT_LOGIN' || !event.data.success) {
    return
  }
  const payload = event.data.data
  authStore.setToken(payload.token)
  authStore.setUser(payload.user)
  popupRef.value?.close()
  popupRef.value = null
  ElMessage.success('登录成功')
  router.push('/upload')
}

const goUpload = async () => {
  loading.value = true
  try {
    const { data } = await http.get('/auth/wechat/qr-url')
    popupRef.value = window.open(data.data.authUrl, 'docbridge_wechat_login', 'width=480,height=680')
    if (!popupRef.value) {
      ElMessage.warning('弹窗被浏览器拦截，请允许弹窗后重试')
    }
  } catch (error: any) {
    const message = error?.response?.data?.message ?? '登录失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  window.addEventListener('message', handleLoginMessage)
})

onUnmounted(() => {
  window.removeEventListener('message', handleLoginMessage)
  if (popupRef.value && !popupRef.value.closed) {
    popupRef.value.close()
  }
})
</script>

<template>
  <section class="page-card">
    <h2 class="page-title">登录页</h2>
    <p>点击微信登录后会打开扫码窗口，完成扫码确认即可自动登录。</p>
    <el-button type="primary" :loading="loading" @click="goUpload">微信扫码登录</el-button>
  </section>
</template>

