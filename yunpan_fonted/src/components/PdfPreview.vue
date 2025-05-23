<template>
  <div class="pdf-preview">
    <div class="pdf-toolbar">
      <el-button-group>
        <el-button @click="prevPage" :disabled="currentPage <= 1">
          上一页
        </el-button>
        <el-button @click="nextPage" :disabled="currentPage >= totalPages">
          下一页
        </el-button>
      </el-button-group>
      <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
      <el-select v-model="scale" placeholder="缩放" style="width: 120px">
        <el-option v-for="item in scaleOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
    </div>
    <div class="pdf-container" ref="container">
      <canvas ref="pdfCanvas"></canvas>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import * as pdfjsLib from 'pdfjs-dist'
import { ElMessage } from 'element-plus'

// 设置PDF.js worker路径
pdfjsLib.GlobalWorkerOptions.workerSrc = `https://cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjsLib.version}/pdf.worker.min.js`

const props = defineProps({
  fileUrl: {
    type: String,
    required: true
  }
})

const container = ref(null)
const pdfCanvas = ref(null)
const currentPage = ref(1)
const totalPages = ref(0)
const scale = ref(1)
const pdfDoc = ref(null)

const scaleOptions = [
  { value: 0.5, label: '50%' },
  { value: 0.75, label: '75%' },
  { value: 1, label: '100%' },
  { value: 1.25, label: '125%' },
  { value: 1.5, label: '150%' },
  { value: 2, label: '200%' }
]

// 加载PDF文档
const loadPDF = async () => {
  try {
    const loadingTask = pdfjsLib.getDocument(props.fileUrl)
    pdfDoc.value = await loadingTask.promise
    totalPages.value = pdfDoc.value.numPages
    renderPage()
  } catch (error) {
    ElMessage.error('PDF加载失败：' + error.message)
  }
}

// 渲染当前页
const renderPage = async () => {
  if (!pdfDoc.value) return

  try {
    const page = await pdfDoc.value.getPage(currentPage.value)
    const viewport = page.getViewport({ scale: scale.value })

    const canvas = pdfCanvas.value
    const context = canvas.getContext('2d')

    canvas.height = viewport.height
    canvas.width = viewport.width

    const renderContext = {
      canvasContext: context,
      viewport: viewport
    }

    await page.render(renderContext).promise
  } catch (error) {
    ElMessage.error('页面渲染失败：' + error.message)
  }
}

// 上一页
const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

// 下一页
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

// 监听页码变化
watch(currentPage, () => {
  renderPage()
})

// 监听缩放比例变化
watch(scale, () => {
  renderPage()
})

// 监听文件URL变化
watch(() => props.fileUrl, () => {
  loadPDF()
})

onMounted(() => {
  loadPDF()
})
</script>

<style scoped>
.pdf-preview {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.pdf-toolbar {
  padding: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.page-info {
  margin: 0 10px;
  color: #606266;
}

.pdf-container {
  flex: 1;
  overflow: auto;
  display: flex;
  justify-content: center;
  padding: 20px;
  background-color: #f0f2f5;
}

canvas {
  background-color: white;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
</style> 