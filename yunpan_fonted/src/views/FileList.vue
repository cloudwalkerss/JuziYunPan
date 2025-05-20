<template>
  <div class="file-list">
    <!-- 添加预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      title="文档预览"
      width="80%"
      :before-close="handlePreviewClose"
    >
      <docx-preview
        v-if="previewVisible"
        :file-id="currentFileId"
      />
    </el-dialog>

    <!-- 文件列表 -->
    <el-table :data="fileList" style="width: 100%">
      <el-table-column prop="fileName" label="文件名" />
      <el-table-column prop="fileSize" label="大小" />
      <el-table-column prop="createTime" label="创建时间" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button
            v-if="isDocxFile(row.fileName)"
            type="primary"
            link
            @click="handlePreview(row)"
          >
            预览
          </el-button>
          <el-button
            type="primary"
            link
            @click="handleDownload(row)"
          >
            下载
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import DocxPreview from '@/components/DocxPreview.vue'

export default {
  components: {
    DocxPreview
  },
  data() {
    return {
      fileList: [],
      previewVisible: false,
      currentFileId: ''
    }
  },
  methods: {
    isDocxFile(fileName) {
      return fileName.toLowerCase().endsWith('.docx')
    },
    handlePreview(file) {
      this.currentFileId = file.fileId
      this.previewVisible = true
    },
    handlePreviewClose() {
      this.previewVisible = false
      this.currentFileId = ''
    },
    // ... existing code ...
  }
}
</script> 