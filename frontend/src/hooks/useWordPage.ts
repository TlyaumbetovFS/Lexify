import { useState } from 'react'
import type { Api, Word } from '../types'
import { useResource } from './useResource'

export function useWordPage(api: Api, scope: string | null = '') {
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)
  const [previousScope, setPreviousScope] = useState(scope)
  if (previousScope !== scope) {
    setPreviousScope(scope)
    setPage(1)
    setSearch('')
  }
  const path = scope === null ? null : `/words/page?page=${page}&query=${encodeURIComponent(search)}${scope}`
  const resource = useResource<{ items: Word[], pages: number, total: number }>(api, path)
  if (resource.data && page > Math.max(1, resource.data.pages)) {
    setPage(Math.max(1, resource.data.pages))
  }
  function changeSearch(value: string) {
    setSearch(value)
    setPage(1)
  }
  return { ...resource, page, setPage, search, changeSearch }
}
