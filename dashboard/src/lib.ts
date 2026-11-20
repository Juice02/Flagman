import { useCallback, useEffect, useState } from 'react'

// Loads data on mount and whenever `deps` change. `reload` fetches again; `setData` applies a local change.
export function useLoad<T>(load: () => Promise<T>, deps: unknown[]) {
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    let current = true // ignore a response that arrives after the inputs changed
    setError(null)
    load().then(
      (result) => current && setData(result),
      (e: Error) => current && setError(e.message),
    )
    return () => {
      current = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, version])

  const reload = useCallback(() => setVersion((v) => v + 1), [])
  return { data, error, reload, setData }
}

export const formatDate = (iso: string) =>
  new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
