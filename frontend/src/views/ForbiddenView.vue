<template>
  <main class="status-page">
    <el-result
      icon="warning"
      title="403"
      sub-title="Forbidden"
    />
  </main>
</template>

<script setup lang="ts">
</script>

<style scoped>
.status-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: #f5f7fa;
}
</style>
