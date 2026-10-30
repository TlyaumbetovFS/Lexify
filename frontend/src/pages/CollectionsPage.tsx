import { useEffect, useState } from 'react'
import {
  ActionIcon,
  Badge,
  Button,
  Card,
  Group,
  Menu,
  Modal,
  Select,
  Skeleton,
  Stack,
  Table,
  Text,
  Title,
  UnstyledButton,
} from '@mantine/core'
import { Ellipsis, Folder, FolderOpen, Pencil, Plus, Trash2, X } from 'lucide-react'
import { showError } from '../api'
import { DetailsModal } from '../components/DetailsModal'
import { WordPronunciation } from '../components/WordPronunciation'
import { confirmDelete, EmptyState, LoadError } from '../components/Feedback'
import { useResource } from '../hooks/useResource'
import type { Api, Collection, Word } from '../types'

export function CollectionsPage({ api }: { api: Api }) {
  const collections = useResource<Collection[]>(api, '/collections')
  const vocabulary = useResource<Word[]>(api, '/words')
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [editing, setEditing] = useState<Collection | null>()
  const [adding, setAdding] = useState(false)
  const selected = collections.data?.find((item) => item.id === selectedId)

  useEffect(() => {
    if (collections.data && !collections.data.some((item) => item.id === selectedId))
      setSelectedId(collections.data[0]?.id ?? null)
  }, [collections.data, selectedId])

  return (
    <Stack gap={28}>
      <div className="page-heading">
        <div>
          <Text className="eyebrow" c="blue">
            У КАЖДОГО СЛОВА СВОЁ МЕСТО
          </Text>
          <Title order={1}>Подборки</Title>
          <Text c="dimmed" size="sm" mt={8}>
            Собирай слова по темам, планам и интересам.
          </Text>
        </div>
        <Button leftSection={<Plus size={18} />} onClick={() => setEditing(null)}>
          Создать подборку
        </Button>
      </div>
      {collections.error ? (
        <LoadError message={collections.error} retry={collections.reload} />
      ) : collections.loading && !collections.data ? (
        <Skeleton height={350} radius="lg" />
      ) : !collections.data?.length ? (
        <EmptyState
          title="Разложим слова по полочкам?"
          description="Путешествия, работа или любимое хобби — создай подборку и добавь в неё слова из своего словаря."
          action={
            <Button
              variant="light"
              leftSection={<Plus size={16} />}
              onClick={() => setEditing(null)}
            >
              Создать первую подборку
            </Button>
          }
        />
      ) : (
        <div className="master-detail">
          <div className="collection-list">
            <Text size="xs" fw={650} c="dimmed" mb="md">
              МОИ ПОДБОРКИ · {collections.data.length}
            </Text>
            <Stack gap="xs">
              {collections.data.map((item) => (
                <UnstyledButton
                  key={item.id}
                  className={`collection-link ${selectedId === item.id ? 'is-selected' : ''}`}
                  onClick={() => setSelectedId(item.id)}
                >
                  <div className="folder-icon">
                    {selectedId === item.id ? <FolderOpen size={22} /> : <Folder size={22} />}
                  </div>
                  <div>
                    <Text fw={600} size="sm" lineClamp={1}>
                      {item.name}
                    </Text>
                    <Text size="xs" c="dimmed">
                      Слов: {item.words.length}
                    </Text>
                  </div>
                </UnstyledButton>
              ))}
            </Stack>
          </div>
          <Card withBorder radius="lg" p={{ base: 'md', sm: 'xl' }} className="collection-detail">
            {selected && (
              <>
                <Group justify="space-between" align="flex-start" wrap="nowrap">
                  <div>
                    <Group gap="sm">
                      <Title order={2}>{selected.name}</Title>
                      <Badge variant="light">{selected.words.length}</Badge>
                    </Group>
                    <Text size="sm" c="dimmed" mt="xs">
                      {selected.description || 'Твоя коллекция полезных слов.'}
                    </Text>
                  </div>
                  <Menu position="bottom-end" shadow="md">
                    <Menu.Target>
                      <ActionIcon variant="subtle" color="gray" aria-label="Действия с подборкой">
                        <Ellipsis size={20} />
                      </ActionIcon>
                    </Menu.Target>
                    <Menu.Dropdown>
                      <Menu.Item
                        leftSection={<Pencil size={15} />}
                        onClick={() => setEditing(selected)}
                      >
                        Редактировать
                      </Menu.Item>
                      <Menu.Item
                        color="red"
                        leftSection={<Trash2 size={15} />}
                        onClick={() =>
                          confirmDelete(
                            'Удалить подборку?',
                            'Слова останутся в личном словаре.',
                            async () => {
                              await api(`/collections/${selected.id}`, 'DELETE')
                              collections.reload()
                            },
                          )
                        }
                      >
                        Удалить подборку
                      </Menu.Item>
                    </Menu.Dropdown>
                  </Menu>
                </Group>
                <Group mt="xl" mb="md" justify="space-between">
                  <Text size="sm" fw={650}>
                    Слова в подборке
                  </Text>
                  <Button
                    variant="light"
                    size="xs"
                    leftSection={<Plus size={14} />}
                    onClick={() => setAdding(true)}
                  >
                    Добавить слово
                  </Button>
                </Group>
                {selected.words.length ? (
                  <Table.ScrollContainer minWidth={380}>
                    <Table verticalSpacing="md" highlightOnHover>
                      <Table.Thead>
                        <Table.Tr>
                          <Table.Th>English</Table.Th>
                          <Table.Th>Перевод</Table.Th>
                          <Table.Th w={40} />
                        </Table.Tr>
                      </Table.Thead>
                      <Table.Tbody>
                        {selected.words.map((word) => (
                          <Table.Tr key={word.id}>
                            <Table.Td>
                              <Text fw={650} c="blue" size="sm">
                                {word.englishText}
                              </Text>
                              <WordPronunciation word={word} />
                              {word.example && (
                                <Text size="xs" c="dimmed" mt={3}>
                                  {word.example}
                                </Text>
                              )}
                            </Table.Td>
                            <Table.Td>
                              <Text size="sm">{word.russianText}</Text>
                            </Table.Td>
                            <Table.Td>
                              <ActionIcon
                                variant="subtle"
                                color="gray"
                                aria-label={`Убрать ${word.russianText} из подборки`}
                                onClick={async () => {
                                  try {
                                    await api(
                                      `/collections/${selected.id}/words/${word.id}`,
                                      'DELETE',
                                    )
                                    collections.reload()
                                  } catch (error) {
                                    showError(error)
                                  }
                                }}
                              >
                                <X size={16} />
                              </ActionIcon>
                            </Table.Td>
                          </Table.Tr>
                        ))}
                      </Table.Tbody>
                    </Table>
                  </Table.ScrollContainer>
                ) : (
                  <EmptyState
                    title="Здесь будут слова"
                    description="Добавь слова из своего словаря — они могут быть сразу в нескольких подборках."
                  />
                )}
              </>
            )}
          </Card>
        </div>
      )}
      {editing !== undefined && (
        <DetailsModal
          title={editing ? 'Редактировать подборку' : 'Новая подборка'}
          initial={editing ?? undefined}
          onClose={() => setEditing(undefined)}
          onSave={async (value) => {
            const saved = await api<Collection>(
              editing ? `/collections/${editing.id}` : '/collections',
              editing ? 'PUT' : 'POST',
              value,
            )
            setSelectedId(saved.id)
            collections.reload()
          }}
        />
      )}
      {adding && selected && (
        <AddWordModal
          words={vocabulary.data ?? []}
          error={vocabulary.error}
          loading={vocabulary.loading}
          selected={selected}
          onRetry={vocabulary.reload}
          onClose={() => setAdding(false)}
          onSave={async (id) => {
            await api(`/collections/${selected.id}/words/${id}`, 'PUT')
            collections.reload()
          }}
        />
      )}
    </Stack>
  )
}

function AddWordModal({
  words,
  selected,
  onClose,
  onSave,
  loading,
  error,
  onRetry,
}: {
  words: Word[]
  selected: Collection
  onClose: () => void
  onSave: (id: string) => Promise<void>
  loading: boolean
  error: string
  onRetry: () => void
}) {
  const [value, setValue] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const options = words
    .filter((word) => !selected.words.some((existing) => existing.id === word.id))
    .map((word) => ({ value: String(word.id), label: `${word.englishText} — ${word.russianText}` }))
  return (
    <Modal opened title="Добавить слово в подборку" centered onClose={() => !busy && onClose()}>
      <Stack>
        {error ? (
          <LoadError message={error} retry={onRetry} />
        ) : (
          <>
            <Text size="sm" c="dimmed">
              Выбери слово из своего словаря. Оно останется доступно и в других подборках.
            </Text>
            <Select
              label="Слово"
              placeholder={loading ? 'Загрузка…' : 'Начни вводить слово'}
              data={options}
              value={value}
              onChange={setValue}
              searchable
              nothingFoundMessage="Слово не найдено"
              disabled={loading || busy}
            />
            {!loading && !options.length && (
              <Text size="sm" c="dimmed">
                Все доступные слова уже добавлены. Новые слова можно добавить в разделе «Мой
                словарь».
              </Text>
            )}
          </>
        )}
        <Group justify="flex-end">
          <Button variant="default" onClick={onClose} disabled={busy}>
            Отмена
          </Button>
          <Button
            disabled={!value || !!error}
            loading={busy}
            onClick={async () => {
              if (!value) return
              setBusy(true)
              try {
                await onSave(value)
                onClose()
              } catch (error) {
                showError(error)
              } finally {
                setBusy(false)
              }
            }}
          >
            Добавить
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}
