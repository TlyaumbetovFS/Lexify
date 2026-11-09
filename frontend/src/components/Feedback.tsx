import { Alert, Button, Center, Stack, Text, ThemeIcon } from '@mantine/core'
import { BookOpen, RefreshCw } from 'lucide-react'

export function EmptyState({
  title,
  description,
  action,
}: {
  title: string
  description: string
  action?: React.ReactNode
}) {
  return (
    <Center className="empty-state">
      <Stack align="center" gap="sm">
        <ThemeIcon size={56} radius="xl" variant="light">
          <BookOpen size={25} />
        </ThemeIcon>
        <Text fw={650} size="lg">
          {title}
        </Text>
        <Text c="dimmed" ta="center" maw={360} size="sm">
          {description}
        </Text>
        {action}
      </Stack>
    </Center>
  )
}

export function LoadError({ message, retry }: { message: string; retry: () => void }) {
  return (
    <Alert color="red" title="Не удалось загрузить данные" role="alert">
      {message}
      <Button
        mt="sm"
        variant="white"
        color="red"
        size="xs"
        leftSection={<RefreshCw size={14} />}
        onClick={retry}
      >
        Повторить
      </Button>
    </Alert>
  )
}
