import { CloseIcon } from './CloseIcon'

interface ToastProps {
  message: string | null
  onClose: () => void
}

/**
 * A confirmation in the corner of the page that stays until the user closes it. Only the text sits in the live
 * region, so screen readers announce the message and not the close button; the region is always present so the
 * announcement happens when the text appears.
 */
export function Toast({ message, onClose }: ToastProps) {
  return (
    <div className="toast-region">
      <div role="status" aria-live="polite">
        {message !== null && (
          <div className="toast">
            <span className="toast-icon" aria-hidden="true">
              ✓
            </span>
            <span>{message}</span>
          </div>
        )}
      </div>
      {message !== null && (
        <button type="button" className="icon-button toast-close" aria-label="Dismiss" onClick={onClose}>
          <CloseIcon />
        </button>
      )}
    </div>
  )
}
