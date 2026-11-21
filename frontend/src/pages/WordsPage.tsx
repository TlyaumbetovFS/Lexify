import { useState } from 'react'
import {
  ActionIcon,
  Badge,
  Button,
  Card,
  Group,
  Menu,
  Modal,
  Pagination,
  SimpleGrid,
  Skeleton,
  Stack,
  Text,
  Textarea,
  TextInput,
  Title,
} from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import {
  Ellipsis,
  Pencil,
  Plus,
  Search,
  Sparkles,
  Trash2,
} from 'lucide-react'
import { showError } from '../api'
import { WordPronunciation } from '../components/WordPronunciation'
import { EmptyState, LoadError } from '../components/Feedback'
import { confirmDelete } from '../components/confirmDelete'
import { useWordPage } from '../hooks/useWordPage'
import type { Api, Word } from '../types'

export function WordsPage({ api }: { api: Api }) {
  const words = useWordPage(api)
  const { search } = words
  const [editing, setEditing] = useState<Word | null>()

  return (
    <Stack gap={28}>
      <div className="page-heading">
        <div>
          <Title order={1}>Мой словарь</Title>
          <Text c="dimmed" size="sm" mt={8}>
            Добавляй слова на русском или английском.
          </Text>
        </div>
        <Button
          className="add-word-button"
          size="lg"
          leftSection={<Plus size={22} />}
          onClick={() => setEditing(null)}
        >
          Добавить слово
        </Button>
      </div>
      <Group justify="space-between" gap="md">
        <Group gap="sm">
          <Text fw={650}>{search ? 'Найдено' : 'Все слова'}</Text>
          <Badge variant="light" color="gray">
            {words.data?.total ?? 0}
          </Badge>
        </Group>
        <TextInput
          className="word-search"
          aria-label="Поиск по словарю"
          placeholder="Найти слово или перевод"
          leftSection={<Search size={17} />}
          value={search}
          onChange={(event) => words.changeSearch(event.currentTarget.value)}
          size="sm"
        />
      </Group>
      {words.error ? (
        <LoadError message={words.error} retry={words.reload} />
      ) : words.loading && !words.data ? (
        <SimpleGrid cols={{ base: 1, sm: 2, xl: 3 }}>
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} height={210} radius="lg" />
          ))}
        </SimpleGrid>
      ) : words.data?.items.length ? (
        <SimpleGrid cols={{ base: 1, sm: 2, xl: 3 }} spacing="lg" aria-busy={words.loading}>
          {words.data.items.map((word) => (
            <Card key={word.id} className="word-card" padding="lg" radius="md" withBorder>
              <Group justify="space-between" mb="xs">
                <Badge variant="light" color="blue" size="xs">
                  {word.sourceLanguage === 'en' ? 'EN → RU' : 'RU → EN'}
                </Badge>
                <Menu position="bottom-end" shadow="md">
                  <Menu.Target>
                    <ActionIcon
                      variant="subtle"
                      color="gray"
                      aria-label={`Действия со словом ${word.russianText}`}
                    >
                      <Ellipsis size={19} />
                    </ActionIcon>
                  </Menu.Target>
                  <Menu.Dropdown>
                    <Menu.Item
                      leftSection={<Sparkles size={15} />}
                      onClick={async () => {
                        try {
                          const saved = await api<Word>(`/words/${word.id}/enrich`, 'POST')
                          words.reload()
                          const available = saved.dictionary?.phonetic || saved.example
                          notifications.show({
                            title: 'Словарные данные',
                            message: available
                              ? 'Карточка дополнена.'
                              : 'Данные пока недоступны. Попробуйте позже.',
                            color: available ? 'blue' : 'gray',
                          })
                        } catch (error) {
                          showError(error)
                        }
                      }}
                    >
                      Дополнить карточку
                    </Menu.Item>
                    <Menu.Item leftSection={<Pencil size={15} />} onClick={() => setEditing(word)}>
                      Редактировать
                    </Menu.Item>
                    <Menu.Item
                      color="red"
                      leftSection={<Trash2 size={15} />}
                      onClick={() =>
                        confirmDelete(
                          'Удалить слово?',
                          `«${word.russianText}» также исчезнет из всех подборок.`,
                          async () => {
                            await api(`/words/${word.id}`, 'DELETE')
                            words.reload()
                          },
                        )
                      }
                    >
                      Удалить
                    </Menu.Item>
                  </Menu.Dropdown>
                </Menu>
              </Group>
              <Text className="word-translation">{word.englishText}</Text>
              <Text mt={4}>
                {word.russianText}
              </Text>
              <WordPronunciation word={word} />
              {word.example && (
                <Text className="word-example" size="sm" c="dimmed" fs="italic">
                  “{word.example}”
                </Text>
              )}
            </Card>
          ))}
        </SimpleGrid>
      ) : (
        <EmptyState
          title={search ? 'Ничего не нашлось' : 'Первое слово — за тобой'}
          description={
            search
              ? 'Попробуй другой запрос или добавь новое слово.'
              : 'Добавь английское или русское слово. Мы найдём перевод и доступные словарные подсказки.'
          }
          action={
            !search && (
              <Button
                variant="light"
                leftSection={<Plus size={16} />}
                onClick={() => setEditing(null)}
              >
                Добавить первое слово
              </Button>
            )
          }
        />
      )}
      {words.data && words.data.pages > 1 && (
        <Pagination value={words.page} total={words.data.pages} onChange={words.setPage} />
      )}
      {editing !== undefined && (
        <WordModal
          api={api}
          word={editing}
          onClose={() => setEditing(undefined)}
          onSaved={() => { if (editing === null) words.changeSearch(''); words.reload() }}
        />
      )}
    </Stack>
  )
}

function WordModal({
  api,
  word,
  onClose,
  onSaved,
}: {
  api: Api
  word: Word | null
  onClose: () => void
  onSaved: () => void
}) {
  const [busy, setBusy] = useState(false)
  const form = useForm({
    initialValues: {
      text: word ? (word.sourceLanguage === 'en' ? word.englishText : word.russianText) : '',
      example: word?.example ?? '',
      russianHint: word?.russianHint ?? '',
    },
    validate: { text: (value) => (value.trim() ? null : 'Введите слово') },
  })
  return (
    <Modal
      opened
      centered
      title={word ? 'Редактировать слово' : 'Новое слово'}
      onClose={() => !busy && onClose()}
    >
      <form
        onSubmit={form.onSubmit(async (values) => {
          setBusy(true)
          try {
            await api(word ? `/words/${word.id}` : '/words', word ? 'PUT' : 'POST', {
              text: values.text.trim(),
              example: values.example.trim() || null,
              russianHint: values.russianHint.trim(),
            })
            onSaved()
            onClose()
            notifications.show({
              title: word ? 'Слово обновлено' : 'Слово в твоём словаре',
              message: word ? 'Изменения сохранены.' : 'Перевод сохранён.',
              color: 'blue',
            })
          } catch (error) {
            showError(error)
          } finally {
            setBusy(false)
          }
        })}
      >
        <Stack>
          <TextInput
            label="Слово или выражение"
            placeholder="Например, hello или привет"
            description="Язык определится автоматически, перевод найдётся сам."
            maxLength={100}
            required
            autoFocus
            disabled={busy}
            {...form.getInputProps('text')}
            onChange={(event) => {
              form.setFieldValue('text', event.currentTarget.value)
              if (word) {
                form.setFieldValue('example', '')
                form.setFieldValue('russianHint', '')
              }
            }}
          />
          <Textarea
            label="Свой пример употребления"
            placeholder="Everything is possible."
            description="Оставь пустым — подберём пример из словаря, если он есть."
            minRows={3}
            maxLength={1000}
            disabled={busy}
            {...form.getInputProps('example')}
          />
          <Textarea
            label="Объяснение на русском"
            description="Покажем его рядом с русским словом до открытия ответа."
            placeholder="Например, возможность войти или воспользоваться чем-либо"
            maxLength={1000}
            disabled={busy}
            {...form.getInputProps('russianHint')}
          />
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose} disabled={busy}>
              Отмена
            </Button>
            <Button type="submit" loading={busy}>
              {word ? 'Сохранить' : 'Добавить и перевести'}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
