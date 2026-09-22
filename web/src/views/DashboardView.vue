<script setup lang="ts">
import { onMounted, ref } from 'vue'

import CourseCard from '../components/CourseCard.vue'
import { getStudySets } from '../services/api'
import type { StudySet } from '../types/study'

const courses = ref<StudySet[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

onMounted(async () => {
  try {
    courses.value = await getStudySets()
  } catch {
    error.value = 'Impossible de charger les cours.'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="dashboard">
    <header>
      <p class="eyebrow">Recallr</p>
      <h1>Mes cours</h1>
      <p>Choisis un cours pour commencer à réviser.</p>
    </header>

    <p v-if="loading">
      Chargement...
    </p>

    <p v-else-if="error">
      {{ error }}
    </p>

    <p v-else-if="courses.length === 0">
      Aucun cours pour le moment.
    </p>

    <section v-else class="course-grid">
      <CourseCard
        v-for="course in courses"
        :key="course.id"
        :course="course"
      />
    </section>
  </main>
</template>

<style scoped>
.dashboard {
  width: min(100% - 2rem, 1100px);
  margin: 0 auto;
  padding: 3rem 0;
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

.course-grid {
  display: grid;
  grid-template-columns: repeat(
    auto-fit,
    minmax(280px, 1fr)
  );
  gap: 1rem;
}
</style>