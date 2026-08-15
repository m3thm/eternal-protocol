import { useEffect, useRef } from 'react'

/**
 * Calls onOutside when a click/tap or Escape happens outside the returned
 * ref's element. Used by every dropdown in the navbar (category menus,
 * user menu) so they close consistently without each one reimplementing
 * the same listener logic.
 */
export function useClickOutside<T extends HTMLElement>(onOutside: () => void, active: boolean) {
  const ref = useRef<T>(null)

  useEffect(() => {
    if (!active) return

    function handlePointer(e: MouseEvent | TouchEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) onOutside()
    }
    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onOutside()
    }

    document.addEventListener('mousedown', handlePointer)
    document.addEventListener('touchstart', handlePointer)
    document.addEventListener('keydown', handleKey)
    return () => {
      document.removeEventListener('mousedown', handlePointer)
      document.removeEventListener('touchstart', handlePointer)
      document.removeEventListener('keydown', handleKey)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active])

  return ref
}
