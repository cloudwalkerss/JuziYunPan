<template>
  <div class="avatar-container">
    <div 
      class="avatar" 
      :style="{ 
        backgroundImage: avatarUrl ? `url(${avatarUrl})` : 'none',
        backgroundColor: !avatarUrl ? backgroundColor : 'transparent'
      }"
      @click="triggerFileInput"
    >
      <span v-if="!avatarUrl" class="avatar-text">{{ avatarText }}</span>
    </div>
    <input 
      ref="fileInput"
      type="file" 
      accept="image/*" 
      class="file-input" 
      @change="handleFileChange" 
    />
  </div>
</template>

<script setup>
import { getHeader } from '@/net/index.js';
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import {get} from "@/net/index.js";
import {ElMessage} from "element-plus";

const props = defineProps({
  userId: {
    type: Number,
    required: true
  },
  name: {
    type: String,
    default: ''
  },
  size: {
    type: Number,
    default: 60
  },
  backgroundColor: {
    type: String,
    default: '#1890ff'
  },
  editable: {
    type: Boolean,
    default: true
  }
});

const emit = defineEmits(['upload-success', 'upload-error']);

const fileInput = ref(null);
const avatarUrl = ref('');
const loading = ref(false);

// 生成头像文本（用户名首字母）
const avatarText = computed(() => {
  if (!props.name) return '';
  return props.name.charAt(0).toUpperCase();
});

// 在组件挂载时获取头像URL
onMounted(() => {
  loadAvatar();
});

// 加载头像
function loadAvatar() {
  // 使用带时间戳的URL避免缓存问题
  avatarUrl.value = `${axios.defaults.baseURL}/user/getAvatar/${props.userId}?t=${new Date().getTime()}`;
  // get(`user/getAvatar/${props.userId}`,()=>{
  //
  // },()=>{
  //   ElMessage.error('获取头像失败')
  // })

}

// 触发文件选择
function triggerFileInput() {
  if (props.editable) {
    fileInput.value.click();
  }
}

// 处理文件选择
async function handleFileChange(event) {
  const file = event.target.files[0];
  if (!file) return;
  
  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    alert('请选择图片文件');
    return;
  }
  
  try {
    loading.value = true;
    
    // 创建FormData对象
    const formData = new FormData();
    formData.append('avatar', file);
    
    // 发送上传请求
    const response = await axios.post('/user/updateUserAvatar', formData, {
      headers: {
        ...getHeader(), // 添加认证头
        'Content-Type': 'multipart/form-data'
      }
    });
    
    // 上传成功，重新加载头像
    if (response.data.code === 200) {
      loadAvatar();
      emit('upload-success', response.data);
    } else {
      emit('upload-error', response.data);
      console.error('头像上传失败:', response.data.message);
    }
  } catch (error) {
    console.error('头像上传出错:', error);
    emit('upload-error', error);
  } finally {
    loading.value = false;
    // 清空输入，允许重复选择同一文件
    event.target.value = '';
  }
}
</script>

<style scoped>
.avatar-container {
  display: inline-block;
  position: relative;
}

.avatar {
  width: v-bind('`${props.size}px`');
  height: v-bind('`${props.size}px`');
  border-radius: 50%;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: v-bind('props.editable ? "pointer" : "default"');
  transition: all 0.3s;
  overflow: hidden;
}

.avatar:hover {
  opacity: v-bind('props.editable ? 0.8 : 1');
}

.avatar-text {
  color: white;
  font-size: v-bind('`${Math.floor(props.size/2)}px`');
  font-weight: bold;
}

.file-input {
  display: none;
}
</style> 