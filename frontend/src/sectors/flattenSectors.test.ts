import { describe, expect, it } from 'vitest'
import { filterOptions, flattenSectors } from './flattenSectors'

const tree = [
  {
    id: 1,
    name: 'Manufacturing',
    children: [
      { id: 6, name: 'Food and Beverage', children: [{ id: 437, name: 'Other', children: [] }] },
      { id: 13, name: 'Furniture', children: [] },
    ],
  },
  { id: 3, name: 'Other', children: [] },
]

describe('flattenSectors', () => {
  it('lists sectors depth-first with their level, parent and full path', () => {
    const options = flattenSectors(tree)

    expect(options.map((option) => [option.id, option.level, option.parentId, option.rootIndex])).toEqual([
      [1, 0, null, 0],
      [6, 1, 1, 0],
      [437, 2, 6, 0],
      [13, 1, 1, 0],
      [3, 0, null, 1],
    ])
    expect(options[2].path).toBe('Manufacturing › Food and Beverage › Other')
    expect(options[4].path).toBe('Other')
  })

  it('returns an empty list for no sectors', () => {
    expect(flattenSectors([])).toEqual([])
  })
})

describe('filterOptions', () => {
  const options = flattenSectors(tree)

  it('returns everything for an empty or blank query', () => {
    expect(filterOptions(options, '')).toEqual(options)
    expect(filterOptions(options, '   ')).toEqual(options)
  })

  it('keeps matching sectors and their ancestors, in the original order', () => {
    expect(filterOptions(options, 'other').map((option) => option.id)).toEqual([1, 6, 437, 3])
    expect(filterOptions(options, 'FURNI').map((option) => option.id)).toEqual([1, 13])
  })

  it('matches partial words anywhere in the full path, so a parent name shows its group', () => {
    expect(filterOptions(options, 'food').map((option) => option.id)).toEqual([1, 6, 437])
    expect(filterOptions(options, 'bever').map((option) => option.id)).toEqual([1, 6, 437])
  })

  it('requires every word of the query, in any order', () => {
    expect(filterOptions(options, 'manuf other').map((option) => option.id)).toEqual([1, 6, 437])
    expect(filterOptions(options, 'other  manuf').map((option) => option.id)).toEqual([1, 6, 437])
    expect(filterOptions(options, 'other banking')).toEqual([])
  })

  it('returns nothing when no sector matches', () => {
    expect(filterOptions(options, 'banking')).toEqual([])
  })
})
