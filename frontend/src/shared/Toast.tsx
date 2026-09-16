interface ToastProps {
  message: string | null
  onClose: () => void
}

/**
 * A confirmation in the corner of the page. It stays until the user closes it with the X in its corner. The
 * live region is always present so that screen readers announce the text when it appears.
 */
export function Toast({ message, onClose }: ToastProps) {
  return (
    <div className="toast-region" role="status" aria-live="polite">
      {message !== null && (
        <div className="toast">
          <span className="toast-icon" aria-hidden="true">
            ✓
          </span>
          <span>{message}</span>
          <button type="button" className="toast-close" aria-label="Dismiss" onClick={onClose}>
            <svg aria-hidden="true" viewBox="0 0 12 12" width="10" height="10">
              <path d="M2 2l8 8M10 2l-8 8" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
            </svg>
          </button>
        </div>
      )}
    </div>
  )
}
