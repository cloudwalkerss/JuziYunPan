<template>
  <div class="preview-image-container">
    <img :src="url" alt="图片预览" style="max-width:100%;max-height:70vh;display:block;margin:auto;" />
  </div>
</template>
<script setup>
defineProps({ url: String });
</script>
<style scoped>
.preview-image-container {
  text-align: center;
}
</style> 