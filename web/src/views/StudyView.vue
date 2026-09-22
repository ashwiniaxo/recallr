<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'

import {
  createStudySession,
  getStudySessionStatus,
  submitAnswer,
} from '../services/api'

import type {
  AnswerResult,
  QuestionType,
  StudySession,
  StudySessionStatus,
} from '../types/study'

const route = useRoute()

const sectionId = Number(route.params.sectionId)

/*
 * Configuration de la session
 */
const questionCount = ref(5)

const selectedTypes = ref<QuestionType[]>([
  'MULTIPLE_CHOICE',
  'TRUE_FALSE',
  'SHORT_ANSWER',
])

/*
 * Session
 */
const session = ref<StudySession | null>(null)

const currentQuestionIndex = ref(0)

const currentQuestion = computed(() => {
  return (
    session.value?.questions[
      currentQuestionIndex.value
    ] ?? null
  )
})

/*
 * Réponse de l'utilisateur
 */
const selectedOptionIndex = ref<number | null>(null)

const textAnswer = ref('')

/*
 * Résultat de la correction
 */
const answerResult = ref<AnswerResult | null>(null)
const sessionStatus =
  ref<StudySessionStatus | null>(null)

const loadingSummary = ref(false)
const submitting = ref(false)

const answerError = ref('')

/*
 * États généraux
 */
const loading = ref(false)

const error = ref<string | null>(null)

/*
 * Création de la session
 */
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

    resetAnswer()
  } catch (err) {
    console.error(err)

    error.value =
      'Impossible de créer la session.'
  } finally {
    loading.value = false
  }
}

/*
 * Validation d'une réponse
 */
async function validateAnswer() {
  if (!session.value || !currentQuestion.value) {
    return
  }

  answerError.value = ''

  /*
   * QCM
   */
  if (
    currentQuestion.value.type ===
      'MULTIPLE_CHOICE' &&
    selectedOptionIndex.value === null
  ) {
    answerError.value = 'Choisis une réponse.'

    return
  }

  /*
   * Vrai / Faux
   */
  if (
    currentQuestion.value.type ===
      'TRUE_FALSE' &&
    !textAnswer.value
  ) {
    answerError.value = 'Choisis vrai ou faux.'

    return
  }

  /*
   * Réponse courte
   */
  if (
    currentQuestion.value.type ===
      'SHORT_ANSWER' &&
    !textAnswer.value.trim()
  ) {
    answerError.value = 'Écris une réponse.'

    return
  }

  submitting.value = true

  try {
    answerResult.value = await submitAnswer(
      session.value.sessionId,
      currentQuestion.value.questionId,
      {
        selectedOptionIndex:
          currentQuestion.value.type ===
          'MULTIPLE_CHOICE'
            ? selectedOptionIndex.value
            : null,

        answer:
          currentQuestion.value.type ===
          'MULTIPLE_CHOICE'
            ? null
            : textAnswer.value,
      },
    )
    if (isLastQuestion.value) {
        await loadSessionSummary()
    }
  } catch (err) {
    console.error(err)

    answerError.value =
      'Impossible de valider la réponse.'
  } finally {
    submitting.value = false
  }
}

/*
 * Réinitialise les champs de réponse
 */
function resetAnswer() {
  selectedOptionIndex.value = null
  textAnswer.value = ''
  answerResult.value = null
  answerError.value = ''
}

async function loadSessionSummary() {
  if (!session.value) {
    return
  }

  loadingSummary.value = true

  try {
    sessionStatus.value =
      await getStudySessionStatus(
        session.value.sessionId,
      )
  } catch (err) {
    console.error(err)

    answerError.value =
      'Impossible de charger le résumé.'
  } finally {
    loadingSummary.value = false
  }
}
/*
 * Question suivante
 */
function nextQuestion() {
  if (!session.value) {
    return
  }

  if (
    currentQuestionIndex.value <
    session.value.questions.length - 1
  ) {
    currentQuestionIndex.value++

    resetAnswer()
  }
}

/*
 * Permet de savoir si on est à la dernière question
 */
const isLastQuestion = computed(() => {
  if (!session.value) {
    return false
  }

  return (
    currentQuestionIndex.value ===
    session.value.questions.length - 1
  )
})
</script>

<template>
  <main class="study-view">
    <RouterLink
      to="/courses/1"
      class="back-link"
    >
      ← Retour au cours
    </RouterLink>

    <!-- ============================= -->
    <!-- CONFIGURATION DE LA SESSION -->
    <!-- ============================= -->

    <section
      v-if="!session"
      class="setup-card"
    >
      <p class="eyebrow">
        Session d'étude
      </p>

      <h1>
        Comment veux-tu réviser ?
      </h1>

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

    <!-- ============================= -->
    <!-- SESSION -->
    <!-- ============================= -->

    <section
      v-else-if="currentQuestion"
      class="question-card"
    >
      <!-- HEADER -->

      <div class="question-header">
        <span>
          Question
          {{ currentQuestionIndex + 1 }}
          /
          {{ session.questionCount }}
        </span>

        <span class="question-type">
          {{
            currentQuestion.type ===
            'MULTIPLE_CHOICE'
              ? 'Choix multiples'
              : currentQuestion.type ===
                  'TRUE_FALSE'
                ? 'Vrai ou faux'
                : 'Réponse courte'
          }}
        </span>
      </div>

      <!-- PROGRESSION -->

      <progress
        :value="currentQuestionIndex + 1"
        :max="session.questionCount"
      />

      <!-- QUESTION -->

      <h1>
        {{ currentQuestion.question }}
      </h1>

      <!-- ========================= -->
      <!-- CHOIX MULTIPLES -->
      <!-- ========================= -->

      <div
        v-if="
          currentQuestion.type ===
          'MULTIPLE_CHOICE'
        "
        class="options"
      >
        <label
          v-for="(option, index) in
            currentQuestion.options"
          :key="index"
          class="option"
          :class="{
            selected:
              selectedOptionIndex === index,
          }"
        >
          <input
            v-model="selectedOptionIndex"
            type="radio"
            name="multiple-choice-answer"
            :value="index"
            :disabled="answerResult !== null"
          >

          <span>
            {{ option }}
          </span>
        </label>
      </div>

      <!-- ========================= -->
      <!-- VRAI / FAUX -->
      <!-- ========================= -->

      <div
        v-else-if="
          currentQuestion.type ===
          'TRUE_FALSE'
        "
        class="options true-false-options"
      >
        <label
          class="option"
          :class="{
            selected: textAnswer === 'True',
          }"
        >
          <input
            v-model="textAnswer"
            type="radio"
            name="true-false-answer"
            value="True"
            :disabled="answerResult !== null"
          >

          <span>
            Vrai
          </span>
        </label>

        <label
          class="option"
          :class="{
            selected: textAnswer === 'False',
          }"
        >
          <input
            v-model="textAnswer"
            type="radio"
            name="true-false-answer"
            value="False"
            :disabled="answerResult !== null"
          >

          <span>
            Faux
          </span>
        </label>
      </div>

      <!-- ========================= -->
      <!-- RÉPONSE COURTE -->
      <!-- ========================= -->

      <textarea
        v-else-if="
          currentQuestion.type ===
          'SHORT_ANSWER'
        "
        v-model="textAnswer"
        rows="5"
        placeholder="Écris ta réponse..."
        :disabled="answerResult !== null"
      />

      <!-- ERREUR -->

      <p
        v-if="answerError"
        class="error answer-error"
      >
        {{ answerError }}
      </p>

      <!-- ========================= -->
      <!-- VALIDER -->
      <!-- ========================= -->

      <button
        v-if="!answerResult"
        type="button"
        class="validate-button"
        :disabled="submitting"
        @click="validateAnswer"
      >
        {{
          submitting
            ? 'Validation...'
            : 'Valider'
        }}
      </button>

      <!-- ========================= -->
      <!-- FEEDBACK -->
      <!-- ========================= -->

      <div
        v-if="answerResult"
        class="answer-feedback"
        :class="{
          correct: answerResult.correct,
          incorrect: !answerResult.correct,
        }"
      >
        <h2>
          {{
            answerResult.correct
              ? '✓ Bonne réponse'
              : 'Réponse à améliorer'
          }}
        </h2>

        <p class="score">
          Score :
          <strong>
            {{ answerResult.score }} %
          </strong>
        </p>

        <p v-if="answerResult.feedback">
          {{ answerResult.feedback }}
        </p>

        <!-- CONCEPTS CORRECTS -->

        <div
          v-if="
            answerResult.correctConcepts &&
            answerResult.correctConcepts.length
          "
          class="feedback-section"
        >
          <strong>
            Concepts compris
          </strong>

          <ul>
            <li
              v-for="concept in
                answerResult.correctConcepts"
              :key="concept"
            >
              {{ concept }}
            </li>
          </ul>
        </div>

        <!-- CONCEPTS MANQUANTS -->

        <div
          v-if="
            answerResult.missingConcepts &&
            answerResult.missingConcepts.length
          "
          class="feedback-section"
        >
          <strong>
            Concepts à améliorer
          </strong>

          <ul>
            <li
              v-for="concept in
                answerResult.missingConcepts"
              :key="concept"
            >
              {{ concept }}
            </li>
          </ul>
        </div>

        <!-- EXPLICATION -->

        <div
          v-if="answerResult.explanation"
          class="feedback-section"
        >
          <strong>
            Explication
          </strong>

          <p>
            {{ answerResult.explanation }}
          </p>
        </div>

        <!-- RÉPONSE ATTENDUE -->

        <div
          v-if="answerResult.expectedAnswer"
          class="feedback-section"
        >
          <strong>
            Réponse attendue
          </strong>

          <p>
            {{ answerResult.expectedAnswer }}
          </p>
        </div>
      </div>

      <!-- ========================= -->
      <!-- QUESTION SUIVANTE -->
      <!-- ========================= -->

      <button
        v-if="
          answerResult &&
          !isLastQuestion
        "
        type="button"
        class="next-button"
        @click="nextQuestion"
      >
        Question suivante →
      </button>

      <!-- ========================= -->
      <!-- DERNIÈRE QUESTION -->
      <!-- ========================= -->

      <div
  v-if="answerResult && isLastQuestion"
  class="session-finished"
>
  <p v-if="loadingSummary">
    Chargement du résumé...
  </p>

  <template v-else-if="sessionStatus">
    <p class="summary-eyebrow">
      Session terminée
    </p>

    <h2>
      🎉 Résumé de ta session
    </h2>

    <div class="summary-score">
      {{ Math.round(sessionStatus.averageScore) }} %
    </div>

    <p class="summary-label">
      Score moyen
    </p>

    <div class="summary-stats">
      <div class="summary-stat">
        <strong>
          {{ sessionStatus.answeredQuestions }}
          /
          {{ sessionStatus.totalQuestions }}
        </strong>

        <span>
          Questions répondues
        </span>
      </div>

      <div class="summary-stat">
        <strong>
          {{ sessionStatus.correctAnswers }}
        </strong>

        <span>
          Bonnes réponses
        </span>
      </div>

      <div class="summary-stat">
        <strong>
          {{ sessionStatus.incorrectAnswers }}
        </strong>

        <span>
          Réponses incorrectes
        </span>
      </div>
    </div>

    <RouterLink
      to="/courses/1"
      class="return-button"
    >
      Retour au cours
    </RouterLink>
  </template>
</div>
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
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.question-type {
  color: #666;
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

.true-false-options {
  grid-template-columns: repeat(2, 1fr);
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

.option:hover {
  border-color: #999;
}

.option.selected {
  border-color: #333;
}

.option:has(input:disabled) {
  cursor: default;
}

textarea {
  width: 100%;
  box-sizing: border-box;

  padding: 1rem;

  resize: vertical;

  font: inherit;

  border: 1px solid #ddd;
  border-radius: 8px;
}

.validate-button {
  margin-top: 2rem;
}

.answer-error {
  margin-top: 1rem;
}

.answer-feedback {
  margin-top: 2rem;
  padding: 1.5rem;

  border: 1px solid #ddd;
  border-radius: 12px;
}

.answer-feedback h2 {
  margin-top: 0;
}

.answer-feedback.correct {
  border-color: #75b798;
}

.answer-feedback.incorrect {
  border-color: #e3a6a1;
}

.score {
  font-size: 1.1rem;
}

.feedback-section {
  margin-top: 1.25rem;
}

.feedback-section p {
  margin-bottom: 0;
  line-height: 1.6;
}

.feedback-section ul {
  margin-bottom: 0;
}

.next-button {
  margin-top: 1.5rem;
}

.session-finished {
  margin-top: 2rem;
  padding: 1.5rem;

  text-align: center;

  border: 1px solid #ddd;
  border-radius: 12px;
}

@media (max-width: 600px) {
  .question-header {
    flex-direction: column;
  }

  .true-false-options {
    grid-template-columns: 1fr;
  }
}
.session-finished {
  margin-top: 2rem;
  padding: 2rem;

  text-align: center;

  border: 1px solid #ddd;
  border-radius: 16px;
}

.summary-eyebrow {
  margin: 0;

  font-size: 0.9rem;
  font-weight: 600;
  color: #666;
}

.session-finished h2 {
  margin: 0.5rem 0 1.5rem;
}

.summary-score {
  font-size: 3rem;
  font-weight: 700;
}

.summary-label {
  margin-top: 0.25rem;
  color: #666;
}

.summary-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);

  gap: 1rem;

  margin: 2rem 0;
}

.summary-stat {
  display: grid;
  gap: 0.4rem;

  padding: 1rem;

  border: 1px solid #ddd;
  border-radius: 10px;
}

.summary-stat strong {
  font-size: 1.4rem;
}

.summary-stat span {
  font-size: 0.85rem;
  color: #666;
}

.return-button {
  display: inline-block;

  padding: 0.8rem 1.25rem;

  border-radius: 8px;

  text-decoration: none;
  font-weight: 600;

  border: 1px solid #ddd;
}

@media (max-width: 600px) {
  .summary-stats {
    grid-template-columns: 1fr;
  }
}
</style>