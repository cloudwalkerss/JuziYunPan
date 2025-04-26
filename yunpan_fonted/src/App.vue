<script setup>
import { useRoute } from 'vue-router';

// 获取当前路由实例，用于动态key
const route = useRoute();
</script>

<template>
  <!-- 添加:key属性，确保路由变化时组件会重新创建 -->
  <RouterView :key="route.fullPath" />
</template>

<!-- 全局样式，专门用于Element Plus组件 -->
<style>
/* Element Plus 消息组件样式自定义 */
.el-message {
  z-index: 9999 !important;
  min-width: 300px;
  padding: 15px 15px 15px 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  border-width: 1px;
  margin-top: 20px;
}

/* 消息组件过渡动画 */
.el-message-fade-enter-active,
.el-message-fade-leave-active {
  transition: opacity 0.4s, transform 0.4s;
}

.el-message-fade-enter-from,
.el-message-fade-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}
</style>

<!-- 其他通用样式可以放在这里 -->
<style scoped>
/* 这里可以放置App组件自身的样式 */
</style>
