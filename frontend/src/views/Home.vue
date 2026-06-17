<template>
  <div class="home-container">
    <el-card>
      <template #header>
        <span>欢迎使用 DevPilot</span>
      </template>
      <p>DevPilot 是您的工程问题排查助手，帮助您高效管理和追踪工程问题。</p>
      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :span="8">
          <el-statistic title="待处理问题" :value="0" />
        </el-col>
        <el-col :span="8">
          <el-statistic title="处理中" :value="0" />
        </el-col>
        <el-col :span="8">
          <el-statistic title="已完成" :value="0" />
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.home-container {
  max-width: 1200px;
}
</style>
