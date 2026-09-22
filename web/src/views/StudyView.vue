<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'

import { createStudySession } from '../services/api'

import type {
  QuestionType,
  StudySession,
} from '../types/study'

const route = useRoute()

const sectionId = Number(route.params.sectionId)

const questionCount = ref(5)

const selectedTypes = ref<QuestionType[]>([
  'MULTIPLE_CHOICE',
  'TRUE_FALSE',
  'SHORT_ANSWER',
])

const session = ref<StudySession | null>(null)

const loading = ref(false)
const error = ref<string | null>(null)

const currentQuestionIndex = ref(0)

const currentQuestion = computed(() => {
  return session.value?.questions[
    currentQuestionIndex.value
  ] ?? null
})

async function startSession() {
  if (selectedTypes.value.length === 0) {
    error.value =
      'Choisis au moins un type de question.'
    return
  }

  loading.value = true
  error.value = null

  try {
    session.value = await createStudySession(
      sectionId,
      {
        questionCount: questionCount.value,
        questionTypes: selectedTypes.value,
      },
    )

    currentQuestionIndex.value = 0
  } catch {
    error.value =
      'Impossible de créer la session.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="study-view">
    <RouterLink
      to="/courses/1"
      class="back-link"
    >
      ← Retour au cours
    </RouterLink>

    <!-- CONFIGURATION -->

    <section
      v-if="!session"
      class="setup-card"
    >
      <p class="eyebrow">
        Session d'étude
      </p>

      <h1>Comment veux-tu réviser ?</h1>

      <div class="field">
        <label for="question-count">
          Nombre de questions
        </label>

        <select
          id="question-count"
          v-model.number="questionCount"
        >
          <option :value="5">
            5 questions
          </option>

          <option :value="10">
            10 questions
          </option>

          <option :value="15">
            15 questions
          </option>
        </select>
      </div>

      <fieldset>
        <legend>
          Types de questions
        </legend>

        <label>
          <input
            v-model="selectedTypes"
            type="checkbox"
            value="MULTIPLE_CHOICE"
          >
          Choix multiples
        </label>

        <label>
          <input
            v-model="selectedTypes"
            type="checkbox"
            value="TRUE_FALSE"
          >
          Vrai ou faux
        </label>

        <label>
          <input
            v-model="selectedTypes"
            type="checkbox"
            value="SHORT_ANSWER"
          >
          Réponse courte
        </label>
      </fieldset>

      <p
        v-if="error"
        class="error"
      >
        {{ error }}
      </p>

      <button
        type="button"
        :disabled="loading"
        @click="startSession"
      >
        {{
          loading
            ? 'Préparation...'
            : 'Commencer la session'
        }}
      </button>
    </section>

    <!-- SESSION CRÉÉE -->

    <section
      v-else-if="currentQuestion"
      class="question-card"
    >
      <div class="question-header">
        <span>
          Question
          {{ currentQuestionIndex + 1 }}
          /
          {{ session.questionCount }}
        </span>

        <span>
          {{ currentQuestion.type }}
        </span>
      </div>

      <progress
        :value="currentQuestionIndex + 1"
        :max="session.questionCount"
      />

      <h1>
        {{ currentQuestion.question }}
      </h1>

      <!-- QCM / VRAI-FAUX -->

      <div
        v-if="
          currentQuestion.type ===
            'MULTIPLE_CHOICE' ||
          currentQuestion.type ===
            'TRUE_FALSE'
        "
        class="options"
      >
        <label
          v-for="(option, index) in currentQuestion.options"
          :key="index"
          class="option"
        >
          <input
            type="radio"
            name="answer"
            :value="index"
          >

          <span>{{ option }}</span>
        </label>
      </div>

      <!-- RÉPONSE COURTE -->

      <textarea
        v-else-if="
          currentQuestion.type ===
          'SHORT_ANSWER'
        "
        rows="5"
        placeholder="Écris ta réponse..."
      />

      <button
        type="button"
        class="validate-button"
      >
        Valider
      </button>
    </section>
  </main>
</template>

<style scoped>
.study-view {
  width: min(100% - 2rem, 800px);
  margin: 0 auto;
  padding: 3rem 0;
}

.back-link {
  display: inline-block;
  margin-bottom: 2rem;
  text-decoration: none;
}

.setup-card,
.question-card {
  padding: 2rem;
  border: 1px solid #ddd;
  border-radius: 16px;
  background: white;
}

.eyebrow {
  margin: 0;
  font-weight: 700;
}

.field {
  display: grid;
  gap: 0.5rem;
  margin: 2rem 0;
}

select {
  width: 100%;
  padding: 0.75rem;
}

fieldset {
  display: grid;
  gap: 0.75rem;

  margin: 0 0 2rem;
  padding: 1rem;

  border: 1px solid #ddd;
  border-radius: 8px;
}

button {
  padding: 0.8rem 1.25rem;
  border: 0;
  border-radius: 8px;

  cursor: pointer;
  font-weight: 600;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.error {
  color: #b42318;
}

.question-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

progress {
  width: 100%;
}

.question-card h1 {
  margin: 2rem 0;
}

.options {
  display: grid;
  gap: 0.75rem;
}

.option {
  display: flex;
  align-items: center;
  gap: 0.75rem;

  padding: 1rem;

  border: 1px solid #ddd;
  border-radius: 8px;

  cursor: pointer;
}

textarea {
  width: 100%;
  padding: 1rem;
  resize: vertical;
}

.validate-button {
  margin-top: 2rem;
}
</style>