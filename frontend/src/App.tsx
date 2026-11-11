import { lazy, Suspense, useCallback, useEffect } from 'react'
import {
  ActionIcon,
  AppShell,
  Avatar,
  Burger,
  CloseButton,
  Group,
  Loader,
  NavLink,
  Stack,
  Text,
  Tooltip,
} from '@mantine/core'
import { useDisclosure, useHash, useSessionStorage } from '@mantine/hooks'
import { ChartNoAxesColumn, BookOpen, ChevronRight, Folder, LogOut, MessageCircle } from 'lucide-react'
import { ApiError, request, showError } from './api'
import { ThemeToggle } from './components/ThemeToggle'
import { AuthPage } from './pages/AuthPage'
import type { Api, Session } from './types'

const WordsPage = lazy(() =>
  import('./pages/WordsPage').then((module) => ({ default: module.WordsPage })),
)
const CollectionsPage = lazy(() =>
  import('./pages/CollectionsPage').then((module) => ({ default: module.CollectionsPage })),
)
const ChatPage = lazy(() =>
  import('./pages/ChatPage').then((module) => ({ default: module.ChatPage })),
)

const StatisticsPage = lazy(() =>
  import('./pages/StatisticsPage').then((module) => ({ default: module.StatisticsPage })),
)

const pages = [
  { id: 'dictionary', label: 'Мой словарь', icon: BookOpen },
  { id: 'collections', label: 'Подборки', icon: Folder },
  { id: 'chat', label: 'Чаты', icon: MessageCircle },
  { id: 'statistics', label: 'Статистика', icon: ChartNoAxesColumn },
]

export default function App() {
  const [session, setSession] = useSessionStorage<Session | null>({
    key: 'lexify-session',
    defaultValue: null,
    getInitialValueInEffect: false,
  })
  const [opened, { toggle, close }] = useDisclosure()
  const [hash] = useHash()
  const current = pages.find((page) => `#${page.id}` === hash) ?? pages[0]
  const token = session?.accessToken
  const logout = useCallback(() => {
    setSession(null)
    close()
  }, [setSession, close])

  useEffect(() => {
    if (!session) return
    const remaining = session.expiresAt - Date.now()
    if (remaining <= 0) {
      logout()
      return
    }
    const timeout = setTimeout(logout, remaining)
    return () => clearTimeout(timeout)
  }, [session, logout])

  const api: Api = useCallback(
    async <T,>(path: string, method?: string, body?: unknown) => {
      try {
        return await request<T>(path, token, method, body)
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) {
          logout()
          showError(error)
        }
        throw error
      }
    },
    [token, logout],
  )

  if (!session) return <AuthPage onLogin={setSession} />

  return (
    <AppShell
      layout="alt"
      header={{ height: 72 }}
      navbar={{ width: 232, breakpoint: 'md', collapsed: { mobile: !opened } }}
      padding={0}
    >
      <AppShell.Navbar className="sidebar">
        <div className="brand sidebar-brand">
          <span className="brand-mark">
            <BookOpen size={22} />
          </span>
          <span>
            lexify<span className="brand-dot">.</span>
          </span>
          <CloseButton hiddenFrom="md" ml="auto" onClick={close} aria-label="Закрыть навигацию" />
        </div>
        <div className="sidebar-menu">
          <Stack gap={6}>
            {pages.map((page) => (
              <NavLink
                key={page.id}
                component="a"
                href={`#${page.id}`}
                label={page.label}
                active={page.id === current.id}
                leftSection={<page.icon size={19} strokeWidth={1.8} />}
                rightSection={page.id === current.id ? <ChevronRight size={14} /> : null}
                onClick={close}
                className="sidebar-link"
              />
            ))}
          </Stack>
        </div>
        <div className="sidebar-bottom">
          <Text size="xs" c="dimmed" mt="sm">
            Ваш ID: {session.userId}
          </Text>
        </div>
      </AppShell.Navbar>
      <AppShell.Header className="topbar">
        <Group justify="space-between" h="100%" px={{ base: 20, md: 36 }} wrap="nowrap">
          <Group gap="sm" wrap="nowrap">
            <Burger
              opened={opened}
              aria-expanded={opened}
              onClick={toggle}
              hiddenFrom="md"
              size="sm"
              aria-label="Открыть навигацию"
            />
            <Text size="sm" fw={600}>
              {current.label}
            </Text>
          </Group>
          <Group gap="sm" wrap="nowrap">
            <Avatar color="blue" radius="xl" size={32}>
              {session.email.slice(0, 2).toUpperCase()}
            </Avatar>
            <Text size="sm" visibleFrom="sm" className="account-email">
              {session.email}
            </Text>
            <ThemeToggle />
            <Tooltip label="Выйти">
              <ActionIcon
                variant="subtle"
                color="gray"
                onClick={logout}
                aria-label="Выйти из аккаунта"
              >
                <LogOut size={17} />
              </ActionIcon>
            </Tooltip>
          </Group>
        </Group>
      </AppShell.Header>
      <AppShell.Main>
        <div className="main-content">
          <Suspense
            fallback={
              <Group justify="center" p="xl" role="status" aria-label="Загрузка раздела">
                <Loader />
              </Group>
            }
          >
            {current.id === 'dictionary' && <WordsPage api={api} />}
            {current.id === 'collections' && <CollectionsPage api={api} />}
            {current.id === 'chat' && <ChatPage api={api} session={session} />}
            {current.id === 'statistics' && <StatisticsPage api={api} />}
          </Suspense>
        </div>
      </AppShell.Main>
    </AppShell>
  )
}
