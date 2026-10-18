<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadRawFile } from 'element-plus'
import { http } from '../api/http'
import { useTranslateStore, type TranslateLine } from '../stores/translate'

const selectedFile = ref<File | null>(null)
const imageUrl = ref<string>('')
const translating = ref(false)
const router = useRouter()
const translateStore = useTranslateStore()

const maxSize = 10 * 1024 * 1024

const beforeUpload = (file: UploadRawFile) => {
  if (file.size > maxSize) {
    ElMessage.error('图片大小超过 10MB 限制')
    return false
  }
  return false
}

const isImageFile = (file: File) => {
  const name = file.name.toLowerCase()
  return name.endsWith('.jpg') || name.endsWith('.jpeg') || name.endsWith('.png')
}

const onFileChange = (file: UploadFile) => {
  if (!file.raw) {
    selectedFile.value = null
    imageUrl.value = ''
    return
  }

  if (!isImageFile(file.raw)) {
    ElMessage.error('仅支持 JPG/PNG 图片')
    selectedFile.value = null
    imageUrl.value = ''
    return
  }

  if (file.raw.size > maxSize) {
    ElMessage.error('图片大小超过 10MB 限制')
    selectedFile.value = null
    imageUrl.value = ''
    return
  }

  if (imageUrl.value) {
    URL.revokeObjectURL(imageUrl.value)
  }
  selectedFile.value = file.raw
  imageUrl.value = URL.createObjectURL(file.raw)
}

const setFileFromClipboard = (file: File) => {
  if (!isImageFile(file)) {
    ElMessage.error('剪贴板内容不是 JPG/PNG 图片')
    return
  }
  if (file.size > maxSize) {
    ElMessage.error('图片大小超过 10MB 限制')
    return
  }
  if (imageUrl.value) {
    URL.revokeObjectURL(imageUrl.value)
  }
  selectedFile.value = file
  imageUrl.value = URL.createObjectURL(file)
  ElMessage.success('已从剪贴板粘贴图片')
}

const onPaste = (event: ClipboardEvent) => {
  const items = event.clipboardData?.items
  if (!items) {
    return
  }
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      const file = item.getAsFile()
      if (file) {
        event.preventDefault()
        setFileFromClipboard(file)
      }
      return
    }
  }
}

const translateImage = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择图片')
    return
  }

  translating.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    const { data } = await http.post('/image/translate', formData)
    const resultLines: TranslateLine[] = data.data?.lines ?? []
    if (resultLines.length === 0) {
      ElMessage.warning('未识别到文字内容')
    } else {
      translateStore.setResult({
        imageUrl: imageUrl.value,
        lines: resultLines,
        sourceName: selectedFile.value.name,
      })
      ElMessage.success(`识别并翻译完成，共 ${resultLines.length} 条`)
      router.push('/result')
    }
  } catch (error: any) {
    const message = error?.response?.data?.message ?? '识别翻译失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    translating.value = false
  }
}

onMounted(() => {
  window.addEventListener('paste', onPaste)
})

onBeforeUnmount(() => {
  // Keep current blob URL alive for Result page after route navigation.
  window.removeEventListener('paste', onPaste)
})
</script>

<template>
  <section class="landing-wrap">
    <div class="hero-block">
      <p class="hero-tag">DocBridge</p>
      <h1>图片翻译工具</h1>
      <p class="hero-subtitle">上传或粘贴截图，立即获得 OCR 识别与中英对照翻译结果</p>
    </div>

    <div class="upload-card">
      <el-upload
        class="upload-area"
        drag
        :auto-upload="false"
        :show-file-list="true"
        :limit="1"
        :before-upload="beforeUpload"
        :on-change="onFileChange"
      >
        <div class="upload-title">拖拽图片到此处，或点击上传</div>
        <template #tip>
          <div class="upload-tip">支持 JPG/PNG，最大 10MB，支持 Ctrl+V 粘贴截图</div>
        </template>
      </el-upload>

      <div class="preview-thumb" v-if="imageUrl">
        <img :src="imageUrl" alt="待翻译图片预览" />
      </div>

      <div class="upload-actions">
        <el-button type="primary" size="large" :loading="translating" @click="translateImage">
          开始翻译
        </el-button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.landing-wrap {
  max-width: 980px;
  margin: 40px auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.hero-block {
  text-align: center;
}

.hero-tag {
  color: #4f6af0;
  font-weight: 600;
  margin: 0;
}

.hero-block h1 {
  margin: 8px 0;
  font-size: 38px;
  color: #1f2a44;
}

.hero-subtitle {
  margin: 0;
  color: #6a748f;
}

.upload-card {
  background: #ffffff;
  border: 1px solid #e6e9f5;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 20px 40px rgba(37, 61, 128, 0.08);
}

.upload-area :deep(.el-upload-dragger) {
  border-radius: 14px;
  border-color: #cfd7ff;
  background: #f8f9ff;
}

.upload-title {
  font-size: 18px;
  color: #2a3553;
}

.upload-tip {
  color: #7f89a3;
}

.preview-thumb {
  margin-top: 16px;
  border: 1px solid #e6e9f5;
  border-radius: 12px;
  background: #f8f9ff;
  padding: 10px;
}

.preview-thumb img {
  width: 100%;
  max-height: 360px;
  object-fit: contain;
}

.upload-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>

