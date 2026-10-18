<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()

const navItems = [
  { path: '/upload', label: '图片翻译' },
  { path: '/result', label: '结果预览' },
]
</script>

<template>
  <div class="app-shell">
    <header class="app-header modern-header">
      <div class="brand">
        <h1>DocBridge</h1>
        <span>Image Translate</span>
      </div>
      <nav class="app-nav modern-nav">
        <button
          v-for="item in navItems"
          :key="item.path"
          class="nav-button"
          :class="{ active: route.path.startsWith(item.path) }"
          @click="router.push(item.path)"
        >
          {{ item.label }}
        </button>
      </nav>
    </header>
    <main class="app-main modern-main">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.modern-header {
  border-bottom: none;
  padding: 22px 32px;
}

.brand {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand h1 {
  font-size: 22px;
  color: #24305f;
}

.brand span {
  color: #7a86ac;
  font-size: 13px;
}

.modern-nav {
  background: #ffffff;
  border: 1px solid #e3e8ff;
  border-radius: 999px;
  padding: 5px;
}

.modern-nav .nav-button {
  border: none;
  border-radius: 999px;
  padding: 8px 16px;
  color: #64719a;
}

.modern-nav .nav-button.active {
  background: #4f6af0;
  color: #ffffff;
}

.modern-main {
  padding: 0 24px 24px;
}
</style>

