import { Text } from '@mantine/core'
import { modals } from '@mantine/modals'
import { showError } from '../api'

export function confirmDelete(title: string, description: string, action: () => Promise<void>) {
  modals.openConfirmModal({
    title,
    children: (
      <Text size="sm" c="dimmed">
        {description}
      </Text>
    ),
    labels: { confirm: 'Удалить', cancel: 'Отмена' },
    confirmProps: { color: 'red' },
    onConfirm: () => action().catch(showError),
  })
}
