<script setup lang="ts">
import { computed, ref, watch, nextTick } from 'vue'

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

const courseId = computed(() => Number(route.params.courseId))

const sectionId = computed(() => Number(route.params.sectionId))

/*
 * CONFIGURATION
 */

const questionCount = ref(5)

const selectedTypes = ref<QuestionType[]>([
  'SHORT_ANSWER',
  'CASE_STUDY',
  'ERROR_ANALYSIS',
  'COMPARISON',
])

const questionTypeOptions: {
  value: QuestionType
  label: string
  description: string
}[] = [
  {
    value: 'CASE_STUDY',
    label: 'Mise en situation',
    description:
      'Applique les notions à un cas concret et justifie ta décision.',
  },
  {
    value: 'ERROR_ANALYSIS',
    label: 'Analyse d’erreur',
    description:
      'Repère une erreur de raisonnement, corrige-la et explique pourquoi.',
  },
  {
    value: 'COMPARISON',
    label: 'Comparaison argumentée',
    description:
      'Compare des approches et défends celle qui convient au contexte.',
  },
  {
    value: 'SHORT_ANSWER',
    label: 'Réponse courte',
    description: 'Explique le raisonnement avec tes propres mots.',
  },
  {
    value: 'MULTIPLE_CHOICE',
    label: 'Choix multiple',
    description: 'Distingue la bonne réponse de confusions plausibles.',
  },
  {
    value: 'TRUE_FALSE',
    label: 'Vrai ou faux',
    description: 'Évalue une affirmation et ses implications.',
  },
]

/*
 * SESSION
 */

const session = ref<StudySession | null>(null)

const currentQuestionIndex = ref(0)

const currentQuestion = computed(
  () => session.value?.questions[currentQuestionIndex.value] ?? null,
)

const isWrittenQuestion = computed(
  () =>
    currentQuestion.value !== null &&
    currentQuestion.value.type !== 'MULTIPLE_CHOICE' &&
    currentQuestion.value.type !== 'TRUE_FALSE',
)

const answerGuidance = computed(() => {
  switch (currentQuestion.value?.type) {
    case 'CASE_STUDY':
      return 'Relie les faits du cas aux notions du cours, justifie ta décision et explique les conséquences.'
    case 'ERROR_ANALYSIS':
      return 'Identifie l’erreur précise, propose une correction et explique le raisonnement qui la justifie.'
    case 'COMPARISON':
      return 'Appuie ta comparaison sur des critères précis et justifie ta conclusion dans le contexte donné.'
    default:
      return 'Réponds à chaque partie de la question et explique pourquoi. Une définition seule ne suffit pas si une application est demandée.'
  }
})

/*
 * RÉPONSE DE L'UTILISATEUR
 */

const selectedOptionIndex = ref<number | null>(null)

const textAnswer = ref('')

const answerResult = ref<AnswerResult | null>(null)

/*
 * RÉSUMÉ
 */

const sessionStatus = ref<StudySessionStatus | null>(null)

const loadingSummary = ref(false)

/*
 * ÉTATS UI
 */

const loading = ref(false)

const submitting = ref(false)

const error = ref<string | null>(null)

const answerError = ref('')

/*
 * SESSION
 */

async function startSession() {
  if (loading.value) return
  if (selectedTypes.value.length === 0) {
    error.value = 'Choisis au moins un type de question.'

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
    session.value = await createStudySession(sectionId.value, {
      questionCount: questionCount.value,

      questionTypes: selectedTypes.value,
    })

    currentQuestionIndex.value = 0

    resetAnswer()
  } catch (err) {
    console.error(err)

    error.value = 'Impossible de créer la session.'
  } finally {
    loading.value = false
  }
}

/*
 * VALIDATION
 */

async function validateAnswer() {
  if (
    submitting.value ||
    answerResult.value !== null ||
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
    currentQuestion.value.type === 'MULTIPLE_CHOICE' &&
    selectedOptionIndex.value === null
  ) {
    answerError.value = 'Choisis une réponse.'

    return
  }

  /*
   * TRUE / FALSE
   */
  if (currentQuestion.value.type === 'TRUE_FALSE' && !textAnswer.value) {
    answerError.value = 'Choisis vrai ou faux.'

    return
  }

  /*
   * RÉPONSE RÉDIGÉE
   */
  if (isWrittenQuestion.value && !textAnswer.value.trim()) {
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
          currentQuestion.value.type === 'MULTIPLE_CHOICE'
            ? selectedOptionIndex.value
            : null,

        answer:
          currentQuestion.value.type === 'MULTIPLE_CHOICE'
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

    answerError.value = 'Impossible de valider la réponse.'
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
    sessionStatus.value = await getStudySessionStatus(session.value.sessionId)
  } catch (err) {
    console.error(err)

    answerError.value = 'Impossible de charger le résumé.'
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

  if (currentQuestionIndex.value < session.value.questions.length - 1) {
    currentQuestionIndex.value++

    resetAnswer()
  }
}

async function restartSession() {
  session.value = null
  sessionStatus.value = null
  currentQuestionIndex.value = 0

  resetAnswer()

  error.value = null
  await nextTick()
  setupHeading.value?.focus({ preventScroll: true })
  window.scrollTo({ top: 0, behavior: 'instant' })
}

/*
 * COMPUTED
 */

const isLastQuestion = computed(() => {
  if (!session.value) {
    return false
  }

  return currentQuestionIndex.value === session.value.questions.length - 1
})

function questionTypeLabel(type: QuestionType) {
  return (
    questionTypeOptions.find((option) => option.value === type)?.label ?? type
  )
}

const answerInput = ref<HTMLTextAreaElement | null>(null)
const setupHeading = ref<HTMLElement | null>(null)
const questionPanel = ref<HTMLElement | null>(null)
const feedbackPanel = ref<HTMLElement | null>(null)
const wordCount = computed(
  () => textAnswer.value.trim().split(/\s+/).filter(Boolean).length,
)
const canSubmit = computed(
  () =>
    !submitting.value &&
    !answerResult.value &&
    (currentQuestion.value?.type === 'MULTIPLE_CHOICE'
      ? selectedOptionIndex.value !== null
      : !!textAnswer.value.trim()),
)
const completedCount = computed(
  () => currentQuestionIndex.value + (answerResult.value ? 1 : 0),
)
const questionParts = computed(() => {
  const text = currentQuestion.value?.question ?? ''
  const firstTask = text.search(/(?:^|\n)\s*1[.)]\s+/)
  if (firstTask < 0) return { context: text, tasks: [] as string[] }
  const tasks = [
    ...text
      .slice(firstTask)
      .matchAll(/(?:^|\n)\s*\d+[.)]\s+([\s\S]*?)(?=\n\s*\d+[.)]\s|$)/g),
  ]
    .map((match) => match[1]?.trim() ?? '')
    .filter(Boolean)
  return { context: text.slice(0, firstTask).trim(), tasks }
})
const activePreset = computed(() =>
  selectedTypes.value.length === 6
    ? 'mixed'
    : selectedTypes.value.length === 4 &&
        selectedTypes.value.every(
          (type) => !['MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(type),
        )
      ? 'deep'
      : '',
)
function choosePreset(preset: string) {
  if (loading.value) return
  selectedTypes.value = questionTypeOptions
    .map((option) => option.value)
    .filter(
      (type) =>
        preset === 'mixed' || !['MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(type),
    )
}
function resizeAnswer() {
  if (!answerInput.value) return
  answerInput.value.style.height = 'auto'
  answerInput.value.style.height =
    Math.max(260, answerInput.value.scrollHeight) + 'px'
}
function moveTo(element: HTMLElement | null) {
  element?.scrollIntoView({
    behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches
      ? 'instant'
      : 'smooth',
    block: 'start',
  })
}
watch(textAnswer, () => nextTick(resizeAnswer))
watch(
  () => currentQuestion.value?.questionId,
  async () => {
    await nextTick()
    resizeAnswer()
    answerInput.value?.focus({ preventScroll: true })
    moveTo(questionPanel.value)
  },
)
watch(answerResult, async (result) => {
  if (!result) return
  await nextTick()
  feedbackPanel.value?.focus({ preventScroll: true })
  moveTo(feedbackPanel.value)
})
</script>

<template>
  <main class="study-view">
    <RouterLink :to="'/courses/' + courseId" class="back-link"
      >← Retour au cours</RouterLink
    >
    <section v-if="!session" class="study-setup" :aria-busy="loading">
      <header class="setup-intro">
        <p class="eyebrow">UN MOMENT POUR APPRENDRE</p>
        <h1 ref="setupHeading" tabindex="-1">
          Fais le point.<br /><span>Va plus loin.</span>
        </h1>
        <p>
          Applique tes notions, explique ton raisonnement et découvre ce qui te
          reste à maîtriser.
        </p>
      </header>
      <fieldset class="setup-section" :disabled="loading">
        <legend><span class="section-number">01</span> À ton rythme</legend>
        <div class="choice-row">
          <label
            v-for="count in [5, 10, 15]"
            :key="count"
            :class="{ chosen: questionCount === count }"
          >
            <input
              v-model="questionCount"
              type="radio"
              name="question-count"
              :value="count"
            />
            <strong>{{ count }} <span>questions</span></strong>
            <small>{{
              count === 5
                ? 'Pour se lancer'
                : count === 10
                  ? 'Pour approfondir'
                  : 'Pour s’entraîner'
            }}</small>
          </label>
        </div>
      </fieldset>
      <fieldset class="setup-section" :disabled="loading">
        <legend>
          <span class="section-number">02</span> Ta façon de pratiquer
        </legend>
        <div class="preset-row">
          <button
            type="button"
            :aria-pressed="activePreset === 'deep'"
            @click="choosePreset('deep')"
          >
            Approfondir <small>Appliquer et expliquer</small>
          </button>
          <button
            type="button"
            :aria-pressed="activePreset === 'mixed'"
            @click="choosePreset('mixed')"
          >
            Varier les exercices <small>Tous les formats</small>
          </button>
        </div>
        <details class="format-details">
          <summary>
            Personnaliser les formats
            <span>{{ selectedTypes.length }} sélectionnés</span>
          </summary>
          <div class="type-options">
            <label
              v-for="option in questionTypeOptions"
              :key="option.value"
              :class="{ chosen: selectedTypes.includes(option.value) }"
            >
              <input
                v-model="selectedTypes"
                type="checkbox"
                :value="option.value"
              />
              <span
                ><strong>{{ option.label }}</strong
                ><small>{{ option.description }}</small></span
              >
            </label>
          </div>
        </details>
      </fieldset>
      <p class="study-expectations">
        Vise 85 % avec un raisonnement complet. Chaque réponse partielle t’aide
        à repérer quoi retravailler.
      </p>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <div class="setup-footer">
        <span
          >{{ questionCount }} questions · {{ selectedTypes.length }} formats
          sélectionnés</span
        >
        <button
          type="button"
          class="primary-button"
          :disabled="loading || !selectedTypes.length"
          @click="startSession"
        >
          <span v-if="loading" class="spinner" aria-hidden="true" />{{
            loading ? 'Préparation de ta session…' : 'Commencer la session →'
          }}
        </button>
      </div>
      <p v-if="loading" class="loading-note" role="status">
        Tes questions se préparent. Ta session commencera dès qu’elles seront
        prêtes.
      </p>
    </section>

    <template v-else-if="currentQuestion">
      <header class="session-header">
        <div>
          <p class="eyebrow">SESSION D’ÉTUDE</p>
          <h1>Une question à la fois.</h1>
        </div>
        <span class="question-type">{{
          questionTypeLabel(currentQuestion.type)
        }}</span>
      </header>
      <div class="session-progress">
        <div class="progress-caption">
          <strong
            >Question {{ currentQuestionIndex + 1 }}
            <span>sur {{ session.questionCount }}</span></strong
          ><span
            >{{ completedCount }} terminée{{
              completedCount > 1 ? 's' : ''
            }}</span
          >
        </div>
        <div
          class="progress-track"
          role="progressbar"
          aria-label="Questions terminées"
          :aria-valuenow="completedCount"
          :aria-valuemin="0"
          :aria-valuemax="session.questionCount"
        >
          <div
            class="progress-value"
            :style="{
              width: (completedCount / session.questionCount) * 100 + '%',
            }"
          />
        </div>
      </div>
      <p
        v-if="session.questionCount < questionCount"
        class="session-notice"
        role="status"
      >
        {{ session.questionCount }} questions sur {{ questionCount }} sont
        prêtes. Commence avec celles-ci.
      </p>
      <div class="study-workspace">
        <section
          ref="questionPanel"
          class="question-pane"
          aria-labelledby="question-title"
        >
          <p class="eyebrow">À TOI DE JOUER</p>
          <h2 id="question-title">
            {{
              isWrittenQuestion
                ? 'Lis, réfléchis, puis explique.'
                : 'Quelle est ta réponse ?'
            }}
          </h2>
          <p class="question-context">{{ questionParts.context }}</p>
          <ol v-if="questionParts.tasks.length" class="question-tasks">
            <li v-for="(task, index) in questionParts.tasks" :key="index">
              {{ task }}
            </li>
          </ol>
          <div v-if="isWrittenQuestion" class="guidance-box">
            <strong>Un repère pour répondre</strong>
            <p>{{ answerGuidance }}</p>
          </div>
        </section>
        <section class="response-pane" aria-labelledby="response-title">
          <div class="response-header">
            <h2 id="response-title">Ta réponse</h2>
            <span>{{ answerResult ? 'Réponse envoyée' : 'À ton rythme' }}</span>
          </div>
          <form @submit.prevent="validateAnswer">
            <fieldset
              v-if="currentQuestion.type === 'MULTIPLE_CHOICE'"
              class="options"
              :disabled="submitting || !!answerResult"
            >
              <legend class="sr-only">Choisis une réponse</legend>
              <label
                v-for="(option, index) in currentQuestion.options"
                :key="index"
                class="option"
                :class="{ selected: selectedOptionIndex === index }"
              >
                <input
                  v-model="selectedOptionIndex"
                  type="radio"
                  name="answer-option"
                  :value="index"
                /><span class="option-letter" aria-hidden="true">{{
                  String.fromCharCode(65 + index)
                }}</span
                ><span>{{ option }}</span>
              </label>
            </fieldset>
            <fieldset
              v-else-if="currentQuestion.type === 'TRUE_FALSE'"
              class="options"
              :disabled="submitting || !!answerResult"
            >
              <legend class="sr-only">Choisis vrai ou faux</legend>
              <label
                v-for="choice in [
                  { value: 'True', label: 'Vrai' },
                  { value: 'False', label: 'Faux' },
                ]"
                :key="choice.value"
                class="option"
                :class="{ selected: textAnswer === choice.value }"
              >
                <input
                  v-model="textAnswer"
                  type="radio"
                  name="true-false"
                  :value="choice.value"
                /><span>{{ choice.label }}</span>
              </label>
            </fieldset>
            <template v-else>
              <label for="written-answer" class="sr-only"
                >Écris ta réponse</label
              >
              <textarea
                id="written-answer"
                ref="answerInput"
                v-model="textAnswer"
                class="answer-input"
                rows="10"
                placeholder="Commence par ton idée principale, puis explique pourquoi…"
                :disabled="submitting"
                :readonly="!!answerResult"
                :aria-invalid="!!answerError"
                aria-describedby="writing-hint"
                @input="resizeAnswer"
                @keydown.ctrl.enter.prevent="canSubmit && validateAnswer()"
                @keydown.meta.enter.prevent="canSubmit && validateAnswer()"
              />
              <div id="writing-hint" class="writing-meta">
                <span>Quelques phrases bien expliquées suffisent.</span
                ><span>{{ wordCount }} mot{{ wordCount > 1 ? 's' : '' }}</span>
              </div>
            </template>
            <p v-if="answerError" class="error" role="alert">
              {{ answerError }}
            </p>
            <div v-if="!answerResult" class="answer-actions">
              <span v-if="isWrittenQuestion" class="keyboard-hint"
                ><kbd>Ctrl / ⌘</kbd> + <kbd>Entrée</kbd></span
              ><button
                type="submit"
                class="primary-button"
                :disabled="!canSubmit"
              >
                <span v-if="submitting" class="spinner" aria-hidden="true" />{{
                  submitting ? 'Correction en cours…' : 'Vérifier ma réponse →'
                }}
              </button>
            </div>
            <p v-if="submitting" class="loading-note" role="status">
              On examine ta réponse. Elle reste visible juste au-dessus.
            </p>
          </form>
        </section>
      </div>

      <section
        v-if="answerResult"
        ref="feedbackPanel"
        tabindex="-1"
        class="feedback"
        :class="{ correct: answerResult.correct }"
        aria-labelledby="feedback-title"
      >
        <header class="feedback-header">
          <div>
            <p class="eyebrow">POUR PROGRESSER</p>
            <h2 id="feedback-title">
              {{
                answerResult.correct
                  ? 'Bien joué, le raisonnement est là.'
                  : 'Tu as une piste pour aller plus loin.'
              }}
            </h2>
          </div>
          <span class="score"
            >{{ answerResult.score }}<small> / 100</small></span
          >
        </header>
        <p v-if="answerResult.feedback" class="feedback-summary">
          {{ answerResult.feedback }}
        </p>
        <div class="feedback-grid">
          <div
            v-if="answerResult.correctConcepts?.length"
            class="feedback-section"
          >
            <h3>Ce qui est acquis</h3>
            <ul>
              <li
                v-for="(item, index) in answerResult.correctConcepts"
                :key="index"
              >
                {{ item }}
              </li>
            </ul>
          </div>
          <div
            v-if="answerResult.missingConcepts?.length"
            class="feedback-section"
          >
            <h3>À compléter</h3>
            <ul>
              <li
                v-for="(item, index) in answerResult.missingConcepts"
                :key="index"
              >
                {{ item }}
              </li>
            </ul>
          </div>
          <div
            v-if="answerResult.incorrectConcepts?.length"
            class="feedback-section"
          >
            <h3>À corriger</h3>
            <ul>
              <li
                v-for="(item, index) in answerResult.incorrectConcepts"
                :key="index"
              >
                {{ item }}
              </li>
            </ul>
          </div>
        </div>
        <div v-if="answerResult.howToImprove" class="improvement">
          <h3>Pour ta prochaine réponse</h3>
          <p>{{ answerResult.howToImprove }}</p>
        </div>
        <details v-if="answerResult.explanation" class="feedback-detail">
          <summary>Comprendre le raisonnement</summary>
          <p>{{ answerResult.explanation }}</p>
        </details>
        <details v-if="answerResult.expectedAnswer" class="feedback-detail">
          <summary>Voir un exemple de réponse complète</summary>
          <p>{{ answerResult.expectedAnswer }}</p>
        </details>
        <div v-if="!isLastQuestion" class="feedback-actions">
          <span>Prends le temps de comprendre, puis continue.</span
          ><button type="button" class="primary-button" @click="nextQuestion">
            Question suivante →
          </button>
        </div>
      </section>
      <section v-if="answerResult && isLastQuestion" class="session-finished">
        <p v-if="loadingSummary" role="status">Préparation du bilan…</p>
        <template v-else-if="sessionStatus">
          <p class="eyebrow">SESSION TERMINÉE</p>
          <h2>Une étape de plus.</h2>
          <p>Voici les repères pour ta prochaine révision.</p>
          <div class="summary-stats">
            <div>
              <strong>{{ Math.round(sessionStatus.averageScore) }} %</strong
              ><span>Score moyen</span>
            </div>
            <div>
              <strong
                >{{ sessionStatus.correctAnswers }} /
                {{ sessionStatus.totalQuestions }}</strong
              ><span>Réponses réussies</span>
            </div>
            <div>
              <strong>{{ sessionStatus.incorrectAnswers }}</strong><span
                >À retravailler</span
              >
            </div>
          </div>
          <div class="summary-actions">
            <button
              type="button"
              class="primary-button"
              @click="restartSession"
            >
              Nouvelle session →</button
            ><RouterLink :to="'/courses/' + courseId" class="secondary-button"
              >Retour au cours</RouterLink
            >
          </div>
        </template>
        <button
          v-else
          type="button"
          class="secondary-button"
          @click="loadSessionSummary"
        >
          Réessayer de charger le bilan
        </button>
      </section>
    </template>
  </main>
</template>

<style scoped>
.study-view {
  width: min(1120px, 100% - 3rem);
  margin: auto;
  padding: 2rem 0 5rem;
}
.back-link {
  display: inline-flex;
  margin-bottom: 2rem;
  font-size: 0.9rem;
  color: var(--muted);
  text-decoration: none;
}
.study-setup {
  max-width: 850px;
  margin: auto;
}
.eyebrow {
  color: var(--accent);
  font-size: 0.7rem;
  letter-spacing: 0.13em;
  font-weight: 750;
  margin: 0 0 0.75rem;
}
.setup-intro h1 {
  font-size: clamp(2.6rem, 6vw, 4.3rem);
  line-height: 1.08;
  letter-spacing: -0.055em;
  margin: 1rem 0;
}
.setup-intro h1 span {
  color: var(--accent);
}
.setup-intro > p {
  max-width: 550px;
  color: var(--muted);
  line-height: 1.8;
}
fieldset {
  min-width: 0;
  border: 0;
  padding: 0;
  margin: 0;
}
.setup-section {
  margin: 2.5rem 0;
}
.setup-section legend {
  font-size: 1rem;
  font-weight: 700;
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  gap: 0.65rem;
}
.section-number {
  font-size: 0.7rem;
  color: var(--accent);
  background: var(--accent-soft);
  border-radius: 8px;
  padding: 0.4rem;
}
.choice-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.75rem;
}
.choice-row label {
  position: relative;
  display: grid;
  gap: 0.4rem;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 1.1rem;
  background: white;
  cursor: pointer;
}
.choice-row input {
  position: absolute;
  right: 1rem;
  top: 1rem;
  accent-color: var(--accent);
}
.choice-row strong {
  font-size: 1.6rem;
  letter-spacing: -0.05em;
}
.choice-row strong span {
  font-size: 0.85rem;
  letter-spacing: 0;
  font-weight: 500;
}
.choice-row small,
.type-options small {
  color: var(--muted);
  font-size: 0.8rem;
  line-height: 1.55;
}
.choice-row .chosen,
.type-options .chosen {
  border-color: var(--accent);
  background: var(--accent-soft);
  box-shadow: 0 0 0 1px var(--accent);
}
.preset-row {
  display: flex;
  gap: 0.65rem;
  margin-bottom: 1rem;
}
.preset-row button {
  flex: 1;
  text-align: left;
  border: 1px solid var(--line);
  background: white;
  color: var(--ink);
  padding: 0.9rem 1rem;
  border-radius: 12px;
}
.preset-row small {
  display: block;
  color: var(--muted);
  font-weight: 400;
  font-size: 0.75rem;
  margin-top: 0.25rem;
}
.preset-row [aria-pressed='true'] {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-soft);
}
.type-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.65rem;
}
.format-details summary {
  cursor: pointer;
  padding: 0.8rem 0;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--accent);
}
.format-details summary span {
  color: var(--muted);
  font-size: 0.72rem;
  font-weight: 400;
  margin-left: 0.5rem;
}
.format-details[open] summary {
  margin-bottom: 0.5rem;
}
.type-options label {
  display: flex;
  align-items: flex-start;
  gap: 0.7rem;
  padding: 1rem;
  border: 1px solid var(--line);
  background: white;
  border-radius: 12px;
  cursor: pointer;
}
.type-options input {
  accent-color: var(--accent);
  margin-top: 0.2rem;
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}
.type-options strong,
.type-options small {
  display: block;
}
.type-options strong {
  font-size: 0.87rem;
  margin-bottom: 0.25rem;
}
.study-expectations {
  font-size: 0.8rem;
  line-height: 1.7;
  color: var(--muted);
  max-width: 650px;
}
.setup-footer {
  border-top: 1px solid var(--line);
  padding-top: 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}
.setup-footer > span {
  font-size: 0.8rem;
  color: var(--muted);
}
.primary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
  border: 1px solid var(--accent);
  background: var(--accent);
  color: white;
  padding: 0.9rem 1.3rem;
  border-radius: 11px;
  font-size: 0.88rem;
  font-weight: 650;
  cursor: pointer;
  box-shadow: 0 4px 10px #5146b718;
}
.primary-button:hover:not(:disabled) {
  background: var(--accent-hover);
  transform: translateY(-1px);
}
button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}
.secondary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--line);
  background: white;
  color: var(--ink);
  padding: 0.9rem 1.3rem;
  border-radius: 11px;
  text-decoration: none;
  font-weight: 600;
}
.loading-note {
  font-size: 0.8rem;
  color: var(--muted);
  line-height: 1.6;
}
.spinner {
  width: 15px;
  height: 15px;
  border: 2px solid #ffffff66;
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.session-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 2rem;
}
.session-header h1 {
  margin: 0;
  font-size: clamp(1.6rem, 4vw, 2.3rem);
  letter-spacing: -0.04em;
}
.question-type {
  font-size: 0.75rem;
  color: var(--accent);
  background: var(--accent-soft);
  border: 1px solid #dedaf6;
  border-radius: 30px;
  padding: 0.55rem 0.85rem;
  white-space: nowrap;
}
.session-progress {
  margin-bottom: 1.7rem;
}
.progress-caption {
  display: flex;
  justify-content: space-between;
  font-size: 0.8rem;
  margin-bottom: 0.7rem;
}
.progress-caption span {
  color: var(--muted);
  font-weight: 400;
}
.progress-track {
  height: 5px;
  background: #e5e3ee;
  border-radius: 5px;
  overflow: hidden;
}
.progress-value {
  height: 100%;
  background: var(--accent);
  transition: width 0.3s;
}
.session-notice {
  padding: 0.8rem 1rem;
  background: var(--accent-soft);
  border-radius: 10px;
  font-size: 0.85rem;
}
.study-workspace {
  display: grid;
  grid-template-columns: 1fr 1.05fr;
  gap: 1.5rem;
  align-items: start;
}
.question-pane {
  padding: 1.6rem 0.5rem 1.6rem 0;
  position: sticky;
  top: 6rem;
  scroll-margin-top: 6rem;
}
.question-pane h2 {
  font-size: 1.25rem;
  letter-spacing: -0.025em;
  line-height: 1.4;
  margin: 0 0 1.4rem;
}
.question-context {
  font-size: 1rem;
  line-height: 1.85;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.question-tasks {
  padding-left: 1.5rem;
  line-height: 1.75;
}
.question-tasks li {
  padding: 0.6rem 0 0.6rem 0.4rem;
}
.question-tasks li::marker {
  color: var(--accent);
  font-weight: 700;
}
.guidance-box {
  border-left: 3px solid var(--accent);
  padding: 0.85rem 1rem;
  background: var(--accent-soft);
  border-radius: 0 10px 10px 0;
  margin-top: 1.7rem;
  font-size: 0.82rem;
  line-height: 1.7;
  color: var(--muted);
}
.guidance-box strong {
  color: var(--accent);
}
.guidance-box p {
  margin: 0.3rem 0 0;
}
.response-pane {
  background: white;
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: 0 8px 32px #25213e06;
  overflow: hidden;
}
.response-header {
  padding: 1.2rem 1.4rem;
  border-bottom: 1px solid var(--line);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.response-header h2 {
  font-size: 0.9rem;
  margin: 0;
}
.response-header > span {
  font-size: 0.72rem;
  color: var(--muted);
}
.response-pane form {
  padding: 1.25rem;
}
.answer-input {
  display: block;
  width: 100%;
  border: 1px solid transparent;
  border-radius: 10px;
  background: #fcfbfe;
  color: var(--ink);
  padding: 1rem;
  font: inherit;
  font-size: 1rem;
  line-height: 1.8;
  min-height: 260px;
  resize: vertical;
  overflow: hidden;
}
.answer-input::placeholder {
  color: #858197;
}
.answer-input:focus {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
  background: white;
}
.answer-input:disabled {
  background: #f5f4f8;
  color: #5f5a71;
}
.writing-meta {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  font-size: 0.7rem;
  line-height: 1.5;
  color: var(--muted);
  padding: 0.8rem 0.2rem 1.2rem;
}
.writing-meta span:last-child {
  white-space: nowrap;
}
.answer-actions {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: center;
  padding-top: 1rem;
  border-top: 1px solid var(--line);
}
.keyboard-hint {
  font-size: 0.65rem;
  color: var(--muted);
  white-space: nowrap;
}
kbd {
  font: inherit;
  border: 1px solid var(--line);
  padding: 0.2rem 0.3rem;
  border-radius: 4px;
  background: #faf9fc;
}
.options {
  display: grid;
  gap: 0.7rem;
  margin-bottom: 1rem;
}
.option {
  display: flex;
  gap: 0.7rem;
  align-items: center;
  border: 1px solid var(--line);
  border-radius: 11px;
  padding: 1rem;
  cursor: pointer;
  font-size: 0.9rem;
  line-height: 1.65;
  overflow-wrap: anywhere;
}
.option input {
  accent-color: var(--accent);
  flex-shrink: 0;
}
.option.selected {
  background: var(--accent-soft);
  border-color: var(--accent);
}
.option-letter {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: #f0eef6;
  color: var(--accent);
  font-size: 0.7rem;
  font-weight: 700;
  flex-shrink: 0;
}
.error {
  padding: 0.85rem 1rem;
  background: #fff0ef;
  color: #a63035;
  border-radius: 10px;
  font-size: 0.85rem;
}
.feedback {
  margin-top: 2rem;
  padding: 2rem;
  background: white;
  border: 1px solid var(--line);
  border-top: 4px solid #c48a3a;
  border-radius: 16px;
  scroll-margin-top: 6rem;
}
.feedback.correct {
  border-top-color: #3d8b70;
}
.feedback-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}
.feedback-header h2 {
  font-size: 1.3rem;
  letter-spacing: -0.025em;
  margin: 0;
}
.score {
  font-size: 2.2rem;
  font-weight: 750;
  color: var(--accent);
  white-space: nowrap;
  letter-spacing: -0.05em;
}
.score small {
  font-size: 0.85rem;
  font-weight: 400;
  letter-spacing: 0;
  color: var(--muted);
}
.feedback-summary {
  line-height: 1.8;
  color: var(--muted);
}
.feedback-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(240px, 100%), 1fr));
  gap: 1rem;
  margin-top: 1.5rem;
}
.feedback-section {
  border: 1px solid var(--line);
  padding: 1rem;
  border-radius: 12px;
}
.feedback h3 {
  font-size: 0.85rem;
  margin: 0 0 0.6rem;
}
.feedback-section ul {
  padding-left: 1.15rem;
  margin: 0;
  font-size: 0.88rem;
  line-height: 1.8;
}
.improvement {
  margin: 1.25rem 0;
  padding: 1rem;
  background: var(--accent-soft);
  border-radius: 10px;
}
.improvement p,
.feedback-detail p {
  font-size: 0.9rem;
  line-height: 1.85;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  margin: 0.5rem 0 0;
}
.feedback-detail {
  border-top: 1px solid var(--line);
  padding: 1rem 0;
}
.feedback-detail summary {
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--accent);
}
.feedback-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  border-top: 1px solid var(--line);
  padding-top: 1.3rem;
  margin-top: 0.5rem;
}
.feedback-actions > span {
  color: var(--muted);
  font-size: 0.8rem;
}
.session-finished {
  margin-top: 2rem;
  padding: 2.5rem;
  text-align: center;
  border-radius: 18px;
  background: var(--accent-soft);
  border: 1px solid #dedaf6;
}
.session-finished h2 {
  font-size: 2rem;
  letter-spacing: -0.045em;
  margin: 0.6rem 0;
}
.session-finished > p {
  color: var(--muted);
  font-size: 0.9rem;
}
.summary-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin: 2rem 0;
}
.summary-stats > div {
  display: grid;
  gap: 0.5rem;
  border-right: 1px solid #d8d3ee;
}
.summary-stats > div:last-child {
  border: 0;
}
.summary-stats strong {
  font-size: 2rem;
  color: var(--accent);
  letter-spacing: -0.045em;
}
.summary-stats span {
  font-size: 0.8rem;
  color: var(--muted);
}
.summary-actions {
  display: flex;
  justify-content: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}
@media (max-width: 800px) {
  .study-workspace {
    grid-template-columns: 1fr;
    gap: 0;
  }
  .question-pane {
    position: static;
    padding: 1rem 0;
  }
  .keyboard-hint {
    display: none;
  }
  .answer-actions .primary-button {
    width: 100%;
  }
}
@media (max-width: 540px) {
  .study-view {
    width: calc(100% - 2rem);
    padding-top: 1.5rem;
  }
  .type-options {
    grid-template-columns: 1fr;
  }
  .choice-row label {
    padding: 0.8rem;
  }
  .choice-row input {
    right: 0.5rem;
    top: 0.5rem;
  }
  .choice-row strong span {
    display: block;
    font-size: 0.75rem;
  }
  .choice-row small {
    font-size: 0.7rem;
  }
  .setup-footer,
  .feedback-actions {
    align-items: stretch;
    flex-direction: column;
  }
  .session-header {
    align-items: start;
    flex-direction: column;
  }
  .feedback {
    padding: 1.2rem;
  }
  .feedback-header {
    align-items: start;
  }
  .feedback-header h2 {
    font-size: 1.05rem;
  }
  .score {
    font-size: 1.8rem;
  }
  .session-finished {
    padding: 1.5rem 1rem;
  }
  .summary-stats strong {
    font-size: 1.5rem;
  }
  .summary-stats span {
    font-size: 0.7rem;
  }
  .writing-meta {
    flex-wrap: wrap;
  }
  .preset-row button {
    font-size: 0.8rem;
  }
}
@media (prefers-reduced-motion: reduce) {
  * {
    scroll-behavior: auto !important;
    transition: none !important;
    animation: none !important;
  }
}
</style>
