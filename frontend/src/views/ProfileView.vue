<template>
  <div class="profile-page">
    <el-card
      class="profile-card"
      shadow="never"
    >
      <h2>个人资料修改</h2>
      <el-form
        ref="formRef"
        :model="form"
        label-width="110px"
      >
        <el-form-item label="工号">
          <el-input
            v-model="form.gh"
            disabled
          />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="角色编号">
          <el-input
            v-model="form.userRoleId"
            disabled
          />
        </el-form-item>
        <el-form-item label="部门编号">
          <el-input v-model="form.deptId" />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="form.mobile" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="岗位">
          <el-input v-model="form.position" />
        </el-form-item>
        <el-form-item label="人员性质">
          <el-input v-model="form.nature" />
        </el-form-item>
        <el-button
          type="primary"
          :loading="saving"
          @click="save"
        >
          保存
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { fetchData, updateData } from '@/api/data'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const saving = ref(false)
const form = reactive({
  gh: '',
  name: '',
  userRoleId: '',
  deptId: '',
  mobile: '',
  email: '',
  position: '',
  nature: '',
})

onMounted(() => {
  loadProfile()
})

async function loadProfile() {
  if (!authStore.user?.gh) {
    return
  }

  const result = await fetchData('user', {
    page: 1,
    size: 1,
    gh: authStore.user.gh,
  })
  const row = result.records[0] || {}
  form.gh = String(row.gh || '')
  form.name = String(row.name || '')
  form.userRoleId = String(row.userRoleId || '')
  form.deptId = String(row.deptId || '')
  form.mobile = String(row.mobile || '')
  form.email = String(row.email || '')
  form.position = String(row.position || '')
  form.nature = String(row.nature || '')
}

async function save() {
  saving.value = true
  try {
    await updateData('user', { ...form })
    await authStore.fetchMe()
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.profile-page {
  padding: 24px;
}

.profile-card {
  width: min(720px, 100%);
  margin: 0 auto;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

h2 {
  margin: 0 0 20px;
  color: #111827;
  font-size: 20px;
}
</style>
