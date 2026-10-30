import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createTheme, MantineProvider } from '@mantine/core'
import { ModalsProvider } from '@mantine/modals'
import { Notifications } from '@mantine/notifications'
import '@mantine/core/styles.css'
import '@mantine/notifications/styles.css'
import './styles.css'
import App from './App'

const theme = createTheme({
  primaryColor: 'blue',
  primaryShade: 5,
  defaultRadius: 'md',
  colors: {
    blue: [
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
    <MantineProvider theme={theme} forceColorScheme="light">
      <ModalsProvider labels={{ confirm: 'Подтвердить', cancel: 'Отмена' }}>
        <Notifications position="top-right" />
        <App />
      </ModalsProvider>
    </MantineProvider>
  </StrictMode>,
)
