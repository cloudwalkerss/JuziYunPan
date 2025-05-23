<template>
  <div class="pptx-preview">
    <div class="pptx-toolbar">
      <el-button-group>
        <el-button @click="prevSlide" :disabled="currentSlide <= 1">
          上一页
        </el-button>
        <el-button @click="nextSlide" :disabled="currentSlide >= totalSlides">
          下一页
        </el-button>
      </el-button-group>
      <span class="slide-info">{{ currentSlide }} / {{ totalSlides }}</span>
      <el-select v-model="scale" placeholder="缩放" style="width: 120px">
        <el-option v-for="item in scaleOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
    </div>
    <div class="pptx-container" ref="container">
      <div id="pptx-viewer" ref="viewer"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { getHeader } from '@/net'

const props = defineProps({
  fileId: {
    type: String,
    required: true
  }
})

const container = ref(null)
const viewer = ref(null)
const currentSlide = ref(1)
const totalSlides = ref(1)
const scale = ref(1)
let pptxjs = null

const scaleOptions = [
  { value: 0.5, label: '50%' },
  { value: 0.75, label: '75%' },
  { value: 1, label: '100%' },
  { value: 1.25, label: '125%' },
  { value: 1.5, label: '150%' },
  { value: 2, label: '200%' }
]

// 加载PPTX文件
const loadPPTX = async () => {
  try {
    if (!props.fileId) {
      throw new Error('文件ID无效');
    }

    // 创建pptx.js实例
    pptxjs = new window.PptxJS();
    
    // 获取文件URL和认证头
    const fileUrl = `http://localhost:8080/file/getFile/${props.fileId}`;
    const headers = getHeader();
    
    // 加载并渲染PPTX，传入认证头
    await pptxjs.load(fileUrl, headers);
    totalSlides.value = pptxjs.getTotalSlides();
    await pptxjs.render(viewer.value);
    
    // 设置初始缩放
    pptxjs.setZoom(scale.value);
    
  } catch (error) {
    console.error('PPTX加载失败:', error);
    ElMessage.error('PPTX加载失败：' + (error.message || '未知错误'));
    
    // 显示更友好的错误信息
    viewer.value.innerHTML = `
      <div style="padding: 20px; text-align: center; color: #666;">
        <h3 style="margin-bottom: 10px;">无法预览该文件</h3>
        <p>${error.message || '文件格式不正确或已损坏'}</p>
        <p style="margin-top: 10px; font-size: 14px;">建议下载文件后使用本地应用程序打开</p>
      </div>
    `;
  }
}

// 上一页
const prevSlide = () => {
  if (currentSlide.value > 1 && pptxjs) {
    currentSlide.value--;
    pptxjs.goToSlide(currentSlide.value);
  }
}

// 下一页
const nextSlide = () => {
  if (currentSlide.value < totalSlides.value && pptxjs) {
    currentSlide.value++;
    pptxjs.goToSlide(currentSlide.value);
  }
}

// 监听缩放比例变化
watch(scale, (newScale) => {
  if (pptxjs) {
    pptxjs.setZoom(newScale);
  }
});

onMounted(() => {
  // 确保pptx.js脚本已加载
  const script = document.createElement('script');
  script.src = '/pptxjs/js/pptxjs.js';
  script.onload = () => {
    loadPPTX();
  };
  document.head.appendChild(script);
});

// 组件卸载时清理
onUnmounted(() => {
  if (pptxjs) {
    pptxjs.destroy();
  }
})
</script>

<style scoped>
.pptx-preview {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f0f2f5;
}

.pptx-toolbar {
  padding: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.slide-info {
  margin: 0 10px;
  color: #606266;
}

.pptx-container {
  flex: 1;
  overflow: auto;
  display: flex;
  justify-content: center;
  padding: 20px;
}

#pptx-viewer {
  position: relative;
  background-color: #f0f2f5;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

:deep(.pptx-slide) {
  position: relative;
  margin: 0 auto;
  background-color: white;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  transform-origin: center top;
}

:deep(.pptx-shape) {
  position: absolute;
  box-sizing: border-box;
}

:deep(.pptx-paragraph) {
  margin: 0;
  padding: 0;
  line-height: 1.5;
}

:deep(.pptx-text) {
  display: inline-block;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 占位符样式 */
:deep(.pptx-placeholder) {
  border: 1px dashed #ccc;
  background-color: rgba(0, 0, 0, 0.02);
}

/* 标题样式 */
:deep(.pptx-title) {
  font-size: 44px;
  font-weight: bold;
  color: #333;
}

/* 副标题样式 */
:deep(.pptx-subtitle) {
  font-size: 32px;
  color: #666;
}

/* 正文样式 */
:deep(.pptx-body) {
  font-size: 24px;
  color: #333;
}
</style> 