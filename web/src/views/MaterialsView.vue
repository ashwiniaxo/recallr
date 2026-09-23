<script setup lang="ts">
import {
  computed,
  onMounted,
  ref,
} from 'vue'

import { useRoute } from 'vue-router'

import {
  createStudyMaterial,
  getStudyMaterials,
  updateStudyMaterial,
} from '../services/api'

import type {
  StudyMaterial,
  StudyMaterialRequest,
} from '../types/study'

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
const saving = ref(false)

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

onMounted(loadMaterials)
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
          :disabled="saving"
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

          <button
            type="button"
            class="secondary-button"
            @click="
              openEditForm(material)
            "
          >
            Modifier
          </button>
        </div>

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

.secondary-button {
  border: 1px solid #ddd;
  background: white;
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