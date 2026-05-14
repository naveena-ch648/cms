import { useEffect, useRef, useCallback } from "react";

/**
 * Runs `fn` immediately, then every `intervalMs` milliseconds.
 * Cleans up on unmount. Restart when `deps` change.
 */
export function usePolling(fn: () => void, intervalMs: number, enabled = true) {
  const fnRef = useRef(fn);
  fnRef.current = fn;

  useEffect(() => {
    if (!enabled) return;
    fnRef.current();
    const id = setInterval(() => fnRef.current(), intervalMs);
    return () => clearInterval(id);
  }, [intervalMs, enabled]);
}

/**
 * Returns a stable `refresh` callback and an `intervalMs`-based auto-refresh.
 * Useful when you need to both auto-poll and manually trigger a refresh.
 */
export function useAutoRefresh(
  fn: () => void,
  intervalMs: number,
  enabled = true,
) {
  const fnRef = useRef(fn);
  fnRef.current = fn;

  const refresh = useCallback(() => {
    fnRef.current();
  }, []);

  useEffect(() => {
    if (!enabled) return;
    const id = setInterval(() => fnRef.current(), intervalMs);
    return () => clearInterval(id);
  }, [intervalMs, enabled]);

  return refresh;
}
