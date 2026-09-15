import { useLayoutEffect, useRef, useState, type ChangeEvent } from 'react'
import { filterOptions, type SectorOption } from './flattenSectors'

interface Props {
  options: SectorOption[]
  value: number[]
  onChange: (sectorIds: number[]) => void
  error?: string
}

const GROUP_COLOURS = 6

/** The sectors above the given one, top level first; empty for a top-level sector. */
function ancestorsOf(option: SectorOption | undefined, byId: Map<number, SectorOption>): SectorOption[] {
  const ancestors: SectorOption[] = []
  for (let parent = option && parentOf(option, byId); parent; parent = parentOf(parent, byId)) {
    ancestors.unshift(parent)
  }
  return ancestors
}

function parentOf(option: SectorOption, byId: Map<number, SectorOption>): SectorOption | undefined {
  return option.parentId === null ? undefined : byId.get(option.parentId)
}

/** CSS class that gives a top-level sector and everything under it one shared colour. */
function groupClass(option: SectorOption): string {
  return `group-${option.rootIndex % GROUP_COLOURS}`
}

/**
 * The "Sectors" select box. A plain click toggles one sector without dropping the others, the
 * filter field narrows the list (keeping parents so the hierarchy stays readable) and the selected
 * sectors are shown as removable chips with their full path.
 */
export function SectorSelect({ options, value, onChange, error }: Props) {
  const [query, setQuery] = useState('')
  const [topValue, setTopValue] = useState<string | null>(null)
  const selectRef = useRef<HTMLSelectElement>(null)
  const keepScroll = useRef<number | null>(null)
  const visible = filterOptions(options, query)
  const selected = options.filter((option) => value.includes(option.id))
  // While a group's header row is scrolled out of view, a breadcrumb over the list names the group and
  // scrolls back to it. It is derived from the row currently at the top of the list.
  const byId = new Map(options.map((option) => [option.id, option]))
  const topOption = (topValue !== null && byId.get(Number(topValue))) || visible[0]
  const breadcrumb = ancestorsOf(topOption, byId)

  // Changing the selection makes the browser scroll the list to the selected row, partly after its own
  // layout pass. For a short moment after a click, keep the list where the user had it.
  useLayoutEffect(restoreScroll)

  function restoreScroll() {
    const select = selectRef.current
    const keep = keepScroll.current
    if (select && keep !== null && select.scrollTop !== keep) {
      select.scrollTop = keep
    }
  }

  function handleScroll() {
    restoreScroll()
    updateTopValue()
  }

  // The first row at least half visible at the top of the list.
  function updateTopValue() {
    const select = selectRef.current
    if (select) {
      const listTop = select.getBoundingClientRect().top
      const topRow = Array.from(select.options).find((row) => {
        const rect = row.getBoundingClientRect()
        return rect.bottom - listTop > rect.height / 2
      })
      setTopValue(topRow ? topRow.value : null)
    }
  }

  function scrollToOption(option: SectorOption) {
    const select = selectRef.current
    const row = select && Array.from(select.options).find((element) => element.value === String(option.id))
    if (select && row) {
      const paddingTop = parseFloat(getComputedStyle(select).paddingTop)
      select.scrollTop += row.getBoundingClientRect().top - select.getBoundingClientRect().top - paddingTop
      updateTopValue()
    }
  }

  function toggle(id: number) {
    const select = selectRef.current
    if (select) {
      keepScroll.current = select.scrollTop
      window.setTimeout(() => {
        keepScroll.current = null
      }, 300)
    }
    onChange(value.includes(id) ? value.filter((selectedId) => selectedId !== id) : [...value, id])
  }

  // Keyboard selection replaces the native selection; keep the ids that are selected but filtered out of view.
  function handleChange(event: ChangeEvent<HTMLSelectElement>) {
    const visibleIds = new Set(visible.map((option) => option.id))
    const hidden = value.filter((id) => !visibleIds.has(id))
    onChange([...hidden, ...Array.from(event.target.selectedOptions, (option) => Number(option.value))])
  }

  return (
    <div className="field">
      <label htmlFor="sectorIds">
        Sectors <span className="required" aria-hidden="true">*</span>
      </label>
      <p id="sectorIds-hint" className="hint">
        Click a sector to select or deselect it; every row, including the main sectors, can be chosen. Selected
        sectors are highlighted and listed below the box. Type below to filter the list.
      </p>
      <div className="filter-wrap">
        <svg aria-hidden="true" viewBox="0 0 20 20" width="16" height="16">
          <path
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            d="M9 15a6 6 0 1 0 0-12 6 6 0 0 0 0 12Zm7 2-3.5-3.5"
          />
        </svg>
        <input
          type="search"
          className="filter"
        placeholder="Filter sectors…"
        aria-label="Filter sectors"
        aria-controls="sectorIds"
        value={query}
        onChange={(event) => {
          setQuery(event.target.value)
          setTopValue(null)
        }}
        />
      </div>
      <div className="list">
        {breadcrumb.length > 0 && (
          <nav className={`list-breadcrumb ${groupClass(breadcrumb[0])}`} aria-label="Group of the visible sectors">
            <span aria-hidden="true">↑</span>
            {breadcrumb.map((ancestor, index) => (
              <span key={ancestor.id}>
                {index > 0 && <span aria-hidden="true"> › </span>}
                <button type="button" onClick={() => scrollToOption(ancestor)}>
                  {ancestor.name}
                </button>
              </span>
            ))}
          </nav>
        )}
      <select
        ref={selectRef}
        id="sectorIds"
        name="sectorIds"
        multiple
        size={12}
        value={value.map(String)}
        aria-required="true"
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? 'sectorIds-hint sectorIds-error' : 'sectorIds-hint'}
        onChange={handleChange}
        onScroll={handleScroll}
      >
        {visible.map((option) => (
          <option
            key={option.id}
            value={option.id}
            className={`level-${option.level} ${groupClass(option)}`}
            onMouseDown={(event) => {
              // Toggle without moving focus into the list: a focused list box paints selected rows in the
              // browser's own colours, which cannot be overridden.
              event.preventDefault()
              toggle(option.id)
            }}
          >
            {option.name}
          </option>
        ))}
      </select>
        {query.trim() !== '' && visible.length === 0 && (
          <p className="list-empty" role="status">
            No sectors match “{query.trim()}”.
          </p>
        )}
      </div>
      {error && (
        <p id="sectorIds-error" className="error">
          {error}
        </p>
      )}
      <div className="selection">
        <div className="selection-header">
          <span className="count">
            {selected.length === 0 ? 'No sectors selected yet' : `${selected.length} selected`}
          </span>
          {selected.length > 0 && (
            <button type="button" className="link-button" onClick={() => onChange([])}>
              Clear all
            </button>
          )}
        </div>
        {selected.length > 0 && (
          <ul className="chips" aria-label="Selected sectors">
            {selected.map((option) => (
              <li key={option.id} className={`chip ${groupClass(option)}`}>
                <span className="chip-dot" aria-hidden="true" />
                <span>{option.path}</span>
                <button
                  type="button"
                  className="chip-remove"
                  aria-label={`Remove ${option.path}`}
                  onClick={() => toggle(option.id)}
                >
                  <svg aria-hidden="true" viewBox="0 0 12 12" width="10" height="10">
                    <path d="M2 2l8 8M10 2l-8 8" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
                  </svg>
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
