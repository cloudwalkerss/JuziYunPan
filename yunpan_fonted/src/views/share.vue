<script setup>
import { ref, onMounted } from 'vue';
import { formatShareTime, calculateExpireTime } from '@/utils/dateUtils.js';
import { ElMessage, ElMessageBox } from 'element-plus';
import { get, post } from '@/net';
import { 
  Document, 
  FolderOpened, 
  Link, 
  Delete, 
  Share, 
  VideoPlay, 
  Picture, 
  Headset, 
  View 
} from '@element-plus/icons-vue';
import {zhCn} from "element-plus/es/locale/index";

// 分页相关
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 分享列表数据
const shareList = ref([]);
const loading = ref(false);

// 加载分享列表
async function loadShareList() {
  loading.value = true;
  try {
    const params = {
      pageNo: currentPage.value,
      pageSize: pageSize.value
    };

    post('share/loadShareList', params, (data) => {
      shareList.value = data.list || [];
      total.value = data.totalCount || 0;
      loading.value = false;
    }, () => {
      loading.value = false;
      ElMessage.error('获取分享列表失败');
    });
  } catch (error) {
    loading.value = false;
    console.error('加载分享列表失败:', error);
  }
}

// 复制分享链接
async function copyShareLink(shareId) {
  const shareLink = `${window.location.origin}/share/${shareId}`;
  try {
    await navigator.clipboard.writeText(shareLink);
    ElMessage.success('分享链接已复制到剪贴板');
  } catch (error) {
    console.error('复制失败:', error);
    ElMessage.error('复制失败，请手动复制');
  }
}

// 取消分享
function cancelShare(shareIds) {
  ElMessageBox.confirm('确定要取消分享吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    post('share/cancelShare', {
      shareIds: shareIds
    }, () => {
      ElMessage.success('取消分享成功');
      loadShareList(); // 重新加载列表
    }, () => {
      ElMessage.error('取消分享失败');
    });
  }).catch(() => {});
}

// 格式化时间


const formatDate = formatShareTime;
const getExpireTime = calculateExpireTime;

// 监听分页变化
function handleCurrentChange(val) {
  currentPage.value = val;
  loadShareList();
}

function handleSizeChange(val) {
  pageSize.value = val;
  currentPage.value = 1;
  loadShareList();
}

// 获取文件图标和样式
function getFileTypeInfo(fileName, folderType) {
  if (folderType === 1) {
    return {
      icon: FolderOpened,
      color: '#ffd04b',
      bgColor: '#fdf6ec',
      type: '文件夹'
    };
  }

  const fileExtension = fileName.toLowerCase().split('.').pop();
  
  // 视频文件
  if (/^(mp4|webm|ogg|mkv|mov|avi)$/.test(fileExtension)) {
    return {
      icon: VideoPlay,
      color: '#ff4e50',
      bgColor: '#ffeded',
      type: '视频'
    };
  }
  
  // 图片文件
  if (/^(jpg|jpeg|png|gif|bmp|webp)$/.test(fileExtension)) {
    return {
      icon: Picture,
      color: '#67c23a',
      bgColor: '#f0f9eb',
      type: '图片'
    };
  }
  
  // 音频文件
  if (/^(mp3|wav|aac|flac|ogg)$/.test(fileExtension)) {
    return {
      icon: Headset,
      color: '#409eff',
      bgColor: '#ecf5ff',
      type: '音频'
    };
  }
  
  // 文档文件
  if (/^(doc|docx)$/.test(fileExtension)) {
    return {
      icon: Document,
      color: '#2b579a',
      bgColor: '#eef5fc',
      type: 'Word文档'
    };
  }
  
  // Excel文件
  if (/^(xls|xlsx)$/.test(fileExtension)) {
    return {
      icon: Document,
      color: '#217346',
      bgColor: '#ebf8f2',
      type: 'Excel表格'
    };
  }
  
  // PPT文件
  if (/^(ppt|pptx)$/.test(fileExtension)) {
    return {
      icon: Document,
      color: '#d24726',
      bgColor: '#fff3ef',
      type: 'PPT演示'
    };
  }
  
  // PDF文件
  if (fileExtension === 'pdf') {
    return {
      icon: Document,
      color: '#f56c6c',
      bgColor: '#fef0f0',
      type: 'PDF文档'
    };
  }
  
  // 压缩文件
  if (/^(zip|rar|7z|tar|gz)$/.test(fileExtension)) {
    return {
      icon: Document,
      color: '#4A90E2',
      bgColor: '#ecf5ff',
      type: '压缩包'
    };
  }
  
  // 默认文件
  return {
    icon: Document,
    color: '#909399',
    bgColor: '#f4f4f5',
    type: '文件'
  };
}

// 页面加载时获取分享列表
onMounted(() => {
  loadShareList();
});
</script>

<template>
  <el-config-provider :locale="zhCn">
    <div class="share-container">
      <div class="share-header">
        <div class="header-left">
          <div class="header-icon">
            <el-icon><Share /></el-icon>
          </div>
          <h2>我的分享</h2>
        </div>
        <div class="header-right">
          <el-tag type="info" effect="plain" round>
            <el-icon><View /></el-icon>
            共 {{ total }} 个分享
          </el-tag>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="shareList"
        style="width: 100%"
        :border="false"
        stripe
      >
        <el-table-column prop="fileName" label="文件名" min-width="300">
          <template #default="scope">
            <div class="file-name">
              <div class="file-icon" 
                :style="{ 
                  color: getFileTypeInfo(scope.row.fileName, scope.row.folderType).color,
                  backgroundColor: getFileTypeInfo(scope.row.fileName, scope.row.folderType).bgColor
                }"
              >
                <el-icon>
                  <component :is="getFileTypeInfo(scope.row.fileName, scope.row.folderType).icon" />
                </el-icon>
              </div>
              <div class="file-info">
                <span class="file-title">{{ scope.row.fileName }}</span>
                <el-tag 
                  size="small" 
                  :type="scope.row.folderType === 1 ? 'warning' : 'info'" 
                  class="file-type-tag"
                  effect="light"
                >
                  {{ getFileTypeInfo(scope.row.fileName, scope.row.folderType).type }}
                </el-tag>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="分享信息" min-width="300">
          <template #default="scope">
            <div class="share-info">
              <div class="share-time">
                <el-icon><Share /></el-icon>
                <span>分享于 {{ formatDate(scope.row.shareTime) }}</span>
              </div>
              <div class="expire-time" :class="{ 'permanent': scope.row.validType === 3 }">
                <el-icon><Link /></el-icon>
                <span>{{ getExpireTime(scope.row.shareTime, scope.row.validType) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="访问信息" width="200">
          <template #default="scope">
            <div class="access-info">
              <el-tag 
                v-if="scope.row.code" 
                type="success" 
                class="code-tag"
                effect="light"
              >
                访问码: {{ scope.row.code }}
              </el-tag>
              <el-tag v-else type="info" effect="light">无访问码</el-tag>
              <div class="view-count">
                <el-icon><View /></el-icon>
                <span>浏览 {{ scope.row.viewCount }} 次</span>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-tooltip content="复制链接" placement="top" :show-after="500">
                <el-button
                  type="primary"
                  circle
                  size="small"
                  @click="copyShareLink(scope.row.shareId)"
                >
                  <el-icon><Link /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="取消分享" placement="top" :show-after="500">
                <el-button
                  type="danger"
                  circle
                  size="small"
                  @click="cancelShare(scope.row.shareId)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          background
        />
      </div>
    </div>
  </el-config-provider>
</template>

<style scoped>
.share-container {
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,21,41,0.08);
  min-height: calc(100vh - 40px);
}

.share-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background-color: #e6f7ff;
  color: #1890ff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-icon .el-icon {
  margin-right: 0;
  font-size: 20px;
}

.share-header h2 {
  font-size: 18px;
  color: #1f2f3d;
  margin: 0;
  font-weight: 600;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  transition: all 0.3s;
}

.file-icon:hover {
  transform: scale(1.05);
}

.file-icon .el-icon {
  margin-right: 0;
}

.file-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-title {
  font-size: 14px;
  color: #1f2f3d;
  transition: color 0.3s;
}

.file-type-tag {
  height: 20px;
  padding: 0 6px;
}

.share-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.share-time, .expire-time {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 13px;
}

.share-time .el-icon, .expire-time .el-icon {
  font-size: 14px;
  color: #909399;
}

.permanent {
  color: #67c23a;
}

.permanent .el-icon {
  color: #67c23a;
}

.access-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
}

.code-tag {
  height: 28px;
  padding: 0 10px;
}

.view-count {
  color: #909399;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.action-buttons .el-button {
  width: 32px;
  height: 32px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.action-buttons .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

/* 表格样式优化 */
:deep(.el-table) {
  --el-table-border-color: #f0f0f0;
  --el-table-header-bg-color: #fafafa;
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-table::before) {
  display: none;
}

:deep(.el-table th) {
  background-color: var(--el-table-header-bg-color);
  color: #606266;
  font-weight: 600;
  border-bottom: 1px solid var(--el-table-border-color);
  padding: 12px 0;
}

:deep(.el-table td) {
  padding: 16px 0;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background-color: #fafafa;
}

:deep(.el-table__body tr:hover > td) {
  background-color: #f5f7fa !important;
}

/* 标签样式 */
:deep(.el-tag) {
  border-radius: 4px;
}

:deep(.el-tag--light) {
  border: 1px solid transparent;
}

/* 按钮过渡效果 */
.el-button {
  transition: all 0.3s ease;
}

.el-button.is-circle {
  transition: all 0.3s ease;
}

.el-button.is-circle:hover {
  transform: scale(1.1);
}

/* 文件名悬停效果 */
.file-name:hover .file-title {
  color: #409eff;
}

/* 自定义滚动条 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

::-webkit-scrollbar-track {
  background: #f5f7fa;
  border-radius: 3px;
}

/* 图标样式优化 */
.el-icon {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  margin-right: 4px;
}

.file-icon .el-icon {
  margin-right: 0;
}

.header-icon .el-icon {
  margin-right: 0;
  font-size: 20px;
}

.el-tag .el-icon {
  margin-right: 4px;
  font-size: 14px;
}

.view-count .el-icon {
  font-size: 14px;
  color: #909399;
}

.action-buttons .el-button .el-icon {
  margin-right: 0;
  font-size: 16px;
}

/* 按钮样式优化 */
.action-buttons .el-button {
  width: 32px;
  height: 32px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* 标签样式优化 */
.el-tag {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 8px;
}

.file-type-tag {
  height: 20px;
  padding: 0 6px;
}

.code-tag {
  height: 28px;
  padding: 0 10px;
}
</style> 