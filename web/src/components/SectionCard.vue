<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { getSectionProgress } from '../services/api'
import type {
  CourseSection,
  SectionProgress,
} from '../types/study'
import { useRouter } from 'vue-router'

import ProgressBar from './ProgressBar.vue'

const props = defineProps<{
  section: CourseSection
}>()
const router = useRouter()
const progress = ref<SectionProgress | null>(null)
const loading = ref(true)
const error = ref(false)

onMounted(async () => {
  try {
    progress.value = await getSectionProgress(
      props.section.id,
    )
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
})

const mastery = computed(() => {
  if (!progress.value?.concepts.length) {
    return 0
  }

  const total = progress.value.concepts.reduce(
    (sum, concept) => sum + concept.masteryScore,
    0,
  )

  return Math.round(
    total / progress.value.concepts.length,
  )
})

const totalConcepts = computed(() => {
  return progress.value?.concepts.length ?? 0
})

const studiedConcepts = computed(() => {
  return (
    progress.value?.concepts.filter(
      (concept) => concept.attempts > 0,
    ).length ?? 0
  )
})

const unstudiedConcepts = computed(() => {
  return totalConcepts.value - studiedConcepts.value
})

function startStudying() {
  router.push(`/study/sections/${props.section.id}`)
}
</script>

<template>
  <article class="section-card">
    <div class="section-content">
      <h2>{{ section.title }}</h2>

      <p v-if="loading">
        Chargement de la progression...
      </p>

      <p v-else-if="error">
        Progression indisponible.
      </p>

      <template v-else>
        <div class="mastery-header">
          <span>Maîtrise globale</span>
          <strong>{{ mastery }} %</strong>
        </div>

        <ProgressBar :value="mastery" />

        <div class="stats">
          <span>
            {{ totalConcepts }} concepts
          </span>

          <span>
            {{ studiedConcepts }} étudiés
          </span>

          <span>
            {{ unstudiedConcepts }} à découvrir
          </span>
        </div>
      </template>
    </div>

    <button
    type="button"
    @click="startStudying"
    >
    Étudier
    </button>
  </article>
</template>

<style scoped>
.section-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 2rem;

  padding: 1.5rem;
  border: 1px solid #ddd;
  border-radius: 12px;
  background: white;
}

.section-content {
  flex: 1;
}

.section-card h2 {
  margin: 0 0 1.25rem;
}

.mastery-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.stats {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-top: 0.75rem;

  color: #666;
  font-size: 0.9rem;
}

.section-card button {
  padding: 0.7rem 1.2rem;
  border: 0;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}
</style>