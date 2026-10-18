import { defineStore } from 'pinia'

export interface TaskItem {
  taskId: string
  status: 'queued' | 'processing' | 'succeeded' | 'failed'
  sourceFilename: string
  sourceType: 'PDF' | 'DOCX' | 'PPTX' | 'JPG' | 'PNG'
  createdAt: string
  finishedAt?: string
}

interface TaskState {
  list: TaskItem[]
}

export const useTaskStore = defineStore('task', {
  state: (): TaskState => ({
    list: [],
  }),
  actions: {
    setTasks(list: TaskItem[]) {
      this.list = list
    },
  },
})

