import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, request, requestSession, showError } from '../api'
import type { Api, Session } from '../types'

export function useSession() {
  const [session, updateSession] = useState<Session | null>()
  const [restoreError, setRestoreError] = useState('')
  const [loggingOut, setLoggingOut] = useState(false)
  const current = useRef<Session | null | undefined>(undefined)
  const refreshing = useRef<Promise<Session> | null>(null)
  const signingOut = useRef(false)

  const setSession = useCallback((value: Session | null) => {
    current.current = value
    updateSession(value)
  }, [])

  const refresh = useCallback(() => {
    if (signingOut.current) return Promise.reject(new ApiError(401))
    const previous = current.current
    refreshing.current ??= requestSession('refresh')
      .then((value) => {
        if (previous && previous.userId !== value.userId) throw new ApiError(401)
        if (!signingOut.current) setSession(value)
        return value
      })
      .catch((error: unknown) => {
        if (error instanceof ApiError && error.status === 401) setSession(null)
        throw error
      })
      .finally(() => { refreshing.current = null })
    return refreshing.current
  }, [setSession])

  const restore = useCallback(() => {
    return refresh().catch((error: unknown) => {
      if (!(error instanceof ApiError && error.status === 401)) {
        setRestoreError(error instanceof Error ? error.message : 'Не удалось восстановить вход.')
      }
    })
  }, [refresh])

  useEffect(() => {
    sessionStorage.removeItem('lexify-session')
    void restore()
  }, [restore])

  useEffect(() => {
    if (!session) return
    const remaining = session.expiresAt - Date.now()
    let timer: ReturnType<typeof setTimeout>
    let active = true
    const renew = async () => {
      try {
        await refresh()
      } catch (error) {
        if (active && !signingOut.current && !(error instanceof ApiError && error.status === 401)) {
          timer = setTimeout(renew, 30_000)
        }
      }
    }
    timer = setTimeout(renew, Math.max(0, remaining > 30_000 ? remaining - 30_000 : remaining + 1000))
    return () => {
      active = false
      clearTimeout(timer)
    }
  }, [session, refresh])

  const api: Api = useCallback(async <T,>(path: string, method?: string, body?: unknown) => {
    const before = current.current
    if (!before || signingOut.current) throw new ApiError(401)
    try {
      return await request<T>(path, before.accessToken, method, body)
    } catch (error) {
      if (!(error instanceof ApiError && error.status === 401) || !current.current || signingOut.current) {
        throw error
      }
      const fresh = current.current === before ? await refresh() : current.current
      if (signingOut.current || current.current !== fresh) throw error
      return request<T>(path, fresh.accessToken, method, body)
    }
  }, [refresh])

  const logout = async () => {
    if (signingOut.current) return
    signingOut.current = true
    setLoggingOut(true)
    try {
      await refreshing.current?.catch(() => undefined)
      await request('/auth/logout', undefined, 'POST')
      setSession(null)
    } catch (error) {
      showError(error)
    } finally {
      signingOut.current = false
      setLoggingOut(false)
    }
  }

  return { session, setSession, restore, restoreError, api, logout, loggingOut }
}
