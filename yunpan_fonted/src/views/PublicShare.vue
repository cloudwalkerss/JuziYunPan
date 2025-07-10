<template>
  <div class="share-page">
    <!-- 提取码输入对话框 -->
    <el-dialog
      v-model="codeDialogVisible"
      title="请输入提取码"
      width="350px"
      :close-on-click-modal="false"
      :show-close="false"
      :close-on-press-escape="false"
      custom-class="code-dialog"
    >
      <p class="code-dialog-tip">此分享内容已加密，请输入提取码访问</p>
      <el-input v-model="inputCode" placeholder="请输入提取码" @keyup.enter="checkCode" size="large"/>
      <template #footer>
        <el-button type="primary" @click="checkCode" class="confirm-code-btn" size="large">确定</el-button>
      </template>
    </el-dialog>

    <FolderSelector v-model="folderSelectorVisible" @folder-selected="handleFolderSelected" />
    <LoginDialog v-model="loginDialogVisible" @login-success="handleLoginSuccess" />

    <div v-if="loading" class="loading-indicator">
      <el-icon class="is-loading"><Loading /></el-icon>
      <p>正在加载分享信息...</p>
    </div>

    <div v-else-if="error" class="error-container">
       <h2>{{ error.title }}</h2>
       <p>{{ error.message }}</p>
       <el-button type="primary" @click="router.push('/')">返回首页</el-button>
    </div>

    <div v-else-if="!codeDialogVisible && shareInfo && Object.keys(shareInfo).length > 0" class="share-content-wrapper">
      <div class="share-header">
        <div class="sharer-info">
          <Avatar :userId="shareInfo.userId" :size="45" />
          <div class="sharer-text">
            <div class="sharer-name">{{ shareInfo.nickName }}</div>
            <div class="sub-text">分享于 {{ shareInfo.shareTime }}</div>
          </div>
        </div>
        <div class="share-info-bar">
            <span>有效期: {{ getExpireTimeText(shareInfo.expireTime) }}</span>
        </div>
      </div>
      
      <div v-if="shareInfo.folderType === 1" class="folder-view-container">
        <div class="top-actions">
            <el-button 
                v-if="!shareInfo.currentUser"
                type="primary" 
                @click="saveToMyPan(true)" 
                :disabled="selectedFileIds.length === 0 || saveLoading"
                :loading="saveLoading"
            >
                <el-icon v-if="!saveLoading"><FolderAdd /></el-icon>
                {{ saveLoading ? '保存中...' : '保存到我的网盘' }}
            </el-button>
            <div v-else class="own-share-tip">
                <el-icon><InfoFilled /></el-icon>
                <span>这是您自己的分享，无需保存到网盘</span>
            </div>
        </div>
        <div class="breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{}" @click="handleBreadcrumbClick(-1)">全部文件</el-breadcrumb-item>
            <el-breadcrumb-item v-for="(item, index) in breadcrumbNav" :key="item.fileId" :to="{}" @click="handleBreadcrumbClick(index)">
              {{ item.fileName }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="file-list-container">
          <el-table :data="fileList" @selection-change="handleSelectionChange" v-loading="fileListLoading">
            <el-table-column type="selection" width="55" />
            <el-table-column label="文件名" prop="fileName">
                <template #default="{ row }">
                     <div class="file-list-item" @click="handleFileClick(row)">
                        <img v-if="row.fileCover" :src="`/file/getImage/${row.fileCover}`" class="list-cover"/>
                        <el-icon v-else class="file-icon"><component :is="getFileTypeInfo(row.fileName).icon" /></el-icon>
                        <span class="file-name">{{ row.fileName }}</span>
                    </div>
                </template>
            </el-table-column>
            <el-table-column label="修改时间" prop="lastUpdateTime" width="200" />
            <el-table-column label="大小" prop="fileSize" width="150">
                 <template #default="{ row }">
                    {{ row.folderType === 1 ? '-' : formatFileSize(row.fileSize) }}
                </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div v-else class="single-file-view-container">
         <div class="file-preview-section" :style="{ backgroundColor: getFileTypeInfo(shareInfo.fileName).bgColor }">
            <img 
              v-if="shareInfo.fileCover" 
              :src="`/file/getImage/${shareInfo.fileCover}`" 
              class="video-cover"
            />
            <el-icon v-else class="file-big-icon" :style="{ color: getFileTypeInfo(shareInfo.fileName).color }">
              <component :is="getFileTypeInfo(shareInfo.fileName).icon" />
            </el-icon>
            <div class="file-type-name">{{ getFileTypeInfo(shareInfo.fileName).type }}</div>
          </div>
          <div class="file-details-section">
            <div class="file-info-panel">
              <h2 class="file-name-title">{{ shareInfo.fileName }}</h2>
               <div class="file-meta">
                <span>文件大小: {{ formatFileSize(shareInfo.fileSize) }}</span>
              </div>
            </div>
            <div class="action-buttons">
              <el-button 
                  v-if="!shareInfo.currentUser"
                  type="primary" 
                  size="large" 
                  @click="saveToMyPan(false)" 
                  :icon="saveLoading ? null : FolderAdd"
                  :loading="saveLoading"
                  :disabled="saveLoading"
              >
                {{ saveLoading ? '保存中...' : '保存到我的网盘' }}
              </el-button>
              <div v-else class="own-share-tip large">
                <el-icon><InfoFilled /></el-icon>
                <span>这是您自己的分享</span>
              </div>
              <el-button type="success" size="large" @click="downloadFile(shareInfo.fileId)" :icon="Download">
                下载文件
              </el-button>
            </div>
          </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { get, post } from '@/net/index.js';
import { saveShareToMyPan } from '@/net/shareApi.js';
import { ElMessage, ElDialog, ElInput, ElButton, ElIcon, ElTable, ElTableColumn, ElBreadcrumb, ElBreadcrumbItem } from 'element-plus';
import Avatar from '@/components/Avatar.vue';
import FolderSelector from '@/components/FolderSelector.vue';
import LoginDialog from '@/components/LoginDialog.vue';
import { FolderAdd, Download, Folder, Document, VideoCamera, Headset, Picture, Loading, QuestionFilled, InfoFilled } from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();

const shareId = ref(route.params.shareId);
const shareInfo = ref({});
const codeDialogVisible = ref(false);
const inputCode = ref('');
const shareCode = ref(''); // 保存验证成功的分享码
const error = ref(null);
const loading = ref(true);
const fileList = ref([]);
const fileListLoading = ref(false);
const selectedFileIds = ref([]);
const breadcrumbNav = ref([]);
const folderSelectorVisible = ref(false);
const loginDialogVisible = ref(false);
const saveLoading = ref(false);

// 获取当前用户ID的辅助函数
const getCurrentUserId = async () => {
  try {
    const token = localStorage.getItem('token');
    if (!token) {
      return -1; // 未登录返回-1
    }
    
    // 调用用户信息接口获取当前用户ID
    const userInfo = await get('/user/getUserInfo');
    return userInfo?.id || -1;
  } catch (error) {
    // 如果获取用户信息失败（比如token过期），清理本地token并返回-1
    console.warn('获取用户信息失败，可能是token过期:', error);
    if (error.message && error.message.includes('401')) {
      localStorage.removeItem('token');
    }
    return -1;
  }
};

onMounted(() => {
  // 清除可能存在的旧的分享验证token，确保每次访问都需要验证
  // 注意：这里不清除当前分享的token，因为用户可能是刷新页面
  loadShareInfo();
});

const loadShareInfo = async () => {
  loading.value = true;
  error.value = null; // Reset on each load
  try {
    // 检查是否已经验证过分享码，同时检查是否有保存的分享码
    if (!sessionStorage.getItem(`share_token_${shareId.value}`) || !shareCode.value) {
      // 未验证分享码或没有保存的分享码，直接显示分享码输入对话框
      codeDialogVisible.value = true;
      loading.value = false;
      return;
    }

    // 已验证分享码且有保存的分享码，获取完整的分享信息
    await loadFullShareInfo();
  } catch (err) {
    console.error("Error loading share info:", err);
    error.value = {
        title: '无法加载此分享',
        message: err.message || '请检查分享链接是否正确，或稍后重试。'
    };
    loading.value = false;
  }
};

const loadFullShareInfo = async () => {
  try {
    // 获取当前用户ID（未登录返回-1）
    const currentUserId = await getCurrentUserId();
    
    // 构造请求参数，传递shareId、userId和code
    const params = new URLSearchParams({
      shareId: shareId.value,
      userId: currentUserId.toString(),
      code: shareCode.value // 传递验证成功的分享码
    });
    
    const data = await new Promise((resolve, reject) => {
      get(`/showShare/getShareInfo?${params.toString()}`, resolve, reject);
    });

    if (!data) {
        throw new Error('分享链接不存在或已失效');
    }

    console.log("Backend Response:", data); // Log the response for debugging
    console.log("ShareInfo before update:", shareInfo.value);
    shareInfo.value = data;
    console.log("ShareInfo after update:", shareInfo.value);
    console.log("CodeDialogVisible:", codeDialogVisible.value);
    
    // 立即重置loading状态，让页面能够显示内容
    loading.value = false;
    console.log("Loading set to false");
    
    // 如果是文件夹类型，异步加载文件列表（不影响主页面显示）
    if (data.folderType === 1) {
      // 使用nextTick确保DOM更新后再加载文件列表
      setTimeout(() => {
        loadFileList('0');
      }, 0);
    }
  } catch (error) {
    console.error("Error in loadFullShareInfo:", error);
    loading.value = false;
    throw error; // 重新抛出错误让调用者处理
  }
};

const checkCode = async () => {
  if (!inputCode.value) return ElMessage.warning('请输入提取码');
  try {
    await post('/showShare/checkShareCode', { shareId: shareId.value, code: inputCode.value });
    
    // 保存验证成功的分享码
    shareCode.value = inputCode.value;
    sessionStorage.setItem(`share_token_${shareId.value}`, "1");
    codeDialogVisible.value = false;
    ElMessage.success('提取成功');
    
    // 验证成功后加载完整的分享信息
    loading.value = true;
    await loadFullShareInfo();
  } catch (error) {
    ElMessage.error(error?.message || '提取码错误');
  }
};

const loadFileList = async (pid) => {
    fileListLoading.value = true;
    try {
        const data = await get(`/showShare/loadFileList?shareId=${shareId.value}&filePid=${pid}`);
        fileList.value = data.list;
    } catch (error) {
        ElMessage.error(error?.message || '加载文件列表失败');
    } finally {
        fileListLoading.value = false;
    }
};

const handleSelectionChange = (val) => {
  selectedFileIds.value = val.map(item => item.fileId);
};

const handleFileClick = (row) => {
    if (row.folderType === 1) {
        breadcrumbNav.value.push({ fileName: row.fileName, fileId: row.fileId });
        loadFileList(row.fileId);
    } else {
        downloadFile(row.fileId);
    }
};

const handleBreadcrumbClick = (index) => {
  if (index === -1) {
    breadcrumbNav.value = [];
    loadFileList('0');
  } else {
    const targetFile = breadcrumbNav.value[index];
    breadcrumbNav.value.splice(index + 1);
    loadFileList(targetFile.fileId);
  }
};

const downloadFile = async (fileId) => {
  try {
    const code = await get(`/showShare/createDownloadUrl/${shareId.value}/${fileId}`);
    const baseUrl = import.meta.env.VITE_API_URL;
    window.location.href = `${baseUrl}/showShare/download/${code}`;
  } catch (error) {
    ElMessage.error(error?.message || '下载失败，请重试');
  }
};

const saveToMyPan = async (isFolder) => {
    // 防止重复点击
    if (saveLoading.value) {
        return;
    }
    
    // 检查分享是否有效
    if (!shareInfo.value || !shareInfo.value.shareId) {
        return ElMessage.error('分享信息无效，请刷新页面重试');
    }
    
    // 检查是否为自己的分享
    if (shareInfo.value.currentUser) {
        return ElMessage.warning('不能保存自己分享的文件到自己的网盘');
    }
    
    // 检查是否选择了文件（针对文件夹分享）
    if (isFolder && selectedFileIds.value.length === 0) {
        return ElMessage.warning('请选择要保存的文件');
    }
    
    // 检查登录状态并验证token有效性
    const token = localStorage.getItem('access_token') || sessionStorage.getItem('access_token');
    console.log('当前token状态:', token ? '存在' : '不存在');
    
    if (token) {
        try {
            // 验证token是否有效
            const userInfo = await get('/user/getUserInfo');
            console.log('用户信息获取成功:', userInfo);
            
            if (userInfo?.id) {
                // token有效，直接显示文件夹选择器
                console.log('token有效，显示文件夹选择器');
                folderSelectorVisible.value = true;
        return;
    }
        } catch (error) {
            console.error('验证token时出错:', error);
            // token无效或过期，清除token
            if (error.message && error.message.includes('401')) {
                localStorage.removeItem('access_token');
                sessionStorage.removeItem('access_token');
                console.log('token已过期，已清除');
            }
        }
    } else {
        console.log('未找到token，需要登录');
    }
    
    // 未登录或token无效，显示登录对话框
    loginDialogVisible.value = true;
};

const handleLoginSuccess = () => {
    // 登录成功后，重新加载分享信息以更新currentUser状态
    loginDialogVisible.value = false;
    
    // 重新加载分享信息，然后显示文件夹选择器
    loadFullShareInfo().then(() => {
        setTimeout(() => {
            folderSelectorVisible.value = true;
        }, 500);
    });
};

const handleFolderSelected = async (folderId) => {
    folderSelectorVisible.value = false;
    saveLoading.value = true;
    
    const isFolder = shareInfo.value.folderType === 1;
    const shareFileIds = isFolder ? selectedFileIds.value.join(',') : shareInfo.value.fileId;
    
    try {
        // 使用专门的分享API进行保存，传递分享码
        const result = await saveShareToMyPan(shareId.value, shareFileIds, folderId, shareCode.value);
        // 检查是否需要重新登录
        if (result && result.needRelogin) {
            // 显示登录对话框
            loginDialogVisible.value = true;
            ElMessage.warning(result.message);
            return;
        }
        ElMessage.success('已成功保存到我的网盘');
        
        // 重置选择状态
        if (isFolder) {
            selectedFileIds.value = [];
        }
    } catch (error) {
        ElMessage.error(error?.message || '保存失败');
    } finally {
        saveLoading.value = false;
    }
};

const formatFileSize = (size) => {
  if (!size) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let index = 0, fileSize = parseFloat(size);
  while (fileSize >= 1024 && index < units.length - 1) {
    fileSize /= 1024;
    index++;
  }
  return `${fileSize.toFixed(1)} ${units[index]}`;
};

const getFileTypeInfo = (fileName) => {
    if (!fileName) return { icon: QuestionFilled, type: '未知', color: '#909399', bgColor: '#f4f4f5' };
    const ext = fileName.split('.').pop().toLowerCase();
    const types = {
        video: { icon: VideoCamera, type: '视频', color: '#6366F1', bgColor: '#EEF2FF' },
        audio: { icon: Headset, type: '音频', color: '#EC4899', bgColor: '#FCE7F3' },
        image: { icon: Picture, type: '图片', color: '#22C55E', bgColor: '#F0FDF4' },
        doc: { icon: Document, type: '文档', color: '#0EA5E9', bgColor: '#F0F9FF' },
        other: { icon: QuestionFilled, type: '其他', color: '#6B7280', bgColor: '#F9FAFB' }
    };
    if (['mp4', 'avi', 'mkv'].includes(ext)) return types.video;
    if (['mp3', 'wav'].includes(ext)) return types.audio;
    if (['png', 'jpg', 'jpeg', 'gif'].includes(ext)) return types.image;
    if (['doc', 'docx', 'pdf', 'txt', 'xls', 'xlsx', 'ppt', 'pptx'].includes(ext)) return types.doc;
    return types.other;
};

const getExpireTimeText = (expireTime) => expireTime ? `至 ${expireTime}` : '永久有效';

</script>

<style scoped>
.share-page { display: flex; justify-content: center; align-items: flex-start; min-height: 100vh; background-image: linear-gradient(120deg, #fdfbfb 0%, #ebedee 100%); padding: 40px 20px; }
.share-content-wrapper { width: 100%; max-width: 1000px; background: #fff; border-radius: 16px; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.07); padding: 30px; }
.share-header { display: flex; justify-content: space-between; align-items: center; padding-bottom: 20px; border-bottom: 1px solid #eee; margin-bottom: 20px; }
.sharer-info { display: flex; align-items: center; }
.sharer-text { margin-left: 15px; }
.sharer-name { font-weight: 600; font-size: 16px; }
.sub-text { font-size: 13px; color: #888; }
.top-actions { margin-bottom: 15px; }
.breadcrumb { margin-bottom: 15px; }
.file-list-item { cursor: pointer; display: flex; align-items: center; }
.file-list-item:hover .file-name { color: #409eff; }
.file-icon { margin-right: 10px; font-size: 18px; }
.single-file-view-container { display: flex; }
.file-preview-section { width: 300px; flex-shrink: 0; display: flex; flex-direction: column; justify-content: center; align-items: center; padding: 40px; text-align: center; border-radius: 12px; }
.file-big-icon { font-size: 100px; margin-bottom: 20px; }
.file-type-name { font-size: 20px; font-weight: 600; }
.file-details-section { flex-grow: 1; padding: 0 40px; display: flex; flex-direction: column; justify-content: center; }
.file-name-title { font-size: 24px; margin-bottom: 15px; }
.file-meta { font-size: 14px; color: #666; margin-bottom: 30px; }
:deep(.code-dialog) { border-radius: 12px; }
.code-dialog-tip { color: #606266; margin-bottom: 20px; text-align: center; }
.confirm-code-btn { width: 100%; }
.loading-indicator, .error-container {
  text-align: center;
  margin-top: 20vh;
  color: #666;
}
.error-container h2 {
    font-size: 24px;
    margin-bottom: 15px;
}
.error-container p {
    margin-bottom: 25px;
}
.video-cover {
  max-width: 100%;
  max-height: 200px;
  border-radius: 8px;
  margin-bottom: 20px;
  object-fit: cover;
}

.list-cover {
    width: 32px;
    height: 32px;
    object-fit: cover;
    border-radius: 5px;
    margin-right: 10px;
}

.file-icon {
    margin-right: 10px;
    font-size: 20px;
    width: 32px; /* 保持与封面图同样的占位宽度 */
    text-align: center;
}

.own-share-tip {
    display: flex;
    align-items: center;
    padding: 10px 15px;
    background-color: #f0f9ff;
    border: 1px solid #0ea5e9;
    border-radius: 6px;
    color: #0369a1;
    font-size: 14px;
}

.own-share-tip.large {
    font-size: 16px;
    padding: 12px 20px;
}

.own-share-tip .el-icon {
    margin-right: 8px;
    font-size: 16px;
}

.own-share-tip.large .el-icon {
    font-size: 18px;
}
</style> 