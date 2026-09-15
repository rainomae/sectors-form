import type { Problem, Sector, Submission, SubmissionInput } from './types'

export class ApiError extends Error {
  readonly status: number
  readonly problem: Problem | undefined

  constructor(status: number, problem: Problem | undefined) {
    super(problem?.detail ?? `Request failed with status ${status}`)
    this.status = status
    this.problem = problem
  }
}

export function fetchSectors(): Promise<Sector[]> {
  return request('/api/sectors')
}

export async function fetchCurrentSubmission(): Promise<Submission | null> {
  try {
    return await request<Submission>('/api/submissions/current')
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return null
    }
    throw error
  }
}

export function createSubmission(input: SubmissionInput): Promise<Submission> {
  return request('/api/submissions', { method: 'POST', body: JSON.stringify(input) })
}

export function updateSubmission(id: number, input: SubmissionInput): Promise<Submission> {
  return request(`/api/submissions/${id}`, { method: 'PUT', body: JSON.stringify(input) })
}

async function request<T>(url: string, init: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json, application/problem+json' }
  if (init.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  const response = await fetch(url, { ...init, headers })
  if (!response.ok) {
    const problem = (await response.json().catch(() => undefined)) as Problem | undefined
    throw new ApiError(response.status, problem)
  }
  return response.json() as Promise<T>
}
