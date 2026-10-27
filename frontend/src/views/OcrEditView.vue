<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http } from '../api/http'
import { useTranslateStore, type TranslateLine } from '../stores/translate'

const router = useRouter()
const translateStore = useTranslateStore()
const translating = ref(false)

onMounted(() => {
  translateStore.hydrateOcrDraft()
  if (!translateStore.ocrText) {
    ElMessage.info('请先上传图片并完成识别')
    router.replace('/upload')
  }
})

const copyRecognizedText = async () => {
  if (!translateStore.ocrText.trim()) {
    ElMessage.warning('暂无可复制内容')
    return
  }
  try {
    await navigator.clipboard.writeText(translateStore.ocrText)
    ElMessage.success('已复制识别文本')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

const translateText = async () => {
  if (!translateStore.ocrText.trim()) {
    ElMessage.warning('请先确认文本内容')
    return
  }

  translating.value = true
  try {
    const { data } = await http.post('/text/translate', { text: translateStore.ocrText })
    const resultLines: TranslateLine[] = data.data?.lines ?? []
    if (resultLines.length === 0) {
      ElMessage.warning('未返回翻译内容')
      return
    }
    translateStore.setResult({
      imageUrl: translateStore.imageUrl,
      lines: resultLines,
      sourceName: translateStore.sourceName || 'clipboard-image.png',
    })
    translateStore.clearOcrDraft()
    ElMessage.success(`翻译完成，共 ${resultLines.length} 条`)
    router.push('/result')
  } catch (error: any) {
    const message = error?.response?.data?.message ?? '翻译失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    translating.value = false
  }
}
</script>

<template>
  <section class="ocr-wrap">
    <div class="ocr-header">
      <div>
        <h1>识别文本确认</h1>
        <p>识别完成（共 {{ translateStore.ocrCount }} 条），可复制和编辑后再翻译</p>
      </div>
      <div class="ocr-actions">
        <el-button @click="router.push('/upload')">重新上传</el-button>
        <el-button @click="copyRecognizedText">复制识别文本</el-button>
        <el-button type="success" :loading="translating" @click="translateText">开始翻译</el-button>
      </div>
    </div>

    <div class="ocr-layout">
      <div class="panel">
        <h3>原始图片</h3>
        <div class="panel-body">
          <img :src="translateStore.imageUrl" alt="识别原图" class="origin-image" />
        </div>
      </div>
      <div class="panel">
        <h3>识别文本（可编辑）</h3>
        <div class="panel-body">
          <el-input v-model="translateStore.ocrText" type="textarea" :rows="20" resize="vertical" />
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.ocr-wrap {
  max-width: 1200px;
  margin: 28px auto 44px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.ocr-header {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e6e9f5;
  box-shadow: 0 14px 32px rgba(24, 39, 75, 0.08);
  padding: 22px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ocr-header h1 {
  margin: 0;
  font-size: 28px;
  color: #1f2a44;
}

.ocr-header p {
  margin: 8px 0 0;
  color: #7481a3;
}

.ocr-actions {
  display: flex;
  gap: 10px;
}

.ocr-layout {
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
</style>
