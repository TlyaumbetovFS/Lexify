import { useCallback, useEffect, useState } from 'react'
import type { Api } from '../types'

type Result<T> = {
  api: Api
  path: string
  revision: number
  data?: T
  error: string
}

export function useResource<T>(api: Api, path: string | null) {
  const [result, setResult] = useState<Result<T>>()
  const [revision, setRevision] = useState(0)
  const reload = useCallback(() => setRevision((value) => value + 1), [])
  const current = result?.api === api && result.path === path ? result : undefined
  const loading = path !== null && current?.revision !== revision

  useEffect(() => {
    if (!path) return
    let active = true
    api<T>(path)
      .then((data) => {
        if (active) setResult({ api, path, revision, data, error: '' })
      })
      .catch((error: unknown) => {
        if (active) {
          setResult({
            api,
            path,
            revision,
            error: error instanceof Error ? error.message : 'Не удалось загрузить данные.',
          })
        }
      })
    return () => {
      active = false
    }
  }, [api, path, revision])

  return { data: current?.data, error: loading ? '' : current?.error ?? '', loading, reload }
}
