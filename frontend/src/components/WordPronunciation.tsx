import { ActionIcon, Anchor, Group, Popover, Stack, Text, Tooltip } from '@mantine/core'
import { Info, Volume2 } from 'lucide-react'
import { showError } from '../api'
import type { Word } from '../types'

export function WordPronunciation({ word }: { word: Word }) {
  const details = word.dictionary
  const supported = 'speechSynthesis' in window && 'SpeechSynthesisUtterance' in window

  function play() {
    if (!supported) return
    const synthesis = window.speechSynthesis
    synthesis.cancel()
    const utterance = new SpeechSynthesisUtterance(word.englishText)
    utterance.lang = 'en-US'
    utterance.rate = 0.9
    const voices = synthesis.getVoices()
    const voice =
      voices.find((voice) => voice.lang === 'en-US') ??
      voices.find((voice) => voice.lang.startsWith('en'))
    if (voice) utterance.voice = voice
    utterance.onerror = ({ error }) => {
      if (error !== 'canceled' && error !== 'interrupted') {
        showError(
          new Error('Не удалось озвучить слово. Проверьте английский голос в настройках устройства.'),
        )
      }
    }
    synthesis.speak(utterance)
  }

  return (
    <Group gap={6} mt={5}>
      {details?.phonetic && (
        <Text size="xs" c="dimmed">
          {details.phonetic}
        </Text>
      )}
      <Tooltip
        label={supported ? 'Послушать произношение' : 'Браузер не поддерживает озвучку'}
      >
        <ActionIcon
          variant="subtle"
          size="sm"
          disabled={!supported}
          aria-label={`Произношение ${word.englishText}`}
          onClick={play}
        >
          <Volume2 size={17} />
        </ActionIcon>
      </Tooltip>
      {(details?.sourceUrl || details?.licenseUrl) && (
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
            </Stack>
          </Popover.Dropdown>
        </Popover>
      )}
    </Group>
  )
}
