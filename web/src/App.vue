<template>
  <main>
    <h1>Recallr</h1>
    <p>{{ backendStatus }}</p>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { checkHealth } from './services/api'

const backendStatus = ref('Connecting...')

onMounted(async () => {
  try {
    backendStatus.value = await checkHealth()
  } catch (error) {
    console.error(error)
    backendStatus.value = 'Backend unavailable'
  }
})
</script>
