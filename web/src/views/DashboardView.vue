<script setup lang="ts">
import { onMounted, ref } from 'vue'

import {
  createStudySet,
  deleteStudySet,
  getStudySets,
  updateStudySet,
} from '../services/api'

import type {
  StudySet,
  StudySetRequest,
} from '../types/study'

const studySets = ref<StudySet[]>([])

const loading = ref(true)
const saving = ref(false)

const error = ref('')

const showForm = ref(false)

const editingStudySetId =
  ref<number | null>(null)

const title = ref('')
const description = ref('')

async function loadStudySets() {
  loading.value = true
  error.value = ''

  try {
    studySets.value =
      await getStudySets()
  } catch (err) {
    console.error(err)

    error.value =
      'Impossible de charger les cours.'
  } finally {
    loading.value = false
  }
}

function openCreateForm() {
  editingStudySetId.value = null

  title.value = ''
  description.value = ''

  showForm.value = true
}

function openEditForm(
  studySet: StudySet,
) {
  editingStudySetId.value =
    studySet.id

  title.value =
    studySet.title

  description.value =
    studySet.description ?? ''

  showForm.value = true
}

function closeForm() {
  showForm.value = false

  editingStudySetId.value = null

  title.value = ''
  description.value = ''

  error.value = ''
}

async function saveStudySet() {
  if (!title.value.trim()) {
    error.value =
      'Le titre du cours est obligatoire.'

    return
  }

  saving.value = true
  error.value = ''

  const request: StudySetRequest = {
    title: title.value.trim(),
    description:
      description.value.trim(),
  }

  try {
    if (
      editingStudySetId.value === null
    ) {
      const created =
        await createStudySet(request)

      studySets.value.push(created)
    } else {
      const updated =
        await updateStudySet(
          editingStudySetId.value,
          request,
        )

      const index =
        studySets.value.findIndex(
          (studySet) =>
            studySet.id === updated.id,
        )

      if (index !== -1) {
        studySets.value[index] =
          updated
      }
    }

    closeForm()
  } catch (err) {
    console.error(err)

    error.value =
      'Impossible d’enregistrer le cours.'
  } finally {
    saving.value = false
  }
}

async function removeStudySet(
  studySet: StudySet,
) {
  const confirmed = window.confirm(
    `Supprimer le cours "${studySet.title}" ?`,
  )

  if (!confirmed) {
    return
  }

  error.value = ''

  try {
    await deleteStudySet(
      studySet.id,
    )

    studySets.value =
      studySets.value.filter(
        (item) =>
          item.id !== studySet.id,
      )
  } catch (err) {
    console.error(err)

    error.value =
      'Impossible de supprimer ce cours. ' +
      'Supprime d’abord ses sections.'
  }
}

onMounted(loadStudySets)
</script>

<template>
  <main class="dashboard">
    <header class="dashboard-header">
      <div>
        <p class="eyebrow">
          Recallr
        </p>

        <h1>Mes cours</h1>

        <p>
          Choisis un cours pour commencer
          à réviser.
        </p>
      </div>

      <button
        type="button"
        @click="openCreateForm"
      >
        + Ajouter un cours
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
      class="course-form"
    >
      <h2>
        {{
          editingStudySetId === null
            ? 'Nouveau cours'
            : 'Modifier le cours'
        }}
      </h2>

      <label>
        Nom du cours

        <input
          v-model="title"
          type="text"
          placeholder="Ex. LOG635"
        >
      </label>

      <label>
        Description

        <textarea
          v-model="description"
          rows="4"
          placeholder="Ex. Systèmes intelligents et algorithmes"
        />
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
          @click="saveStudySet"
        >
          {{
            saving
              ? 'Enregistrement...'
              : 'Enregistrer'
          }}
        </button>
      </div>
    </section>

    <p v-if="loading">
      Chargement des cours...
    </p>

    <p
      v-else-if="studySets.length === 0"
      class="empty-state"
    >
      Aucun cours pour le moment.
    </p>

    <section
      v-else
      class="course-grid"
    >
      <article
        v-for="studySet in studySets"
        :key="studySet.id"
        class="course-card"
      >
        <h2>
          {{ studySet.title }}
        </h2>

        <p>
          {{ studySet.description }}
        </p>

        <div class="course-actions">
          <RouterLink
            :to="`/courses/${studySet.id}`"
            class="open-link"
          >
            Ouvrir
          </RouterLink>

          <button
            type="button"
            class="secondary-button"
            @click="
              openEditForm(studySet)
            "
          >
            Modifier
          </button>

          <button
            type="button"
            class="delete-button"
            @click="
              removeStudySet(studySet)
            "
          >
            Supprimer
          </button>
        </div>
      </article>
    </section>
  </main>
</template>

<style scoped>
.dashboard {
  width: min(100% - 2rem, 1000px);
  margin: 0 auto;
  padding: 3rem 0;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 2rem;
  margin-bottom: 2rem;
}

.eyebrow {
  margin: 0;
  font-weight: 700;
}

.dashboard-header h1 {
  margin-bottom: 0.5rem;
}

button,
.open-link {
  padding: 0.75rem 1rem;
  border-radius: 8px;
  font-weight: 600;
}

button {
  border: 0;
  cursor: pointer;
}

.open-link {
  text-decoration: none;
  border: 1px solid #ddd;
}

.secondary-button {
  border: 1px solid #ddd;
  background: white;
}

.delete-button {
  border: 1px solid #ddd;
  background: white;
}

.course-form {
  display: grid;
  gap: 1.25rem;
  margin-bottom: 2rem;
  padding: 1.5rem;
  border: 1px solid #ddd;
  border-radius: 12px;
}

.course-form label {
  display: grid;
  gap: 0.5rem;
  font-weight: 600;
}

.course-form input,
.course-form textarea {
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

.course-grid {
  display: grid;
  grid-template-columns:
    repeat(auto-fit, minmax(260px, 1fr));
  gap: 1rem;
}

.course-card {
  padding: 1.5rem;
  border: 1px solid #ddd;
  border-radius: 12px;
  background: white;
}

.course-card h2 {
  margin-top: 0;
}

.course-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 1.5rem;
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
  .dashboard-header {
    flex-direction: column;
  }
}
</style>