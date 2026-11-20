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
  const [name, setName] = useState('')
  const [busy, setBusy] = useState(false)
  const [result, setResult] = useState<AnkiImportResult | null>(null)
  const selected = preview?.noteTypes.find(item => String(item.id) === model)
  const fields = selected?.fields.map((label, index) => ({ value: String(index), label })) ?? []
  const valid = english !== null && russian !== null && english !== russian
    && (example === null || example !== english && example !== russian)

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
        body.append('name', name.trim())
        const imported = await api<AnkiImportResult>('/anki/import', 'POST', body)
        setResult(imported)
        onImported(imported.collectionId)
      } else {
        const data = await api<AnkiPreview>('/anki/preview', 'POST', body)
        setPreview(data)
        setModel(data.noteTypes[0] ? String(data.noteTypes[0].id) : null)
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
                      value={model} onChange={value => { setModel(value); setEnglish('0'); setRussian('1'); setExample(null) }} disabled={busy} />
                    <Group grow align="start">
                      <Select label="Английское слово" data={fields} value={english} onChange={setEnglish} disabled={busy} />
                      <Select label="Русский перевод" data={fields} value={russian} onChange={setRussian} disabled={busy} />
                    </Group>
                    <Select clearable label="Пример (необязательно)" data={fields} value={example} onChange={setExample} disabled={busy} />
                    {!valid && <Text c="red" size="sm">Для слова, перевода и примера выберите разные поля.</Text>}
                    <Table.ScrollContainer minWidth={380}>
                      <Table>
                        <Table.Thead><Table.Tr><Table.Th>English</Table.Th><Table.Th>Перевод</Table.Th><Table.Th>Пример</Table.Th></Table.Tr></Table.Thead>
                        <Table.Tbody>{selected?.samples.map((sample, index) => <Table.Tr key={index}>
                          <Table.Td>{sample[Number(english)]}</Table.Td><Table.Td>{sample[Number(russian)]}</Table.Td><Table.Td>{example === null ? '—' : sample[Number(example)]}</Table.Td>
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
