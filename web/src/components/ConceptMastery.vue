<script setup lang="ts">
import { computed } from 'vue'
import type { ConceptProgress } from '../types/study'

const props = defineProps<{
  progress?: ConceptProgress
  loading: boolean
  error: boolean
}>()

const studied = computed(() => (props.progress?.attempts ?? 0) > 0)
const score = computed(() => Math.min(100, Math.max(0, props.progress?.masteryScore ?? 0)))
const formattedScore = computed(() => new Intl.NumberFormat('fr-CA', {
  maximumFractionDigits: 1,
}).format(score.value))
const level = computed(() => {
  if (!studied.value) return { label: 'Pas encore étudié', tone: 'new' }
  if (score.value >= 85) return { label: 'Maîtrise élevée', tone: 'high' }
  if (score.value >= 50) return { label: 'En progression', tone: 'medium' }
  return { label: 'À retravailler', tone: 'low' }
})
</script>

<template>
  <div class="concept-mastery" :class="level.tone" :aria-busy="loading">
    <p v-if="loading" class="mastery-status">Chargement de la maîtrise…</p>
    <p v-else-if="error" class="mastery-status">Maîtrise indisponible</p>
    <template v-else>
      <div class="mastery-header">
        <div class="mastery-label"><span>Maîtrise</span><span class="level">{{ level.label }}</span></div>
        <strong v-if="studied">{{ formattedScore }} %</strong>
        <span v-else class="mastery-status">À découvrir</span>
      </div>
      <div v-if="studied" class="mastery-track" role="meter"
        :aria-label="'Maîtrise du concept ' + (progress?.concept ?? '')"
        :aria-valuenow="score" aria-valuemin="0" aria-valuemax="100"
        :aria-valuetext="formattedScore + ' %, ' + level.label">
        <div class="mastery-fill" :style="{ width: score + '%' }" />
      </div>
      <p v-if="studied" class="mastery-detail">
        {{ progress?.attempts }} réponse{{ (progress?.attempts ?? 0) > 1 ? 's' : '' }}
        · {{ progress?.correctAnswers }} réussie{{ (progress?.correctAnswers ?? 0) > 1 ? 's' : '' }}
      </p>
      <p v-else class="mastery-detail">Étudie ce concept pour découvrir ton niveau.</p>
    </template>
  </div>
</template>

<style scoped>
.concept-mastery { --mastery-color: #6d687f; padding-top: 1.1rem; margin-top: 1.25rem; border-top: 1px solid var(--line, #e5e1ee); }
.high { --mastery-color: #287356; }
.medium { --mastery-color: #886019; }
.low { --mastery-color: #ac3944; }
.mastery-header, .mastery-label { display: flex; align-items: center; gap: .7rem; }
.mastery-header { justify-content: space-between; }
.mastery-label { flex-wrap: wrap; font-size: .8rem; color: var(--muted, #6d687f); }
.mastery-header strong { color: var(--mastery-color); font-size: 1rem; white-space: nowrap; }
.level { color: var(--mastery-color); font-size: .75rem; font-weight: 600; }
.mastery-track { height: 6px; overflow: hidden; border-radius: 6px; background: #eeecf3; margin-top: .8rem; }
.mastery-fill { height: 100%; background: var(--mastery-color); border-radius: inherit; }
.mastery-detail, .mastery-status { color: var(--muted, #6d687f); font-size: .75rem; line-height: 1.6; margin: .5rem 0 0; }
.mastery-header .mastery-status { margin: 0; white-space: nowrap; }
</style>
