import { describe, expect, it } from 'vitest'
import { NAME_MAX_LENGTH, serverErrorsOf, validateSubmission } from './validation'

const valid = { name: 'Mari Maasikas', sectorIds: [6, 42], agreedToTerms: true }

describe('validateSubmission', () => {
  it('accepts a complete form', () => {
    expect(validateSubmission(valid)).toEqual({})
  })

  it('requires a non-blank name', () => {
    expect(validateSubmission({ ...valid, name: '' })).toEqual({ name: 'Name is required' })
    expect(validateSubmission({ ...valid, name: '   ' })).toEqual({ name: 'Name is required' })
  })

  it('limits the name length after trimming', () => {
    expect(validateSubmission({ ...valid, name: 'x'.repeat(NAME_MAX_LENGTH + 1) })).toEqual({
      name: `Name must be at most ${NAME_MAX_LENGTH} characters`,
    })
    expect(validateSubmission({ ...valid, name: ` ${'x'.repeat(NAME_MAX_LENGTH)} ` })).toEqual({})
  })

  it('requires at least one sector', () => {
    expect(validateSubmission({ ...valid, sectorIds: [] })).toEqual({ sectorIds: 'Select at least one sector' })
  })

  it('requires agreeing to the terms', () => {
    expect(validateSubmission({ ...valid, agreedToTerms: false })).toEqual({
      agreedToTerms: 'You must agree to the terms',
    })
  })

  it('reports every invalid field at once', () => {
    expect(validateSubmission({ name: '', sectorIds: [], agreedToTerms: false })).toEqual({
      name: 'Name is required',
      sectorIds: 'Select at least one sector',
      agreedToTerms: 'You must agree to the terms',
    })
  })
})

describe('serverErrorsOf', () => {
  it('maps known fields and keeps the first message per field', () => {
    expect(
      serverErrorsOf([
        { field: 'name', message: 'Name is required' },
        { field: 'name', message: 'Name must be at most 255 characters' },
        { field: 'sectorIds', message: 'Select at least one sector' },
      ]),
    ).toEqual({ name: 'Name is required', sectorIds: 'Select at least one sector' })
  })

  it('ignores fields the form does not have', () => {
    expect(serverErrorsOf([{ field: 'something', message: 'x' }])).toEqual({})
  })
})
