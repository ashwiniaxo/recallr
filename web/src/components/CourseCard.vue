<script setup lang="ts">
import type { StudySet } from '../types/study'

defineProps<{
  course: StudySet
}>()
</script>

<template>
  <article class="course-card">
    <div>
      <h2>{{ course.title }}</h2>
      <p>{{ course.description }}</p>
    </div>

    <RouterLink :to="`/courses/${course.id}`">
      Ouvrir le cours →
    </RouterLink>
  </article>
</template>

<style scoped>
.course-card {
  padding: 1.5rem;
  border: 1px solid #ddd;
  border-radius: 12px;
}

.course-card h2 {
  margin: 0 0 0.5rem;
}

.course-card p {
  margin-bottom: 1.5rem;
}

.course-card a {
  text-decoration: none;
  font-weight: 600;
}
</style>