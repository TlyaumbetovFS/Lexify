import { useEffect, useState } from 'react'
import { Alert, Badge, Button, Card, Group, SegmentedControl, Select, Skeleton, Stack, Text, Title } from '@mantine/core'
import { RefreshCw } from 'lucide-react'
import { showError } from '../api'
import { LoadError } from '../components/Feedback'
import { WordPronunciation } from '../components/WordPronunciation'
import { useResource } from '../hooks/useResource'
import type { Api, Collection, StudyQueue } from '../types'

const ratings = [
  { rating: 1, label: 'Не помню', hint: 'Не вспомнил ответ' },
  { rating: 2, label: 'Трудно', hint: 'Вспомнил с большим усилием' },
  { rating: 3, label: 'Помню', hint: 'Вспомнил нормально' },
  { rating: 4, label: 'Легко', hint: 'Вспомнил сразу' },
]

function interval(due: string) {
  const minutes = Math.max(1, Math.round((Date.parse(due) - Date.now()) / 60_000))
  if (minutes < 60) return `${minutes} мин`
  if (minutes < 1440) return `${Math.round(minutes / 60)} ч`
  return `${Math.round(minutes / 1440)} дн`
}

export function StudyPage({ api }: { api: Api }) {
  const [direction, setDirection] = useState('ru')
  const [collection, setCollection] = useState<string | null>(null)
  const [revealedKey, setRevealedKey] = useState('')
  const [busy, setBusy] = useState(false)
  const collections = useResource<Collection[]>(api, '/collections/summaries')
  const queue = useResource<StudyQueue>(api, `/study?direction=${direction}${collection ? `&collectionId=${collection}` : ''}`)
  const card = queue.data?.card
  const key = card ? `${collection}-${card.word.id}-${card.direction}-${card.revision}` : ''
  const revealed = !!key && key === revealedKey

  useEffect(() => {
    if (queue.loading || queue.error || queue.data?.card || !queue.data?.nextDue) return
    const wait = Math.min(2_147_000_000, Math.max(1000, Date.parse(queue.data.nextDue) - Date.now() + 1000))
    const timer = setTimeout(queue.reload, wait)
    return () => clearTimeout(timer)
  }, [queue.data, queue.loading, queue.error, queue.reload])

  const change = () => setRevealedKey('')
  const review = async (rating: number) => {
    if (!card || busy || !revealed) return
    setBusy(true)
    try {
      await api('/study/reviews', 'POST', {
        wordId: card.word.id, direction: card.direction, rating, revision: card.revision,
      })
      change()
      queue.reload()
    } catch (error) {
      showError(error)
      change()
      queue.reload()
    } finally {
      setBusy(false)
    }
  }

  return (
    <Stack gap="xl">
      <div className="page-heading">
        <div>
          <Title order={1}>Учить слова</Title>
          <Text c="dimmed" size="sm" mt={8}>Вспомни перевод, открой ответ и оцени себя честно.</Text>
        </div>
        <Button variant="default" leftSection={<RefreshCw size={16} />} onClick={() => { change(); queue.reload() }} disabled={busy || queue.loading}>
          Обновить
        </Button>
      </div>
      <Group align="end">
        <Select label="Подборка" placeholder="Весь словарь" clearable searchable w={260}
          data={(collections.data ?? []).map(item => ({ value: String(item.id), label: item.name }))}
          value={collection} onChange={value => { change(); setCollection(value) }} disabled={busy} />
        <SegmentedControl aria-label="Направление перевода" value={direction}
          onChange={value => { change(); setDirection(value) }} disabled={busy}
          data={[{ value: 'en', label: 'EN → RU' }, { value: 'ru', label: 'RU → EN' }]} />
      </Group>
      {collections.error && <LoadError message={collections.error} retry={collections.reload} />}
      {queue.error ? <LoadError message={queue.error} retry={queue.reload} /> : queue.loading ? <Skeleton height={320} radius="lg" /> : (
        <>
          <Group gap="sm">
            <Badge variant="light">Новых: {queue.data?.newCards ?? 0}</Badge>
            <Badge variant="light">К повторению: {queue.data?.dueCards ?? 0}</Badge>
            <Text size="sm" c="dimmed">Всего слов: {queue.data?.total ?? 0}</Text>
          </Group>
          <Card withBorder radius="lg" p={{ base: 'lg', sm: 'xl' }} maw={760} w="100%" mx="auto">
            {card ? (
              <Stack gap="xl" align="center" ta="center" py="lg">
                <Text size="sm" c="dimmed">{card.direction === 'en' ? 'Как это по-русски?' : 'Как это по-английски?'}</Text>
                <Title order={2} style={{ overflowWrap: 'anywhere' }}>
                  {card.direction === 'en' ? card.word.englishText : card.word.russianText}
                </Title>
                {card.direction === 'ru' && card.word.russianHint && (
                  <Text c="dimmed" style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>
                    {card.word.russianHint}
                  </Text>
                )}
                {revealed ? (
                  <>
                    <Stack gap="sm" align="center">
                      <Text size="xl" fw={600} style={{ overflowWrap: 'anywhere' }}>
                        {card.direction === 'en' ? card.word.russianText : card.word.englishText}
                      </Text>
                      <WordPronunciation word={card.word} />
                      {card.word.example && <Text c="dimmed" size="sm">{card.word.example}</Text>}
                    </Stack>
                    <div className="study-ratings">
                      {ratings.map(item => (
                        <Button key={item.rating} title={item.hint} variant={item.rating === 3 ? 'filled' : 'light'}
                          onClick={() => void review(item.rating)} disabled={busy} h="auto" py="sm">
                          <Stack gap={3}>
                            <span>{item.label}</span>
                            <Text component="span" size="xs" inherit>
                              {interval(card.options.find(option => option.rating === item.rating)!.due)}
                            </Text>
                          </Stack>
                        </Button>
                      ))}
                    </div>
                    <Text c="dimmed" size="xs">Не вспомнил — выбирай «Не помню». «Трудно» означает, что ответ всё-таки вспомнил.</Text>
                  </>
                ) : <Button size="lg" onClick={() => setRevealedKey(key)}>Показать ответ</Button>}
              </Stack>
            ) : (
              <Stack align="center" ta="center" py="xl">
                <Title order={2}>{queue.data?.total ? 'На сейчас всё!' : 'Пока нет слов'}</Title>
                <Text c="dimmed">{queue.data?.nextDue
                  ? `Следующее повторение: ${new Date(queue.data.nextDue).toLocaleString('ru-RU')}.`
                  : 'Добавь слова в словарь или импортируй подборку из Anki.'}</Text>
                {!queue.data?.total && <Button component="a" href="#dictionary">Открыть словарь</Button>}
              </Stack>
            )}
          </Card>
          <Alert variant="light" maw={760} w="100%" mx="auto">Забытые слова будут появляться чаще, знакомые — реже. Прогресс двух направлений перевода хранится отдельно. Сначала повторяем, затем учим новое.</Alert>
        </>
      )}
    </Stack>
  )
}
