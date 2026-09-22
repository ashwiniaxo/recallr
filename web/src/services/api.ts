import type {
  CourseSection,
  CreateStudySessionRequest,
  SectionProgress,
  StudySession,
  StudySet,
  StudyMaterial,
  StudyMaterialRequest,
  AnswerRequest,
  AnswerResult,
} from '../types/study'

const API_URL = 'http://localhost:8080/api'

export async function checkHealth(): Promise<string> {
  const response = await fetch(`${API_URL}/health`)

  if (!response.ok) {
    throw new Error('API unavailable')
  }

  return response.text()
}

export async function askAi(prompt: string): Promise<string> {
  const response = await fetch(`${API_URL}/ai/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ prompt }),
  })

  if (!response.ok) {
    throw new Error('AI request failed')
  }

  const data: { response: string } = await response.json()

  return data.response
}

export async function getStudySets(): Promise<StudySet[]> {
  const response = await fetch(`${API_URL}/study-sets`)

  if (!response.ok) {
    throw new Error('Failed to load study sets')
  }

  return response.json()
}

export async function getCourseSections(
  studySetId: number,
): Promise<CourseSection[]> {
  const response = await fetch(
    `${API_URL}/study-sets/${studySetId}/sections`,
  )

  if (!response.ok) {
    throw new Error('Failed to load course sections')
  }

  return response.json()
}

export async function getSectionProgress(
  sectionId: number,
): Promise<SectionProgress> {
  const response = await fetch(
    `${API_URL}/progress/sections/${sectionId}`,
  )

  if (!response.ok) {
    throw new Error('Failed to load section progress')
  }

  return response.json()
}

export async function createStudySession(
  sectionId: number,
  request: CreateStudySessionRequest,
): Promise<StudySession> {
  const response = await fetch(
    `${API_URL}/study/sections/${sectionId}/sessions`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    },
  )

  if (!response.ok) {
    throw new Error('Failed to create study session')
  }

  return response.json()
}

export async function getStudyMaterials(
  sectionId: number,
): Promise<StudyMaterial[]> {
  const response = await fetch(
    `${API_URL}/sections/${sectionId}/materials`,
  )

  if (!response.ok) {
    throw new Error('Failed to load study materials')
  }

  return response.json()
}

export async function createStudyMaterial(
  sectionId: number,
  material: StudyMaterialRequest,
): Promise<StudyMaterial> {
  const response = await fetch(
    `${API_URL}/sections/${sectionId}/materials`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(material),
    },
  )

  if (!response.ok) {
    throw new Error('Failed to create study material')
  }

  return response.json()
}

export async function updateStudyMaterial(
  sectionId: number,
  materialId: number,
  material: StudyMaterialRequest,
): Promise<StudyMaterial> {
  const response = await fetch(
    `${API_URL}/sections/${sectionId}/materials/${materialId}`,
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(material),
    },
  )

  if (!response.ok) {
    throw new Error('Failed to update study material')
  }

  return response.json()
}

export async function submitAnswer(
  sessionId: string,
  questionId: string,
  request: AnswerRequest,
): Promise<AnswerResult> {
  const response = await fetch(
    `${API_URL}/study/sessions/${sessionId}/questions/${questionId}/answer`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    },
  )

  if (!response.ok) {
    throw new Error('Failed to submit answer')
  }

  return response.json()
}