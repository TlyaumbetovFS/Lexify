import { useEffect, useRef, useState } from 'react'
import {
  ActionIcon,
  Avatar,
  Badge,
  Button,
  Card,
  Group,
  Menu,
  Modal,
  NumberInput,
  ScrollArea,
  Skeleton,
  Stack,
  Text,
  Textarea,
  TextInput,
  Title,
  UnstyledButton,
} from '@mantine/core'
import {
  Ellipsis,
  Hash,
  MessageCircle,
  Pencil,
  Plus,
  Search,
  Send,
  ShieldCheck,
  Trash2,
  X,
} from 'lucide-react'
import { showError } from '../api'
import { DetailsModal } from '../components/DetailsModal'
import { confirmDelete, EmptyState, LoadError } from '../components/Feedback'
import { useChat } from '../hooks/useChat'
import { useResource } from '../hooks/useResource'
import type { Api, ChatEvent, ChatMessage, Room, Session } from '../types'

export function ChatPage({ api, session }: { api: Api; session: Session }) {
  const rooms = useResource<Room[]>(api, '/rooms')
  const [roomId, setRoomId] = useState<number | null>(null)
  const [roomSearch, setRoomSearch] = useState('')
  const [page, setPage] = useState(0)
  const [newMessages, setNewMessages] = useState(false)
  const [draft, setDraft] = useState('')
  const [pending, setPending] = useState<string | null>(null)
  const [editingRoom, setEditingRoom] = useState<Room | null>()
  const [editingMessage, setEditingMessage] = useState<ChatMessage>()
  const [moderating, setModerating] = useState(false)
  const room = rooms.data?.find((item) => item.id === roomId)
  const history = useResource<ChatMessage[]>(
    api,
    roomId ? `/rooms/${roomId}/messages?page=${page}&size=50` : null,
  )
  const bottom = useRef<HTMLDivElement>(null)
  const isOwner = room?.ownerId === session.userId
  const isModerator = room?.moderatorIds.includes(session.userId)

  useEffect(() => {
    if (rooms.data && !rooms.data.some((item) => item.id === roomId)) {
      setRoomId(rooms.data[0]?.id ?? null)
      setPage(0)
    }
  }, [rooms.data, roomId])

  const chat = useChat(
    session.accessToken,
    roomId,
    (event: ChatEvent) => {
      if (!event.message) {
        rooms.reload()
        if (event.type === 'ROOM_DELETED' && event.roomId === roomId) {
          setRoomId(null)
          setDraft('')
          setPending(null)
        }
      } else if (event.roomId === roomId) {
        if (page === 0 || event.type !== 'MESSAGE_CREATED') history.reload()
        else setNewMessages(true)
        if (
          event.type === 'MESSAGE_CREATED' &&
          event.message.authorId === session.userId &&
          event.message.text === pending
        ) {
          setPending(null)
          setDraft((value) => (value.trim() === pending ? '' : value))
        }
      }
    },
    (error) => {
      setPending(null)
      showError(error)
    },
  )

  useEffect(() => {
    if (page === 0) bottom.current?.scrollIntoView({ block: 'end', behavior: 'smooth' })
  }, [history.data, page])

  useEffect(() => {
    if (!pending) return
    const timer = setTimeout(() => {
      setPending(null)
      showError(
        new Error(
          'Не получили подтверждение отправки. Проверьте историю перед повторной отправкой.',
        ),
      )
    }, 10000)
    return () => clearTimeout(timer)
  }, [pending])

  function send() {
    const text = draft.trim()
    if (!text || pending || !chat.connected) return
    try {
      setPage(0)
      setNewMessages(false)
      setPending(text)
      chat.send(text)
    } catch (error) {
      setPending(null)
      showError(error)
    }
  }

  return (
    <Stack gap={24}>
      <div className="page-heading">
        <div>
          <Text className="eyebrow" c="blue">
            АНГЛИЙСКИЙ ОБЪЕДИНЯЕТ
          </Text>
          <Title order={1}>Разговорные комнаты</Title>
          <Text c="dimmed" size="sm" mt={8}>
            Слова становятся живыми, когда мы общаемся.
          </Text>
        </div>
        <Button leftSection={<Plus size={18} />} onClick={() => setEditingRoom(null)}>
          Создать комнату
        </Button>
      </div>
      {rooms.error ? (
        <LoadError message={rooms.error} retry={rooms.reload} />
      ) : rooms.loading && !rooms.data ? (
        <Skeleton height={500} radius="lg" />
      ) : !rooms.data?.length ? (
        <EmptyState
          title="Начни первый разговор"
          description="Создай комнату на интересную тему. Присоединиться смогут все пользователи Lexify."
          action={
            <Button
              variant="light"
              onClick={() => setEditingRoom(null)}
              leftSection={<Plus size={16} />}
            >
              Создать комнату
            </Button>
          }
        />
      ) : (
        <Card withBorder radius="lg" padding={0} className="chat-layout">
          <aside className="room-list">
            <TextInput
              size="sm"
              placeholder="Найти комнату"
              aria-label="Найти комнату"
              leftSection={<Search size={15} />}
              value={roomSearch}
              onChange={(event) => setRoomSearch(event.currentTarget.value)}
            />
            <Text size="xs" fw={650} c="dimmed" mt="sm" mb={5}>
              КОМНАТЫ · {rooms.data.length}
            </Text>
            <ScrollArea className="room-scroll">
              <Stack gap={5}>
                {rooms.data
                  .filter((item) => item.name.toLowerCase().includes(roomSearch.toLowerCase()))
                  .map((item) => (
                    <UnstyledButton
                      key={item.id}
                      className={`room-link ${roomId === item.id ? 'is-selected' : ''}`}
                      onClick={() => {
                        setRoomId(item.id)
                        setPage(0)
                        setNewMessages(false)
                        setDraft('')
                        setPending(null)
                        setModerating(false)
                      }}
                    >
                      <div className="room-hash">
                        <Hash size={18} />
                      </div>
                      <div>
                        <Text size="sm" fw={650} lineClamp={1}>
                          {item.name}
                        </Text>
                        <Text size="xs" c="dimmed" lineClamp={1}>
                          {item.description || 'Практикуем английский вместе'}
                        </Text>
                      </div>
                    </UnstyledButton>
                  ))}
              </Stack>
            </ScrollArea>
          </aside>
          <section className="conversation">
            {room && (
              <>
                <div className="conversation-header">
                  <Group gap="sm" wrap="nowrap">
                    <div className="room-hash">
                      <Hash size={20} />
                    </div>
                    <div>
                      <Text fw={650}>{room.name}</Text>
                      <Group gap={5}>
                        <span className={`connection-dot ${chat.connected ? 'online' : ''}`} />
                        <Text size="xs" c="dimmed">
                          {chat.connected ? 'Подключено' : 'Подключаемся…'}
                        </Text>
                      </Group>
                    </div>
                  </Group>
                  {isOwner && (
                    <Menu shadow="md" position="bottom-end">
                      <Menu.Target>
                        <ActionIcon variant="subtle" color="gray" aria-label="Настройки комнаты">
                          <Ellipsis size={20} />
                        </ActionIcon>
                      </Menu.Target>
                      <Menu.Dropdown>
                        <Menu.Item
                          leftSection={<Pencil size={15} />}
                          onClick={() => setEditingRoom(room)}
                        >
                          Редактировать комнату
                        </Menu.Item>
                        <Menu.Item
                          leftSection={<ShieldCheck size={15} />}
                          onClick={() => setModerating(true)}
                        >
                          Модераторы
                        </Menu.Item>
                        <Menu.Item
                          color="red"
                          leftSection={<Trash2 size={15} />}
                          onClick={() =>
                            confirmDelete(
                              'Удалить комнату?',
                              'Комната и все сообщения в ней будут удалены.',
                              async () => {
                                await api(`/rooms/${room.id}`, 'DELETE')
                                rooms.reload()
                              },
                            )
                          }
                        >
                          Удалить комнату
                        </Menu.Item>
                      </Menu.Dropdown>
                    </Menu>
                  )}
                </div>
                {room.description && (
                  <Text size="xs" c="dimmed" className="room-description">
                    {room.description}
                  </Text>
                )}
                <div className="history-controls">
                  <Group justify="space-between">
                    <Button
                      variant="subtle"
                      size="compact-xs"
                      disabled={history.loading || (history.data?.length ?? 0) < 50}
                      onClick={() => setPage((value) => value + 1)}
                    >
                      Ранее
                    </Button>
                    <Text size="xs" c="dimmed">
                      {page === 0 ? 'Последние сообщения' : `Страница ${page + 1}`}
                    </Text>
                    <Button
                      variant="subtle"
                      size="compact-xs"
                      disabled={history.loading || page === 0}
                      onClick={() => {
                        setPage((value) => value - 1)
                        setNewMessages(false)
                      }}
                    >
                      Новее
                    </Button>
                  </Group>
                  {newMessages && (
                    <Button
                      size="xs"
                      variant="light"
                      fullWidth
                      mt="xs"
                      onClick={() => {
                        setPage(0)
                        setNewMessages(false)
                        history.reload()
                      }}
                    >
                      Есть новые сообщения — перейти
                    </Button>
                  )}
                </div>
                <ScrollArea className="messages-scroll" offsetScrollbars>
                  {history.error ? (
                    <LoadError message={history.error} retry={history.reload} />
                  ) : history.loading && !history.data ? (
                    <Stack p="lg">
                      <Skeleton height={55} w="65%" />
                      <Skeleton height={55} w="55%" ml="auto" />
                    </Stack>
                  ) : history.data?.some((message) => message.roomId === roomId) ? (
                    <div className="messages">
                      {history.data
                        .filter((message) => message.roomId === roomId)
                        .slice()
                        .reverse()
                        .map((message) => {
                          const own = message.authorId === session.userId
                          return (
                            <div key={message.id} className={`message-row ${own ? 'own' : ''}`}>
                              {!own && (
                                <Avatar size={30} radius="xl" color="blue">
                                  {String(message.authorId).slice(-2)}
                                </Avatar>
                              )}
                              <div className="message-content">
                                <Group gap={6} className="message-author">
                                  <Text size="xs" fw={600}>
                                    {own ? 'Вы' : `Участник #${message.authorId}`}
                                  </Text>
                                  {room.moderatorIds.includes(message.authorId) && (
                                    <ShieldCheck size={12} aria-label="Модератор" />
                                  )}
                                </Group>
                                <div className="message-bubble">
                                  <Text size="sm" className="message-text">
                                    {message.text}
                                  </Text>
                                  <Group justify="space-between" gap="sm" mt={4}>
                                    <Text size="10px" className="message-time">
                                      {new Date(message.createdAt).toLocaleTimeString('ru-RU', {
                                        hour: '2-digit',
                                        minute: '2-digit',
                                      })}
                                    </Text>
                                    {(own || isModerator) && (
                                      <Menu position="bottom-end" shadow="md">
                                        <Menu.Target>
                                          <ActionIcon
                                            variant="transparent"
                                            size="xs"
                                            color={own ? 'white' : 'gray'}
                                            aria-label={`Действия с сообщением ${message.id}`}
                                          >
                                            <Ellipsis size={14} />
                                          </ActionIcon>
                                        </Menu.Target>
                                        <Menu.Dropdown>
                                          {own && (
                                            <Menu.Item
                                              leftSection={<Pencil size={14} />}
                                              onClick={() => setEditingMessage(message)}
                                            >
                                              Редактировать
                                            </Menu.Item>
                                          )}
                                          <Menu.Item
                                            color="red"
                                            leftSection={<Trash2 size={14} />}
                                            onClick={() =>
                                              confirmDelete(
                                                'Удалить сообщение?',
                                                'Сообщение исчезнет у всех участников комнаты.',
                                                async () => {
                                                  await api(
                                                    `/rooms/${message.roomId}/messages/${message.id}`,
                                                    'DELETE',
                                                  )
                                                  history.reload()
                                                },
                                              )
                                            }
                                          >
                                            Удалить сообщение
                                          </Menu.Item>
                                        </Menu.Dropdown>
                                      </Menu>
                                    )}
                                  </Group>
                                </div>
                              </div>
                            </div>
                          )
                        })}
                      <div ref={bottom} />
                    </div>
                  ) : (
                    <EmptyState
                      title={page ? 'Более ранних сообщений нет' : 'Скажи hello!'}
                      description={
                        page
                          ? 'Вернись к более новым сообщениям кнопкой «Новее».'
                          : 'Здесь ещё нет сообщений. Начни разговор и попрактикуй английский.'
                      }
                      action={<MessageCircle size={22} color="#3775f6" />}
                    />
                  )}
                </ScrollArea>
                <form
                  className="composer"
                  onSubmit={(event) => {
                    event.preventDefault()
                    send()
                  }}
                >
                  <Group gap="sm" align="flex-end" wrap="nowrap">
                    <Textarea
                      aria-label="Сообщение"
                      placeholder="Напиши что-нибудь на английском…"
                      autosize
                      minRows={1}
                      maxRows={4}
                      maxLength={2000}
                      value={draft}
                      onChange={(event) => setDraft(event.currentTarget.value)}
                      disabled={!chat.connected || !!pending}
                      className="composer-input"
                      onKeyDown={(event) => {
                        if (
                          event.key === 'Enter' &&
                          !event.shiftKey &&
                          !event.nativeEvent.isComposing
                        ) {
                          event.preventDefault()
                          send()
                        }
                      }}
                    />
                    <ActionIcon
                      type="submit"
                      size={42}
                      radius="md"
                      variant="filled"
                      aria-label="Отправить сообщение"
                      disabled={!draft.trim() || !chat.connected}
                      loading={!!pending}
                    >
                      <Send size={18} />
                    </ActionIcon>
                  </Group>
                  <Text size="10px" c="dimmed" mt={7}>
                    Enter — отправить · Shift + Enter — новая строка
                  </Text>
                </form>
              </>
            )}
          </section>
        </Card>
      )}
      {editingRoom !== undefined && (
        <DetailsModal
          title={editingRoom ? 'Редактировать комнату' : 'Новая комната'}
          initial={editingRoom ?? undefined}
          onClose={() => setEditingRoom(undefined)}
          onSave={async (value) => {
            const saved = await api<Room>(
              editingRoom ? `/rooms/${editingRoom.id}` : '/rooms',
              editingRoom ? 'PUT' : 'POST',
              value,
            )
            setRoomId(saved.id)
            setPage(0)
            rooms.reload()
          }}
        />
      )}
      {editingMessage && (
        <EditMessage
          api={api}
          message={editingMessage}
          onClose={() => setEditingMessage(undefined)}
          onSaved={history.reload}
        />
      )}
      {moderating && room && isOwner && (
        <ModeratorsModal
          api={api}
          room={room}
          onClose={() => setModerating(false)}
          onSaved={rooms.reload}
        />
      )}
    </Stack>
  )
}

function EditMessage({
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

function ModeratorsModal({
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
