<script setup lang="ts">
defineProps<{
  value: number
}>()
</script>

<template>
  <div class="progress">
    <div
      class="progress-fill"
      :style="{ width: `${value}%` }"
    />
  </div>
</template>

<style scoped>
.progress {
  width: 100%;
  height: 8px;
  overflow: hidden;
  background: #e5e5e5;
  border-radius: 999px;
}

.progress-fill {
  height: 100%;
  background: #202124;
  border-radius: inherit;
  transition: width 0.3s ease;
}
</style>