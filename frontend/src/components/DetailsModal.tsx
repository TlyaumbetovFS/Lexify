import { Button, Group, Modal, Stack, Textarea, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useState } from 'react'
import { showError } from '../api'

export function DetailsModal({
  title,
  initial,
  onSave,
  onClose,
}: {
  title: string
  initial?: { name: string; description: string | null }
  onSave: (value: { name: string; description: string | null }) => Promise<void>
  onClose: () => void
}) {
  const [busy, setBusy] = useState(false)
  const form = useForm({
    initialValues: { name: initial?.name ?? '', description: initial?.description ?? '' },
    validate: { name: (value) => (value.trim() ? null : 'Введите название') },
  })
  return (
    <Modal opened onClose={() => !busy && onClose()} title={title} centered>
      <form
        onSubmit={form.onSubmit(async (value) => {
          setBusy(true)
          try {
            await onSave({ name: value.name.trim(), description: value.description.trim() || null })
            onClose()
          } catch (error) {
            showError(error)
          } finally {
            setBusy(false)
          }
        })}
      >
        <Stack>
          <TextInput
            label="Название"
            placeholder="Например, путешествия"
            maxLength={100}
            required
            autoFocus
            disabled={busy}
            {...form.getInputProps('name')}
          />
          <Textarea
            label="Описание"
            placeholder="Коротко о самом главном"
            maxLength={1000}
            minRows={3}
            disabled={busy}
            {...form.getInputProps('description')}
          />
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose} disabled={busy}>
              Отмена
            </Button>
            <Button type="submit" loading={busy}>
              Сохранить
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
