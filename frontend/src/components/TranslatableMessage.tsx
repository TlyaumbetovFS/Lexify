import { useEffect, useRef, useState } from 'react'
import { Button, CloseButton, Group, Loader, Popover, Stack, Text } from '@mantine/core'

interface Selection {
  index: number
  word: string
  translation?: string
  error?: string
}

export function TranslatableMessage({
  text,
  translate,
}: {
  text: string
  translate: (word: string) => Promise<string>
}) {
  const [selection, setSelection] = useState<Selection | null>(null)
  const [opened, setOpened] = useState(false)
  const request = useRef(0)

  useEffect(
    () => () => {
      request.current++
    },
    [],
  )

  async function select(index: number, word: string) {
    const current = ++request.current
    setSelection({ index, word })
    setOpened(true)
    try {
      const translation = await translate(word)
      if (request.current === current) setSelection({ index, word, translation })
    } catch (error) {
      if (request.current === current) {
        setSelection({
          index,
          word,
          error: error instanceof Error ? error.message : 'Не удалось получить перевод.',
        })
      }
    }
  }

  return (
    <Popover
      opened={opened}
      onChange={setOpened}
      position="top"
      width={250}
      withArrow
      shadow="md"
      returnFocus
    >
      <Text size="sm" className="message-text">
        {text.split(/([a-z]+(?:['’-][a-z]+)*)/gi).map((part, index) => {
          if (!/^[a-z]+(?:['’-][a-z]+)*$/i.test(part) || part.length > 100) return part
          const button = (
            <button
              type="button"
              className="chat-word"
              aria-label={`Перевести ${part}`}
              onClick={() => {
                if (opened && selection?.index === index) setOpened(false)
                else void select(index, part)
              }}
            >
              {part}
            </button>
          )
          return selection?.index === index ? (
            <Popover.Target key={index}>{button}</Popover.Target>
          ) : (
            <span key={index}>{button}</span>
          )
        })}
      </Text>
      <Popover.Dropdown aria-label="Перевод слова">
        {selection && (
          <Stack gap="xs">
            <Group justify="space-between" wrap="nowrap">
              <Text fw={650}>{selection.word}</Text>
              <CloseButton size="sm" aria-label="Закрыть перевод" onClick={() => setOpened(false)} />
            </Group>
            <div aria-live="polite">
              {selection.translation ? (
                <Text size="sm" c="blue">
                  {selection.translation}
                </Text>
              ) : selection.error ? (
                <Stack gap="xs">
                  <Text size="sm" c="red">
                    {selection.error}
                  </Text>
                  <Button
                    size="compact-xs"
                    variant="light"
                    onClick={() => select(selection.index, selection.word)}
                  >
                    Попробовать снова
                  </Button>
                </Stack>
              ) : (
                <Group gap="xs">
                  <Loader size="xs" />
                  <Text size="sm" c="dimmed">
                    Переводим…
                  </Text>
                </Group>
              )}
            </div>
          </Stack>
        )}
      </Popover.Dropdown>
    </Popover>
  )
}
