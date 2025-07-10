import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { login } from '@/net/index.js';
import axios from 'axios';

export function useAuth() {
  const baseurl = axios.defaults.baseURL;
  
  // 验证码相关
  const captchaId = ref('');
  const captchaUrl = ref('');
  
  // 表单数据
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
  
  // 加载状态
  const loading = ref(false);
  const remember = ref(false);
  
  // 刷新验证码
  const refreshCaptcha = () => {
    const timestamp = new Date().getTime();
    captchaUrl.value = `${baseurl}/captcha/generate?t=${timestamp}`;

    axios.get(captchaUrl.value, {
      responseType: 'blob'
    })
    .then(response => {
      captchaId.value = response.headers['captcha-id'];
      const blob = new Blob([response.data], { type: 'image/jpeg' });
      captchaUrl.value = URL.createObjectURL(blob);
    })
    .catch(error => {
      ElMessage.error('获取验证码失败，请刷新页面重试');
    });
  };
  
  // 重置表单
  const resetForm = () => {
    form.username = '';
    form.password = '';
    form.captcha = '';
    remember.value = false;
    refreshCaptcha();
  };
  
  // 登录处理
  const handleLogin = async (formRef, successCallback, failureCallback) => {
    if (!formRef) return;
    
    return new Promise((resolve, reject) => {
      formRef.validate((valid) => {
        if (valid) {
          loading.value = true;
          login(
            form.username, 
            form.password, 
            remember.value, 
            form.captcha, 
            captchaId.value,
            () => {
              loading.value = false;
              ElMessage.success('登录成功');
              if (successCallback) successCallback();
              resolve();
            },
            (message) => {
              loading.value = false;
              ElMessage.error('登录失败: ' + message);
              refreshCaptcha();
              if (failureCallback) failureCallback(message);
              reject(message);
            }
          );
        } else {
          reject('表单验证失败');
        }
      });
    });
  };
  
  return {
    form,
    rules,
    loading,
    remember,
    captchaId,
    captchaUrl,
    refreshCaptcha,
    resetForm,
    handleLogin
  };
} 