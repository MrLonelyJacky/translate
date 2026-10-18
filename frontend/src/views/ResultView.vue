<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useTranslateStore } from '../stores/translate'

const router = useRouter()
const translateStore = useTranslateStore()

onMounted(() => {
  translateStore.hydrate()
  if (!translateStore.imageUrl || translateStore.lines.length === 0) {
    ElMessage.info('请先上传图片并完成翻译')
    router.replace('/upload')
  }
})

const copyAll = async () => {
  const text = translateStore.lines
    .map((line) => `${line.sourceText}\n${line.translatedText}`)
    .join('\n\n')
  if (!text) {
    ElMessage.warning('暂无可复制内容')
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制全部译文')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

const lineCount = computed(() => translateStore.lines.length)
</script>

<template>
  <section class="result-wrap">
    <div class="result-header">
      <div>
        <h1>翻译结果</h1>
        <p>原图 + OCR识别 + 译文对照（共 {{ lineCount }} 条）</p>
      </div>
      <div class="result-actions">
        <el-button @click="router.push('/upload')">重新上传</el-button>
        <el-button type="primary" @click="copyAll">复制全部</el-button>
      </div>
    </div>

    <div class="result-layout">
      <div class="panel">
        <h3>原始图片</h3>
        <div class="panel-body">
          <img :src="translateStore.imageUrl" alt="翻译原图" class="origin-image" />
        </div>
      </div>
      <div class="panel">
        <h3>识别与翻译结果</h3>
        <div class="panel-body list-body">
          <div v-for="line in translateStore.lines" :key="line.order" class="line-card">
            <div class="source">{{ line.sourceText }}</div>
            <div class="target">{{ line.translatedText }}</div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.result-wrap {
  max-width: 1200px;
  margin: 28px auto 44px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.result-header {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e6e9f5;
  box-shadow: 0 14px 32px rgba(24, 39, 75, 0.08);
  padding: 22px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-header h1 {
  margin: 0;
  font-size: 28px;
  color: #1f2a44;
}

.result-header p {
  margin: 8px 0 0;
  color: #7481a3;
}

.result-actions {
  display: flex;
  gap: 10px;
}

.result-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.panel {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e6e9f5;
  box-shadow: 0 14px 32px rgba(24, 39, 75, 0.06);
  padding: 16px;
  min-height: 620px;
  display: flex;
  flex-direction: column;
}

.panel h3 {
  margin: 0;
  color: #2a3655;
}

.panel-body {
  margin-top: 14px;
  flex: 1;
  overflow: auto;
}

.origin-image {
  width: 100%;
  object-fit: contain;
}

.list-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.line-card {
  border: 1px solid #ecf0ff;
  border-radius: 12px;
  padding: 12px;
  background: #fafbff;
}

.source {
  color: #2d3a5f;
  margin-bottom: 6px;
}

.target {
  color: #1f7a38;
}
</style>

