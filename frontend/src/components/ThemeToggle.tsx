import { ActionIcon, Tooltip, useComputedColorScheme, useMantineColorScheme } from '@mantine/core'
import { Moon, Sun } from 'lucide-react'

export function ThemeToggle() {
  const isDark = useComputedColorScheme('light') === 'dark'
  const { toggleColorScheme } = useMantineColorScheme()
  const label = isDark ? 'Включить светлую тему' : 'Включить тёмную тему'
  const Icon = isDark ? Sun : Moon

  return (
    <Tooltip label={label}>
      <ActionIcon
        variant="subtle"
        color="gray"
        onClick={toggleColorScheme}
        aria-label={label}
      >
        <Icon size={17} />
      </ActionIcon>
    </Tooltip>
  )
}
