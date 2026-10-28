<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http } from '../api/http'
import { useTranslateStore, type TranslateLine } from '../stores/translate'

const router = useRouter()
const translateStore = useTranslateStore()
const translating = ref(false)
const editableText = computed({
  get: () => translateStore.ocrText,
  set: (value: string) => translateStore.updateOcrText(value),
})
const translatedText = computed(() => translateStore.lines.map((line) => line.translatedText).join('\n'))

onMounted(() => {
  translateStore.hydrateOcrDraft()
  if (!translateStore.ocrText) {
    ElMessage.info('请先上传图片并完成识别')
    router.replace('/upload')
  } else {
    translateStore.hydrate()
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
    const sourceText = translateStore.ocrText
    const { data } = await http.post('/text/translate', { text: sourceText })
    if (sourceText !== translateStore.ocrText) {
      ElMessage.warning('识别文本已修改，请重新点击翻译')
      return
    }
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
    ElMessage.success(`翻译完成，共 ${resultLines.length} 条`)
  } catch (error: any) {
    const message = error?.response?.data?.message ?? '翻译失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    translating.value = false
  }
}

const copyTranslation = async () => {
  if (!translatedText.value.trim()) {
    ElMessage.warning('暂无可复制译文')
    return
  }
  try {
    await navigator.clipboard.writeText(translatedText.value)
    ElMessage.success('已复制译文')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}
</script>

<template>
  <section class="ocr-wrap">
    <div class="ocr-header">
      <div>
        <h1>识别文本确认</h1>
        <p>识别完成（共 {{ translateStore.ocrCount }} 条），可复制和编辑；翻译结果会显示在右侧</p>
      </div>
      <div class="ocr-actions">
        <el-button @click="router.push('/upload')">重新上传</el-button>
        <el-button @click="copyRecognizedText">复制识别文本</el-button>
        <el-button type="success" :loading="translating" :disabled="!translateStore.ocrText.trim()" @click="translateText">开始翻译</el-button>
      </div>
    </div>

    <div class="ocr-layout" :class="{ 'three-panels': translateStore.lines.length > 0 }">
      <div class="panel">
        <h3>原始图片</h3>
        <div class="panel-body">
          <img :src="translateStore.imageUrl" alt="识别原图" class="origin-image" />
        </div>
      </div>
      <div class="panel">
        <h3>识别文本（可编辑）</h3>
        <div class="panel-body">
          <el-input v-model="editableText" type="textarea" :rows="20" resize="vertical" />
        </div>
      </div>
      <div v-if="translateStore.lines.length" class="panel">
        <div class="translation-header">
          <h3>翻译结果</h3>
          <el-button @click="copyTranslation">复制译文</el-button>
        </div>
        <div class="panel-body translation-text">{{ translatedText }}</div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.ocr-wrap {
  max-width: 1600px;
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
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.ocr-layout.three-panels {
  grid-template-columns: repeat(3, minmax(0, 1fr));
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
  min-width: 0;
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

.translation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.translation-text {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  color: #1f7a38;
  line-height: 1.7;
}

@media (max-width: 900px) {
  .ocr-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .ocr-actions {
    flex-wrap: wrap;
  }

  .ocr-layout,
  .ocr-layout.three-panels {
    grid-template-columns: 1fr;
  }

  .panel {
    min-height: 320px;
  }
}
</style>
