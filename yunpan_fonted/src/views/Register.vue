<script setup>
import {Form, FormItem, Input, Button, Checkbox, message} from 'ant-design-vue';
import { reactive, ref, watch } from 'vue';
import {get, post} from "@/net/index.js";
import {ElMessage} from "element-plus";
import { useCaptchaCountdown } from '@/utils/captchaUtils.js';
import {computed} from "vue";
import router from "@/router/index.js";

// 注册组件
const AForm = Form;
const AFormItem = FormItem;
const AInput = Input;
const AInputPassword = Input.Password;
const AButton = Button;
const ACheckbox = Checkbox;

const formref = ref();

// 定义表单数据
const formState = reactive({
  username: '',
  password: '',
  repeatpassword:'',
  email:'',
  code:''
});

// 判断邮箱是否正确
const emailValidate = computed(() => /^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$/.test(formState.email))

// 使用验证码倒计时工具
const { countdown, startCountdown, getSavedEmail } = useCaptchaCountdown('register');
const codeBtnText = computed(() => {
  return countdown.value > 0 ? `${countdown.value}秒后重试` : '获取验证码';
});
const codeBtnDisabled = computed(() => countdown.value > 0 || !emailValidate.value || formState.email === '');

// 恢复保存的邮箱
const savedEmail = getSavedEmail();
if (savedEmail) {
  formState.email = savedEmail;
}

function getcode(){
  if (countdown.value > 0 || codeBtnDisabled.value) return; // 如果还在倒计时中或按钮被禁用，直接返回
  
  if(!emailValidate.value || formState.email === ''){
    ElMessage.warning('请先输入正确的邮箱地址');
    return;
  }
  
  // 立即禁用按钮并启动倒计时，防止用户多次点击
  startCountdown(formState.email); 
  
  ElMessage.info('正在发送验证码，请稍候...');
  
  get(`user/ask-code?email=${formState.email}&type=register`, () => {
    ElMessage.success("邮件已经发送，请注意查收");
  }, (message) => {
    // 请求失败时的处理 - 即使失败也维持倒计时，防止用户频繁点击
    ElMessage.error(message || '发送验证码失败，请稍后再试');
  });
}

const validatePassword_repeat=(rule,value,callback)=>{
  if(value===''){
    callback(new Error('请再次输入密码'))
  }else if(value!==formState.password){
    callback(new Error('必须与上栏输入的密码一致'))
  }else{
    callback()
  }
}
function register(){
  formref.value.validate().then(()=>{

    post('user/register',{
      username: formState.username,
      password: formState.password,
        repeatpassword:formState.repeatpassword,
      email: formState.email,
      code: formState.code
    },()=>{
      router.push('/')

      ElMessage.success('注册成功,快来登录吧！')
    })
  })
}

function goToLogin() {
  router.push('/');
}
</script>

<template>
  <div class="register-container">
    <div class="register-card">
      <!-- 左侧装饰区域 -->
      <div class="card-left">
        <div class="logo-area">
          <div class="logo-icon">
            <span class="icon-text">橘</span>
          </div>
          <h1 class="logo-text">橘子云盘</h1>
        </div>
        <div class="slogan">
          <h2>安全存储，随时随地访问您的文件</h2>
          <p>加入我们，开启您的云存储之旅</p>
        </div>
        <div class="decoration-circles">
          <div class="circle circle-1"></div>
          <div class="circle circle-2"></div>
          <div class="circle circle-3"></div>
        </div>
      </div>
      
      <!-- 右侧表单区域 -->
      <div class="card-right">
        <h2 class="form-title">注册账号</h2>
        <p class="form-subtitle">创建一个账号，开始使用橘子云盘</p>
        
        <a-form 
          ref="formref"
          :model="formState"
          name="register"
          class="register-form"
          :label-col="{ span: 0 }"
          :wrapper-col="{ span: 24 }">
          
          <a-form-item
            name="username"
            :rules="[
              { required: true, message: '请输入账号', trigger:['blur','changer'] },
              { min:6, message: '账号长度不能低于六位' },
              { max:20, message: '账号长度不能高于20位' },
              { pattern: /^[a-zA-Z0-9]{6,20}$/, message: '只能包含字母和数字，长度6-20位' }
            ]"
          >
            <a-input 
              placeholder="账号"
              v-model:value="formState.username"
              size="large" 
              class="input-style" />
          </a-form-item>

          <a-form-item
            name="password"
            :rules="[
              { required: true, message: '请输入你的密码', trigger:['blur','changer'] },
              { min:8, message: '密码长度不能低于八位' },
              { max:20, message: '密码长度不能高于20位' }
            ]"
          >
            <a-input-password 
              placeholder="密码" 
              v-model:value="formState.password" 
              size="large"
              class="input-style" />
          </a-form-item>

          <a-form-item
            name="repeatpassword"
            :rules="[{ validator: validatePassword_repeat, trigger: ['blur', 'change'] }]"
          >
            <a-input-password
              placeholder="确认密码" 
              v-model:value="formState.repeatpassword" 
              size="large"
              class="input-style" />
          </a-form-item>

          <a-form-item
            name="email"
            :rules="[
              { required: true, message: '请输入你的邮箱', trigger:['blur'] },
              { type:'email', message: '请输入正确的电子邮件', trigger:['blur'] }
            ]"
          >
            <a-input 
              placeholder="邮箱" 
              v-model:value="formState.email" 
              size="large"
              class="input-style" />
          </a-form-item>
          
          <div class="code-container">
            <a-form-item
              name="code"
              :rules="[{ required: true, message: '请输入验证码', trigger:['blur','changer'] }]"
              class="code-input-item"
            >
              <a-input 
                placeholder="验证码" 
                v-model:value="formState.code" 
                size="large"
                class="input-style" />
            </a-form-item>

            <a-button 
              type="primary" 
              @click="getcode" 
              :disabled="codeBtnDisabled" 
              size="large"
              class="code-button">
              {{ codeBtnText }}
            </a-button>
          </div>

          <a-form-item>
            <a-button 
              type="primary" 
              @click="register" 
              size="large"
              class="register-button">
              注册
            </a-button>
          </a-form-item>
          
          <div class="login-link">
            已有账号? <a @click="goToLogin">立即登录</a>
          </div>
        </a-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(to right, #f0f2f5, #e6f7ff);
  overflow: hidden;
}

.register-card {
  width: 900px;
  height: 600px;
  display: flex;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

.card-left {
  width: 40%;
  background: linear-gradient(135deg, #1890ff, #722ed1);
  padding: 40px;
  color: white;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
}

.logo-area {
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
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.slogan {
  margin-top: 40px;
  z-index: 1;
}

.slogan h2 {
  font-size: 24px;
  margin-bottom: 12px;
}

.slogan p {
  font-size: 16px;
  opacity: 0.8;
}

.decoration-circles {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  pointer-events: none;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.circle-1 {
  width: 250px;
  height: 250px;
  bottom: -100px;
  left: -100px;
}

.circle-2 {
  width: 150px;
  height: 150px;
  top: 10%;
  right: -50px;
}

.circle-3 {
  width: 100px;
  height: 100px;
  top: 40%;
  left: 20%;
  background: rgba(255, 255, 255, 0.15);
}

.card-right {
  width: 60%;
  background: white;
  padding: 40px;
  display: flex;
  flex-direction: column;
}

.form-title {
  font-size: 28px;
  margin-bottom: 8px;
  color: #262626;
  text-align: center;
}

.form-subtitle {
  color: #8c8c8c;
  margin-bottom: 32px;
  text-align: center;
}

.register-form {
  flex: 1;
}

.input-style {
  border-radius: 6px;
  height: 40px;
}

.code-container {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.code-input-item {
  flex: 1;
  margin-bottom: 0;
}

.code-button {
  min-width: 120px;
  border-radius: 6px;
}

.register-button {
  width: 100%;
  height: 40px;
  font-size: 16px;
  border-radius: 6px;
}

.login-link {
  margin-top: 16px;
  text-align: center;
  color: #8c8c8c;
}

.login-link a {
  color: #1890ff;
  cursor: pointer;
}

.login-link a:hover {
  text-decoration: underline;
}

@media (max-width: 992px) {
  .register-card {
    width: 100%;
    height: 100%;
    flex-direction: column;
    border-radius: 0;
  }
  
  .card-left {
    width: 100%;
    height: 30%;
    padding: 20px;
  }
  
  .card-right {
    width: 100%;
    padding: 20px;
  }
}

@media (max-width: 576px) {
  .card-left {
    display: none;
  }
  
  .card-right {
    padding: 20px;
  }
}
</style>