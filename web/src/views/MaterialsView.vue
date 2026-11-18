<script setup lang="ts">
import {
  computed,
  watch,
  ref,
} from 'vue'

import { useRoute } from 'vue-router'

import {
  createStudyMaterial,
  deleteStudyMaterial,
  getStudyMaterials,
  getSectionProgress,
  updateStudyMaterial,
} from '../services/api'

import type {
  ConceptProgress,
  StudyMaterial,
  StudyMaterialRequest,
} from '../types/study'

import ConceptMastery from '../components/ConceptMastery.vue'

const route = useRoute()

const courseId = computed(() =>
  Number(route.params.courseId),
)

const sectionId = computed(() =>
  Number(route.params.sectionId),
)

const materials =
  ref<StudyMaterial[]>([])

const loading = ref(true)
const progressLoading = ref(true)
const progressError = ref(false)
const conceptProgress = ref<ConceptProgress[]>([])
const progressByMaterial = computed(() => new Map(
  conceptProgress.value.map((progress) => [progress.materialId, progress]),
))

async function loadProgress() {
  progressLoading.value = true
  progressError.value = false
  conceptProgress.value = []
  try {
    const progress = await getSectionProgress(sectionId.value)
    conceptProgress.value = progress.concepts
  } catch (err) {
    console.error(err)
    progressError.value = true
  } finally {
    progressLoading.value = false
  }
}
const saving = ref(false)
const deletingMaterialId =
  ref<number | null>(null)

const error = ref('')

const showForm = ref(false)

const editingMaterialId =
  ref<number | null>(null)

const concept = ref('')
const content = ref('')

async function loadMaterials() {
  loading.value = true
  error.value = ''

  try {
    materials.value =
      await getStudyMaterials(
        sectionId.value,
      )
  } catch (err) {
    console.error(err)

    error.value =
      'Impossible de charger les concepts.'
  } finally {
    loading.value = false
  }
}

function openCreateForm() {
  editingMaterialId.value = null

  concept.value = ''
  content.value = ''

  showForm.value = true
}

function openEditForm(
  material: StudyMaterial,
) {
  editingMaterialId.value =
    material.id

  concept.value =
    material.concept

  content.value =
    material.content

  showForm.value = true
}

function closeForm() {
  showForm.value = false

  editingMaterialId.value = null

  concept.value = ''
  content.value = ''

  error.value = ''
}

async function saveMaterial() {
  if (!concept.value.trim()) {
    error.value =
      'Le nom du concept est obligatoire.'

    return
  }

  if (!content.value.trim()) {
    error.value =
      'Le contenu du concept est obligatoire.'

    return
  }

  saving.value = true
  error.value = ''

  const request: StudyMaterialRequest = {
    concept: concept.value.trim(),
    content: content.value.trim(),
  }

  try {
    if (
      editingMaterialId.value === null
    ) {
      const created =
        await createStudyMaterial(
          sectionId.value,
          request,
        )

      materials.value.push(created)
    } else {
      const updated =
        await updateStudyMaterial(
          sectionId.value,
          editingMaterialId.value,
          request,
        )

      const index =
        materials.value.findIndex(
          (material) =>
            material.id === updated.id,
        )

      if (index !== -1) {
        materials.value[index] =
          updated
      }
    }

    closeForm()
  } catch (err) {
    console.error(err)

    error.value =
      'Impossible d’enregistrer le concept.'
  } finally {
    saving.value = false
  }
}

async function removeMaterial(
  material: StudyMaterial,
) {
  if (
    saving.value ||
    deletingMaterialId.value !== null
  ) {
    return
  }

  const confirmed = window.confirm(
    `Supprimer le concept "${material.concept}" et sa progression ?`,
  )

  if (!confirmed) {
    return
  }

  deletingMaterialId.value = material.id
  error.value = ''

  try {
    await deleteStudyMaterial(
      sectionId.value,
      material.id,
    )

    materials.value =
      materials.value.filter(
        (item) => item.id !== material.id,
      )

    if (editingMaterialId.value === material.id) {
      closeForm()
    }
  } catch (err) {
    console.error(err)

    error.value =
      'Impossible de supprimer ce concept. Réessaie.'
  } finally {
    deletingMaterialId.value = null
  }
}

watch(sectionId, () => {
  void loadMaterials()
  void loadProgress()
}, { immediate: true })
</script>

<template>
  <main class="materials-view">
    <RouterLink
      :to="`/courses/${courseId}`"
      class="back-link"
    >
      ← Retour au cours
    </RouterLink>

    <header class="materials-header">
      <div>
        <p class="eyebrow">
          Concepts
        </p>

        <h1>Contenu de la section</h1>

        <p>
          Ajoute les concepts que Recallr
          utilisera pour générer tes questions.
        </p>
        <p class="progress-hint">La maîtrise reflète les résultats de tes réponses pendant les sessions d’étude.</p>
      </div>

      <button
        type="button"
        @click="openCreateForm"
      >
        + Ajouter un concept
      </button>
    </header>

    <p
      v-if="error"
      class="error"
    >
      {{ error }}
    </p>

    <!-- FORMULAIRE -->
    <p v-if="progressError" class="progress-notice" role="status">
      La progression n’a pas pu être chargée.
      <button type="button" class="secondary-button" @click="loadProgress">Réessayer</button>
    </p>

    <section
      v-if="showForm"
      class="material-form"
    >
      <h2>
        {{
          editingMaterialId === null
            ? 'Nouveau concept'
            : 'Modifier le concept'
        }}
      </h2>

      <label>
        Concept

        <input
          v-model="concept"
          type="text"
          placeholder="Ex. Test de Turing"
        >
      </label>

      <label>
        Contenu

        <textarea
          v-model="content"
          rows="8"
          placeholder="Écris ici les informations importantes à connaître..."
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
          :disabled="saving || deletingMaterialId !== null"
          @click="saveMaterial"
        >
          {{
            saving
              ? 'Enregistrement...'
              : 'Enregistrer'
          }}
        </button>
      </div>
    </section>

    <!-- CHARGEMENT -->

    <p v-if="loading">
      Chargement des concepts...
    </p>

    <!-- AUCUN CONCEPT -->

    <section
      v-else-if="materials.length === 0"
      class="empty-state"
    >
      <h2>Aucun concept</h2>

      <p>
        Ajoute ton premier concept pour
        commencer à construire cette section.
      </p>

      <button
        type="button"
        @click="openCreateForm"
      >
        + Ajouter un concept
      </button>
    </section>

    <!-- LISTE -->

    <section
      v-else
      class="materials-list"
    >
      <article
        v-for="material in materials"
        :key="material.id"
        class="material-card"
      >
        <div class="material-header">
          <div>
            <p class="material-id">
              Concept #{{ material.id }}
            </p>

            <h2>
              {{ material.concept }}
            </h2>
          </div>

          <div class="material-actions">
            <button
              type="button"
              class="secondary-button"
              :disabled="saving || deletingMaterialId !== null"
              @click="openEditForm(material)"
            >
              Modifier
            </button>

            <button
              type="button"
              class="delete-button"
              :aria-label="`Supprimer le concept ${material.concept}`"
              :disabled="saving || deletingMaterialId !== null"
              @click="removeMaterial(material)"
            >
              {{
                deletingMaterialId === material.id
                  ? 'Suppression...'
                  : 'Supprimer'
              }}
            </button>
          </div>
        </div>

        <ConceptMastery
          :progress="progressByMaterial.get(material.id)"
          :loading="progressLoading"
          :error="progressError"
        />
        <p class="material-content">
          {{ material.content }}
        </p>
      </article>
    </section>
  </main>
</template>

<style scoped>
.materials-view {
  width: min(100% - 2rem, 900px);
  margin: 0 auto;
  padding: 3rem 0;
}

.back-link {
  display: inline-block;
  margin-bottom: 2rem;
  text-decoration: none;
}

.materials-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 2rem;
  margin-bottom: 2rem;
}

.materials-header h1 {
  margin: 0.25rem 0 0.5rem;
}

.progress-hint { color: var(--muted, #6d687f); font-size: .8rem; line-height: 1.6; }
.progress-notice { color: var(--muted, #6d687f); font-size: .85rem; display: flex; gap: .75rem; align-items: center; flex-wrap: wrap; }

.eyebrow {
  margin: 0;
  font-size: 0.9rem;
  font-weight: 700;
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

.delete-button {
  color: #b42318;
}

.material-form {
  display: grid;
  gap: 1.25rem;
  margin-bottom: 2rem;
  padding: 1.5rem;
  border: 1px solid #ddd;
  border-radius: 12px;
  background: white;
}

.material-form h2 {
  margin: 0;
}

.material-form label {
  display: grid;
  gap: 0.5rem;
  font-weight: 600;
}

.material-form input,
.material-form textarea {
  box-sizing: border-box;
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font: inherit;
}

.material-form textarea {
  resize: vertical;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.materials-list {
  display: grid;
  gap: 1rem;
}

.material-card {
  padding: 1.5rem;
  border: 1px solid #ddd;
  border-radius: 12px;
  background: white;
}

.material-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.material-header h2 {
  margin: 0.25rem 0 0;
}

.material-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.material-id {
  margin: 0;
  font-size: 0.8rem;
  color: #777;
}

.material-content {
  margin: 1rem 0 0;
  line-height: 1.6;
  white-space: pre-wrap;
}

.empty-state {
  padding: 3rem 2rem;
  text-align: center;
  border: 1px dashed #ddd;
  border-radius: 12px;
}

.error {
  margin-bottom: 1rem;
  color: #b42318;
}

@media (max-width: 650px) {
  .materials-header,
  .material-header {
    flex-direction: column;
  }
}
</style>
