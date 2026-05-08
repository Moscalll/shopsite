import { useCallback, useRef } from 'react'

export function useThrottledCallback<T extends (...args: never[]) => void>(
  fn: T,
  delayMs: number,
): T {
  const last = useRef(0)

  return useCallback(
    ((...args: never[]) => {
      const now = Date.now()
      if (now - last.current >= delayMs) {
        last.current = now
        fn(...args)
      }
    }) as T,
    [fn, delayMs],
  )
}
