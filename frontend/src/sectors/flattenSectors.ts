import type { Sector } from '../api/types'

export interface SectorOption {
  id: number
  name: string
  parentId: number | null
  /** 0 for a top-level sector, 1 for its children, and so on. */
  level: number
  /** Position of the top-level ancestor among the top-level sectors; used to colour each group. */
  rootIndex: number
  /** Full path from the top-level sector, e.g. "Manufacturing › Food and Beverage › Other". */
  path: string
}

/** Turns the sector tree into the depth-first list shown in the select box. */
export function flattenSectors(sectors: Sector[], level = 0, parent: SectorOption | null = null): SectorOption[] {
  return sectors.flatMap((sector, index) => {
    const option: SectorOption = {
      id: sector.id,
      name: sector.name,
      parentId: parent?.id ?? null,
      level,
      rootIndex: parent ? parent.rootIndex : index,
      path: parent ? `${parent.path} › ${sector.name}` : sector.name,
    }
    return [option, ...flattenSectors(sector.children, level + 1, option)]
  })
}

/**
 * Options matching the query plus their ancestors, so the hierarchy stays readable. Every word of the
 * query must appear somewhere in the option's full path, case-insensitively and in any order, so a
 * partial word, several words, or a parent's name all work ("food" shows the whole Food and Beverage group).
 */
export function filterOptions(options: SectorOption[], query: string): SectorOption[] {
  const words = query.toLowerCase().split(/\s+/).filter(Boolean)
  if (words.length === 0) {
    return options
  }
  const byId = new Map(options.map((option) => [option.id, option]))
  const keep = new Set<number>()
  for (const option of options) {
    const path = option.path.toLowerCase()
    if (words.every((word) => path.includes(word))) {
      for (let current: SectorOption | undefined = option; current; current = parentOf(current, byId)) {
        keep.add(current.id)
      }
    }
  }
  return options.filter((option) => keep.has(option.id))
}

export function parentOf(option: SectorOption, byId: Map<number, SectorOption>): SectorOption | undefined {
  return option.parentId === null ? undefined : byId.get(option.parentId)
}
