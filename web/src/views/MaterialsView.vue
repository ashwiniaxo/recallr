<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

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

const sectionId = computed(() =>
  Number(route.params.sectionId),
)

const materials = ref<StudyMaterial[]>([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')

const showForm = ref(false)
const editingMaterialId = ref<number | null>(null)

const concept = ref('')
const content = ref('')

async function loadMaterials() {
  loading.value = true
  error.value = ''

  try {
    materials.value =
      await getStudyMaterials(sectionId.value)
  } catch (err) {
    console.error(err)
    error.value = 'Impossible de charger les concepts.'
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

function openEditForm(material: StudyMaterial) {
  editingMaterialId.value = material.id
  concept.value = material.concept
  content.value = material.content
  showForm.value = true
}

function closeForm() {
  showForm.value = false
  editingMaterialId.value = null
  concept.value = ''
  content.value = ''
}

async function saveMaterial() {
  if (!concept.value.trim() || !content.value.trim()) {
    error.value =
      'Le concept et le contenu sont obligatoires.'
    return
  }

  saving.value = true
  error.value = ''

  const request: StudyMaterialRequest = {
    concept: concept.value.trim(),
    content: content.value.trim(),
  }

  try {
    if (editingMaterialId.value === null) {
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

      const index = materials.value.findIndex(
        (material) => material.id === updated.id,
      )

      if (index !== -1) {
        materials.value[index] = updated
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
  <main class="materials-page">
    <RouterLink to="/courses/1" class="back-link">
      ← Retour au cours
    </RouterLink>

    <div class="page-header">
      <div>
        <p class="eyebrow">Cours {{ sectionId }}</p>
        <h1>Concepts</h1>
        <p>
          Gère le contenu utilisé par Recallr pour
          générer tes questions.
        </p>
      </div>

      <button
        class="primary-button"
        type="button"
        @click="openCreateForm"
      >
        + Ajouter un concept
      </button>
    </div>

    <p v-if="error" class="error">
      {{ error }}
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
        />
      </label>

      <label>
        Contenu

        <textarea
          v-model="content"
          rows="8"
          placeholder="Écris ici ce que Recallr doit connaître sur ce concept..."
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
          class="primary-button"
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

    <p v-if="loading">
      Chargement des concepts...
    </p>

    <p
      v-else-if="materials.length === 0"
      class="empty-state"
    >
      Aucun concept pour ce cours.
    </p>

    <section v-else class="materials-list">
      <article
        v-for="material in materials"
        :key="material.id"
        class="material-card"
      >
        <div class="material-header">
          <h2>{{ material.concept }}</h2>

          <button
            type="button"
            class="edit-button"
            @click="openEditForm(material)"
          >
            Modifier
          </button>
        </div>

        <p>{{ material.content }}</p>
      </article>
    </section>
  </main>
</template>

<style scoped>
.materials-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 24px;
}

.back-link {
  display: inline-block;
  margin-bottom: 32px;
  text-decoration: none;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
  margin-bottom: 32px;
}

.page-header h1 {
  margin: 4px 0 8px;
}

.page-header p {
  margin: 0;
}

.eyebrow {
  font-size: 0.85rem;
  opacity: 0.7;
}

.materials-list {
  display: grid;
  gap: 16px;
}

.material-card,
.material-form {
  padding: 24px;
  border: 1px solid #ddd;
  border-radius: 12px;
}

.material-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.material-header h2 {
  margin-top: 0;
}

.material-card p {
  white-space: pre-wrap;
  line-height: 1.6;
}

.material-form {
  display: grid;
  gap: 20px;
  margin-bottom: 32px;
}

.material-form label {
  display: grid;
  gap: 8px;
  font-weight: 600;
}

.material-form input,
.material-form textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 12px;
  font: inherit;
  border: 1px solid #ccc;
  border-radius: 8px;
}

.material-form textarea {
  resize: vertical;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.primary-button,
.secondary-button,
.edit-button {
  padding: 10px 16px;
  cursor: pointer;
  border-radius: 8px;
}

.primary-button {
  border: none;
}

.secondary-button,
.edit-button {
  background: transparent;
  border: 1px solid #ccc;
}

.error {
  margin-bottom: 20px;
}

.empty-state {
  padding: 32px;
  text-align: center;
  border: 1px dashed #ccc;
  border-radius: 12px;
}

@media (max-width: 650px) {
  .page-header {
    flex-direction: column;
  }
}
</style>