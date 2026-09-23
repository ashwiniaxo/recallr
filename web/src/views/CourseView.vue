<script setup lang="ts">
import {
  computed,
  onMounted,
  ref,
} from 'vue'

import {
  createCourseSection,
  deleteCourseSection,
  getCourseSections,
  getStudySets,
  updateCourseSection,
} from '../services/api'

import type {
  CourseSection,
  CourseSectionRequest,
  StudySet,
} from '../types/study'

import { useRoute } from 'vue-router'

import SectionCard from '../components/SectionCard.vue'

const route = useRoute()

const studySetId = computed(() =>
  Number(route.params.id),
)

const studySet =
  ref<StudySet | null>(null)

const sections =
  ref<CourseSection[]>([])

const loading = ref(true)
const saving = ref(false)

const error = ref('')

const showForm = ref(false)

const editingSectionId =
  ref<number | null>(null)

const sectionNumber =
  ref<number>(1)

const title = ref('')

async function loadCourse() {
  loading.value = true
  error.value = ''

  try {
    const studySets =
      await getStudySets()

    studySet.value =
      studySets.find(
        (item) =>
          item.id === studySetId.value,
      ) ?? null

    sections.value =
      await getCourseSections(
        studySetId.value,
      )
  } catch (err) {
    console.error(err)

    error.value =
      'Impossible de charger le cours.'
  } finally {
    loading.value = false
  }
}

function openCreateForm() {
  editingSectionId.value = null

  /*
   * Propose automatiquement le prochain numéro.
   */
  const highestNumber =
    sections.value.reduce(
      (highest, section) =>
        Math.max(
          highest,
          section.sectionNumber ?? 0,
        ),
      0,
    )

  sectionNumber.value =
    highestNumber + 1

  title.value = ''

  showForm.value = true
}

function openEditForm(
  section: CourseSection,
) {
  editingSectionId.value =
    section.id

  sectionNumber.value =
    section.sectionNumber

  title.value =
    section.title

  showForm.value = true
}

function closeForm() {
  showForm.value = false

  editingSectionId.value = null

  sectionNumber.value = 1
  title.value = ''

  error.value = ''
}

async function saveSection() {
  if (!title.value.trim()) {
    error.value =
      'Le titre de la section est obligatoire.'

    return
  }

  if (sectionNumber.value <= 0) {
    error.value =
      'Le numéro de section doit être supérieur à 0.'

    return
  }

  saving.value = true
  error.value = ''

  const request: CourseSectionRequest = {
    sectionNumber:
      sectionNumber.value,

    title:
      title.value.trim(),
  }

  try {
    if (
      editingSectionId.value === null
    ) {
      await createCourseSection(
        studySetId.value,
        request,
      )
    } else {
      await updateCourseSection(
        studySetId.value,
        editingSectionId.value,
        request,
      )
    }

    /*
     * On recharge pour conserver
     * l'ordre sectionNumber du backend.
     */
    sections.value =
      await getCourseSections(
        studySetId.value,
      )

    closeForm()
  } catch (err) {
    console.error(err)

    error.value =
      'Impossible d’enregistrer la section.'
  } finally {
    saving.value = false
  }
}

async function removeSection(
  section: CourseSection,
) {
  const confirmed =
    window.confirm(
      `Supprimer "${section.title}" ?`,
    )

  if (!confirmed) {
    return
  }

  error.value = ''

  try {
    await deleteCourseSection(
      studySetId.value,
      section.id,
    )

    sections.value =
      sections.value.filter(
        (item) =>
          item.id !== section.id,
      )
  } catch (err) {
    console.error(err)

    error.value =
      'Impossible de supprimer cette section. ' +
      'Supprime d’abord ses concepts.'
  }
}

onMounted(loadCourse)
</script>

<template>
  <main class="course-view">
    <RouterLink
      to="/"
      class="back-link"
    >
      ← Mes cours
    </RouterLink>

    <p v-if="loading">
      Chargement...
    </p>

    <template v-else>
      <header class="course-header">
        <div>
          <h1>
            {{
              studySet?.title ??
              'Cours'
            }}
          </h1>

          <p v-if="studySet?.description">
            {{ studySet.description }}
          </p>
        </div>

        <button
          type="button"
          @click="openCreateForm"
        >
          + Ajouter une section
        </button>
      </header>

      <p
        v-if="error"
        class="error"
      >
        {{ error }}
      </p>

      <!-- FORMULAIRE -->

      <section
        v-if="showForm"
        class="section-form"
      >
        <h2>
          {{
            editingSectionId === null
              ? 'Nouvelle section'
              : 'Modifier la section'
          }}
        </h2>

        <label>
          Numéro de section

          <input
            v-model.number="sectionNumber"
            type="number"
            min="1"
          >
        </label>

        <label>
          Titre

          <input
            v-model="title"
            type="text"
            placeholder="Ex. Cours 02 - Apprentissage"
          >
        </label>

        <div class="form-actions">
          <button
            type="button"
            class="secondary-button"
            @click="closeForm"
          >
            Annuler
          </button>

          <button
            type="button"
            :disabled="saving"
            @click="saveSection"
          >
            {{
              saving
                ? 'Enregistrement...'
                : 'Enregistrer'
            }}
          </button>
        </div>
      </section>

      <p
        v-if="sections.length === 0"
        class="empty-state"
      >
        Aucune section pour ce cours.
      </p>

      <section
        v-else
        class="sections"
      >
        <div
          v-for="section in sections"
          :key="section.id"
          class="section-wrapper"
        >
          <SectionCard
            :section="section"
            :study-set-id="studySetId"
          />

          <div class="management-actions">
            <button
              type="button"
              class="secondary-button"
              @click="
                openEditForm(section)
              "
            >
              Modifier
            </button>

            <button
              type="button"
              class="delete-button"
              @click="
                removeSection(section)
              "
            >
              Supprimer
            </button>
          </div>
        </div>
      </section>
    </template>
  </main>
</template>

<style scoped>
.course-view {
  width: min(100% - 2rem, 1000px);
  margin: 0 auto;
  padding: 3rem 0;
}

.back-link {
  display: inline-block;
  margin-bottom: 2rem;
  text-decoration: none;
}

.course-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 2rem;
  margin-bottom: 2rem;
}

.course-header h1 {
  margin-top: 0;
  margin-bottom: 0.5rem;
}

button {
  padding: 0.75rem 1rem;
  border: 0;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.secondary-button,
.delete-button {
  border: 1px solid #ddd;
  background: white;
}

.section-form {
  display: grid;
  gap: 1.25rem;
  margin-bottom: 2rem;
  padding: 1.5rem;
  border: 1px solid #ddd;
  border-radius: 12px;
  background: white;
}

.section-form label {
  display: grid;
  gap: 0.5rem;
  font-weight: 600;
}

.section-form input {
  box-sizing: border-box;
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font: inherit;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.sections {
  display: grid;
  gap: 1rem;
}

.section-wrapper {
  display: grid;
  gap: 0.5rem;
}

.management-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.error {
  color: #b42318;
}

.empty-state {
  padding: 2rem;
  text-align: center;
  border: 1px dashed #ddd;
  border-radius: 12px;
}

@media (max-width: 650px) {
  .course-header {
    flex-direction: column;
  }
}
</style>