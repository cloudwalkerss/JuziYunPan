<template>
  <el-dialog
    :model-value="modelValue"
    title="选择文件夹"
    width="500px"
    @update:model-value="close"
    :close-on-click-modal="false"
  >
    <div v-loading="loading" class="folder-list">
      <el-tree
        :data="folderTree"
        :props="treeProps"
        @node-click="handleNodeClick"
        :highlight-current="true"
        ref="treeRef"
      />
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="close">取消</el-button>
        <el-button type="primary" @click="confirmSelection">确定</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { get, post } from '@/net';
import { ElMessage } from 'element-plus';

const props = defineProps({
  modelValue: Boolean,
});

const emit = defineEmits(['update:modelValue', 'folder-selected']);

const loading = ref(false);
const folderTree = ref([]);
const selectedFolder = ref(null);
const treeRef = ref(null);

const treeProps = {
  children: 'children',
  label: 'fileName',
};

const loadAllFolder = async () => {
  loading.value = true;
  try {
    const data = await new Promise((resolve, reject) => {
      // 调用正确的接口获取文件夹列表
      post('/file/loadAllFolder', { filePId: '0', excludeFileIds: '' }, (data) => resolve(data), (err) => reject(err));
    });
    
    // 添加根目录选项
    const rootFolder = {
      fileName: '根目录',
      fileId: '0',
      children: data || []
    };
    
    folderTree.value = [rootFolder];
  } catch (error) {
    ElMessage.error('加载目录失败');
  } finally {
    loading.value = false;
  }
};

const handleNodeClick = (data) => {
  selectedFolder.value = data;
};

const confirmSelection = () => {
  if (!selectedFolder.value) {
    ElMessage.warning('请选择一个文件夹');
    return;
  }
  emit('folder-selected', selectedFolder.value.fileId);
  close();
};

const close = () => {
  emit('update:modelValue', false);
};

watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    loadAllFolder();
  }
});

</script>

<style scoped>
.folder-list {
  max-height: 400px;
  overflow-y: auto;
}
</style> 