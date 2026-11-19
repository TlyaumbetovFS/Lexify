import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createTheme, MantineProvider, virtualColor } from '@mantine/core'
import { ModalsProvider } from '@mantine/modals'
import { Notifications } from '@mantine/notifications'
import '@mantine/core/styles.css'
import '@mantine/notifications/styles.css'
import './styles.css'
import App from './App'

const theme = createTheme({
  primaryColor: 'blue',
  primaryShade: { light: 5, dark: 7 },
  defaultRadius: 'md',
  colors: {
    dark: [
      '#eeeef2',
      '#d0d2da',
      '#afb2bd',
      '#8d929f',
      '#666c78',
      '#454b56',
      '#2b3038',
      '#1e2228',
      '#15181c',
      '#0d0f12',
    ],
    blue: virtualColor({ name: 'blue', light: 'spark', dark: 'lavender' }),
    lavender: [
      '#f2eefc',
      '#e1d8fa',
      '#cbbdf5',
      '#b4a4f5',
      '#a28bdf',
      '#8971c8',
      '#725cb6',
      '#6550a5',
      '#514084',
      '#3e3264',
    ],
    spark: [
      '#edf3ff',
      '#dbe7ff',
      '#b7cfff',
      '#8bb1ff',
      '#5f92ff',
      '#3775f6',
      '#285ed6',
      '#204cba',
      '#1c4198',
      '#193774',
    ],
  },
  fontFamily: "Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
  headings: { fontWeight: '750' },
  components: {
    Modal: { defaultProps: { closeButtonProps: { 'aria-label': 'Закрыть окно' } } },
    Button: { defaultProps: { fw: 600 } },
    TextInput: { defaultProps: { size: 'md' } },
    PasswordInput: { defaultProps: { size: 'md' } },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <MantineProvider theme={theme} defaultColorScheme="light">
      <ModalsProvider labels={{ confirm: 'Подтвердить', cancel: 'Отмена' }}>
        <Notifications position="top-right" />
        <App />
      </ModalsProvider>
    </MantineProvider>
  </StrictMode>,
)
