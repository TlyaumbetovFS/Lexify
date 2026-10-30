import { Client } from '@stomp/stompjs'
import { useEffect, useRef, useState } from 'react'
import { ApiError } from '../api'
import type { ChatEvent } from '../types'

export function useChat(
  token: string,
  roomId: number | null,
  onEvent: (event: ChatEvent) => void,
  onError: (error: Error) => void,
) {
  const client = useRef<Client | null>(null)
  const callbacks = useRef({ onEvent, onError })
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    callbacks.current = { onEvent, onError }
  }, [onEvent, onError])

  useEffect(() => {
    const connection = new Client({
      brokerURL: `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      connectionTimeout: 10000,
      onConnect: () => {
        connection.subscribe('/topic/rooms', (frame) =>
          callbacks.current.onEvent(JSON.parse(frame.body)),
        )
        connection.subscribe('/user/queue/errors', (frame) => {
          const error = JSON.parse(frame.body)
          callbacks.current.onError(new ApiError(error.status, error.message))
        })
        setConnected(true)
      },
      onWebSocketClose: () => setConnected(false),
      onStompError: () =>
        callbacks.current.onError(
          new Error('Не удалось подключиться к чату. Обновите страницу или войдите ещё раз.'),
        ),
    })
    client.current = connection
    connection.activate()
    return () => {
      client.current = null
      void connection.deactivate()
    }
  }, [token])

  useEffect(() => {
    if (!connected || !roomId || !client.current) return
    const subscription = client.current.subscribe(`/topic/rooms/${roomId}`, (frame) => {
      const event: ChatEvent = JSON.parse(frame.body)
      if (event.message) callbacks.current.onEvent(event)
    })
    return () => subscription.unsubscribe()
  }, [roomId, connected])

  function send(text: string) {
    if (!client.current?.connected || !roomId) throw new Error('Дождитесь подключения к чату.')
    client.current.publish({
      destination: `/app/rooms/${roomId}/messages`,
      body: JSON.stringify({ text }),
    })
  }

  return { connected, send }
}
