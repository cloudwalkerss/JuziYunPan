<template>
  <div class="user-manage">
    <div class="page-header">
      <h2>用户管理</h2>
    </div>

    <el-table
      v-loading="loading"
      :data="userList"
      style="width: 100%"
      border
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="150" />
      <el-table-column prop="nickname" label="昵称" width="150" />
      <el-table-column prop="email" label="邮箱" width="200" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="scope">
          <el-tag :type="Number(scope.row.role) === 1 ? 'danger' : 'info'" effect="plain">
            {{ Number(scope.row.role) === 1 ? '管理员' : '普通用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="registerTime" label="注册时间" width="180">
        <template #default="scope">
          {{ formatDate(scope.row.registerTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="banned" label="状态" width="100">
        <template #default="scope">
          <el-switch
            :model-value="Number(scope.row.banned) === 1"
            :active-value="true"
            :inactive-value="false"
            :active-text="'启用'"
            :inactive-text="'禁用'"
            @change="(val) => handleStatusChange(scope.row, val ? 0 : 1)"
          />
        </template>
      </el-table-column>
      <el-table-column label="存储空间" width="200">
        <template #default="scope">
          <div>
            {{ formatFileSize(scope.row.useSpace) }} / {{ formatFileSize(scope.row.totalSpace) }}
            <el-button type="primary" link @click="openSpaceDialog(scope.row)">
              修改配额
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 用户表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="formType === 'add' ? '添加用户' : '编辑用户'"
      width="500px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="formType === 'add'">
          <el-input v-model="form.password" type="password" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role">
            <el-option label="普通用户" :value="0" />
            <el-option label="管理员" :value="1" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 修改存储空间对话框 -->
    <el-dialog
      v-model="spaceDialogVisible"
      title="修改存储空间"
      width="400px"
    >
      <el-form :model="spaceForm" label-width="100px">
        <el-form-item label="用户">
          {{ spaceForm.nickname }}
        </el-form-item>
        <el-form-item label="当前配额">
          {{ formatFileSize(spaceForm.totalSpace) }}
        </el-form-item>
        <el-form-item label="修改配额(MB)">
          <el-input-number 
            v-model="spaceForm.changeSpace" 
            :min="1" 
            :max="1024000"
            :step="100"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="spaceDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleUpdateSpace">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Edit, Delete } from '@element-plus/icons-vue';
import { get, post } from '@/net';
import {userStore} from "@/store/index.js";

const loading = ref(false);
const userList = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const dialogVisible = ref(false);
const formType = ref('add');
const formRef = ref(null);

// 存储空间对话框
const spaceDialogVisible = ref(false);
const spaceForm = ref({
  userId: null,
  nickname: '',
  totalSpace: 0,
  changeSpace: 1
});

const form = ref({
  username: '',
  nickname: '',
  email: '',
  password: '',
  role: 0
});

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ]
};

// 加载用户列表
const loadUserList = async () => {
  loading.value = true;
  try {
    const params = {
      pageNo: currentPage.value,
      pageSize: pageSize.value
    };
    
    post('admin/loadUserList', params, (data) => {
      userList.value = data.list;
      total.value = data.totalCount;
      loading.value = false;
    }, () => {
      loading.value = false;
      ElMessage.error('获取用户列表失败');
    });
  } catch (error) {
    loading.value = false;
    console.error('加载用户列表失败:', error);
  }
};

// 处理分页变化
const handleSizeChange = (val) => {
  pageSize.value = val;
  loadUserList();
};

const handleCurrentChange = (val) => {
  currentPage.value = val;
  loadUserList();
};

// 添加用户
const handleAdd = () => {
  formType.value = 'add';
  form.value = {
    username: '',
    nickname: '',
    email: '',
    password: '',
    role: 0
  };
  dialogVisible.value = true;
};

// 编辑用户
const handleEdit = (row) => {
  formType.value = 'edit';
  form.value = { ...row };
  dialogVisible.value = true;
};

// 删除用户
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    post('admin/user/delete', { userId: row.id }, () => {
      ElMessage.success('删除成功');
      loadUserList();
    }, () => {
      ElMessage.error('删除失败');
    });
  }).catch(() => {});
};

// 修改用户状态
const handleStatusChange = (row, newBannedStatus) => {
  // 如果当前状态未定义，先不触发请求
  if (row.banned === undefined || row.banned === null) {
    row.banned = newBannedStatus; // 先同步本地状态
    return;
  }

  post('admin/updateUserStatus', {
    userId: row.id,
    banned: newBannedStatus
  }, () => {
    row.banned = newBannedStatus; // 请求成功后更新状态
    ElMessage.success(newBannedStatus === 0 ? '用户已启用' : '用户已禁用');
  }, () => {
    row.banned = row.banned === 1 ? 0 : 1; // 请求失败时恢复原状态
    ElMessage.error('状态修改失败');
  });
};

// 提交表单
const handleSubmit = () => {
  formRef.value?.validate((valid) => {
    if (valid) {
      const url = formType.value === 'add' ? 'admin/user/add' : 'admin/user/update';
      post(url, form.value, () => {
        ElMessage.success(formType.value === 'add' ? '添加成功' : '更新成功');
        dialogVisible.value = false;
        loadUserList();
      }, () => {
        ElMessage.error(formType.value === 'add' ? '添加失败' : '更新失败');
      });
    }
  });
};

// 打开修改存储空间对话框
const openSpaceDialog = (user) => {
  spaceForm.value = {
    userId: user.id,
    nickname: user.nickname,
    totalSpace: user.totalSpace,
    changeSpace: Math.floor(user.totalSpace / (1024 * 1024)) // 转换为MB
  };
  spaceDialogVisible.value = true;
};

// 修改存储空间
const handleUpdateSpace = () => {
  if (!spaceForm.value.changeSpace || spaceForm.value.changeSpace <= 0) {
    ElMessage.warning('请输入有效的存储空间');
    return;
  }

  post('admin/updateUserSpace', {
    userId: spaceForm.value.userId,
    totalSpace: spaceForm.value.changeSpace // 直接发送MB值
  }, () => {
    ElMessage.success('存储空间修改成功');
    spaceDialogVisible.value = false;
    loadUserList(); // 重新加载列表
  }, () => {
    ElMessage.error('存储空间修改失败');
  });
};

// 格式化日期
const formatDate = (date) => {
  if (!date) return '';
  return new Date(date).toLocaleString();
};

// 格式化文件大小
const formatFileSize = (size) => {
  if (!size) return '0 B';
  
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let index = 0;
  let fileSize = parseFloat(size);
  
  while (fileSize >= 1024 && index < units.length - 1) {
    fileSize /= 1024;
    index++;
  }
  
  return fileSize.toFixed(2) + ' ' + units[index];
};

onMounted(() => {
  loadUserList();
});
</script>

<style scoped>
.user-manage {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-tag) {
  text-align: center;
  min-width: 80px;
}

.el-button-group {
  display: flex;
  gap: 8px;
}
</style> 