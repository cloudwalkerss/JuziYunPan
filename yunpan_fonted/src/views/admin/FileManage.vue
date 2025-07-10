<template>
  <div class="file-manage">
    <div class="page-header">
      <h2>文件管理</h2>
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索文件名"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </div>
    </div>

    <div class="action-bar" style="margin-bottom: 15px;">
      <el-button 
        type="danger" 
        plain 
        :disabled="multipleSelection.length === 0"
        @click="handleBatchDelete"
      >
        批量删除
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="fileList"
      style="width: 100%"
      border
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="fileName" label="文件名" min-width="200">
        <template #default="scope">
          <div class="file-name" @click="handleFilePreview(scope.row)">
            <!-- 文件夹图标 -->
            <template v-if="scope.row.folderType === 1">
              <el-icon class="file-icon folder-icon"><Folder /></el-icon>
            </template>
            <!-- 文件图标和缩略图 -->
            <template v-else>
              <!-- 视频文件 -->
              <template v-if="scope.row.fileCategory === 1 || /\.(mp4|webm|ogg|mkv|mov|avi)$/i.test(scope.row.fileName)">
                <div class="file-thumbnail" v-if="scope.row.fileCover">
                  <img :src="`/file/getImage/${scope.row.fileCover}`" alt="视频缩略图">
                  <div class="play-icon"><el-icon><VideoPlay /></el-icon></div>
                </div>
                <el-icon v-else class="file-icon video-icon"><VideoCamera /></el-icon>
              </template>
              <!-- 图片文件 -->
              <template v-else-if="scope.row.fileCategory === 3 || /\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(scope.row.fileName)">
                <div class="file-thumbnail" v-if="scope.row.fileCover">
                  <img :src="`/file/getImage/${scope.row.fileCover}`" alt="图片缩略图">
                </div>
                <el-icon v-else class="file-icon image-icon"><Picture /></el-icon>
              </template>
              <!-- 音频文件 -->
              <template v-else-if="scope.row.fileCategory === 2 || /\.(mp3|wav|aac|flac|ogg)$/i.test(scope.row.fileName)">
                <el-icon class="file-icon audio-icon"><Headset /></el-icon>
              </template>
              <!-- PDF文件 -->
              <template v-else-if="/\.pdf$/i.test(scope.row.fileName)">
                <el-icon class="file-icon pdf-icon"><Document /></el-icon>
              </template>
              <!-- Word文档 -->
              <template v-else-if="/\.(doc|docx)$/i.test(scope.row.fileName)">
                <el-icon class="file-icon word-icon"><Document /></el-icon>
              </template>
              <!-- Excel文件 -->
              <template v-else-if="/\.(xls|xlsx)$/i.test(scope.row.fileName)">
                <el-icon class="file-icon excel-icon"><Document /></el-icon>
              </template>
              <!-- PPT文件 -->
              <template v-else-if="/\.(ppt|pptx)$/i.test(scope.row.fileName)">
                <el-icon class="file-icon ppt-icon"><Document /></el-icon>
              </template>
              <!-- 压缩文件 -->
              <template v-else-if="/\.(zip|rar|7z|tar|gz)$/i.test(scope.row.fileName)">
                <el-icon class="file-icon zip-icon"><Files /></el-icon>
              </template>
              <!-- 其他文件 -->
              <template v-else>
                <el-icon class="file-icon other-icon"><Document /></el-icon>
              </template>
            </template>
            <span :class="{'preview-link': isPreviewable(scope.row)}">
              {{ scope.row.fileName }}
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="所属用户" width="120">
        <template #default="scope">
          {{ getUserName(scope.row) }}
        </template>
      </el-table-column>
      <el-table-column prop="fileSize" label="大小" width="120">
        <template #default="scope">
          {{ formatFileSize(scope.row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="上传时间" width="180">
        <template #default="scope">
          {{ formatDate(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button
            v-if="scope.row.folderType === 1"
            type="primary"
            link
            @click="handleViewFolder(scope.row)"
          >
            查看文件夹
          </el-button>
          <el-button
            type="danger"
            link
            @click="handleDelete(scope.row)"
          >
            删除
          </el-button>
          <el-button
            v-if="scope.row.folderType === 0"
            type="primary"
            link
            @click="handleDownload(scope.row)"
          >
            下载
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 文件夹信息对话框 -->
    <el-dialog
      v-model="folderDialogVisible"
      title="文件夹信息"
      width="500px"
    >
      <div v-if="folderInfo">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="文件夹名称">{{ folderInfo.fileName }}</el-descriptions-item>
          <el-descriptions-item label="文件数量">{{ folderInfo.fileCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="总大小">{{ formatFileSize(folderInfo.size || 0) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(folderInfo.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="最后修改时间">{{ formatDate(folderInfo.updateTime) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 文件预览对话框 -->
    <el-dialog
      v-model="previewDialogVisible"
      :title="currentPreviewFile ? currentPreviewFile.fileName : '文件预览'"
      width="800px"
      fullscreen
      @close="handlePreviewClose"
    >
      <!-- 图片预览 -->
      <div v-if="previewFileType === 'image'" class="preview-container">
        <PreviewImage :url="previewImageUrl" />
      </div>
      
      <!-- 视频预览 -->
      <div v-else-if="previewFileType === 'video'" class="preview-container">
        <VideoPreview 
          :fileId="`${previewUserId}/${previewFileId}`" 
          :visible="previewDialogVisible" 
          :isAdmin="true" />
      </div>
      
      <!-- 音频预览 -->
      <div v-else-if="previewFileType === 'audio'" class="preview-container">
        <PreviewAudio :audioUrl="previewAudioUrl" />
      </div>
      
      <!-- PDF预览 -->
      <div v-else-if="previewFileType === 'pdf'" class="preview-container">
        <PdfPreview :fileUrl="previewPdfUrl" />
      </div>
      
      <!-- Word文档预览 -->
      <div v-else-if="previewFileType === 'docx'" class="preview-container">
        <div class="docx-preview-container">
          <DocxPreview :fileId="previewDocId" />
        </div>
      </div>
      
      <!-- 其他格式 -->
      <div v-else class="preview-container">
        <div class="no-preview">
          <el-icon class="no-preview-icon"><Document /></el-icon>
          <p>此文件类型暂不支持预览，请下载后查看</p>
          <el-button type="primary" @click="handleDownload(currentPreviewFile)">下载文件</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { formatFileTime } from '@/utils/dateUtils.js';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Document, Folder, VideoPlay, VideoCamera, Headset, Picture, Files } from '@element-plus/icons-vue';
import { get, post, getHeader } from '@/net';
import axios from 'axios';
import DocxPreview from '@/components/DocxPreview.vue';
import PreviewImage from '@/components/PreviewImage.vue';
import VideoPreview from '@/components/VideoPreview.vue';
import PreviewAudio from '@/components/PreviewAudio.vue';
import PdfPreview from '@/components/PdfPreview.vue';

const loading = ref(false);
const fileList = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const searchKeyword = ref('');
const folderDialogVisible = ref(false);
const folderInfo = ref(null);
const multipleSelection = ref([]);

// 预览相关的响应式变量
const previewDialogVisible = ref(false);
const previewFileType = ref('');
const previewImageUrl = ref('');
const previewAudioUrl = ref('');
const previewPdfUrl = ref('');
const currentPreviewFile = ref(null);
let previewImageObjectUrl = null;
let previewAudioObjectUrl = null;
const previewDocId = ref('');
const previewFileId = ref(''); // 通用文件ID，用于组件预览
const previewUserId = ref(''); // 用户ID，用于组件预览

// 加载文件列表
const loadFileList = () => {
  loading.value = true;
  
  const requestData = {
    pageNo: currentPage.value,
    pageSize: pageSize.value,
    fileName: searchKeyword.value
  };
  
  console.log('管理端文件列表请求参数:', requestData);
  
  post('admin/loadFileList', requestData, (data) => {
    // 处理数据，确保nickName字段存在
    if (data && data.list && Array.isArray(data.list)) {
      data.list.forEach(item => {
        // 如果nickName不存在但userId存在，设置为用户ID
        if (!item.nickName && !item.nickname && item.userId) {
          item.nickName = `用户ID: ${item.userId}`;
        }
      });
    }
    
    fileList.value = data.list;
    total.value = data.totalCount;
    loading.value = false;
    
    console.log('管理端文件列表响应:', {
      listLength: data.list?.length,
      totalCount: data.totalCount,
      currentPage: currentPage.value,
      pageSize: pageSize.value
    });
    
    // 调试信息
    if (fileList.value && fileList.value.length > 0) {
      console.log('文件列表第一项:', fileList.value[0]);
      console.log('用户名称字段:', fileList.value[0].nickname, fileList.value[0].nickName);
    }
    
  }, () => {
    loading.value = false;
    ElMessage.error('获取文件列表失败');
  });
};

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1;
  loadFileList();
};

// 处理分页变化
const handleSizeChange = (val) => {
  pageSize.value = val;
  loadFileList();
};

const handleCurrentChange = (val) => {
  currentPage.value = val;
  loadFileList();
};

// 删除文件
const handleDelete = (file) => {
  ElMessageBox.confirm('确定要删除该文件吗？此操作不可恢复！', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    post('admin/delFile', {
      fileIdAndUserIds: `${file.userId}_${file.fileId}`
    }, () => {
      ElMessage.success('文件删除成功');
      loadFileList();
    }, () => {
      ElMessage.error('文件删除失败');
    });
  }).catch(() => {});
};

// 下载文件
const handleDownload = async (file) => {
  if (!file) return;
  
  try {
    // 获取下载链接
    const response = await new Promise((resolve, reject) => {
      get(`admin/createDownloadUrl/${file.userId}/${file.fileId}`, (data) => {
        resolve(data);
      }, (error) => {
        reject(error);
      });
    });

    // 下载文件
    const downloadResponse = await axios({
      method: 'get',
      url: `admin/download/${response}`,
      responseType: 'blob',
      headers: getHeader()  // 添加认证头
    });

    // 使用 File-Saver 或创建一个隐藏的 a 标签来下载
    const blob = new Blob([downloadResponse.data]);
    const downloadUrl = window.URL.createObjectURL(blob);
    const tempLink = document.createElement('a');
    tempLink.style.display = 'none';
    tempLink.href = downloadUrl;
    tempLink.setAttribute('download', file.fileName);
    
    try {
      document.body.appendChild(tempLink);
      tempLink.click();
    } finally {
      document.body.removeChild(tempLink);
      // 确保总是清理 URL 对象
      window.URL.revokeObjectURL(downloadUrl);
    }

    ElMessage.success('文件下载完成');
  } catch (error) {
    console.error('下载文件失败:', error);
    ElMessage.error('下载文件失败');
  }
};


// 格式化日期
const formatDate = formatFileTime;

// 格式化文件大小
const formatFileSize = (size) => {
  if (!size) return '0 B';
  
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let index = 0;
  let fileSize = parseFloat(size);
  
  while (fileSize >= 1024 && index < units.length - 1) {
    fileSize /= 1024;
    index++;
  }
  
  return fileSize.toFixed(2) + ' ' + units[index];
};

// 查看文件夹信息
const handleViewFolder = (folder) => {
  const encodedPath = encodeURIComponent(folder.filePath);
  get(`admin/getFolderInfo?path=${encodedPath}`, (data) => {
    folderInfo.value = data;
    folderDialogVisible.value = true;
  }, () => {
    ElMessage.error('获取文件夹信息失败');
  });
};

// 获取用户名称
const getUserName = (file) => {
  // 尝试获取用户名称，优先级: nickname > nickName > userId > '未知用户'
  return file.nickname || file.nickName || (file.userId ? `用户ID: ${file.userId}` : '未知用户');
};

// 处理文件预览
const handleFilePreview = async (row) => {
  // 如果是文件夹，不预览
  if (row.folderType === 1) {
    return;
  }
  
  const fileName = row.fileName || '';
  const fileType = row.fileType;
  const fileCategory = row.fileCategory;
  
  // 设置当前预览文件信息
  currentPreviewFile.value = row;
  
  // 设置通用预览参数
  previewUserId.value = row.userId;
  previewFileId.value = row.fileId;
  
  // 关闭前释放旧的objectURL
  if (previewImageObjectUrl) { URL.revokeObjectURL(previewImageObjectUrl); previewImageObjectUrl = null; }
  if (previewAudioObjectUrl) { URL.revokeObjectURL(previewAudioObjectUrl); previewAudioObjectUrl = null; }

  // 视频
  if (fileCategory === 1 || fileType === 1 || /\.(mp4|webm|ogg|mkv|mov|avi)$/i.test(fileName)) {
    previewFileType.value = 'video';
    previewDialogVisible.value = true;
    return;
  }
  
  // 图片
  if (fileCategory === 3 || fileType === 3 || /\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(fileName)) {
    try {
      // 使用axios，但带上认证头
      const response = await axios.get(`/admin/getFile/${row.userId}/${row.fileId}`, { 
        responseType: 'blob',
        headers: getHeader()
      });
      previewImageObjectUrl = URL.createObjectURL(response.data);
      previewImageUrl.value = previewImageObjectUrl;
      previewFileType.value = 'image';
      previewDialogVisible.value = true;
    } catch (e) {
      ElMessage.error('图片加载失败');
      console.error('图片预览失败:', e);
    }
    return;
  }
  
  // 音频
  if (fileCategory === 2 || fileType === 2 || /\.(mp3|wav|aac|flac|ogg)$/i.test(fileName)) {
    try {
      // 使用axios，但带上认证头
      const response = await axios.get(`/admin/getFile/${row.userId}/${row.fileId}`, { 
        responseType: 'blob',
        headers: getHeader()
      });
      previewAudioObjectUrl = URL.createObjectURL(response.data);
      previewAudioUrl.value = previewAudioObjectUrl;
      previewFileType.value = 'audio';
      previewDialogVisible.value = true;
    } catch (e) {
      ElMessage.error('音频加载失败');
      console.error('音频预览失败:', e);
    }
    return;
  }
  
  // PDF文件
  if (/\.pdf$/i.test(fileName)) {
    // 获取token
    const headers = getHeader();
    const token = headers.Authorization ? headers.Authorization.split(' ')[1] : '';
    previewPdfUrl.value = `/admin/getFile/${row.userId}/${row.fileId}`;
    previewFileType.value = 'pdf';
    previewDialogVisible.value = true;
    return;
  }
  
  // Word文档 (doc/docx)
  if (/\.(doc|docx)$/i.test(fileName)) {
    previewDocId.value = `${row.userId}/${row.fileId}`;
    previewFileType.value = 'docx';
    previewDialogVisible.value = true;
    return;
  }
  
  // 不支持预览的文件类型
  previewFileType.value = '';
  previewDialogVisible.value = true;
};

// 处理预览关闭
const handlePreviewClose = () => {
  previewFileType.value = '';
  if (previewImageObjectUrl) { 
    URL.revokeObjectURL(previewImageObjectUrl); 
    previewImageObjectUrl = null; 
    previewImageUrl.value = ''; 
  }
  if (previewAudioObjectUrl) { 
    URL.revokeObjectURL(previewAudioObjectUrl); 
    previewAudioObjectUrl = null; 
    previewAudioUrl.value = ''; 
  }
  previewPdfUrl.value = '';
  previewDocId.value = '';
  previewFileId.value = '';
  previewUserId.value = '';
  previewDialogVisible.value = false;
  currentPreviewFile.value = null;
};

// 判断文件是否可预览
const isPreviewable = (file) => {
  if (!file || !file.fileName || file.folderType === 1) return false;
  
  const fileName = file.fileName.toLowerCase();
  return (
    // 图片文件
    file.fileCategory === 3 || 
    /\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(fileName) ||
    // 视频文件
    file.fileCategory === 1 || 
    /\.(mp4|webm|ogg|mkv|mov|avi)$/i.test(fileName) ||
    // 音频文件
    file.fileCategory === 2 || 
    /\.(mp3|wav|aac|flac|ogg)$/i.test(fileName) ||
    // PDF文件
    /\.pdf$/i.test(fileName) ||
    // Office文档
    /\.(doc|docx|ppt|pptx|xls|xlsx)$/i.test(fileName)
  );
};

const handleSelectionChange = (val) => {
  multipleSelection.value = val;
};

const handleBatchDelete = () => {
  if (multipleSelection.value.length === 0) {
    ElMessage.warning('请至少选择一个文件进行删除。');
    return;
  }

  ElMessageBox.confirm(
    `确定要删除选中的 ${multipleSelection.value.length} 个文件吗？此操作不可恢复！`,
    '警告',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    const fileIdAndUserIdsArray = multipleSelection.value.map(file => `${file.userId}_${file.fileId}`);
    const fileIdAndUserIdsString = fileIdAndUserIdsArray.join(',');

    post('admin/delFile', { fileIdAndUserIds: fileIdAndUserIdsString }, 
      () => {
        ElMessage.success('选定文件删除成功。');
        loadFileList(); // 重新加载列表
        multipleSelection.value = []; // 清空选择
      },
      () => {
        ElMessage.error('文件删除失败，请重试。');
      }
    );
  }).catch(() => {
    ElMessage.info('已取消删除操作。');
  });
};

onMounted(() => {
  loadFileList();
});
</script>

<style scoped>
.file-manage {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-bar {
  width: 300px;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

:deep(.el-descriptions__label) {
  width: 120px;
  font-weight: bold;
}

.file-thumbnail {
  width: 40px;
  height: 40px;
  overflow: hidden;
  border-radius: 4px;
  margin-right: 8px;
  position: relative;
}

.file-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background-color: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  padding: 4px;
  color: white;
}

.preview-link {
  cursor: pointer;
  color: #409EFF;
}

.preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: calc(100vh - 120px);
  width: 100%;
}

.preview-container > * {
  width: 100%;
  height: 100%;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.preview-video {
  width: 100%;
  max-height: 80vh;
}

.preview-audio {
  width: 100%;
  max-width: 500px;
}

.preview-pdf {
  width: 100%;
  height: 100%;
  border: none;
}

.docx-preview-container {
  width: 100%;
  height: 100%;
  background-color: #f4f4f4;
  display: flex;
  justify-content: center;
  overflow: auto;
}

.no-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.no-preview-icon {
  font-size: 64px;
  color: #909399;
}

.file-icon {
  font-size: 20px;
}

.folder-icon {
  color: #FFC107;
}

.video-icon {
  color: #FF5722;
}

.image-icon {
  color: #4CAF50;
}

.audio-icon {
  color: #2196F3;
}

.pdf-icon, .word-icon, .excel-icon, .ppt-icon {
  color: #E91E63;
}

.zip-icon {
  color: #795548;
}

.other-icon {
  color: #9E9E9E;
}
</style> 