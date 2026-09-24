<script setup lang="ts">
import {
  computed,
  ref,
} from 'vue'

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

const courseId = computed(() =>
  Number(route.params.courseId),
)

const sectionId = computed(() =>
  Number(route.params.sectionId),
)

/*
 * CONFIGURATION
 */

const questionCount = ref(5)

const selectedTypes =
  ref<QuestionType[]>([
    'MULTIPLE_CHOICE',
    'TRUE_FALSE',
    'SHORT_ANSWER',
  ])

/*
 * SESSION
 */

const session =
  ref<StudySession | null>(null)

const currentQuestionIndex = ref(0)

const currentQuestion = computed(() =>
  session.value?.questions[
    currentQuestionIndex.value
  ] ?? null,
)

/*
 * RÉPONSE DE L'UTILISATEUR
 */

const selectedOptionIndex =
  ref<number | null>(null)

const textAnswer = ref('')

const answerResult =
  ref<AnswerResult | null>(null)

/*
 * RÉSUMÉ
 */

const sessionStatus =
  ref<StudySessionStatus | null>(null)

const loadingSummary = ref(false)

/*
 * ÉTATS UI
 */

const loading = ref(false)

const submitting = ref(false)

const error =
  ref<string | null>(null)

const answerError = ref('')

/*
 * SESSION
 */

async function startSession() {
  if (selectedTypes.value.length === 0) {
    error.value =
      'Choisis au moins un type de question.'

    return
  }

  loading.value = true
  error.value = null

  /*
   * Important si l'utilisateur recommence
   * une nouvelle session.
   */
  sessionStatus.value = null

  try {
    session.value =
      await createStudySession(
        sectionId.value,
        {
          questionCount:
            questionCount.value,

          questionTypes:
            selectedTypes.value,
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
 * VALIDATION
 */

async function validateAnswer() {
  if (
    !session.value ||
    !currentQuestion.value
  ) {
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
    answerError.value =
      'Choisis une réponse.'

    return
  }

  /*
   * TRUE / FALSE
   */
  if (
    currentQuestion.value.type ===
      'TRUE_FALSE' &&
    !textAnswer.value
  ) {
    answerError.value =
      'Choisis vrai ou faux.'

    return
  }

  /*
   * SHORT ANSWER
   */
  if (
    currentQuestion.value.type ===
      'SHORT_ANSWER' &&
    !textAnswer.value.trim()
  ) {
    answerError.value =
      'Écris une réponse.'

    return
  }

  submitting.value = true

  try {
    answerResult.value =
      await submitAnswer(
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

    /*
     * Après la dernière réponse,
     * récupère immédiatement le résumé.
     */
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
 * RÉSUMÉ
 */

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
 * NAVIGATION ENTRE QUESTIONS
 */

function resetAnswer() {
  selectedOptionIndex.value = null
  textAnswer.value = ''
  answerResult.value = null
  answerError.value = ''
}

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

function restartSession() {
  session.value = null
  sessionStatus.value = null
  currentQuestionIndex.value = 0

  resetAnswer()

  error.value = null
}

/*
 * COMPUTED
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

function questionTypeLabel(
  type: QuestionType,
) {
  switch (type) {
    case 'MULTIPLE_CHOICE':
      return 'Choix multiple'

    case 'TRUE_FALSE':
      return 'Vrai ou faux'

    case 'SHORT_ANSWER':
      return 'Réponse courte'
  }
}
</script>

<template>
  <main class="study-view">
    <RouterLink
      :to="`/courses/${courseId}`"
      class="back-link"
    >
      ← Retour au cours
    </RouterLink>

    <!-- CONFIGURATION -->

    <section
      v-if="!session"
      class="study-setup"
    >
      <p class="eyebrow">
        Session d'étude
      </p>

      <h1>Commencer à réviser</h1>

      <p>
        Choisis le nombre et les types
        de questions que tu veux pratiquer.
      </p>

      <div class="setup-section">
        <h2>
          Nombre de questions
        </h2>

        <div class="choice-row">
          <label>
            <input
              v-model="questionCount"
              type="radio"
              :value="5"
            >
            5
          </label>

          <label>
            <input
              v-model="questionCount"
              type="radio"
              :value="10"
            >
            10
          </label>

          <label>
            <input
              v-model="questionCount"
              type="radio"
              :value="15"
            >
            15
          </label>
        </div>
      </div>

      <div class="setup-section">
        <h2>
          Types de questions
        </h2>

        <div class="type-options">
          <label>
            <input
              v-model="selectedTypes"
              type="checkbox"
              value="MULTIPLE_CHOICE"
            >
            Choix multiple
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
        </div>
      </div>

      <p
        v-if="error"
        class="error"
      >
        {{ error }}
      </p>

      <button
        type="button"
        class="primary-button"
        :disabled="loading"
        @click="startSession"
      >
        {{
          loading
            ? 'Génération des questions...'
            : 'Commencer'
        }}
      </button>
    </section>

    <!-- SESSION -->

    <template
      v-else-if="currentQuestion"
    >
      <header class="session-header">
        <div>
          <p class="eyebrow">
            Session d'étude
          </p>

          <h1>
            Question
            {{ currentQuestionIndex + 1 }}
            /
            {{ session.questionCount }}
          </h1>
        </div>

        <span class="question-type">
          {{
            questionTypeLabel(
              currentQuestion.type,
            )
          }}
        </span>
      </header>

      <!-- PROGRESSION -->

      <div class="progress-track">
        <div
          class="progress-value"
          :style="{
            width:
              `${
                ((currentQuestionIndex + 1) /
                  session.questionCount) *
                100
              }%`,
          }"
        />
      </div>

      <!-- QUESTION -->

      <section class="question-card">
        <h2>
          {{ currentQuestion.question }}
        </h2>

        <!-- MULTIPLE CHOICE -->

        <div
          v-if="
            currentQuestion.type ===
            'MULTIPLE_CHOICE'
          "
          class="options"
        >
          <label
            v-for="(
              option,
              index
            ) in currentQuestion.options"
            :key="index"
            class="option"
            :class="{
              selected:
                selectedOptionIndex ===
                index,
            }"
          >
            <input
              v-model="selectedOptionIndex"
              type="radio"
              :value="index"
              :disabled="
                answerResult !== null
              "
            >

            <span>
              {{ option }}
            </span>
          </label>
        </div>

        <!-- TRUE / FALSE -->

        <div
          v-else-if="
            currentQuestion.type ===
            'TRUE_FALSE'
          "
          class="options"
        >
          <label
            class="option"
            :class="{
              selected:
                textAnswer === 'True',
            }"
          >
            <input
              v-model="textAnswer"
              type="radio"
              value="True"
              :disabled="
                answerResult !== null
              "
            >

            <span>Vrai</span>
          </label>

          <label
            class="option"
            :class="{
              selected:
                textAnswer === 'False',
            }"
          >
            <input
              v-model="textAnswer"
              type="radio"
              value="False"
              :disabled="
                answerResult !== null
              "
            >

            <span>Faux</span>
          </label>
        </div>

        <!-- SHORT ANSWER -->

        <textarea
          v-else-if="
            currentQuestion.type ===
            'SHORT_ANSWER'
          "
          v-model="textAnswer"
          class="answer-input"
          rows="6"
          placeholder="Écris ta réponse..."
          :disabled="
            answerResult !== null
          "
        />

        <p
          v-if="answerError"
          class="error"
        >
          {{ answerError }}
        </p>

        <button
          v-if="!answerResult"
          type="button"
          class="primary-button"
          :disabled="submitting"
          @click="validateAnswer"
        >
          {{
            submitting
              ? 'Validation...'
              : 'Valider ma réponse'
          }}
        </button>

        <!-- FEEDBACK -->

        <section
          v-if="answerResult"
          class="feedback"
          :class="{
            correct:
              answerResult.correct,

            incorrect:
              !answerResult.correct,
          }"
        >
          <div class="feedback-header">
            <h3>
              {{
                answerResult.correct
                  ? '✓ Bonne réponse'
                  : '✗ Réponse à améliorer'
              }}
            </h3>

            <span class="score">
              {{ answerResult.score }} %
            </span>
          </div>

          <p
            v-if="answerResult.feedback"
            class="feedback-summary"
          >
            {{ answerResult.feedback }}
          </p>

          <!-- CE QUI EST BIEN COMPRIS -->

          <div
            v-if="
              answerResult.correctConcepts
                ?.length
            "
            class="feedback-section"
          >
            <h4>
              ✓ Ce que tu as bien compris
            </h4>

            <ul>
              <li
                v-for="
                  item in
                  answerResult.correctConcepts
                "
                :key="item"
              >
                {{ item }}
              </li>
            </ul>
          </div>

          <!-- CE QUI MANQUE -->

          <div
            v-if="
              answerResult.missingConcepts
                ?.length
            "
            class="feedback-section"
          >
            <h4>
              ⚠ Ce qu'il manquait
            </h4>

            <ul>
              <li
                v-for="
                  item in
                  answerResult.missingConcepts
                "
                :key="item"
              >
                {{ item }}
              </li>
            </ul>
          </div>

          <!-- CE QUI EST INCORRECT -->

          <div
            v-if="
              answerResult.incorrectConcepts
                ?.length
            "
            class="feedback-section"
          >
            <h4>
              ✗ Ce qui est à corriger
            </h4>

            <ul>
              <li
                v-for="
                  item in
                  answerResult.incorrectConcepts
                "
                :key="item"
              >
                {{ item }}
              </li>
            </ul>
          </div>

          <!-- EXPLICATION -->

          <div
            v-if="
              answerResult.explanation
            "
            class="feedback-section"
          >
            <h4>
              💡 Explication
            </h4>

            <p>
              {{
                answerResult.explanation
              }}
            </p>
          </div>

          <!-- COMMENT AMÉLIORER -->

          <div
            v-if="
              answerResult.howToImprove
            "
            class="feedback-section"
          >
            <h4>
              📝 Comment améliorer ta réponse
            </h4>

            <p>
              {{
                answerResult.howToImprove
              }}
            </p>
          </div>

          <!-- RÉPONSE COMPLÈTE -->

          <div
            v-if="
              answerResult.expectedAnswer
            "
            class="
              feedback-section
              expected-answer
            "
          >
            <h4>
              ✨ Exemple de réponse complète
            </h4>

            <p>
              {{
                answerResult.expectedAnswer
              }}
            </p>
          </div>
        </section>

        <!-- NEXT -->

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
      </section>

      <!-- RÉSUMÉ -->

      <section
        v-if="
          answerResult &&
          isLastQuestion
        "
        class="session-finished"
      >
        <p v-if="loadingSummary">
          Chargement du résumé...
        </p>

        <template
          v-else-if="sessionStatus"
        >
          <p class="summary-eyebrow">
            Session terminée
          </p>

          <h2>
            🎉 Résumé de ta session
          </h2>

          <div class="summary-score">
            {{
              Math.round(
                sessionStatus.averageScore,
              )
            }}
            %
          </div>

          <p class="summary-label">
            Score moyen
          </p>

          <div class="summary-stats">
            <div class="summary-stat">
              <strong>
                {{
                  sessionStatus
                    .answeredQuestions
                }}
                /
                {{
                  sessionStatus
                    .totalQuestions
                }}
              </strong>

              <span>
                Questions répondues
              </span>
            </div>

            <div class="summary-stat">
              <strong>
                {{
                  sessionStatus
                    .correctAnswers
                }}
              </strong>

              <span>
                Bonnes réponses
              </span>
            </div>

            <div class="summary-stat">
              <strong>
                {{
                  sessionStatus
                    .incorrectAnswers
                }}
              </strong>

              <span>
                Réponses incorrectes
              </span>
            </div>
          </div>

          <div class="summary-actions">
            <button
              type="button"
              class="secondary-button"
              @click="restartSession"
            >
              Refaire une session
            </button>

            <RouterLink
              :to="`/courses/${courseId}`"
              class="return-button"
            >
              Retour au cours
            </RouterLink>
          </div>
        </template>
      </section>
    </template>
  </main>
</template>

<style scoped>
.study-view {
  width: min(100% - 2rem, 850px);
  margin: 0 auto;
  padding: 3rem 0;
}

.back-link {
  display: inline-block;
  margin-bottom: 2rem;
  text-decoration: none;
}

.eyebrow,
.summary-eyebrow {
  margin: 0;
  font-size: 0.9rem;
  font-weight: 700;
  color: #666;
}

.study-setup,
.question-card {
  padding: 2rem;
  border: 1px solid #ddd;
  border-radius: 16px;
  background: white;
}

.study-setup h1 {
  margin-bottom: 0.5rem;
}

.setup-section {
  margin: 2rem 0;
}

.setup-section h2 {
  margin-bottom: 1rem;
  font-size: 1rem;
}

.choice-row,
.type-options {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.choice-row label,
.type-options label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.session-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1rem;
}

.session-header h1 {
  margin: 0.25rem 0 0;
}

.question-type {
  padding: 0.5rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 999px;
  font-size: 0.85rem;
}

.progress-track {
  width: 100%;
  height: 8px;
  margin-bottom: 2rem;
  overflow: hidden;
  border-radius: 999px;
  background: #eee;
}

.progress-value {
  height: 100%;
  background: #222;
  transition: width 0.2s ease;
}

.question-card h2 {
  margin-top: 0;
  line-height: 1.4;
}

.options {
  display: grid;
  gap: 0.75rem;
  margin: 1.5rem 0;
}

.option {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem;
  border: 1px solid #ddd;
  border-radius: 10px;
  cursor: pointer;
}

.option.selected {
  border-color: #222;
}

.option input {
  margin-top: 0.2rem;
}

.answer-input {
  box-sizing: border-box;
  width: 100%;
  margin: 1.5rem 0;
  padding: 1rem;
  border: 1px solid #ddd;
  border-radius: 10px;
  resize: vertical;
  font: inherit;
}

button,
.return-button {
  padding: 0.8rem 1.25rem;
  border-radius: 8px;
  font-weight: 600;
}

button {
  border: 0;
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.primary-button {
  background: #222;
  color: white;
}

.secondary-button {
  border: 1px solid #ddd;
  background: white;
}

.next-button {
  margin-top: 1rem;
}

/*
 * FEEDBACK
 */

.feedback {
  display: grid;
  gap: 1rem;
  margin-top: 1.5rem;
  padding: 1.5rem;
  border: 1px solid #ddd;
  border-radius: 12px;
}

.feedback.correct {
  border-color: #8abf9a;
}

.feedback.incorrect {
  border-color: #d49a9a;
}

.feedback-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.feedback-header h3 {
  margin: 0;
}

.score {
  flex-shrink: 0;
  font-weight: 700;
}

.feedback-summary {
  margin: 0;
  line-height: 1.6;
}

.feedback-section {
  padding-top: 1rem;
  border-top: 1px solid #eee;
}

.feedback-section h4 {
  margin: 0 0 0.75rem;
}

.feedback-section p {
  margin: 0;
  line-height: 1.6;
}

.feedback-section ul {
  margin: 0;
  padding-left: 1.25rem;
}

.feedback-section li {
  margin-bottom: 0.4rem;
  line-height: 1.5;
}

.feedback-section li:last-child {
  margin-bottom: 0;
}

.expected-answer {
  padding: 1rem;
  border: 1px solid #ddd;
  border-radius: 10px;
}

.error {
  color: #b42318;
}

/*
 * SESSION SUMMARY
 */

.session-finished {
  margin-top: 2rem;
  padding: 2rem;
  text-align: center;
  border: 1px solid #ddd;
  border-radius: 16px;
  background: white;
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
  grid-template-columns:
    repeat(3, 1fr);
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

.summary-actions {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.return-button {
  display: inline-block;
  border: 1px solid #ddd;
  text-decoration: none;
}

@media (max-width: 600px) {
  .summary-stats {
    grid-template-columns: 1fr;
  }

  .session-header {
    flex-direction: column;
  }

  .feedback-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>