<template>
  <div class="preview-video-container">
    <video :src="url" controls style="max-width:100%;max-height:70vh;display:block;margin:auto;" />
  </div>
</template>
<script setup>
defineProps({ url: String });
</script>
<style scoped>
.preview-video-container {
  text-align: center;
}
</style> 