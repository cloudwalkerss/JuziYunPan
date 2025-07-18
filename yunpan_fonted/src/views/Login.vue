<script setup>
import {ref, onMounted, reactive} from 'vue';
import {get, login} from "@/net/index.js";
import {ElMessage, ElForm, ElFormItem, ElInput, ElButton, ElCheckbox} from "element-plus";
import router from "@/router/index.js";
import axios from "axios";
import {userStore} from "@/store/index.js";

const baseurl = axios.defaults.baseURL;
const captchaInput = ref('');
// 验证码ID
const captchaId = ref('');
// 验证码图片URL
const captchaUrl = ref('');
// 登录表单引用
const loginFormRef = ref(null);
// 是否正在登录
const loading = ref(false);
const store = userStore();

//表单数据
const form = reactive({
  username: '',
  password: '',
  captcha: '',
});

// 表单验证规则
const rules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 6, max: 20, message: '用户名长度应在6到20个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 20, message: '密码长度应在8到20个字符之间', trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9]{4,6}$/, message: '验证码格式不正确', trigger: 'blur' }
  ]
});

// 记住我选项
const rememberMe = ref(false);

// 刷新验证码
const refreshCaptcha = () => {
  // 生成一个时间戳，防止缓存
  const timestamp = new Date().getTime();
  captchaUrl.value = `${baseurl}/captcha/generate?t=${timestamp}`;

  // 使用axios获取验证码ID，确保请求头可以正确获取
  axios.get(captchaUrl.value, {
    responseType: 'blob'  // 接收二进制数据
  })
  .then(response => {
    // 从响应头中获取验证码ID
    captchaId.value = response.headers['captcha-id'];
    
    // 将blob转为图片URL
    const blob = new Blob([response.data], { type: 'image/jpeg' });
    captchaUrl.value = URL.createObjectURL(blob);
  })
  .catch(error => {
    ElMessage.error('获取验证码失败，请刷新页面重试');
  });
};
// 获取用户信息
function getUserInfo() {
  get('user/getUserInfo',(data)=>{
    store.user=data
  })
}
//登录请求
function loginIndex() {
  if (!loginFormRef.value) return;
  
  loginFormRef.value.validate((valid) => {
    if (valid) {
      loading.value = true;
      login(form.username, form.password, rememberMe.value, form.captcha, captchaId.value, 
        () => {
          getUserInfo()
          loading.value = false;
          router.push('/index');
        }, 
        (message) => {
          loading.value = false;
          ElMessage.error('登录失败: ' + message);
          // 登录失败时刷新验证码
          refreshCaptcha();
        }
      );
    } else {
      return false;
    }
  });
}

// 注册页面导航
function goToRegister() {
  router.push('/register');
}

// 重置密码导航 
function goToResetPassword() {
  router.push('/resetPassword');
}

// 页面加载时刷新验证码
onMounted(() => {
  refreshCaptcha();
});
</script>

<template>
  <div class="login-container">
    <!-- 左侧装饰区域 -->
    <div class="left-decoration">
      <div class="decoration-circles">
        <div class="circle circle-1"></div>
        <div class="circle circle-2"></div>
        <div class="circle circle-3"></div>
      </div>
      <div class="decoration-text">
        <h3>橘子网盘</h3>
        <p>安全存储，随时随地访问您的文件</p>
      </div>
    </div>
    
    <!-- 右侧登录区域 -->
    <div class="content-wrapper">
      <!-- Logo -->
      <div class="logo">
        <div class="logo-icon">
          <span class="icon-text">橘</span>
        </div>
        <span class="logo-text">橘子网盘</span>
      </div>
      
      <!-- 标题 -->
      <div class="title">
        <h2>用户登录</h2>
        <p class="subtitle">登录您的账号，开始使用橘子云盘</p>
      </div>
      
      <div class="card">
        <div class="form-container">
          <el-form :model="form" :rules="rules" ref="loginFormRef" label-position="left" label-width="70px">
            <el-form-item prop="username" label="用户名">
              <div class="input-wrap">
                <span class="input-icon">👤</span>
                <el-input
                  id="username"
                  v-model="form.username"
                  placeholder="请输入用户名"
                  class="input-field"
                />
              </div>
            </el-form-item>
            
            <el-form-item prop="password" label="密码">
              <div class="input-wrap">
                <span class="input-icon">🔒</span>
                <el-input
                  id="password"
                  type="password"
                  v-model="form.password"
                  placeholder="请输入密码"
                  class="input-field"
                  show-password
                />
              </div>
            </el-form-item>
            
            <el-form-item prop="captcha" label="验证码">
              <div class="captcha-wrap">
                <div class="input-wrap captcha-input">
                  <span class="input-icon">🔑</span>
                  <el-input
                    id="captcha"
                    v-model="form.captcha"
                    placeholder="请输入验证码"
                    class="input-field"
                  />
                </div>
                <div class="captcha-image" @click="refreshCaptcha">
                  <img :src="captchaUrl" alt="验证码" title="点击刷新验证码" />
                </div>
              </div>
            </el-form-item>
            
            <div class="form-options">
              <div class="remember-me">
                <el-checkbox v-model="rememberMe" label="记住我" />
              </div>
              <a @click="goToResetPassword" class="forgot-password">忘记密码?</a>
            </div>
            
            <el-button 
              type="primary" 
              class="login-button" 
              @click="loginIndex" 
              :loading="loading"
            >
              登录
            </el-button>
            
            <div class="register-link">
              还没有账号? <a @click="goToRegister">立即注册</a>
            </div>
          </el-form>
        </div>
      </div>
      
      <div class="footer">
        <p>© 2024 橘子网盘 版权所有</p>
        <div class="footer-links">
          <a href="javascript:void(0)">使用条款</a>
          <a href="javascript:void(0)">隐私政策</a>
          <a href="javascript:void(0)">联系我们</a>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  width: 100%;
  min-height: 100vh;
  display: flex;
  background-color: #f0f2f5;
}

.left-decoration {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 40%;
  background: linear-gradient(135deg, #1890ff, #722ed1);
  color: white;
  padding: 40px;
  position: relative;
  overflow: hidden;
}

.decoration-circles {
  position: absolute;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.circle-1 {
  width: 300px;
  height: 300px;
  bottom: -150px;
  left: -100px;
}

.circle-2 {
  width: 200px;
  height: 200px;
  top: 10%;
  right: -50px;
}

.circle-3 {
  width: 150px;
  height: 150px;
  top: 40%;
  left: 20%;
  background: rgba(255, 255, 255, 0.15);
}

.decoration-text {
  position: relative;
  z-index: 1;
  text-align: center;
}

.decoration-text h3 {
  font-size: 36px;
  margin-bottom: 16px;
  font-weight: 600;
}

.decoration-text p {
  font-size: 18px;
  opacity: 0.8;
  max-width: 360px;
  line-height: 1.6;
}

.content-wrapper {
  width: 60%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px;
}

.logo {
  display: flex;
  align-items: center;
  margin-bottom: 30px;
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

.title {
  text-align: center;
  margin-bottom: 24px;
}

.title h2 {
  font-size: 28px;
  font-weight: 500;
  color: #1f2f3d;
  margin-bottom: 8px;
}

.subtitle {
  color: #606266;
  font-size: 16px;
}

.card {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  padding: 32px;
  width: 100%;
  max-width: 480px;
}

.form-container {
  margin-top: 8px;
}

.form-group {
  margin-bottom: 24px;
}

.form-group label {
  display: block;
  color: #606266;
  font-size: 14px;
  margin-bottom: 8px;
}

.input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  transition: all 0.2s;
  background-color: #fff;
  box-shadow: none;
}

.input-wrap:focus-within {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.input-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  color: #909399;
  font-size: 16px;
  opacity: 0.5;
}

.input-field {
  flex: 1;
  border: none !important;
  outline: none;
  height: 40px;
  padding: 0 12px 0 0;
  font-size: 14px;
  color: #606266;
  background: transparent !important;
  box-shadow: none !important;
  -webkit-appearance: none;
  -moz-appearance: none;
  appearance: none;
}

/* 覆盖Element Plus样式 */
:deep(.el-input__wrapper) {
  background-color: transparent !important;
  box-shadow: none !important;
  padding: 0;
}

:deep(.el-input__inner) {
  height: 40px;
  border: none;
  background-color: transparent;
}

.input-field::placeholder {
  color: #c0c4cc;
}

/* 防止自动填充样式问题 */
.input-field:-webkit-autofill,
.input-field:-webkit-autofill:hover, 
.input-field:-webkit-autofill:focus {
  -webkit-box-shadow: 0 0 0 30px white inset !important;
  transition: background-color 5000s ease-in-out 0s;
}

.captcha-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}

.captcha-input {
  flex: 1;
}

.captcha-image {
  width: 120px;
  height: 40px;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #606266;
}

.forgot-password {
  font-size: 14px;
  color: #1890ff;
  cursor: pointer;
  text-decoration: none;
}

.forgot-password:hover {
  text-decoration: underline;
}

.login-button {
  width: 100%;
  height: 40px;
  margin-bottom: 16px;
}

.register-link {
  margin-top: 16px;
  text-align: center;
  font-size: 14px;
  color: #606266;
}

.register-link a {
  color: #1890ff;
  text-decoration: none;
  cursor: pointer;
}

.register-link a:hover {
  text-decoration: underline;
}

.footer {
  margin-top: 40px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}

.footer-links {
  margin-top: 8px;
  display: flex;
  justify-content: center;
  gap: 16px;
}

.footer-links a {
  color: #909399;
  text-decoration: none;
}

.footer-links a:hover {
  color: #1890ff;
}

/* 响应式调整 */
@media (max-width: 992px) {
  .left-decoration {
    display: none;
  }
  
  .content-wrapper {
    width: 100%;
  }
}

/* 调整表单项样式 */
:deep(.el-form-item__label) {
  color: #606266;
  font-size: 14px;
  line-height: 40px;
  padding: 0;
  text-align: left;
}

:deep(.el-form-item) {
  margin-bottom: 24px;
}
</style>

<!-- 添加全局样式，覆盖浏览器自动填充样式 -->
<style>
/* 覆盖浏览器默认的自动填充样式 */
input:-webkit-autofill,
input:-webkit-autofill:hover, 
input:-webkit-autofill:focus {
  -webkit-box-shadow: 0 0 0 30px white inset !important;
  -webkit-text-fill-color: #606266 !important;
}
</style>