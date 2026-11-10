import { Button, Card, SimpleGrid, Skeleton, Stack, Text, Title } from '@mantine/core'
import { RefreshCw } from 'lucide-react'
import { LoadError } from '../components/Feedback'
import { useResource } from '../hooks/useResource'
import type { Api, Statistics } from '../types'

const metrics = [
  { key: 'users', label: 'Пользователи' },
  { key: 'rooms', label: 'Комнаты чата' },
  { key: 'messages', label: 'Сообщения' },
  { key: 'words', label: 'Слова в словарях' },
  { key: 'uniqueWords', label: 'Уникальные слова' },
] as const

export function StatisticsPage({ api }: { api: Api }) {
  const statistics = useResource<Statistics>(api, '/statistics')

  return (
    <Stack gap="xl">
      <div className="page-heading">
        <div>
          <Title order={1}>Статистика</Title>
          <Text c="dimmed" size="sm" mt={8}>Общие показатели Lexify на данный момент.</Text>
        </div>
        <Button variant="light" leftSection={<RefreshCw size={16} />}
          onClick={statistics.reload} loading={statistics.loading}>
          Обновить
        </Button>
      </div>
      {statistics.error ? (
        <LoadError message={statistics.error} retry={statistics.reload} />
      ) : (
        <SimpleGrid cols={{ base: 1, xs: 2, lg: 3 }} aria-busy={statistics.loading}>
          {metrics.map(({ key, label }) => (
            <Card key={key} withBorder padding="lg" radius="md">
              <Text c="dimmed" size="sm">{label}</Text>
              {statistics.data ? (
                <Text size="32px" fw={700} c="blue" mt="xs">
                  {statistics.data[key].toLocaleString('ru-RU')}
                </Text>
              ) : <Skeleton height={40} width={90} mt="xs" />}
            </Card>
          ))}
        </SimpleGrid>
      )}
      <Text size="sm" c="dimmed">
        Считаются существующие записи. Одно слово в словарях разных людей учитывается несколько раз.
        Уникальные слова считаются по английскому написанию без учёта регистра и пробелов по краям.
      </Text>
    </Stack>
  )
}
