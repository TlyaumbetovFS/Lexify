import { useCallback, useEffect, useRef, useState } from 'react'
import type { Api } from '../types'

export function useResource<T>(api: Api, path: string | null) {
  const [data, setData] = useState<T>()
  const previousPath = useRef(path)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [revision, setRevision] = useState(0)
  const reload = useCallback(() => setRevision((value) => value + 1), [])

  useEffect(() => {
    let active = true
    if (previousPath.current !== path) {
      setData(undefined)
      previousPath.current = path
    }
    if (!path) {
      setData(undefined)
      setLoading(false)
      setError('')
      return
    }
    setLoading(true)
    setError('')
    api<T>(path)
      .then((value) => {
        if (active) setData(value)
      })
      .catch((error) => {
        if (active) setError(error.message)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [api, path, revision])

  return { data, error, loading, reload }
}
