import { useState } from 'react'
import { Alert, Button, FileInput, Group, Modal, Select, Stack, Table, Text, TextInput } from '@mantine/core'
import { showError } from '../api'
import type { AnkiImportResult, AnkiPreview, Api } from '../types'

export function AnkiImportModal({ api, onClose, onImported }: {
  api: Api; onClose: () => void; onImported: (id: number) => void
}) {
  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<AnkiPreview | null>(null)
  const [model, setModel] = useState<string | null>(null)
  const [english, setEnglish] = useState<string | null>('0')
  const [russian, setRussian] = useState<string | null>('1')
  const [example, setExample] = useState<string | null>(null)
  const [hint, setHint] = useState<string | null>(null)
  const [name, setName] = useState('')
  const [busy, setBusy] = useState(false)
  const [result, setResult] = useState<AnkiImportResult | null>(null)
  const selected = preview?.noteTypes.find(item => String(item.id) === model)
  const fields = selected?.fields.map((label, index) => ({ value: String(index), label })) ?? []
  const assigned = [english, russian, example, hint].filter(value => value !== null)
  const distinct = new Set(assigned).size === assigned.length
  const emptyInPreview = (field: string | null) => field !== null
    && !!selected?.samples.length && selected.samples.every(sample => !sample[Number(field)]?.trim())
  const emptyRussian = emptyInPreview(russian)
  const valid = english !== null && russian !== null && distinct

  const selectModel = (type: AnkiPreview['noteTypes'][number] | undefined) => {
    setModel(type ? String(type.id) : null)
    const contains = (index: number, pattern: RegExp) =>
      type?.samples.some(sample => pattern.test(sample[index] ?? ''))
    const russianIndex = type?.fields.findIndex((_, index) => contains(index, /\p{Script=Cyrillic}/u)) ?? -1
    const englishIndex = type?.fields.findIndex((_, index) => index !== russianIndex
      && contains(index, /\p{Script=Latin}/u)) ?? -1
    setEnglish(englishIndex >= 0 ? String(englishIndex) : null)
    setRussian(russianIndex >= 0 ? String(russianIndex) : null)
    setExample(null)
    setHint(null)
  }

  const upload = async (commit: boolean) => {
    if (!file || busy) return
    if (file.size > 100 * 1024 * 1024) { showError(new Error('Максимальный размер файла — 100 МБ.')); return }
    const body = new FormData()
    body.append('file', file)
    setBusy(true)
    try {
      if (commit) {
        body.append('modelId', model!)
        body.append('englishField', english!)
        body.append('russianField', russian!)
        if (example !== null) body.append('exampleField', example)
        if (hint !== null) body.append('hintField', hint)
        body.append('name', name.trim())
        const imported = await api<AnkiImportResult>('/anki/import', 'POST', body)
        setResult(imported)
        onImported(imported.collectionId)
      } else {
        const data = await api<AnkiPreview>('/anki/preview', 'POST', body)
        setPreview(data)
        selectModel(data.noteTypes[0])
      }
    } catch (error) { showError(error) }
    finally { setBusy(false) }
  }

  return (
    <Modal opened title="Импорт из Anki" centered size="lg" onClose={() => !busy && onClose()} closeOnClickOutside={!busy} closeOnEscape={!busy}>
      <Stack>
        {result ? (
          <>
            <Alert title="Подборка создана">Добавлено слов: {result.created}. Использовано существующих: {result.reused}. Пропущено заметок: {result.skipped}.</Alert>
            {result.warnings.map((text, index) => <Text size="sm" c="dimmed" key={index}>{text}</Text>)}
            {result.skipped > result.warnings.length && <Text size="xs" c="dimmed">Показаны первые {result.warnings.length} причин пропуска.</Text>}
            <Button onClick={onClose}>Открыть подборку</Button>
          </>
        ) : (
          <>
            <Text size="sm" c="dimmed">Импортируем текстовые поля в словарь и новую подборку. Медиа, шаблоны и прогресс Anki не переносятся. Одна заметка — одно слово.</Text>
            <FileInput label="Файл .apkg" accept=".apkg" value={file} disabled={busy}
              onChange={value => { setFile(value); setPreview(null); setName(value?.name.replace(/\.apkg$/i, '').slice(0, 100) ?? '') }} />
            {!preview ? <Button disabled={!file} loading={busy} onClick={() => void upload(false)}>Посмотреть содержимое</Button> : (
              <>
                <Text size="sm">Заметок в архиве: {preview.totalNotes}. Неподдерживаемых: {preview.unsupportedNotes}.</Text>
                {!preview.noteTypes.length ? <Alert color="orange">Подходящих текстовых заметок не найдено. Карточки с пропусками и изображения без текста не поддерживаются.</Alert> : (
                  <>
                    <TextInput label="Название подборки" maxLength={100} value={name} onChange={event => setName(event.currentTarget.value)} disabled={busy} />
                    <Select label="Тип заметки" data={preview.noteTypes.map(item => ({ value: String(item.id), label: `${item.name} (${item.notes})` }))}
                      value={model} onChange={value => selectModel(preview.noteTypes.find(type => String(type.id) === value))} disabled={busy} />
                    <Group grow align="start">
                      <Select label="Английское слово (ответ)" placeholder="Выберите поле с ответом" data={fields} value={english} onChange={setEnglish} disabled={busy} />
                      <Select label="Русское слово (лицевая сторона)" placeholder="Выберите поле с переводом" data={fields} value={russian} onChange={setRussian} disabled={busy} />
                    </Group>
                    <Select clearable label="Объяснение на русском (необязательно)" description="Покажем до открытия ответа в режиме RU → EN."
                      data={fields.filter(field => ![english, russian, example].includes(field.value))} value={hint} onChange={setHint} disabled={busy} />
                    <Select clearable label="Пример после ответа (необязательно)"
                      data={fields.filter(field => ![english, russian, hint].includes(field.value))} value={example} onChange={setExample} disabled={busy} />
                    {!distinct && <Text c="red" size="sm">Выберите отдельное поле для каждого значения.</Text>}
                    {(russian === null || emptyRussian) && (
                      <Alert color="orange">В предпросмотре не видно русского перевода. Проверьте, в каком поле колоды он хранится.</Alert>
                    )}
                    <Table.ScrollContainer minWidth={380}>
                      <Table>
                        <Table.Thead><Table.Tr><Table.Th>Русское слово</Table.Th>{hint !== null && <Table.Th>Объяснение</Table.Th>}<Table.Th>Английский ответ</Table.Th>{example !== null && <Table.Th>Пример</Table.Th>}</Table.Tr></Table.Thead>
                        <Table.Tbody>{selected?.samples.map((sample, index) => <Table.Tr key={index}>
                          <Table.Td>{russian === null ? '—' : sample[Number(russian)] || '—'}</Table.Td>
                          {hint !== null && <Table.Td>{sample[Number(hint)] || '—'}</Table.Td>}
                          <Table.Td>{english === null ? '—' : sample[Number(english)] || '—'}</Table.Td>
                          {example !== null && <Table.Td>{sample[Number(example)] || '—'}</Table.Td>}
                        </Table.Tr>)}</Table.Tbody>
                      </Table>
                    </Table.ScrollContainer>
                    <Text size="xs" c="dimmed">Другие типы заметок будут пропущены. Лимиты импорта: 100 МБ на файл, 100 МБ на распакованную базу; словарь — до 10 000 слов и 30 МБ текста.</Text>
                    <Button loading={busy} disabled={!valid || !model || !name.trim()} onClick={() => void upload(true)}>Импортировать в подборку</Button>
                  </>
                )}
              </>
            )}
          </>
        )}
      </Stack>
    </Modal>
  )
}
