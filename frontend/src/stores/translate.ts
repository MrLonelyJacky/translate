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
const OCR_DRAFT_STORAGE_KEY = 'docbridge_image_ocr_draft'
const OCR_EDIT_STORAGE_KEY = 'docbridge_image_ocr_edit'

const isBlobUrl = (value: string) => value.startsWith('blob:')

export const useTranslateStore = defineStore('translate', () => {
  const imageUrl = ref<string>('')
  const lines = ref<TranslateLine[]>([])
  const sourceName = ref<string>('')
  const ocrText = ref<string>('')
  const ocrCount = ref<number>(0)

  const revokeIfBlob = (url: string) => {
    if (url && isBlobUrl(url)) {
      URL.revokeObjectURL(url)
    }
  }

  const clearTranslation = () => {
    lines.value = []
    sessionStorage.removeItem(STORAGE_KEY)
  }

  const updateOcrText = (text: string) => {
    ocrText.value = text
    clearTranslation()
    sessionStorage.setItem(OCR_EDIT_STORAGE_KEY, text)
  }

  const setResult = (payload: { imageUrl: string; lines: TranslateLine[]; sourceName: string }) => {
    if (imageUrl.value && imageUrl.value !== payload.imageUrl) {
      revokeIfBlob(imageUrl.value)
    }

    imageUrl.value = payload.imageUrl
    lines.value = payload.lines
    sourceName.value = payload.sourceName

    // Blob URL is only valid in current page lifecycle, so do not persist it.
    const persistableImageUrl =
      isBlobUrl(payload.imageUrl) || sessionStorage.getItem(OCR_DRAFT_STORAGE_KEY) ? '' : payload.imageUrl
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
      imageUrl.value = parsed.imageUrl || imageUrl.value
      lines.value = Array.isArray(parsed.lines) ? parsed.lines : []
      sourceName.value = parsed.sourceName ?? ''
    } catch {
      clearResult()
    }
  }

  const setOcrDraft = (payload: { imageUrl: string; sourceName: string; ocrText: string; ocrCount: number }) => {
    if (imageUrl.value && imageUrl.value !== payload.imageUrl) {
      revokeIfBlob(imageUrl.value)
    }
    clearTranslation()
    imageUrl.value = payload.imageUrl
    sourceName.value = payload.sourceName
    ocrText.value = payload.ocrText
    ocrCount.value = payload.ocrCount
    sessionStorage.removeItem(OCR_EDIT_STORAGE_KEY)
    const persistableImageUrl = isBlobUrl(payload.imageUrl) ? '' : payload.imageUrl
    sessionStorage.setItem(
      OCR_DRAFT_STORAGE_KEY,
      JSON.stringify({
        imageUrl: persistableImageUrl,
        sourceName: payload.sourceName,
        ocrText: payload.ocrText,
        ocrCount: payload.ocrCount,
      }),
    )
  }

  const hydrateOcrDraft = () => {
    if (ocrText.value) {
      return
    }
    const raw = sessionStorage.getItem(OCR_DRAFT_STORAGE_KEY)
    if (!raw) {
      return
    }
    try {
      const parsed = JSON.parse(raw)
      imageUrl.value = parsed.imageUrl ?? imageUrl.value
      sourceName.value = parsed.sourceName ?? sourceName.value
      ocrText.value = sessionStorage.getItem(OCR_EDIT_STORAGE_KEY) ?? parsed.ocrText ?? ''
      ocrCount.value = Number(parsed.ocrCount ?? 0)
    } catch {
      clearOcrDraft()
    }
  }

  const clearOcrDraft = () => {
    ocrText.value = ''
    ocrCount.value = 0
    sessionStorage.removeItem(OCR_DRAFT_STORAGE_KEY)
    sessionStorage.removeItem(OCR_EDIT_STORAGE_KEY)
  }

  const clearResult = () => {
    revokeIfBlob(imageUrl.value)
    imageUrl.value = ''
    lines.value = []
    sourceName.value = ''
    clearOcrDraft()
    sessionStorage.removeItem(STORAGE_KEY)
  }

  return {
    imageUrl,
    lines,
    sourceName,
    ocrText,
    ocrCount,
    setResult,
    setOcrDraft,
    updateOcrText,
    clearTranslation,
    clearOcrDraft,
    clearResult,
    hydrate,
    hydrateOcrDraft,
  }
})
