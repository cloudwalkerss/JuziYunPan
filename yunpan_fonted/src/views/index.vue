<script setup>
import { ElButton, ElMessage, ElPopconfirm } from 'element-plus';
import { logout } from '@/net/index.js';
import router from '@/router/index.js';

// 退出登录
function handleLogout() {
  logout(() =>
    router.push('/')

  , () =>
    ElMessage.error(`退出失败`)
  );
}
</script>

<template>
  <div class="index-container">
    <div class="header">
      <div class="logo">
        <div class="logo-icon">
          <span class="icon-text">橘</span>
        </div>
        <span class="logo-text">橘子网盘</span>
      </div>
      <div class="user-actions">
        <el-popconfirm
          title="确定要退出登录吗?"
          confirm-button-text="确定"
          cancel-button-text="取消"
          @confirm="handleLogout"
        >
          <template #reference>
            <el-button type="danger" size="small">
              <i class="logout-icon">🚪</i> 退出登录
            </el-button>
          </template>
        </el-popconfirm>
      </div>
    </div>
    
    <div class="content">
      <h2>欢迎使用橘子网盘</h2>
      <p>这是主页面，您可以在这里管理您的文件</p>
    </div>
  </div>
</template>

<style scoped>
.index-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background-color: #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.logo {
  display: flex;
  align-items: center;
}

.logo-icon {
  width: 36px;
  height: 36px;
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
  font-size: 20px;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #1f2f3d;
}

.user-actions {
  display: flex;
  align-items: center;
}

.logout-icon {
  margin-right: 4px;
}

.content {
  flex: 1;
  padding: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #f0f2f5;
}

.content h2 {
  font-size: 24px;
  color: #1f2f3d;
  margin-bottom: 16px;
}

.content p {
  font-size: 16px;
  color: #606266;
}
</style>