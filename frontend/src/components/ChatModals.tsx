import { useState } from 'react'
import { ActionIcon, Badge, Button, Group, Modal, NumberInput, Stack, Text, Textarea } from '@mantine/core'
import { ShieldCheck, X } from 'lucide-react'
import { showError } from '../api'
import type { Api, ChatMessage, Room } from '../types'

export function EditMessage({
  api,
  message,
  onClose,
  onSaved,
}: {
  api: Api
  message: ChatMessage
  onClose: () => void
  onSaved: () => void
}) {
  const [text, setText] = useState(message.text)
  const [busy, setBusy] = useState(false)
  return (
    <Modal opened title="Редактировать сообщение" onClose={() => !busy && onClose()} centered>
      <form
        onSubmit={async (event) => {
          event.preventDefault()
          setBusy(true)
          try {
            await api(`/rooms/${message.roomId}/messages/${message.id}`, 'PUT', {
              text: text.trim(),
            })
            onSaved()
            onClose()
          } catch (error) {
            showError(error)
          } finally {
            setBusy(false)
          }
        }}
      >
        <Stack>
          <Textarea
            label="Сообщение"
            value={text}
            onChange={(event) => setText(event.currentTarget.value)}
            maxLength={2000}
            minRows={3}
            autoFocus
            required
            disabled={busy}
          />
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose} disabled={busy}>
              Отмена
            </Button>
            <Button type="submit" loading={busy} disabled={!text.trim()}>
              Сохранить
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}

export function ModeratorsModal({
  api,
  room,
  onClose,
  onSaved,
}: {
  api: Api
  room: Room
  onClose: () => void
  onSaved: () => void
}) {
  const [id, setId] = useState<string | number>('')
  const [busy, setBusy] = useState(false)
  async function change(userId: number, method: string) {
    setBusy(true)
    try {
      await api(`/rooms/${room.id}/moderators/${userId}`, method)
      setId('')
      onSaved()
    } catch (error) {
      showError(error)
    } finally {
      setBusy(false)
    }
  }
  return (
    <Modal opened centered title="Модераторы комнаты" onClose={() => !busy && onClose()}>
      <Stack>
        <Text size="sm" c="dimmed">
          Модераторы могут удалять сообщения в этой комнате. ID участника виден рядом с его
          сообщениями; свой ID можно найти в меню слева.
        </Text>
        {room.moderatorIds.map((userId) => (
          <Group key={userId} justify="space-between">
            <Group gap="xs">
              <ShieldCheck size={17} color="#3775f6" />
              <Text size="sm">Участник #{userId}</Text>
              {userId === room.ownerId && (
                <Badge size="xs" variant="light">
                  Создатель
                </Badge>
              )}
            </Group>
            {userId !== room.ownerId && (
              <ActionIcon
                variant="subtle"
                color="gray"
                aria-label={`Снять модератора ${userId}`}
                disabled={busy}
                onClick={() => change(userId, 'DELETE')}
              >
                <X size={15} />
              </ActionIcon>
            )}
          </Group>
        ))}
        <NumberInput
          label="ID пользователя"
          placeholder="Например, 12"
          min={1}
          max={Number.MAX_SAFE_INTEGER}
          allowDecimal={false}
          allowNegative={false}
          value={id}
          onChange={setId}
          disabled={busy}
        />
        <Button
          loading={busy}
          disabled={!Number.isSafeInteger(Number(id)) || Number(id) < 1}
          onClick={() => change(Number(id), 'PUT')}
        >
          Назначить модератором
        </Button>
      </Stack>
    </Modal>
  )
}
