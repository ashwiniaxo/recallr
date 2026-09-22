<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import SectionCard from '../components/SectionCard.vue'
import { getCourseSections } from '../services/api'
import type { CourseSection } from '../types/study'

const route = useRoute()

const sections = ref<CourseSection[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const courseId = Number(route.params.id)

onMounted(async () => {
  try {
    sections.value = await getCourseSections(courseId)
  } catch {
    error.value = 'Impossible de charger les sections.'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="course-view">
    <RouterLink to="/" class="back-link">
      ← Mes cours
    </RouterLink>

    <header>
      <p class="eyebrow">Recallr</p>
      <h1>LOG635</h1>
      <p>Choisis une section à réviser.</p>
    </header>

    <p v-if="loading">
      Chargement...
    </p>

    <p v-else-if="error">
      {{ error }}
    </p>

    <p v-else-if="sections.length === 0">
      Aucune section pour ce cours.
    </p>

    <section v-else class="sections">
      <SectionCard
        v-for="section in sections"
        :key="section.id"
        :section="section"
      />
    </section>
  </main>
</template>

<style scoped>
.course-view {
  width: min(100% - 2rem, 1100px);
  margin: 0 auto;
  padding: 3rem 0;
}

.back-link {
  display: inline-block;
  margin-bottom: 2rem;
  text-decoration: none;
}

header {
  margin-bottom: 2rem;
}

.eyebrow {
  font-weight: 700;
}

h1 {
  margin: 0.25rem 0;
  font-size: 2.5rem;
}

.sections {
  display: grid;
  gap: 1rem;
}
</style>