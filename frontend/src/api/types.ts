export interface Sector {
  id: number
  name: string
  children: Sector[]
}

export interface Submission {
  id: number
  name: string
  sectorIds: number[]
  agreedToTerms: boolean
  createdAt: string
  updatedAt: string
}

export interface SubmissionInput {
  name: string
  sectorIds: number[]
  agreedToTerms: boolean
}

export interface FieldViolation {
  field: string
  message: string
}

/** RFC 9457 problem details as returned by the backend; `errors` is present on validation failures. */
export interface Problem {
  status: number
  title?: string
  detail?: string
  errors?: FieldViolation[]
}
