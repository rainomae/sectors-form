import type { FieldViolation, SubmissionInput } from '../api/types'

export const NAME_MAX_LENGTH = 255

type Field = keyof SubmissionInput

export type FormErrors = Partial<Record<Field, string>>

export const FIELDS: Field[] = ['name', 'sectorIds', 'agreedToTerms']

/** The same rules as on the server; run before sending so nothing invalid leaves the browser. */
export function validateSubmission(input: SubmissionInput): FormErrors {
  const errors: FormErrors = {}
  const name = input.name.trim()
  if (name === '') {
    errors.name = 'Name is required'
  } else if (name.length > NAME_MAX_LENGTH) {
    errors.name = `Name must be at most ${NAME_MAX_LENGTH} characters`
  }
  if (input.sectorIds.length === 0) {
    errors.sectorIds = 'Select at least one sector'
  }
  if (!input.agreedToTerms) {
    errors.agreedToTerms = 'You must agree to the terms'
  }
  return errors
}

/** Maps the backend's field violations onto the form fields; the first message per field wins. */
export function serverErrorsOf(violations: FieldViolation[]): FormErrors {
  const errors: FormErrors = {}
  for (const { field, message } of violations) {
    if (isField(field) && !errors[field]) {
      errors[field] = message
    }
  }
  return errors
}

function isField(field: string): field is Field {
  return (FIELDS as string[]).includes(field)
}
