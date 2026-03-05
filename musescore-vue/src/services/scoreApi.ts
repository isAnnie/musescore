import type { Score } from '@/types/score'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

type ScorePayload = {
  id: string
  title: string
  composer: string
  tempo: number
  timeSignature: { numerator: number; denominator: number }
  keySignature: string
  notes: any[]
  measures: any[]
  createdAt: string
  updatedAt: string
  isDraft: boolean
  tags?: string[]
  description?: string
  visibility?: 'public' | 'private' | 'unlisted'
}

const getToken = () => localStorage.getItem('token')

const authHeaders = () => {
  const token = getToken()
  if (!token) {
    throw new Error('未登录或登录已过期')
  }
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`
  }
}

const toScore = (payload: ScorePayload): Score => ({
  ...payload,
  createdAt: new Date(payload.createdAt),
  updatedAt: new Date(payload.updatedAt)
})

const parseErrorMessage = async (response: Response) => {
  try {
    const data = await response.json()
    return data.message || '请求失败'
  } catch {
    return '请求失败'
  }
}

const toUpsertPayload = (score: Score) => ({
  title: score.title,
  composer: score.composer,
  tempo: score.tempo,
  timeSignature: score.timeSignature,
  keySignature: score.keySignature,
  notes: score.notes,
  measures: score.measures,
  isDraft: score.isDraft,
  tags: score.tags ?? [],
  description: score.description ?? '',
  visibility: score.visibility ?? 'private'
})

export const listMyScores = async (): Promise<Score[]> => {
  const response = await fetch(`${API_BASE_URL}/api/scores/my`, {
    headers: authHeaders()
  })
  if (!response.ok) {
    throw new Error(await parseErrorMessage(response))
  }
  const data = await response.json() as ScorePayload[]
  return data.map(toScore)
}

export const getScoreById = async (id: string): Promise<Score> => {
  const response = await fetch(`${API_BASE_URL}/api/scores/${id}`, {
    headers: authHeaders()
  })
  if (!response.ok) {
    throw new Error(await parseErrorMessage(response))
  }
  const data = await response.json() as ScorePayload
  return toScore(data)
}

export const createScoreOnServer = async (score: Score): Promise<Score> => {
  const response = await fetch(`${API_BASE_URL}/api/scores`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(toUpsertPayload(score))
  })
  if (!response.ok) {
    throw new Error(await parseErrorMessage(response))
  }
  const data = await response.json() as ScorePayload
  return toScore(data)
}

export const updateScoreOnServer = async (score: Score): Promise<Score> => {
  const response = await fetch(`${API_BASE_URL}/api/scores/${score.id}`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(toUpsertPayload(score))
  })
  if (!response.ok) {
    throw new Error(await parseErrorMessage(response))
  }
  const data = await response.json() as ScorePayload
  return toScore(data)
}

export const saveScoreOnServer = async (score: Score): Promise<Score> => {
  try {
    return await updateScoreOnServer(score)
  } catch (error) {
    const message = error instanceof Error ? error.message : ''
    if (message.includes('不存在') || message.includes('404')) {
      return createScoreOnServer(score)
    }
    throw error
  }
}

export const deleteScoreOnServer = async (id: string): Promise<void> => {
  const response = await fetch(`${API_BASE_URL}/api/scores/${id}`, {
    method: 'DELETE',
    headers: authHeaders()
  })
  if (!response.ok) {
    throw new Error(await parseErrorMessage(response))
  }
}
