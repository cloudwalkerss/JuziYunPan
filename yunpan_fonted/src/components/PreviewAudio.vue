<template>
  <div class="preview-audio-container">
    <audio :src="url" controls style="width:100%;margin:auto;" />
  </div>
</template>
<script setup>
defineProps({ url: String });
</script>
<style scoped>
.preview-audio-container {
  text-align: center;
  padding: 30px 0;
}
</style> 