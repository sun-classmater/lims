<template>
  <main class="login-page">
    <el-card
      class="login-card"
      shadow="never"
    >
      <h1>LIMS</h1>
      <el-divider />
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @keyup.enter="submit"
      >
        <el-form-item
          label="账号"
          prop="username"
        >
          <el-input
            v-model="form.username"
            placeholder="请输入工号"
          />
        </el-form-item>
        <el-form-item
          label="密码"
          prop="password"
        >
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
          />
        </el-form-item>
        <el-button
          class="submit-button"
          type="primary"
          :loading="authStore.loading"
          @click="submit"
        >
          登录
        </el-button>
      </el-form>
    </el-card>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [
    {
      required: true,
      message: '请输入工号',
      trigger: 'blur',
    },
  ],
  password: [
    {
      required: true,
      message: '请输入密码',
      trigger: 'blur',
    },
  ],
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  try {
    await authStore.login(form.username, form.password)
    ElMessage.success('登录成功')

    const redirect = typeof route.query.redirect === 'string'
      ? route.query.redirect
      : '/service/files'
    await router.replace(redirect)
  } catch {
    ElMessage.error('用户名或密码错误')
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: #f5f7fa;
}

.login-card {
  width: min(400px, calc(100vw - 32px));
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.submit-button {
  width: 100%;
}

h1 {
  margin: 0;
  text-align: center;
  font-size: 28px;
  letter-spacing: 0;
}
</style>
