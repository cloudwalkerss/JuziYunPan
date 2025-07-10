<template>
  <div class="share-manage">
    <div class="page-header">
      <h2>分享管理</h2>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索文件名/分享人"
          clearable
          @keyup.enter="handleSearch"
          style="width: 200px"
        >
          <template #append>
            <el-button @click="handleSearch">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="shareList"
      style="width: 100%"
      border
    >
      <el-table-column prop="shareId" label="分享ID" width="100" />
      <el-table-column label="文件名" min-width="200">
        <template #default="scope">
          <div class="file-name-cell">
            <el-icon 
              class="file-icon"
              :class="getFileIconClass(scope.row.fileName)"
            >
              <component :is="getFileIcon(scope.row.fileName)" />
            </el-icon>
            <span>{{ scope.row.fileName }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="分享人" width="120">
        <template #default="scope">
          {{ scope.row.nickName || scope.row.username || `用户${scope.row.userId}` }}
        </template>
      </el-table-column>
      <el-table-column prop="shareTime" label="分享时间" width="180">
        <template #default="scope">
          {{ formatDate(scope.row.shareTime) }}
        </template>
      </el-table-column>
      <el-table-column label="有效期" width="180">
        <template #default="scope">
          {{ getExpireTime(scope.row.shareTime, scope.row.validType) }}
        </template>
      </el-table-column>
      <el-table-column prop="code" label="访问码" width="100">
        <template #default="scope">
          {{ scope.row.code || '无' }}
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览次数" width="100" align="center" />
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="getShareStatusType(scope.row)">
            {{ getShareStatus(scope.row) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="scope">
          <el-button-group>
            <el-tooltip content="复制链接" placement="top" :show-after="500">
              <el-button
                type="primary"
                link
                @click="copyShareLink(scope.row)"
              >
                <el-icon><Link /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="取消分享" placement="top" :show-after="500">
              <el-button
                type="danger"
                link
                @click="cancelShare(scope.row)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-tooltip>
          </el-button-group>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { formatShareTime, calculateExpireTime } from '@/utils/dateUtils.js';
import { ElMessage, ElMessageBox } from 'element-plus';
import { 
  Search,
  Link,
  Delete,
  Document,
  Picture,
  VideoCamera,
  Headset
} from '@element-plus/icons-vue';
import { get, post } from '@/net';

// 状态变量
const loading = ref(false);
const shareList = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const searchKeyword = ref('');

// 加载分享列表
const loadShareList = async () => {
  loading.value = true;
  try {
    const params = {
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value
    };
    
    post('admin/share/list', params, (data) => {
      shareList.value = data.list || [];
      total.value = data.totalCount || 0;  // 修正字段名：total -> totalCount
      loading.value = false;
      
      console.log('管理端分享列表响应:', {
        listLength: data.list?.length,
        totalCount: data.totalCount,
        currentPage: currentPage.value,
        pageSize: pageSize.value
      });
    }, () => {
      loading.value = false;
      ElMessage.error('获取分享列表失败');
    });
  } catch (error) {
    loading.value = false;
    console.error('加载分享列表失败:', error);
  }
};

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1;
  loadShareList();
};

// 处理分页变化
const handleSizeChange = (val) => {
  pageSize.value = val;
  loadShareList();
};

const handleCurrentChange = (val) => {
  currentPage.value = val;
  loadShareList();
};

// 获取文件图标
const getFileIcon = (fileName) => {
  if (!fileName) return Document;
  
  const extension = fileName.toLowerCase().split('.').pop();
  
  if (/^(jpg|jpeg|png|gif|bmp|webp)$/i.test(extension)) {
    return Picture;
  } else if (/^(mp4|webm|ogg|mkv|mov|avi)$/i.test(extension)) {
    return VideoCamera;
  } else if (/^(mp3|wav|ogg|m4a|flac)$/i.test(extension)) {
    return Headset;
  }
  
  return Document;
};

// 获取文件图标样式类
const getFileIconClass = (fileName) => {
  if (!fileName) return 'other-icon';
  
  const extension = fileName.toLowerCase().split('.').pop();
  
  if (/^(jpg|jpeg|png|gif|bmp|webp)$/i.test(extension)) {
    return 'image-icon';
  } else if (/^(mp4|webm|ogg|mkv|mov|avi)$/i.test(extension)) {
    return 'video-icon';
  } else if (/^(mp3|wav|ogg|m4a|flac)$/i.test(extension)) {
    return 'audio-icon';
  } else if (/^(doc|docx)$/i.test(extension)) {
    return 'word-icon';
  } else if (/^(xls|xlsx)$/i.test(extension)) {
    return 'excel-icon';
  } else if (/^(ppt|pptx)$/i.test(extension)) {
    return 'ppt-icon';
  } else if (extension === 'pdf') {
    return 'pdf-icon';
  }
  
  return 'other-icon';
};


// 格式化日期
const formatDate = formatShareTime;

// 计算分享状态
const getShareStatus = (share) => {
  if (!share) return '';
  
  const now = new Date().getTime();
  const shareTime = new Date(share.shareTime).getTime();
  
  // 永久有效
  if (share.validType === 3) {
    return '永久有效';
  }
  
  // 计算有效期（天）
  const validDays = {
    0: 1,  // 1天
    1: 7,  // 7天
    2: 30  // 30天
  }[share.validType] || 0;
  
  const expireTime = shareTime + validDays * 24 * 60 * 60 * 1000;
  
  return now > expireTime ? '已过期' : '有效';
};

// 获取分享状态样式
const getShareStatusType = (share) => {
  const status = getShareStatus(share);
  switch (status) {
    case '永久有效':
      return 'success';
    case '有效':
      return 'primary';
    case '已过期':
      return 'info';
    default:
      return '';
  }
};

// 计算过期时间
const getExpireTime = calculateExpireTime;

// 复制分享链接
const copyShareLink = async (share) => {
  const shareLink = `${window.location.origin}/share/${share.shareId}`;
  try {
    await navigator.clipboard.writeText(shareLink);
    ElMessage.success('分享链接已复制到剪贴板');
  } catch (error) {
    console.error('复制失败:', error);
    ElMessage.error('复制失败，请手动复制');
  }
};

// 取消分享
const cancelShare = (share) => {
  ElMessageBox.confirm('确定要取消该分享吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    post('admin/share/cancel', {
      shareId: share.shareId
    }, () => {
      ElMessage.success('取消分享成功');
      loadShareList();
    }, () => {
      ElMessage.error('取消分享失败');
    });
  }).catch(() => {});
};

onMounted(() => {
  loadShareList();
});
</script>

<style scoped>
.share-manage {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
}

.header-actions {
  display: flex;
  gap: 16px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  font-size: 20px;
}

.image-icon {
  color: #67c23a;
}

.video-icon {
  color: #ff4e50;
}

.audio-icon {
  color: #409eff;
}

.word-icon {
  color: #2b579a;
}

.excel-icon {
  color: #217346;
}

.ppt-icon {
  color: #d24726;
}

.pdf-icon {
  color: #f56c6c;
}

.other-icon {
  color: #909399;
}

:deep(.el-tag) {
  text-align: center;
  min-width: 80px;
}

.el-button-group {
  display: flex;
  gap: 8px;
}
</style> 