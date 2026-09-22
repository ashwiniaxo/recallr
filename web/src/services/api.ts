import type { StudySet } from '../types/study'

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
