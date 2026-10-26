<template>
  <main class="login-page">
    <span class="login-glow login-glow-a" />
    <span class="login-glow login-glow-b" />
    <el-card
      class="login-card"
      shadow="never"
    >
      <div class="login-brand">
        <div class="login-logo">
          <el-icon>
            <Platform />
          </el-icon>
        </div>
        <div class="login-title">
          LIMS
        </div>
        <div class="login-subtitle">
          实验室信息管理系统
        </div>
      </div>
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
      <p class="login-foot">
        南通理工学院 · 软件专业综合实践
      </p>
    </el-card>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Platform } from '@element-plus/icons-vue'
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
  position: relative;
  min-height: 100vh;
  display: grid;
  place-items: center;
  overflow: hidden;
  background: linear-gradient(135deg, #14304a 0%, #1f4a6b 45%, #2f6fb3 100%);
}

.login-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.45;
  pointer-events: none;
}

.login-glow-a {
  width: 420px;
  height: 420px;
  top: -120px;
  left: -80px;
  background: #4f95cf;
}

.login-glow-b {
  width: 380px;
  height: 380px;
  right: -100px;
  bottom: -140px;
  background: #6fb3d9;
}

.login-card {
  position: relative;
  z-index: 1;
  width: min(420px, calc(100vw - 32px));
  padding: 10px 8px 4px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.97);
  box-shadow: 0 24px 60px rgba(6, 24, 42, 0.35);
}

.login-brand {
  margin-bottom: 18px;
  text-align: center;
}

.login-logo {
  width: 52px;
  height: 52px;
  margin: 0 auto 10px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  color: #ffffff;
  font-size: 26px;
  background: linear-gradient(135deg, #4f95cf, #2f6fb3);
  box-shadow: 0 8px 20px rgba(47, 111, 179, 0.35);
}

.login-title {
  color: #17324d;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 2px;
}

.login-subtitle {
  margin-top: 4px;
  color: #7b8da3;
  font-size: 13px;
  letter-spacing: 1px;
}

.submit-button {
  width: 100%;
  height: 42px;
  border: none;
  font-size: 15px;
  letter-spacing: 2px;
  background: linear-gradient(90deg, #2f6fb3, #4f95cf);
}

.submit-button:hover {
  background: linear-gradient(90deg, #265a93, #3f86c4);
}

.login-foot {
  margin: 18px 0 0;
  color: #95a5b8;
  font-size: 12px;
  text-align: center;
}
</style>
