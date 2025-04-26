/**
 * 验证码工具函数，用于管理验证码的倒计时和状态
 */

import { ref, onMounted, onUnmounted } from 'vue';

/**
 * 创建验证码倒计时管理器
 * @param {string} type - 验证码类型，用于区分不同页面的验证码
 * @param {number} time - 倒计时时间（秒）
 * @param {number} failureTime - 失败时的倒计时时间（秒），默认为10秒
 * @returns {object} - 倒计时状态和管理函数
 */
export function useCaptchaCountdown(type = 'default', time = 60, failureTime = 10) {
  const countdown = ref(0);
  const countdownKey = `captcha_countdown_${type}`;
  const emailKey = `captcha_email_${type}`;
  let countdownInterval = null;
  
  // 开始倒计时
  const startCountdown = (email, isFailure = false) => {
    // 设置倒计时时间，失败时使用较短时间
    const countdownTime = isFailure ? failureTime : time;
    
    // 保存状态到localStorage
    const endTime = Date.now() + countdownTime * 1000;
    localStorage.setItem(countdownKey, endTime.toString());
    if (email) {
      localStorage.setItem(emailKey, email);
    }
    
    // 设置倒计时
    countdown.value = countdownTime;
    if (countdownInterval) clearInterval(countdownInterval);
    
    countdownInterval = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) {
        clearInterval(countdownInterval);
        countdownInterval = null;
        localStorage.removeItem(countdownKey);
        localStorage.removeItem(emailKey);
      }
    }, 1000);
  };
  
  // 恢复倒计时状态
  const restoreCountdown = () => {
    const endTimeStr = localStorage.getItem(countdownKey);
    
    if (endTimeStr) {
      const endTime = parseInt(endTimeStr);
      const now = Date.now();
      const remaining = Math.floor((endTime - now) / 1000);
      
      if (remaining > 0) {
        countdown.value = remaining;
        
        countdownInterval = setInterval(() => {
          countdown.value--;
          if (countdown.value <= 0) {
            clearInterval(countdownInterval);
            countdownInterval = null;
            localStorage.removeItem(countdownKey);
            localStorage.removeItem(emailKey);
          }
        }, 1000);
        
        return true;
      } else {
        // 倒计时已结束，清除存储
        localStorage.removeItem(countdownKey);
        localStorage.removeItem(emailKey);
      }
    }
    
    return false;
  };
  
  // 获取保存的邮箱
  const getSavedEmail = () => {
    return localStorage.getItem(emailKey);
  };
  
  // 清除倒计时
  const clearCountdown = () => {
    if (countdownInterval) {
      clearInterval(countdownInterval);
      countdownInterval = null;
    }
    localStorage.removeItem(countdownKey);
    localStorage.removeItem(emailKey);
    countdown.value = 0;
  };
  
  // 组件挂载时恢复倒计时
  onMounted(() => {
    restoreCountdown();
  });
  
  // 组件卸载时清除定时器
  onUnmounted(() => {
    if (countdownInterval) {
      clearInterval(countdownInterval);
      countdownInterval = null;
    }
  });
  
  return {
    countdown,
    startCountdown,
    restoreCountdown,
    getSavedEmail,
    clearCountdown
  };
}