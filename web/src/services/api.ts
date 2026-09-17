const API_URL = 'http://localhost:8080/api'

export async function checkHealth(): Promise<string> {
  const response = await fetch(`${API_URL}/health`)

  if (!response.ok) {
    throw new Error('API unavailable')
  }

  return response.text()
}
