import { defineStore } from 'pinia'
import { ref } from 'vue'

export type TranslateLine = {
  order: number
  sourceText: string
  translatedText: string
  left?: number
  top?: number
  right?: number
  bottom?: number
}

const STORAGE_KEY = 'docbridge_image_translate_result'

const isBlobUrl = (value: string) => value.startsWith('blob:')

export const useTranslateStore = defineStore('translate', () => {
  const imageUrl = ref<string>('')
  const lines = ref<TranslateLine[]>([])
  const sourceName = ref<string>('')

  const revokeIfBlob = (url: string) => {
    if (url && isBlobUrl(url)) {
      URL.revokeObjectURL(url)
    }
  }

  const setResult = (payload: { imageUrl: string; lines: TranslateLine[]; sourceName: string }) => {
    if (imageUrl.value && imageUrl.value !== payload.imageUrl) {
      revokeIfBlob(imageUrl.value)
    }

    imageUrl.value = payload.imageUrl
    lines.value = payload.lines
    sourceName.value = payload.sourceName

    // Blob URL is only valid in current page lifecycle, so do not persist it.
    const persistableImageUrl = isBlobUrl(payload.imageUrl) ? '' : payload.imageUrl
    sessionStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({ imageUrl: persistableImageUrl, lines: payload.lines, sourceName: payload.sourceName }),
    )
  }

  const hydrate = () => {
    if (imageUrl.value && lines.value.length > 0) {
      return
    }
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) {
      return
    }
    try {
      const parsed = JSON.parse(raw)
      imageUrl.value = parsed.imageUrl ?? ''
      lines.value = Array.isArray(parsed.lines) ? parsed.lines : []
      sourceName.value = parsed.sourceName ?? ''
    } catch {
      clearResult()
    }
  }

  const clearResult = () => {
    revokeIfBlob(imageUrl.value)
    imageUrl.value = ''
    lines.value = []
    sourceName.value = ''
    sessionStorage.removeItem(STORAGE_KEY)
  }

  return {
    imageUrl,
    lines,
    sourceName,
    setResult,
    clearResult,
    hydrate,
  }
})
