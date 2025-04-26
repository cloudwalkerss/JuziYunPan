<script setup>
import {ref, reactive, computed, watch} from "vue";
import {ElMessage} from "element-plus";
import {get, post} from "@/net/index.js";
import router from "@/router/index.js";
import { useCaptchaCountdown } from '@/utils/captchaUtils.js';

const active=ref(0)
const form=reactive({
  email:'',
  code:''
})
const formref=ref();
const form1=reactive({
  email:form.email,
  newPassword:'',
  newPasswordRepeat:''
})
const form1Ref = ref();
const emailValidate = computed(() => /^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$/.test(form.email))

// 使用验证码倒计时工具
const { countdown, startCountdown, getSavedEmail } = useCaptchaCountdown('reset');
const codeBtnText = computed(() => {
  return countdown.value > 0 ? `${countdown.value}秒后重试` : '获取验证码';
});
const codeBtnDisabled = computed(() => countdown.value > 0 || !emailValidate.value);

// 恢复保存的邮箱
const savedEmail = getSavedEmail();
if (savedEmail) {
  form.email = savedEmail;
}

// 监听form.email变更，同步到form1.email
watch(() => form.email, (newEmail) => {
  form1.email = newEmail;
});

// 返回登录页
function goToLogin() {
  // 使用Vue Router的方式，依赖App.vue中的动态key机制
  router.push('/');
}

function askcode() {
  if (countdown.value > 0 || codeBtnDisabled.value) return; // 如果还在倒计时中或按钮被禁用，直接返回
    
  if (emailValidate.value) {
    // 立即禁用按钮并启动倒计时，防止用户多次点击
    startCountdown(form.email);
    
    ElMessage.info('正在发送验证码，请稍候...');
    get(`user/ask-code?email=${form.email}&type=reset`, () => {
      ElMessage.success('验证码已发送，请查收邮件');
    }, (message) => {
      // 即使请求失败也保持倒计时状态，防止频繁点击
      ElMessage.error(message || '发送验证码失败，请稍后再试');
    });
  } else {
    ElMessage.warning("请填写正确的邮件地址");
  }
}

function confirm(){
  formref.value.validate((valid) => {
    if (valid) {
      post(`user/resetConfirm`,{
        email: form.email,
        code: form.code
      },() => {
        active.value = 1; // 验证成功后进入下一步
      });
    } else {
      // 验证失败的处理
      ElMessage.error('请按照要求填写表单');
    }
  });
}

function confirmReset() {
  form1Ref.value.validate((valid) => {
    if (valid) {
      post('user/resetPassword',{
        email: form.email,
        repeatNewPassword: form1.newPasswordRepeat,
        newPassword: form1.newPassword
      },() => {
        active.value++;
        ElMessage.success('密码修改成功');
        router.push('/');
      });
    } else {
      ElMessage.error('请按照要求填写表单');
    }
  });
}

const rule={
  email:[{required:true,message:'请输入绑定的邮箱'},{
    type:'email',message: "请输入正确格式的邮箱地址",trigger:['blur']
  }],
  code:[{required:true,message:'请输入验证码'},{
     min:6,message: '六位验证码'
  },{max:6,message: '六位验证码'}]
}

const rule1 = {
  newPassword: [
    { required: true, message: '请输入新密码' },
    { min: 8, message: '密码长度至少8位' }
  ],
  newPasswordRepeat: [
    { required: true, message: '请再次输入新密码' },
    { 
      validator: (rule1, value, callback) => {
        if (value !== form1.newPassword) {
          callback(new Error('两次密码输入不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
}
</script>

<template>
  <div class="reset-password-container">
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

    <!-- 右侧内容区域 -->
    <div class="content-wrapper">
      <!-- Logo 和返回按钮 -->
      <div class="header-container">
        <div class="logo">
          <div class="logo-icon">
            <span class="icon-text">橘</span>
          </div>
          <span class="logo-text">橘子网盘</span>
        </div>
        <el-button @click="goToLogin" class="back-button" type="text">
          <i class="back-icon">←</i> 返回登录
        </el-button>
      </div>

      <!-- 标题 -->
      <div class="title">
        <h2>重置密码</h2>
        <p class="subtitle">重新设置您的账户密码</p>
      </div>

      <div class="card">
        <!-- 步骤条 -->
        <div class="steps-container">
          <el-steps :active="active" finish-status="success" align-center process-status="process">
            <el-step title="接收验证码" />
            <el-step title="修改密码" />
          </el-steps>
        </div>

        <!-- 验证码表单 -->
        <div v-if="active === 0" class="form-container">
          <el-form :model="form" ref="formref" :rules="rule" label-position="top">
            <!-- 输入邮箱 -->
            <el-form-item prop="email" label="邮箱">
              <el-input v-model="form.email" placeholder="请输入绑定的邮箱">
                <template #prefix>
                  <i class="el-icon-message form-icon">📧</i>
                </template>
              </el-input>
            </el-form-item>
            
            <!-- 输入验证码 -->
            <el-form-item prop="code" label="验证码">
              <div class="code-input-container">
                <el-input v-model="form.code" placeholder="请输入验证码">
                  <template #prefix>
                    <i class="el-icon-key form-icon">🔑</i>
                  </template>
                </el-input>
                <el-button 
                  class="code-button" 
                  type="primary" 
                  @click="askcode" 
                  :disabled="codeBtnDisabled">
                  {{ codeBtnText }}
                </el-button>
              </div>
            </el-form-item>
          </el-form>
          
          <div class="button-container">
            <el-button @click="confirm" type="primary" class="submit-button">继续</el-button>
          </div>
          
          <div class="help-text">
            <p>没有收到验证码？<a href="javascript:void(0)" @click.prevent="askcode" :class="{ 'disabled-link': codeBtnDisabled }">重新发送</a></p>
          </div>
        </div>

        <!-- 修改表单 -->
        <div v-if="active === 1" class="form-container">
          <el-form :model="form1" ref="form1Ref" :rules="rule1" label-position="top">
            <el-form-item prop="newPassword" label="新密码">
              <el-input v-model="form1.newPassword" type="password" placeholder="请输入新密码" show-password>
                <template #prefix>
                  <i class="el-icon-lock form-icon">🔒</i>
                </template>
              </el-input>
              <div class="password-strength">
                <div class="strength-text">密码强度：</div>
                <div class="strength-meter">
                  <div class="strength-level" :class="form1.newPassword.length >= 15 ? 'strong' : (form1.newPassword.length >= 10 ? 'medium' : 'weak')"></div>
                </div>
              </div>
            </el-form-item>
            
            <el-form-item prop="newPasswordRepeat" label="确认密码">
              <el-input v-model="form1.newPasswordRepeat" type="password" placeholder="请再次输入新密码" show-password>
                <template #prefix>
                  <i class="el-icon-check form-icon">✓</i>
                </template>
              </el-input>
            </el-form-item>
          </el-form>
          
          <div class="password-tips">
            <p>提示：密码至少包含8个字符，建议使用字母、数字和符号的组合</p>
          </div>
          
          <div class="button-container">
            <el-button type="primary" @click="confirmReset" class="submit-button">确认修改</el-button>
          </div>
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
.reset-password-container {
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

.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  max-width: 480px;
  margin-bottom: 30px;
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

.back-button {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #409EFF;
}

.back-icon {
  margin-right: 4px;
  font-size: 16px;
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

.steps-container {
  margin-bottom: 32px;
}

.form-container {
  margin-top: 24px;
}

.form-icon {
  margin-right: 8px;
}

.code-input-container {
  display: flex;
  gap: 12px;
}

.code-button {
  flex-shrink: 0;
  white-space: nowrap;
  min-width: 120px; /* 固定宽度，防止倒计时时按钮宽度变化 */
}

.button-container {
  margin-top: 24px;
}

.submit-button {
  width: 100%;
  height: 40px;
  font-size: 16px;
}

.help-text {
  margin-top: 16px;
  text-align: center;
  font-size: 14px;
  color: #606266;
}

.help-text a {
  color: #1890ff;
  text-decoration: none;
}

.help-text a.disabled-link {
  color: #c0c4cc;
  cursor: not-allowed;
  text-decoration: none;
}

.password-strength {
  display: flex;
  align-items: center;
  margin-top: 8px;
  font-size: 12px;
}

.strength-text {
  color: #606266;
  margin-right: 8px;
}

.strength-meter {
  height: 6px;
  width: 100px;
  background-color: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
}

.strength-level {
  height: 100%;
  width: 33%;
  transition: all 0.3s;
}

.weak {
  width: 33%;
  background-color: #f56c6c;
}

.medium {
  width: 66%;
  background-color: #e6a23c;
}

.strong {
  width: 100%;
  background-color: #67c23a;
}

.password-tips {
  margin-top: 16px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
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
  color: #606266;
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
</style>