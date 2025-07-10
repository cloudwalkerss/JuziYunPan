<template>
  <el-dialog
    :model-value="modelValue"
    title="登录后保存到网盘"
    width="400px"
    @update:model-value="close"
    :close-on-click-modal="false"
    custom-class="login-dialog"
  >
    <div class="login-content">
      <p class="login-tip">请登录您的账号以保存文件到网盘</p>
      
      <el-form :model="form" :rules="rules" ref="loginFormRef" label-position="top">
        <el-form-item prop="username" label="用户名">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        
        <el-form-item prop="password" label="密码">
          <el-input
            type="password"
            v-model="form.password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLoginSubmit"
          />
        </el-form-item>
        
        <el-form-item prop="captcha" label="验证码">
          <div class="captcha-container">
            <el-input
              v-model="form.captcha"
              placeholder="请输入验证码"
              prefix-icon="Key"
              size="large"
              @keyup.enter="handleLoginSubmit"
            />
            <div class="captcha-image" @click="refreshCaptcha" title="点击刷新验证码">
              <img :src="captchaUrl" alt="验证码" />
            </div>
          </div>
        </el-form-item>
        
        <div class="login-options">
          <el-checkbox v-model="remember" label="记住我" />
        </div>
      </el-form>
    </div>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="close" size="large">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleLoginSubmit" 
          :loading="loading"
          size="large"
        >
          登录
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue';
import { useAuth } from '@/composables/useAuth.js';

const props = defineProps({
  modelValue: Boolean,
});

const emit = defineEmits(['update:modelValue', 'login-success']);

// 使用登录逻辑
const {
  form,
  rules,
  loading,
  remember,
  captchaUrl,
  refreshCaptcha,
  resetForm,
  handleLogin
} = useAuth();

const loginFormRef = ref(null);

// 处理登录提交
const handleLoginSubmit = async () => {
  try {
    await handleLogin(loginFormRef.value, 
      () => {
        // 登录成功回调
        emit('login-success');
        close();
      }
    );
  } catch (error) {
    // 登录失败已在useAuth中处理
  }
};

// 关闭弹窗
const close = () => {
  emit('update:modelValue', false);
};

// 监听弹窗打开，初始化验证码
watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    resetForm();
  }
});

// 组件挂载时初始化验证码
onMounted(() => {
  refreshCaptcha();
});
</script>

<style scoped>
.login-content {
  padding: 10px 0;
}

.login-tip {
  text-align: center;
  color: #666;
  margin-bottom: 20px;
  font-size: 14px;
}

.captcha-container {
  display: flex;
  gap: 10px;
  align-items: center;
}

.captcha-image {
  width: 100px;
  height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:deep(.login-dialog) {
  .el-dialog__header {
    padding-bottom: 10px;
  }
  
  .el-dialog__body {
    padding-top: 10px;
  }
}
</style> 