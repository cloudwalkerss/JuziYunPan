<script setup>
import { ElButton, ElMessage, ElPopconfirm, ElDropdown, ElDropdownMenu, ElDropdownItem, ElInput, ElMenu, ElMenuItem, ElProgress, ElTable, ElTableColumn, ElPagination, ElDialog, ElLoading, ElUpload, ElIcon, ElTree } from 'element-plus';
import {logout, getHeader, get, post} from '@/net/index.js';
import { onMounted, ref, reactive, watch } from 'vue';
import router from '@/router/index.js';
import Avatar from '@/components/Avatar.vue';
import axios from 'axios';
import {userStore} from "@/store/index.js";
import { UploadFilled, Document, Folder, Delete, Share, Edit, Download, Close } from '@element-plus/icons-vue';
import PreviewImage from '@/components/PreviewImage.vue';
import PreviewAudio from '@/components/PreviewAudio.vue';
import DocxPreview from '@/components/DocxPreview.vue';
import PreviewPdf from '@/components/PreviewPdf.vue';
import PdfPreview from '../components/PdfPreview.vue';
import PptxPreview from '../components/PptxPreview.vue';
import VideoPreview from '../components/VideoPreview.vue';

// 用户信息
const userInfo = ref(null);
const store=userStore()
// 获取用户信息
function getUserInfo() {
  // axios.get('/user/getUserInfo', {
  //   headers: getHeader()
  // }).then(response => {
  //   if (response.data.code === 200) {
  //     userInfo.value = response.data.data;
  //   } else {
  //     ElMessage.error('获取用户信息失败');
  //   }
  // }).catch(error => {
  //   console.error('获取用户信息失败', error);
  //   ElMessage.error('获取用户信息失败');
  // });
  get('user/getUserInfo',(data)=>{
          store.user=data
  })
}

// 退出登录
function handleLogout() {
  logout(() =>
    router.push('/')
  , () =>
    ElMessage.error(`退出失败`)
  );
}

// 头像上传成功处理
function handleAvatarSuccess() {
  ElMessage.success('头像上传成功');
}

// 页面加载时获取用户信息
onMounted(() => {
  getUserInfo();
  loadFileList();
  getUserSpace(); // 获取用户空间使用情况
  createMD5Worker();
});

// 当前活动菜单
const activeMenu = ref('files');
// 当前路径
const currentPath = ref('/');
// 当前页码
const currentPage = ref(1);
// 页面大小
const pageSize = ref(10);
// 文件列表
const fileList = ref([]);
// 总文件数
const totalCount = ref(0);
// 文件分类
const fileCategory = ref('');
// 文件表格加载状态
const tableLoading = ref(false);
// 上传对话框可见性
const uploadDialogVisible = ref(false);
// 新建文件夹对话框可见性
const newFolderDialogVisible = ref(false);
// 重命名对话框可见性
const renameDialogVisible = ref(false);
// 新文件夹名称
const newFolderName = ref('');
// 重命名文件信息
const renameFileInfo = ref({
  fileId: '',
  folderType: 0,
  fileName: '',
  newFileName: '',
  fileSuffix: '',
  originalName: ''
});
// 搜索关键词
const searchKeyword = ref('');
// 选中的文件ID列表
const selectedFileIds = ref([]);
// 文件路径数组
const pathArray = ref([{ name: '全部文件', filePid: '0' }]);

// 上传相关状态
const uploadFileList = ref([]);
const uploadProgress = ref({});
const chunkSize = 2 * 1024 * 1024; // 每片2MB
const maxFileSize = 10 * 1024 * 1024 * 1024; // 最大文件大小：10GB

// 移动文件对话框可见性
const moveDialogVisible = ref(false);
// 文件夹树数据
const folderTreeData = ref([]);
// 选中的目标文件夹
const targetFolderId = ref('');
// 文件移动加载状态
const moveLoading = ref(false);

// 用户空间信息
const userSpace = ref({
  useSpace: 0,
  totalSpace: 0,
  percentage: 0
});

// 预览相关状态
const previewDialogVisible = ref(false);
const previewFileType = ref(''); // image, video, audio, doc, pdf, pptx
const previewImageUrl = ref('');
const previewAudioUrl = ref('');
const previewDocUrl = ref('');
const previewDocName = ref('');
const previewDocId = ref('');
const previewPdfUrl = ref('');
const previewPptxId = ref('');
const previewVideoFileId = ref('');
let previewImageObjectUrl = null;
let previewAudioObjectUrl = null;

// 加载文件列表
function loadFileList() {
  tableLoading.value = true;
  const params = {
    pageNo: currentPage.value,
    pageSize: pageSize.value,
    filePid: pathArray.value[pathArray.value.length - 1].filePid
  };
  
  if (searchKeyword.value) {
    params.fileName = searchKeyword.value;
  }
  
  if (fileCategory.value) {
    params.category = fileCategory.value;
  }
  
  get('file/loadFileList?' + new URLSearchParams(params), (data) => {
    fileList.value = data.list || [];
    totalCount.value = data.totalCount || 0;
    tableLoading.value = false;
  }, () => {
    tableLoading.value = false;
    ElMessage.error('获取文件列表失败');
  });
}

// 监听分页变化
watch(currentPage, () => {
  loadFileList();
});

// 监听文件分类变化
watch(fileCategory, () => {
  currentPage.value = 1;
  loadFileList();
});

// 进入文件夹
function enterFolder(row) {
  if (row.folderType === 1) {
    pathArray.value.push({
      name: row.fileName,
      filePid: row.fileId
    });
    currentPage.value = 1;
    loadFileList();
  }
}

// 面包屑导航点击
function handleBreadcrumbClick(index) {
  if (index === pathArray.value.length - 1) {
    return;
  }
  pathArray.value = pathArray.value.slice(0, index + 1);
  currentPage.value = 1;
  loadFileList();
}

// 新建文件夹
function createFolder() {
  if (!newFolderName.value) {
    ElMessage.warning('请输入文件夹名称');
    return;
  }
  
  post('file/newFolder', {
    filePid: pathArray.value[pathArray.value.length - 1].filePid,
    fileName: newFolderName.value
  }, () => {
    ElMessage.success('文件夹创建成功');
    newFolderDialogVisible.value = false;
    newFolderName.value = '';
    loadFileList();
  });
}

// 打开重命名对话框
function openRenameDialog(row) {
  // 判断是文件还是文件夹
  if (row.folderType === 1) {
    // 文件夹，直接使用文件夹名
    renameFileInfo.value = {
      fileId: row.fileId,
      folderType: row.folderType,
      fileName: row.fileName,
      newFileName: row.fileName,
      fileSuffix: '',
      originalName: row.fileName // 保存原始名称
    };
  } else {
    // 文件，需要分离文件名和后缀
    const lastDotIndex = row.fileName.lastIndexOf('.');
    
    if (lastDotIndex > 0) {
      // 有后缀的文件
      const suffix = row.fileName.substring(lastDotIndex);
      let nameWithoutSuffix = row.fileName.substring(0, lastDotIndex);
      
      // 检查是否有重复的后缀（例如 file.docx.docx）
      // 使用正则表达式查找文件名中是否有重复后缀
      const suffixEscaped = suffix.replace(/\./g, '\\.'); // 转义点号
      const duplicateSuffixRegex = new RegExp(suffixEscaped + '$', 'i');
      
      // 检查倒数第二个后缀
      const secondLastDotIndex = nameWithoutSuffix.lastIndexOf('.');
      if (secondLastDotIndex > 0) {
        const possibleDuplicateSuffix = nameWithoutSuffix.substring(secondLastDotIndex);
        if (duplicateSuffixRegex.test(possibleDuplicateSuffix)) {
          // 有重复后缀，移除它
          nameWithoutSuffix = nameWithoutSuffix.substring(0, secondLastDotIndex);
        }
      }
      
      renameFileInfo.value = {
        fileId: row.fileId,
        folderType: row.folderType,
        fileName: row.fileName,
        newFileName: nameWithoutSuffix,
        fileSuffix: suffix,
        originalName: row.fileName // 保存原始名称
      };
    } else {
      // 没有后缀的文件
      renameFileInfo.value = {
        fileId: row.fileId,
        folderType: row.folderType,
        fileName: row.fileName,
        newFileName: row.fileName,
        fileSuffix: '',
        originalName: row.fileName // 保存原始名称
      };
    }
  }
  
  renameDialogVisible.value = true;
}

// 重命名文件
function renameFile() {
  if (!renameFileInfo.value.newFileName) {
    ElMessage.warning('请输入新名称');
    return;
  }
  
  // 检查是否真的修改了文件名
  if (renameFileInfo.value.folderType === 0) {
    // 对于文件，计算不含后缀的原始文件名
    const lastDotIndex = renameFileInfo.value.originalName.lastIndexOf('.');
    const originalNameWithoutSuffix = lastDotIndex > 0 
      ? renameFileInfo.value.originalName.substring(0, lastDotIndex)
      : renameFileInfo.value.originalName;
      
    // 如果文件名没有变化，无需发送请求
    if (originalNameWithoutSuffix === renameFileInfo.value.newFileName) {
      ElMessage.info('文件名未修改');
      renameDialogVisible.value = false;
      return;
    }
  } else {
    // 对于文件夹，直接比较
    if (renameFileInfo.value.originalName === renameFileInfo.value.newFileName) {
      ElMessage.info('文件夹名未修改');
      renameDialogVisible.value = false;
      return;
    }
  }
  
  // 只发送不含后缀的新文件名
  post('file/rename', {
    fileId: renameFileInfo.value.fileId,
    fileName: renameFileInfo.value.newFileName // 只发送文件名部分，不带后缀
  }, () => {
    ElMessage.success('重命名成功');
    renameDialogVisible.value = false;
    loadFileList();
  }, () => {
    // 处理异常错误
    ElMessage.warning('重命名失败，请确保该文件夹下没有重名文件');
  });
}

// 删除文件
function deleteFiles() {
  if (selectedFileIds.value.length === 0) {
    ElMessage.warning('请选择要删除的文件');
    return;
  }
  
  post('file/delFile', {
    fileIds: selectedFileIds.value.join(',')
  }, () => {
    ElMessage.success('文件删除成功');
    refreshUserSpace(); // 刷新用户空间使用情况
    loadFileList();
  });
}

// 下载文件
function downloadFile(row) {
  // 只能下载文件，不能下载文件夹
  if (row.folderType === 1) {
    ElMessage.warning('不能下载文件夹');
    return;
  }
  
  get(`file/createDownloadUrl/${row.fileId}`, (data) => {
    window.open(`file/download/${data}`, '_blank');
  });
}

// 文件选择变化
function handleSelectionChange(selection) {
  selectedFileIds.value = selection.map(item => item.fileId);
}

// 搜索文件
function searchFiles() {
  currentPage.value = 1;
  loadFileList();
}

// 切换文件分类
function switchCategory(category) {
  fileCategory.value = category;
  activeMenu.value = category || 'files';
  // 回到根目录
  pathArray.value = [{ name: '全部文件', filePid: '0' }];
}

// 创建Web Worker脚本以计算MD5
function createMD5Worker() {
  const workerScript = `
    self.importScripts('https://cdnjs.cloudflare.com/ajax/libs/spark-md5/3.0.2/spark-md5.min.js');
    
    self.onmessage = function(e) {
      const fileArrayBuffer = e.data.fileArrayBuffer;
      const chunkSize = 2097152; // 2MB
      const chunks = Math.ceil(fileArrayBuffer.byteLength / chunkSize);
      let currentChunk = 0;
      const spark = new SparkMD5.ArrayBuffer();
      
      function processChunk() {
        const start = currentChunk * chunkSize;
        const end = Math.min(fileArrayBuffer.byteLength, start + chunkSize);
        const chunk = fileArrayBuffer.slice(start, end);
        
        spark.append(chunk);
        currentChunk++;
        
        self.postMessage({
          progress: Math.floor((currentChunk / chunks) * 100)
        });
        
        if (currentChunk < chunks) {
          // 使用setTimeout避免UI阻塞
          setTimeout(processChunk, 0);
        } else {
          const md5 = spark.end();
          self.postMessage({ md5 });
        }
      }
      
      processChunk();
    };
  `;
  
  const blob = new Blob([workerScript], { type: 'application/javascript' });
  const url = URL.createObjectURL(blob);
  
  // 使用动态创建的Worker
  window.md5WorkerUrl = url;
}

// 计算文件MD5
function calculateFileMD5(file) {
  return new Promise((resolve, reject) => {
    if (!file || !(file instanceof Blob)) {
      reject(new Error('无效的文件对象'));
      return;
    }
    
    const reader = new FileReader();
    
    reader.onload = function(e) {
      const arrayBuffer = e.target.result;
      const worker = new Worker(window.md5WorkerUrl);
      
      worker.onmessage = (e) => {
        if (e.data.md5) {
          resolve(e.data.md5);
          worker.terminate();
        } else if (e.data.error) {
          reject(new Error(e.data.error));
          worker.terminate();
        } else if (e.data.progress) {
          // 更新MD5计算进度
          const fileObj = uploadFileList.value.find(item => 
            item.file === file || item.name === file.name
          );
          if (fileObj) {
            fileObj.md5Progress = e.data.progress;
          }
        }
      };
      
      worker.onerror = (error) => {
        reject(error);
        worker.terminate();
      };
      
      worker.postMessage({ fileArrayBuffer: arrayBuffer });
    };
    
    reader.onerror = function() {
      reject(new Error('读取文件失败'));
    };
    
    reader.readAsArrayBuffer(file);
  });
}

// 文件上传处理
async function handleFileUpload(file) {
  // Element Plus 上传组件返回的是一个带有raw属性的对象，需要使用raw获取真实的File对象
  const rawFile = file.raw || file;
  
  // 文件对象
  const fileObj = {
    file: rawFile,
    name: rawFile.name,
    size: rawFile.size,
    uploadProgress: 0,
    md5Progress: 0,
    fileId: '',
    status: 'calculating' // calculating, uploading, success, error
  };
  
  uploadFileList.value.push(fileObj);
  
  try {
    // 计算文件MD5值
    fileObj.status = 'calculating';
    const fileMd5 = await calculateFileMD5(rawFile);
    
    // 开始分片上传
    fileObj.status = 'uploading';
    await uploadFileByChunks(rawFile, fileMd5, fileObj);
  } catch (error) {
    console.error('文件上传失败:', error);
    fileObj.status = 'error';
    ElMessage.error(`文件 ${rawFile.name} 上传失败: ${error.message}`);
  }
}

// 分片上传文件
async function uploadFileByChunks(file, fileMd5, fileObj) {
  try {
    // 总分片数
    const chunks = Math.ceil(file.size / chunkSize);
    fileObj.fileId = fileObj.fileId || StringTools.getRandomString(10);
    
    // 分片上传
    for (let i = 0; i < chunks; i++) {
      const start = i * chunkSize;
      const end = Math.min(file.size, start + chunkSize);
      const chunk = file.slice(start, end);
      
      const formData = new FormData();
      formData.append('file', new Blob([chunk], { type: file.type }));  // 使用Blob包装分片数据，确保类型正确
      formData.append('fileMd5', fileMd5);
      formData.append('fileName', file.name);
      formData.append('filePid', pathArray.value[pathArray.value.length - 1].filePid);
      formData.append('chunkIndex', i);
      formData.append('chunks', chunks);
      formData.append('fileId', fileObj.fileId);
      
      // 添加重试逻辑
      let retryCount = 0;
      const maxRetries = 3;
      let success = false;
      
      while (!success && retryCount < maxRetries) {
        try {
          // 发送请求
          const response = await axios.post('file/uploadFile', formData, {
            headers: {
              'Content-Type': 'multipart/form-data',
              ...getHeader()
            },
            onUploadProgress: (progressEvent) => {
              if (progressEvent.total > 0) { // 确保total有效
                // 计算当前分片的上传进度
                const chunkProgress = (progressEvent.loaded / progressEvent.total) * (1 / chunks) * 100;
                // 总进度 = 已上传完成的分片进度 + 当前分片进度
                fileObj.uploadProgress = Math.min(100, Math.floor((i / chunks) * 100 + chunkProgress));
              } else {
                // 如果total无效，使用简化的进度计算
                fileObj.uploadProgress = Math.min(100, Math.floor((i / chunks) * 100));
              }
            },
            // 增加超时时间，避免大文件上传超时
            timeout: 600000 // 10分钟
          });
          
          // 处理响应
          if (response.data.code === 200) {
            success = true;
            const result = response.data.data;
            
            // 如果是秒传
            if (result.status === 2) {
              fileObj.uploadProgress = 100;
              fileObj.status = 'success';
              ElMessage.success(`文件 ${file.name} 秒传成功`);
              refreshUserSpace(); // 刷新用户空间使用情况
              loadFileList(); // 刷新文件列表
              return;
            }
            
            // 如果是最后一个分片
            if (i === chunks - 1) {
              fileObj.uploadProgress = 100;
              fileObj.status = 'success';
              ElMessage.success(`文件 ${file.name} 上传成功`);
              refreshUserSpace(); // 刷新用户空间使用情况
              loadFileList(); // 刷新文件列表
            }
          } else {
            throw new Error(response.data.message || '上传失败');
          }
        } catch (error) {
          retryCount++;
          console.error(`分片 ${i+1}/${chunks} 上传失败 (尝试 ${retryCount}/${maxRetries}):`, error);
          
          if (retryCount >= maxRetries) {
            fileObj.status = 'error';
            
            // 提取错误信息
            let errorMsg = '网络错误';
            if (error.response && error.response.data && error.response.data.message) {
              errorMsg = error.response.data.message;
            } else if (error.message) {
              errorMsg = error.message;
            }
            
            ElMessage.error(`文件 ${file.name} 上传失败: ${errorMsg}`);
            return; // 终止上传
          }
          
          // 等待一段时间后重试
          await new Promise(resolve => setTimeout(resolve, 2000));
          ElMessage.warning(`正在重试第 ${i+1} 个分片 (${retryCount}/${maxRetries})...`);
        }
      }
    }
  } catch (error) {
    console.error('上传过程发生错误:', error);
    fileObj.status = 'error';
    
    // 提取错误信息
    let errorMsg = '未知错误';
    if (error.response && error.response.data && error.response.data.message) {
      errorMsg = error.response.data.message;
    } else if (error.message) {
      errorMsg = error.message;
    }
    
    ElMessage.error(`文件 ${file.name} 上传失败: ${errorMsg}`);
  }
}

// StringTools工具类
const StringTools = {
  getRandomString(length) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    for (let i = 0; i < length; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
  }
};

// 文件表格数据
const fileTableData = [
  {
    name: '我的文档',
    type: 'folder',
    icon: '📁',
    date: '2023-05-11 21:45:26',
    size: '-'
  },
  {
    name: '示例视频.mp4',
    type: 'video',
    icon: '🎬',
    date: '2023-05-11 21:43:59',
    size: '1.57GB'
  },
  {
    name: '安装程序.exe',
    type: 'exe',
    icon: '📦',
    date: '2023-05-11 21:43:34',
    size: '346MB'
  },
  {
    name: '音乐-轻音.mp3',
    type: 'audio',
    icon: '🎵',
    date: '2023-05-11 21:43:20',
    size: '8.32MB'
  },
  {
    name: '文档资料.pdf',
    type: 'pdf',
    icon: '📕',
    date: '2023-05-11 21:43:20',
    size: '3.48MB'
  },
  {
    name: '头像照片.jpg',
    type: 'image',
    icon: '🖼️',
    date: '2023-05-11 21:43:20',
    size: '0.46MB'
  },
  {
    name: '工作文档.docx',
    type: 'doc',
    icon: '📘',
    date: '2023-05-11 21:43:20',
    size: '0.47MB'
  }
];

// 分页配置
const pagination = ref({
  currentPage: 1,
  pageSize: 10,
  total: 12
});

// 视图类型
const viewType = ref('table');

// 格式化文件大小
function formatFileSize(size) {
  if (!size) return '0 B';
  
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let index = 0;
  let fileSize = parseFloat(size);
  
  while (fileSize >= 1024 && index < units.length - 1) {
    fileSize /= 1024;
    index++;
  }
  
  return fileSize.toFixed(2) + ' ' + units[index];
}

// 获取用户空间使用情况
function getUserSpace() {
  get('user/getUseSpace', (data) => {
    userSpace.value.useSpace = data.useSpace || 0;
    userSpace.value.totalSpace = data.totalSpace || 0;
    
    // 计算百分比
    if (userSpace.value.totalSpace > 0) {
      userSpace.value.percentage = parseFloat((userSpace.value.useSpace / userSpace.value.totalSpace * 100).toFixed(2));
    } else {
      userSpace.value.percentage = 0;
    }
  });
}

// 刷新用户空间使用情况
function refreshUserSpace() {
  get('user/refreshUserSpace', (data) => {
    userSpace.value.useSpace = data.useSpace || 0;
    userSpace.value.totalSpace = data.totalSpace || 0;
    
    // 计算百分比
    if (userSpace.value.totalSpace > 0) {
      userSpace.value.percentage = parseFloat((userSpace.value.useSpace / userSpace.value.totalSpace * 100).toFixed(2));
    } else {
      userSpace.value.percentage = 0;
    }
  });
}

// 加载回收站
function loadRecycleBin() {
  activeMenu.value = 'recycleBin';
  pathArray.value = [{ name: '回收站', filePid: '0' }];
  
  tableLoading.value = true;
  const params = {
    pageNo: currentPage.value,
    pageSize: pageSize.value,
    delFlag: 1 // 回收站标志
  };
  
  get('recycle/loadRecycleList?' + new URLSearchParams(params), (data) => {
    fileList.value = data.list;
    totalCount.value = data.totalCount;
    tableLoading.value = false;
  }, () => {
    tableLoading.value = false;
    ElMessage.error('获取回收站文件列表失败');
  });
}

// 恢复文件
function recoverFiles() {
  if (selectedFileIds.value.length === 0) {
    ElMessage.warning('请选择要恢复的文件');
    return;
  }
  
  post('recycle/recoverFile', {
    fileIds: selectedFileIds.value.join(',')
  }, () => {
    ElMessage.success('文件恢复成功');
    refreshUserSpace(); // 刷新用户空间使用情况
    loadRecycleBin();
  });
}

// 彻底删除文件
function deleteFilesCompletely() {
  if (selectedFileIds.value.length === 0) {
    ElMessage.warning('请选择要删除的文件');
    return;
  }
  
  post('recycle/delFile', {
    fileIds: selectedFileIds.value.join(',')
  }, () => {
    ElMessage.success('文件已彻底删除');
    refreshUserSpace(); // 刷新用户空间使用情况
    loadRecycleBin();
  });
}

// 恢复单个文件
function recoverSingleFile(file) {
  post('recycle/recoverFile', {
    fileIds: file.fileId
  }, () => {
    ElMessage.success('文件恢复成功');
    refreshUserSpace(); // 刷新用户空间使用情况
    loadRecycleBin();
  });
}

// 彻底删除单个文件
function deleteSingleFileCompletely(file) {
  post('recycle/delFile', {
    fileIds: file.fileId
  }, () => {
    ElMessage.success('文件已彻底删除');
    refreshUserSpace(); // 刷新用户空间使用情况
    loadRecycleBin();
  });
}

// 加载所有文件夹
function loadAllFolders(filePid, excludeIds) {
  return new Promise((resolve, reject) => {
    const params = {
      filePId: filePid || '0',
      excludeFileIds: typeof excludeIds === 'object' && Array.isArray(excludeIds) ? excludeIds.join(',') : excludeIds || ''
    };
    
    post('file/loadAllFolder', params, (data) => {
      resolve(data);
    }, () => {
      reject();
      ElMessage.error('获取文件夹列表失败');
    });
  });
}

// 构建文件夹树
async function buildFolderTree(filePid, excludeFileIds) {
  return new Promise((resolve, reject) => {
    post('file/loadAllFolder', {
      filePId: filePid,
      excludeFileIds: excludeFileIds // 传递要排除的文件ID，后端需要支持此参数
    }, (data) => {
      const treeNodes = [];
      
      for (const folder of data) {
        // 跳过被排除的文件夹
        if (excludeFileIds && typeof excludeFileIds === 'string' && excludeFileIds.split(',').includes(folder.fileId)) {
          continue;
        }
        
        const node = {
          id: folder.fileId,
          label: folder.fileName,
          isLeaf: false
        };
        
        treeNodes.push(node);
      }
      
      resolve(treeNodes);
    }, () => {
      reject();
      ElMessage.error('获取文件夹列表失败');
    });
  });
}

// 加载文件夹的子节点
async function loadFolderChildren(node, resolve) {
  try {
    if (node.level === 0) {
      // 根节点
      const children = await buildFolderTree('0', selectedFileIds.value.length > 0 ? selectedFileIds.value.join(',') : '');
      resolve(children);
    } else {
      // 子节点
      const children = await buildFolderTree(node.data.id, selectedFileIds.value.length > 0 ? selectedFileIds.value.join(',') : '');
      resolve(children);
    }
  } catch (error) {
    resolve([]);
    console.error('加载子文件夹失败:', error);
  }
}

// 打开移动文件对话框 - 批量
async function openMoveDialog() {
  if (selectedFileIds.value.length === 0) {
    ElMessage.warning('请选择要移动的文件');
    return;
  }
  
  moveDialogVisible.value = true;
  targetFolderId.value = '';
  
  // 重置文件夹树数据
  folderTreeData.value = [];
}

// 打开移动文件对话框 - 单文件版本
async function openMoveDialogForSingleFile(file) {
  moveDialogVisible.value = true;
  targetFolderId.value = '';
  selectedFileIds.value = [file.fileId];
  
  // 重置文件夹树数据
  folderTreeData.value = [];
}

// 删除单个文件
function deleteFile(file) {
  if (!file || !file.fileId) {
    ElMessage.warning('无效的文件');
    return;
  }
  
  post('file/delFile', {
    fileIds: file.fileId
  }, () => {
    ElMessage.success('文件删除成功');
    refreshUserSpace(); // 刷新用户空间使用情况
    loadFileList();
  });
}

// 移动文件夹节点被点击
function handleFolderNodeClick(data) {
  targetFolderId.value = data.id;
}

// 移动文件
function moveFiles() {
  if (!targetFolderId.value) {
    ElMessage.warning('请选择目标文件夹');
    return;
  }
  
  if (selectedFileIds.value.length === 0) {
    ElMessage.warning('请选择要移动的文件');
    return;
  }
  
  // 检查是否移动到自身
  if (selectedFileIds.value.includes(targetFolderId.value)) {
    ElMessage.warning('不能将文件夹移动到自身内部');
    return;
  }
  
  // 检查是否移动到当前文件夹
  const currentFolderId = pathArray.value[pathArray.value.length - 1].filePid;
  if (targetFolderId.value === currentFolderId) {
    ElMessage.warning('文件已经在当前文件夹中');
    return;
  }
  
  moveLoading.value = true;
  
  post('file/changeFileFolder', {
    fileIds: selectedFileIds.value.join(','),
    filePid: targetFolderId.value
  }, () => {
    ElMessage.success('文件移动成功');
    moveDialogVisible.value = false;
    
    // 重置选中状态
    selectedFileIds.value = [];
    
    // 刷新文件列表
    loadFileList();
    moveLoading.value = false;
  }, (error) => {
    moveLoading.value = false;
    if (error && error.code === 600) {
      ElMessage.error(error.msg || '移动失败，目标文件夹不存在');
    } else if (error && error.code === 904) {
      ElMessage.error('移动失败，存储空间不足');
    } else {
      ElMessage.error(error?.msg || '移动失败，请确保目标文件夹没有同名文件');
    }
  });
}

// 文件预览处理（根据类型分发）
async function handleFilePreview(row) {
  const fileName = row.fileName || '';
  const fileType = row.fileType;
  const fileCategory = row.fileCategory;
  
  // 关闭前释放旧的objectURL
  if (previewImageObjectUrl) { URL.revokeObjectURL(previewImageObjectUrl); previewImageObjectUrl = null; }
  if (previewAudioObjectUrl) { URL.revokeObjectURL(previewAudioObjectUrl); previewAudioObjectUrl = null; }

  // 视频
  if (fileCategory === 1 || fileType === 1 || /\.(mp4|webm|ogg|mkv|mov|avi)$/i.test(fileName)) {
    previewFileType.value = 'video';
    previewVideoFileId.value = row.fileId;
    previewDialogVisible.value = true;
    return;
  }
  // 图片
  if (fileCategory === 3 || fileType === 3 || /\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(fileName)) {
    try {
      const response = await axios.get(`/file/getFile/${row.fileId}`, { responseType: 'blob', headers: getHeader() });
      previewImageObjectUrl = URL.createObjectURL(response.data);
      previewImageUrl.value = previewImageObjectUrl;
      previewFileType.value = 'image';
      previewDialogVisible.value = true;
    } catch (e) {
      previewImageUrl.value = '';
      previewFileType.value = 'image';
      previewDialogVisible.value = true;
    }
    return;
  }
  // 音频
  if (fileCategory === 2 || fileType === 2 || /\.(mp3|wav|aac|flac|ogg)$/i.test(fileName)) {
    try {
      const response = await axios.get(`/file/getFile/${row.fileId}`, { responseType: 'blob', headers: getHeader() });
      previewAudioObjectUrl = URL.createObjectURL(response.data);
      previewAudioUrl.value = previewAudioObjectUrl;
      previewFileType.value = 'audio';
      previewDialogVisible.value = true;
    } catch (e) {
      previewAudioUrl.value = '';
      previewFileType.value = 'audio';
      previewDialogVisible.value = true;
    }
    return;
  }
  // PDF文件
  if (/\.pdf$/i.test(fileName)) {
    previewPdfUrl.value = `/file/getFile/${row.fileId}`;
    previewFileType.value = 'pdf';
    previewDialogVisible.value = true;
    return;
  }
  // doc/docx
  if (/\.(doc|docx)$/i.test(fileName)) {
    previewDocId.value = row.fileId;
    previewFileType.value = 'docx';
    previewDialogVisible.value = true;
    return;
  }
  // ppt/pptx
  if (/\.(ppt|pptx)$/i.test(fileName)) {
    previewPptxId.value = row.fileId;
    previewFileType.value = 'pptx';
    previewDialogVisible.value = true;
    return;
  }
  // 其它类型
  previewFileType.value = '';
  previewDialogVisible.value = false;
}

// 关闭弹窗时释放objectURL
watch(previewDialogVisible, (val) => {
  if (!val) {
    if (previewImageObjectUrl) { URL.revokeObjectURL(previewImageObjectUrl); previewImageObjectUrl = null; previewImageUrl.value = ''; }
    if (previewAudioObjectUrl) { URL.revokeObjectURL(previewAudioObjectUrl); previewAudioObjectUrl = null; previewAudioUrl.value = ''; }
    previewDocUrl.value = '';
    previewDocName.value = '';
    previewDocId.value = '';
    previewPdfUrl.value = '';
    previewPptxId.value = '';
  }
});
</script>

<template>
  <div class="app-container">
    <!-- 顶部导航栏 -->
    <el-header class="app-header">
      <div class="logo">
        <div class="logo-icon">
          <span class="icon-text">橘</span>
        </div>
        <span class="logo-text">橘子网盘</span>
      </div>
      
      <div class="search-bar">
        <el-input 
          placeholder="输入文件名称搜索" 
          prefix-icon="Search" 
          clearable
        >
            <el-icon><Search /></el-icon>
        </el-input>
      </div>
      
      <div class="user-actions">
        <div class="user-info">
          <span class="username">{{ store.user.nickname }}</span>
          <el-dropdown trigger="click">
            <div class="avatar-container">
              <Avatar 
                :userId="store.user.id"
                :name="store.user.nickname"
                :size="40" 
                @upload-success="handleAvatarSuccess"
              />
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人设置</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <span class="logout-text">退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>
    
    <!-- 主体内容区 -->
    <div class="main-content">
      <!-- 左侧导航 -->
      <el-aside width="200px" class="side-nav">
        <el-menu
          :default-active="activeMenu"
          class="nav-menu"
          :collapse="false"
        >
          <el-menu-item index="files" @click="switchCategory('')">
            <el-icon><i class="nav-icon">📁</i></el-icon>
            <span>全部文件</span>
          </el-menu-item>
          <el-menu-item index="videos" @click="switchCategory('video')">
            <el-icon><i class="nav-icon">🎬</i></el-icon>
            <span>视频</span>
          </el-menu-item>
          <el-menu-item index="images" @click="switchCategory('image')">
            <el-icon><i class="nav-icon">🖼️</i></el-icon>
            <span>图片</span>
          </el-menu-item>
          <el-menu-item index="documents" @click="switchCategory('doc')">
            <el-icon><i class="nav-icon">📄</i></el-icon>
            <span>文档</span>
          </el-menu-item>
          <el-menu-item index="music" @click="switchCategory('audio')">
            <el-icon><i class="nav-icon">🎵</i></el-icon>
            <span>音乐</span>
          </el-menu-item>
          <el-menu-item index="others" @click="switchCategory('other')">
            <el-icon><i class="nav-icon">📦</i></el-icon>
            <span>其他</span>
          </el-menu-item>
          <el-menu-item index="recycleBin" @click="loadRecycleBin">
            <el-icon><i class="nav-icon">🗑️</i></el-icon>
            <span>回收站</span>
          </el-menu-item>
        </el-menu>
        
        <div class="storage-info">
          <div class="storage-text">
            <span>存储空间</span>
            <span>{{ formatFileSize(userSpace.useSpace) }} / {{ formatFileSize(userSpace.totalSpace) }}</span>
          </div>
          <el-progress 
            :percentage="userSpace.percentage" 
            :show-text="false" 
            :stroke-width="6" 
            :color="userSpace.percentage > 90 ? '#F56C6C' : (userSpace.percentage > 70 ? '#E6A23C' : '#409EFF')"
          />
        </div>
      </el-aside>
      
      <!-- 右侧文件区 -->
      <el-main class="file-area">
        <!-- 功能按钮区 -->
        <div class="action-bar">
          <div class="left-actions">
            <template v-if="activeMenu !== 'recycleBin'">
              <el-button type="primary" plain @click="uploadDialogVisible = true">
                <el-icon><upload-filled /></el-icon>
                上传
              </el-button>
              <el-button type="success" plain @click="newFolderDialogVisible = true">
                <el-icon><folder /></el-icon>
                新建文件夹
              </el-button>
              <el-button type="warning" plain :disabled="selectedFileIds.length === 0" @click="openMoveDialog">
                <el-icon><document /></el-icon>
                移动
              </el-button>
              <el-button type="danger" plain :disabled="selectedFileIds.length === 0" @click="deleteFiles">
                <el-icon><delete /></el-icon>
                批量删除
            </el-button>
          </template>
            <template v-else>
              <el-button type="success" plain :disabled="selectedFileIds.length === 0" @click="recoverFiles">
                <el-icon><folder /></el-icon>
                恢复文件
              </el-button>
              <el-button type="danger" plain :disabled="selectedFileIds.length === 0" @click="deleteFilesCompletely">
                <el-icon><delete /></el-icon>
                彻底删除
              </el-button>
            </template>
          </div>
          <div class="search-actions">
            <el-input 
              v-model="searchKeyword" 
              placeholder="搜索文件名" 
              clearable
              @keyup.enter="searchFiles"
            >
              <template #append>
                <el-button @click="searchFiles">搜索</el-button>
              </template>
            </el-input>
      </div>
    </div>
    
        <!-- 文件路径导航 -->
        <el-breadcrumb separator=">" class="path-nav">
          <el-breadcrumb-item 
            v-for="(path, index) in pathArray" 
            :key="index" 
            @click="handleBreadcrumbClick(index)"
          >
            {{ path.name }}
          </el-breadcrumb-item>
        </el-breadcrumb>
        
        <!-- 文件列表 -->
        <el-table
          v-loading="tableLoading"
          :data="fileList"
          style="width: 100%"
          row-key="fileId"
          border
          stripe
          highlight-current-row
          @selection-change="handleSelectionChange"
          @row-dblclick="row => enterFolder(row)"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column label="文件名" min-width="280">
            <template #default="scope">
              <div class="file-name-cell">
                <el-icon class="file-icon" v-if="scope.row.folderType === 1"><Folder /></el-icon>
                <el-icon class="file-icon" v-else><Document /></el-icon>
                <span
                  @click="handleFilePreview(scope.row)"
                  :style="(scope.row.fileCategory === 3 || scope.row.fileType === 3 || (scope.row.fileName && /\\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(scope.row.fileName))) ? 'color:#409EFF;cursor:pointer;text-decoration:underline;' : ''"
                >
                  {{ scope.row.fileName }}
                </span>
    </div>
            </template>
          </el-table-column>
          <el-table-column prop="fileSize" label="大小" width="120" align="right">
            <template #default="scope">
              {{ scope.row.folderType === 1 ? '-' : formatFileSize(scope.row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="修改时间" min-width="180" />
          <el-table-column label="操作" width="220">
            <template #default="scope">
              <div class="file-actions">
                <el-dropdown>
                  <el-button type="primary" size="small">
                    操作<i class="el-icon-arrow-down el-icon--right"></i>
            </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="downloadFile(scope.row)" v-if="scope.row.folderType === 0">
                        <el-icon><download /></el-icon>下载
                      </el-dropdown-item>
                      <el-dropdown-item @click="openRenameDialog(scope.row)">
                        <el-icon><edit /></el-icon>重命名
                      </el-dropdown-item>
                      <el-dropdown-item @click="openMoveDialogForSingleFile(scope.row)">
                        <el-icon><document /></el-icon>移动
                      </el-dropdown-item>
                      <el-dropdown-item @click="deleteFile(scope.row)">
                        <el-icon><delete /></el-icon>删除
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :page-sizes="[10, 20, 30, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="totalCount"
            background
          />
        </div>
        
        <!-- 新建文件夹对话框 -->
        <el-dialog v-model="newFolderDialogVisible" title="新建文件夹" width="400px">
          <el-input v-model="newFolderName" placeholder="请输入文件夹名称" />
          <template #footer>
            <span class="dialog-footer">
              <el-button @click="newFolderDialogVisible = false">取消</el-button>
              <el-button type="primary" @click="createFolder">确定</el-button>
            </span>
          </template>
        </el-dialog>
        
        <!-- 重命名对话框 -->
        <el-dialog v-model="renameDialogVisible" title="重命名" width="400px">
          <div v-if="renameFileInfo.folderType === 0" class="rename-info">
            <span>您正在重命名文件，只能修改文件名部分，文件后缀将保持不变</span>
          </div>
          <div class="rename-input-container">
            <el-input v-model="renameFileInfo.newFileName" placeholder="请输入新名称" />
            <span v-if="renameFileInfo.folderType === 0 && renameFileInfo.fileSuffix" class="file-suffix">
              {{ renameFileInfo.fileSuffix }}
            </span>
          </div>
          <template #footer>
            <span class="dialog-footer">
              <el-button @click="renameDialogVisible = false">取消</el-button>
              <el-button type="primary" @click="renameFile">确定</el-button>
            </span>
          </template>
        </el-dialog>
        
        <!-- 上传文件对话框 -->
        <el-dialog v-model="uploadDialogVisible" title="上传文件" width="600px" :close-on-click-modal="false">
          <el-upload
            class="upload-area"
            drag
            multiple
            action="#"
            :auto-upload="false"
            :on-change="handleFileUpload"
            :http-request="() => {}"
            :before-upload="(file) => file.size <= maxFileSize"
            :on-exceed="() => ElMessage.warning('文件数量超出限制')"
            :show-file-list="false"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处 或 <em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">
                支持任意类型文件上传，单个文件不超过10GB
              </div>
            </template>
          </el-upload>
          
          <!-- 上传文件列表和进度 -->
          <div v-if="uploadFileList.length > 0" class="upload-list">
            <div v-for="(item, index) in uploadFileList" :key="index" class="upload-item">
              <div class="upload-item-info">
                <div class="upload-item-name">{{ item.name }}</div>
                <div class="upload-item-size">{{ formatFileSize(item.size) }}</div>
              </div>
              <div class="upload-item-progress">
                <el-progress 
                  :percentage="item.uploadProgress" 
                  :status="item.status === 'error' ? 'exception' : item.status === 'success' ? 'success' : ''"
                />
                <div class="upload-item-status">
                  <span v-if="item.status === 'calculating'">计算MD5... {{ item.md5Progress || 0 }}%</span>
                  <span v-else-if="item.status === 'uploading'">上传中 {{ item.uploadProgress }}%</span>
                  <span v-else-if="item.status === 'success'" class="success-text">上传成功</span>
                  <span v-else-if="item.status === 'error'" class="error-text">上传失败</span>
                </div>
              </div>
            </div>
          </div>
        </el-dialog>
        
        <!-- 移动文件对话框 -->
        <el-dialog
          v-model="moveDialogVisible"
          title="移动到"
          width="400px"
          :close-on-click-modal="false"
          :close-on-press-escape="!moveLoading"
          :show-close="!moveLoading"
        >
          <div class="folder-tree-container">
            <el-tree
              :data="folderTreeData"
              node-key="id"
              :props="{ label: 'label', children: 'children', isLeaf: 'isLeaf' }"
              lazy
              :load="loadFolderChildren"
              @node-click="handleFolderNodeClick"
              highlight-current
              :default-expanded-keys="['0']"
              v-loading="moveLoading"
            >
              <template #default="{ node }">
                <div class="folder-node" :class="{ 'selected-folder': targetFolderId === node.key }">
                  <el-icon><Folder /></el-icon>
                  <span>{{ node.label }}</span>
                </div>
              </template>
            </el-tree>
            
            <div 
              class="root-folder" 
              @click="targetFolderId = '0'" 
              :class="{ 'selected-folder': targetFolderId === '0' }"
            >
              <el-icon><Folder /></el-icon>
              <span>全部文件</span>
            </div>
          </div>
    
          <template #footer>
            <div class="dialog-footer">
              <el-button @click="moveDialogVisible = false" :disabled="moveLoading">取消</el-button>
              <el-button 
                type="primary" 
                @click="moveFiles" 
                :loading="moveLoading"
                :disabled="!targetFolderId"
              >
                确定
              </el-button>
            </div>
          </template>
        </el-dialog>
        
        <!-- 文件预览对话框 -->
        <el-dialog
          v-model="previewDialogVisible"
          :title="'预览 - ' + currentFile?.fileName"
          width="80%"
          :destroy-on-close="true"
          class="preview-dialog"
        >
          <template v-if="previewFileType === 'video'">
            <div class="video-preview-container">
              <VideoPreview :fileId="previewVideoFileId" />
            </div>
          </template>
          <template v-if="previewFileType === 'image'">
            <div class="image-preview-container">
            <el-image :src="previewImageUrl" fit="contain" />
          </div>
          </template>
          <template v-if="previewFileType === 'audio'">
            <div class="audio-preview-container">
            <audio :src="previewAudioUrl" controls style="width: 100%;">
              您的浏览器不支持音频播放
            </audio>
          </div>
          </template>
          <template v-if="previewFileType === 'pdf'">
            <div class="pdf-preview-container">
            <PdfPreview :fileUrl="previewPdfUrl" />
          </div>
          </template>
          <template v-if="previewFileType === 'pptx'">
            <div class="pptx-preview-container">
            <PptxPreview :fileId="previewPptxId" />
          </div>
          </template>
          <template v-if="previewFileType === 'docx'">
            <div class="docx-preview-container">
            <DocxPreview :fileId="previewDocId" />
          </div>
          </template>
        </el-dialog>
      </el-main>
    </div>
  </div>
</template>

<style scoped>
/* 全局样式 */
.app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f5f6fa;
  overflow: hidden;
}

/* 头部样式 */
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
  background-color: #ffffff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
  z-index: 100;
}

.logo {
  display: flex;
  align-items: center;
}

.logo-icon {
  width: 40px;
  height: 40px;
  background-color: #ff9500;
  border-radius: 8px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-right: 12px;
}

.icon-text {
  color: white;
  font-weight: bold;
  font-size: 22px;
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
  color: #1f2f3d;
}

.search-bar {
  flex: 1;
  max-width: 500px;
  margin: 0 20px;
}

.user-actions {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
}

.username {
  margin-right: 12px;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.avatar-container {
  cursor: pointer;
}

.logout-text {
  color: #f56c6c;
}

/* 主体内容区样式 */
.main-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* 侧边导航样式 */
.side-nav {
  background-color: #ffffff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  padding: 20px 0;
  justify-content: space-between;
}

.nav-menu {
  border-right: none;
}

.nav-icon {
  margin-right: 8px;
  font-size: 18px;
}

.storage-info {
  padding: 15px;
  border-top: 1px solid #e8e8e8;
  margin-top: auto;
}

.storage-text {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

/* 文件区域样式 */
.file-area {
  background-color: #f5f6fa;
  padding: 20px;
  overflow-y: auto;
}

/* 功能按钮区 */
.action-bar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 15px;
}

.left-actions {
  display: flex;
  gap: 10px;
}

.search-actions {
  width: 280px;
}

.btn-icon, .view-icon {
  margin-right: 4px;
}

.upload-area {
  width: 100%;
}

/* 路径导航 */
.path-nav {
  margin-bottom: 15px;
}

/* 文件名单元格样式 */
.file-name-cell {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.file-icon {
  margin-right: 10px;
  font-size: 20px;
  color: #409EFF;
}

.file-actions {
  display: flex;
  gap: 8px;
}

.el-breadcrumb__item {
  cursor: pointer;
}

/* 分页容器 */
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.upload-list {
  margin-top: 15px;
  max-height: 300px;
  overflow-y: auto;
}

.upload-item {
  padding: 10px;
  border-bottom: 1px solid #eee;
}

.upload-item-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
}

.upload-item-name {
  font-size: 14px;
  color: #333;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.upload-item-size {
  font-size: 12px;
  color: #909399;
  margin-left: 10px;
}

.upload-item-status {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.success-text {
  color: #67C23A;
}

.error-text {
  color: #F56C6C;
}

/* 重命名对话框样式 */
.rename-info {
  margin-bottom: 10px;
  font-size: 12px;
  color: #909399;
}

.rename-input-container {
  display: flex;
  align-items: center;
}

.file-suffix {
  margin-left: 5px;
  font-size: 14px;
  color: #606266;
  line-height: 32px;
  white-space: nowrap;
}

/* 文件夹树样式 */
.folder-tree-container {
  max-height: 350px;
  overflow-y: auto;
  padding: 10px 0;
}

.folder-node {
  display: flex;
  align-items: center;
  padding: 5px 0;
  cursor: pointer;
}

.folder-node .el-icon {
  margin-right: 8px;
  color: #909399;
}

.root-folder {
  display: flex;
  align-items: center;
  padding: 10px;
  margin-top: 10px;
  border-top: 1px solid #ebeef5;
  cursor: pointer;
}

.root-folder .el-icon {
  margin-right: 8px;
  color: #909399;
}

.selected-folder {
  color: #409EFF;
  font-weight: bold;
}

.selected-folder .el-icon {
  color: #409EFF;
}

.preview-dialog :deep(.el-dialog__body) {
  padding: 0;
  height: 80vh;
  overflow: hidden;
}

.image-preview-container {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
}

.audio-preview-container {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
}

.pdf-preview-container {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
}

.pptx-preview-container {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
}

.docx-preview-container {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
}

.video-preview-container {
  width: 100%;
  height: 600px;
  background-color: #000;
}
</style>