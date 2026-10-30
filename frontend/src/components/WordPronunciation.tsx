import { useEffect, useRef, useState } from 'react'
import { ActionIcon, Anchor, Group, Popover, Stack, Text, Tooltip } from '@mantine/core'
import { Info, Volume2 } from 'lucide-react'
import { showError } from '../api'
import type { Word } from '../types'

export function WordPronunciation({ word }: { word: Word }) {
  const audio = useRef<HTMLAudioElement>(null)
  const details = word.dictionary
  const [loading, setLoading] = useState(false)
  const timer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined)

  useEffect(() => {
    const player = audio.current
    return () => {
      clearTimeout(timer.current)
      player?.pause()
    }
  }, [details?.audioUrl])

  async function play() {
    const player = audio.current
    if (!player || loading) return
    document.querySelectorAll('audio').forEach((other) => {
      if (other !== player) other.pause()
    })
    player.currentTime = 0
    setLoading(true)
    let timedOut = false
    timer.current = setTimeout(() => {
      timedOut = true
      player.pause()
      setLoading(false)
      showError(new Error('Аудиосервер не ответил. Попробуйте позже.'))
    }, 10000)
    try {
      await player.play()
    } catch (error) {
      if (!timedOut && !(error instanceof DOMException && error.name === 'AbortError')) {
        showError(new Error('Запись пока недоступна. Попробуйте позже.'))
      }
    } finally {
      clearTimeout(timer.current)
      setLoading(false)
    }
  }

  return (
    <Group gap={6} mt={5}>
      {details?.phonetic && (
        <Text size="xs" c="dimmed">
          {details.phonetic}
        </Text>
      )}
      <Tooltip
        label={details?.audioUrl ? 'Послушать произношение' : 'Для этого слова пока нет записи'}
      >
        <ActionIcon
          variant="subtle"
          size="sm"
          disabled={!details?.audioUrl}
          loading={loading}
          aria-label={`Произношение ${word.englishText}`}
          onClick={play}
        >
          <Volume2 size={17} />
        </ActionIcon>
      </Tooltip>
      {(details?.sourceUrl ||
        details?.audioSourceUrl ||
        details?.licenseUrl ||
        details?.audioLicenseUrl) && (
        <Popover width={245} position="bottom-start" withArrow shadow="md">
          <Popover.Target>
            <ActionIcon
              variant="subtle"
              color="gray"
              size="sm"
              aria-label={`Источники и лицензии ${word.englishText}`}
            >
              <Info size={15} />
            </ActionIcon>
          </Popover.Target>
          <Popover.Dropdown>
            <Stack gap={6}>
              <Text size="xs" fw={650}>
                Источники словарных данных
              </Text>
              {details.sourceUrl && (
                <Anchor
                  size="xs"
                  href={details.sourceUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Словарная статья
                </Anchor>
              )}
              {details.licenseUrl && (
                <Anchor
                  size="xs"
                  href={details.licenseUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Лицензия словаря
                </Anchor>
              )}
              {details.audioSourceUrl && (
                <Anchor
                  size="xs"
                  href={details.audioSourceUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Источник аудиозаписи
                </Anchor>
              )}
              {details.audioLicenseUrl && (
                <Anchor
                  size="xs"
                  href={details.audioLicenseUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Лицензия аудиозаписи
                </Anchor>
              )}
            </Stack>
          </Popover.Dropdown>
        </Popover>
      )}
      {details?.audioUrl && <audio ref={audio} src={details.audioUrl} preload="none" />}
    </Group>
  )
}
