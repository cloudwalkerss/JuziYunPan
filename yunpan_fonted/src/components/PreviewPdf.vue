<template>
  <div class="pdf">
    <vue-pdf-embed
      v-if="pdfObjectUrl"
      ref="pdfRef"
      :source="pdfObjectUrl"
      class="vue-pdf-embed"
      width="100%"
      height="600"
      :page="state.pageNum"
    />
    <div v-else>加载中...</div>
  </div>
</template>

<script setup>
import VuePdfEmbed from "vue-pdf-embed";
import { ref, onMounted, onBeforeUnmount } from "vue";
import { getHeader } from '@/net/index.js';

const props = defineProps({
  url: String, // 兼容老用法
  fileId: String, // 推荐用fileId
});

const state = ref({
  pageNum: 1,
});
const pdfObjectUrl = ref("");
let objectUrl = null;

onMounted(async () => {
  // 优先用fileId
  const fileId = props.fileId || (props.url ? props.url.split('/').pop() : '');
  const pdfUrl = `http://localhost:8080/file/getFile/${fileId}`;
  try {
    const response = await fetch(pdfUrl, {
      headers: getHeader()
    });
    if (!response.ok) throw new Error('PDF加载失败');
    const blob = await response.blob();
    objectUrl = URL.createObjectURL(blob);
    pdfObjectUrl.value = objectUrl;
  } catch (e) {
    pdfObjectUrl.value = '';
  }
});

onBeforeUnmount(() => {
  if (objectUrl) URL.revokeObjectURL(objectUrl);
});
</script>

<style scoped>
.pdf {
  width: 100%;
  text-align: center;
  background: #fff;
}
.vue-pdf-embed {
  margin: 0 auto;
  box-shadow: 0 2px 8px #eee;
}
</style> 