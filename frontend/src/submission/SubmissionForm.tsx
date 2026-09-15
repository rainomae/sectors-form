import { useEffect, useState, type FormEvent } from 'react'
import { ApiError, createSubmission, fetchCurrentSubmission, fetchSectors, updateSubmission } from '../api/client'
import type { Submission, SubmissionInput } from '../api/types'
import { flattenSectors, type SectorOption } from '../sectors/flattenSectors'
import { SectorSelect } from '../sectors/SectorSelect'
import { NAME_MAX_LENGTH, serverErrorsOf, validateSubmission, type FormErrors } from './validation'

const EMPTY: SubmissionInput = { name: '', sectorIds: [], agreedToTerms: false }

type Status = 'loading' | 'failed' | 'ready' | 'saving'

interface Message {
  kind: 'success' | 'error'
  text: string
}

/**
 * The form. On load it fetches the sectors and the submission saved in this session, if any; Save
 * creates or updates that submission and refills the form from what the server stored.
 */
export function SubmissionForm() {
  const [options, setOptions] = useState<SectorOption[]>([])
  const [submission, setSubmission] = useState<Submission | null>(null)
  const [values, setValues] = useState<SubmissionInput>(EMPTY)
  const [errors, setErrors] = useState<FormErrors>({})
  const [status, setStatus] = useState<Status>('loading')
  const [message, setMessage] = useState<Message | null>(null)
  const [loadAttempt, setLoadAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    Promise.all([fetchSectors(), fetchCurrentSubmission()])
      .then(([sectors, current]) => {
        if (cancelled) return
        setOptions(flattenSectors(sectors))
        if (current) {
          setSubmission(current)
          setValues(toInput(current))
        }
        setStatus('ready')
      })
      .catch(() => {
        if (!cancelled) setStatus('failed')
      })
    return () => {
      cancelled = true
    }
  }, [loadAttempt])

  // Fields that already show an error are re-checked as they change, so the message disappears as soon as
  // the value is valid. Fields without an error stay quiet until the next Save.
  function change(patch: Partial<SubmissionInput>) {
    const next = { ...values, ...patch }
    setValues(next)
    if (Object.keys(errors).length > 0) {
      const current = validateSubmission(next)
      const remaining: FormErrors = {}
      for (const field of Object.keys(errors) as (keyof FormErrors)[]) {
        if (current[field]) {
          remaining[field] = current[field]
        }
      }
      setErrors(remaining)
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setMessage(null)
    const validationErrors = validateSubmission(values)
    setErrors(validationErrors)
    if (Object.keys(validationErrors).length > 0) {
      focusFirstInvalid(validationErrors)
      return
    }

    setStatus('saving')
    try {
      const saved = submission ? await updateSubmission(submission.id, values) : await createSubmission(values)
      setSubmission(saved)
      setValues(toInput(saved))
      setMessage({ kind: 'success', text: 'Saved. You can keep editing your data during this session.' })
    } catch (error) {
      await handleSaveError(error)
    } finally {
      setStatus('ready')
    }
  }

  async function handleSaveError(error: unknown) {
    if (!(error instanceof ApiError)) {
      setMessage({ kind: 'error', text: 'Could not reach the server. Please try again.' })
      return
    }
    if (error.status === 400 && error.problem?.errors) {
      const serverErrors = serverErrorsOf(error.problem.errors)
      setErrors(serverErrors)
      focusFirstInvalid(serverErrors)
      return
    }
    if (error.status === 409) {
      // This session already saved data (for example in another tab): load it and continue editing that.
      const current = await fetchCurrentSubmission()
      if (current) {
        setSubmission(current)
        setValues(toInput(current))
      }
      setMessage({ kind: 'error', text: 'This session already has saved data; it has been loaded so you can edit it.' })
      return
    }
    if (submission && (error.status === 403 || error.status === 404)) {
      setSubmission(null)
      setMessage({ kind: 'error', text: 'Your session has expired. Press Save again to store your data as a new entry.' })
      return
    }
    setMessage({ kind: 'error', text: error.problem?.detail ?? 'Saving failed. Please try again.' })
  }

  if (status === 'loading') {
    return (
      <p className="status" role="status">
        Loading…
      </p>
    )
  }

  if (status === 'failed') {
    return (
      <p className="message error" role="alert">
        Could not load the form.{' '}
        <button
          type="button"
          onClick={() => {
            setStatus('loading')
            setLoadAttempt(loadAttempt + 1)
          }}
        >
          Try again
        </button>
      </p>
    )
  }

  const saving = status === 'saving'
  return (
    <form onSubmit={handleSubmit} noValidate>
      {submission && (
        <p className="editing">
          Editing your saved data · last saved {new Date(submission.updatedAt).toLocaleString()}
        </p>
      )}

      <p className="hint">
        Fields marked with <span className="required">*</span> are required.
      </p>

      <div className="field">
        <label htmlFor="name">
          Name <span className="required" aria-hidden="true">*</span>
        </label>
        <input
          id="name"
          name="name"
          type="text"
          autoComplete="name"
          placeholder="e.g. John Doe"
          maxLength={NAME_MAX_LENGTH}
          value={values.name}
          aria-required="true"
          aria-invalid={errors.name ? true : undefined}
          aria-describedby={errors.name ? 'name-error' : undefined}
          onChange={(event) => change({ name: event.target.value })}
        />
        {errors.name && (
          <p id="name-error" className="error">
            {errors.name}
          </p>
        )}
      </div>

      <SectorSelect
        options={options}
        value={values.sectorIds}
        error={errors.sectorIds}
        onChange={(sectorIds) => change({ sectorIds })}
      />

      <div className="field">
        <label className="checkbox" htmlFor="agreedToTerms">
          <input
            id="agreedToTerms"
            name="agreedToTerms"
            type="checkbox"
            checked={values.agreedToTerms}
            aria-required="true"
            aria-invalid={errors.agreedToTerms ? true : undefined}
            aria-describedby={errors.agreedToTerms ? 'agreedToTerms-error' : undefined}
            onChange={(event) => change({ agreedToTerms: event.target.checked })}
          />
          Agree to terms <span className="required" aria-hidden="true">*</span>
        </label>
        {errors.agreedToTerms && (
          <p id="agreedToTerms-error" className="error">
            {errors.agreedToTerms}
          </p>
        )}
      </div>

      <div className="actions">
        <button type="submit" disabled={saving}>
          {saving ? 'Saving…' : 'Save'}
        </button>
        {message && (
          <p className={`message ${message.kind}`} role={message.kind === 'error' ? 'alert' : 'status'}>
            {message.text}
          </p>
        )}
      </div>
    </form>
  )
}

function toInput(submission: Submission): SubmissionInput {
  return { name: submission.name, sectorIds: submission.sectorIds, agreedToTerms: submission.agreedToTerms }
}

function focusFirstInvalid(errors: FormErrors) {
  const first = (['name', 'sectorIds', 'agreedToTerms'] as const).find((field) => errors[field])
  if (first) {
    document.getElementById(first)?.focus()
  }
}
