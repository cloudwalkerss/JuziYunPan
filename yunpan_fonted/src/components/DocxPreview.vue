<template>
  <div class="docx-preview">
    <div v-if="loading" class="loading">
      <el-progress :percentage="progress" />
      <span>文件加载中...</span>
    </div>
    <div v-else-if="error" class="error">
      <el-alert
        :title="error"
        type="error"
        :closable="false"
        show-icon
      />
    </div>
    <div v-else class="content" v-html="content"></div>
  </div>
</template>

<script>
import mammoth from 'mammoth'
import { ElMessage } from 'element-plus'
import axios from "axios";
import {getHeader} from "@/net/index.js";

export default {
  name: 'DocxPreview',
  props: {
    fileId: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      loading: true,
      error: null,
      content: '',
      progress: 0
    }
  },
  async created() {
    await this.loadFile()
  },
  methods: {
    async loadFile() {
      try {
        this.loading = true
        this.error = null
        
        // 检查fileId格式，判断是否包含userId（管理员界面传入的格式为userId/fileId）
        let url = '';
        // 获取token
        const headers = getHeader();
        const token = headers.Authorization ? headers.Authorization.split(' ')[1] : '';
        
        if (this.fileId.includes('/')) {
          // 管理员界面格式：userId/fileId
          url = `/admin/getFile/${this.fileId}`;
        } else {
          // 用户界面格式：fileId
          url = `/file/getFile/${this.fileId}`;
        }
        
        // 获取文件
        const response = await fetch(url, {
          headers: {
            ...getHeader()
          }
        })
        
        if (!response.ok) {
          throw new Error('文件加载失败')
        }
        
        // 获取文件大小
        const contentLength = +response.headers.get('Content-Length')
        const reader = response.body.getReader()
        const chunks = []
        let receivedLength = 0
        
        // 读取文件流
        while(true) {
          const {done, value} = await reader.read()
          
          if (done) {
            break
          }
          
          chunks.push(value)
          receivedLength += value.length
          
          // 更新进度
          this.progress = Math.round((receivedLength / contentLength) * 100)
        }
        
        // 合并文件块
        const blob = new Blob(chunks)
        const arrayBuffer = await blob.arrayBuffer()
        
        // 转换为HTML
        const result = await mammoth.convertToHtml({ arrayBuffer })
        this.content = result.value
        
        // 添加样式
        this.addStyles()
        
      } catch (err) {
        console.error('文件加载错误:', err)
        this.error = '文件加载失败，请稍后重试'
        ElMessage.error('文件加载失败')
      } finally {
        this.loading = false
      }
    },
    
    addStyles() {
      const style = document.createElement('style')
      style.textContent = `
        .docx-preview {
          padding: 20px;
          background: white;
          box-shadow: 0 2px 4px rgba(0,0,0,0.1);
          border-radius: 4px;
        }
        .docx-preview img {
          max-width: 100%;
          height: auto;
        }
        .docx-preview table {
          border-collapse: collapse;
          width: 100%;
          margin: 10px 0;
        }
        .docx-preview td, .docx-preview th {
          border: 1px solid #ddd;
          padding: 8px;
        }
        .docx-preview p {
          margin: 8px 0;
          line-height: 1.6;
        }
        .docx-preview h1, .docx-preview h2, .docx-preview h3 {
          margin: 16px 0 8px;
        }
      `
      document.head.appendChild(style)
    }
  }
}
</script>

<style scoped>
.docx-preview {
  min-height: 200px;
  margin: 20px;
}

.loading {
  text-align: center;
  padding: 20px;
  color: #666;
}

.error {
  margin: 20px;
}

.content {
  line-height: 1.6;
  overflow-x: auto;
}
</style> 