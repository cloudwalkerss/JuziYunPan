<template>
  <div class="storage-manage">
    <div class="page-header">
      <h2>存储管理</h2>
    </div>

    <el-card class="settings-card">
      <template #header>
        <div class="card-header">
          <span>系统存储设置</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        label-width="200px"
        class="settings-form"
      >
        <el-form-item label="用户初始存储空间(MB)">
          <el-input-number
            v-model="form.userInitUseSpace"
            :min="1"
            :max="1024000"
            :step="100"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSave">保存设置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { get, post } from '@/net';

const formRef = ref(null);
const form = ref({
  userInitUseSpace: 1024
});

// 加载系统设置
const loadSettings = () => {
  get('admin/getSysSettings', (data) => {
    form.value = data;
  }, () => {
    ElMessage.error('获取系统设置失败');
  });
};

// 保存设置
const handleSave = () => {
  post('admin/saveSysSettings', form.value, () => {
    ElMessage.success('设置保存成功');
  }, () => {
    ElMessage.error('设置保存失败');
  });
};

onMounted(() => {
  loadSettings();
});
</script>

<style scoped>
.storage-manage {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.settings-card {
  max-width: 800px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.settings-form {
  padding: 20px;
}
</style> 