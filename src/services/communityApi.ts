const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const authHeaders = () => {
  const token = localStorage.getItem('token')
  return token
    ? { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }
    : { 'Content-Type': 'application/json' }
}

const requireAuthHeaders = () => {
  const token = localStorage.getItem('token')
  if (!token) {
    throw new Error('请先登录后再进行点赞或收藏')
  }
  return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }
}

const parseErrorMessage = async (response: Response) => {
  try {
    const data = await response.json()
    return data.message || 'Request failed'
  } catch {
    return 'Request failed'
  }
}

export interface DiscussionItem {
  id: string
  title: string
  content: string
  author: string
  tag: string
  likes: number
  bookmarks: number
  likedByMe?: boolean
  bookmarkedByMe?: boolean
  imageName?: string | null
  imageDataUrl?: string | null
  mxlFileName?: string | null
  mxlDataBase64?: string | null
  createdAt: string
}

export interface AnswerItem {
  id: string
  questionId: string
  content: string
  author: string
  upvotes: number
  createdAt: string
}

export interface QuestionItem {
  id: string
  title: string
  content: string
  author: string
  level: string
  resolved: boolean
  acceptedAnswerId: string | null
  answers: AnswerItem[]
  createdAt: string
}

export const listDiscussions = async (): Promise<DiscussionItem[]> => {
  const response = await fetch(`${API_BASE_URL}/api/community/discussions`, {
    headers: authHeaders()
  })
  if (!response.ok) throw new Error(await parseErrorMessage(response))
  return await response.json() as DiscussionItem[]
}

export const createDiscussion = async (payload: {
  title: string
  content: string
  tag: string
  imageName?: string | null
  imageDataUrl?: string | null
  mxlFileName?: string | null
  mxlDataBase64?: string | null
}): Promise<DiscussionItem> => {
  const response = await fetch(`${API_BASE_URL}/api/community/discussions`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(payload)
  })
  if (!response.ok) throw new Error(await parseErrorMessage(response))
  return await response.json() as DiscussionItem
}

export const likeDiscussion = async (id: string): Promise<DiscussionItem> => {
  const response = await fetch(`${API_BASE_URL}/api/community/discussions/${id}/like`, {
    method: 'POST',
    headers: requireAuthHeaders()
  })
  if (!response.ok) throw new Error(await parseErrorMessage(response))
  return await response.json() as DiscussionItem
}

export const bookmarkDiscussion = async (id: string): Promise<DiscussionItem> => {
  const response = await fetch(`${API_BASE_URL}/api/community/discussions/${id}/bookmark`, {
    method: 'POST',
    headers: requireAuthHeaders()
  })
  if (!response.ok) throw new Error(await parseErrorMessage(response))
  return await response.json() as DiscussionItem
}

export const listQuestions = async (openOnly: boolean): Promise<QuestionItem[]> => {
  const response = await fetch(`${API_BASE_URL}/api/community/questions?openOnly=${openOnly ? 'true' : 'false'}`)
  if (!response.ok) throw new Error(await parseErrorMessage(response))
  return await response.json() as QuestionItem[]
}

export const createQuestion = async (payload: {
  title: string
  content: string
  level: string
}): Promise<QuestionItem> => {
  const response = await fetch(`${API_BASE_URL}/api/community/questions`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(payload)
  })
  if (!response.ok) throw new Error(await parseErrorMessage(response))
  return await response.json() as QuestionItem
}

export const createAnswer = async (questionId: string, payload: { content: string }): Promise<AnswerItem> => {
  const response = await fetch(`${API_BASE_URL}/api/community/questions/${questionId}/answers`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(payload)
  })
  if (!response.ok) throw new Error(await parseErrorMessage(response))
  return await response.json() as AnswerItem
}

export const upvoteAnswer = async (questionId: string, answerId: string): Promise<AnswerItem> => {
  const response = await fetch(`${API_BASE_URL}/api/community/questions/${questionId}/answers/${answerId}/upvote`, {
    method: 'POST',
    headers: authHeaders()
  })
  if (!response.ok) throw new Error(await parseErrorMessage(response))
  return await response.json() as AnswerItem
}

export const acceptAnswer = async (questionId: string, answerId: string): Promise<QuestionItem> => {
  const response = await fetch(`${API_BASE_URL}/api/community/questions/${questionId}/accept/${answerId}`, {
    method: 'POST',
    headers: authHeaders()
  })
  if (!response.ok) throw new Error(await parseErrorMessage(response))
  return await response.json() as QuestionItem
}

export const setQuestionResolved = async (questionId: string, resolved: boolean): Promise<QuestionItem> => {
  const response = await fetch(`${API_BASE_URL}/api/community/questions/${questionId}/resolved`, {
    method: 'PATCH',
    headers: authHeaders(),
    body: JSON.stringify({ resolved })
  })
  if (!response.ok) throw new Error(await parseErrorMessage(response))
  return await response.json() as QuestionItem
}
