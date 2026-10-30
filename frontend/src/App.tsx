import { useCallback, useEffect } from 'react'
import {
  ActionIcon,
  AppShell,
  Avatar,
  Badge,
  Burger,
  CloseButton,
  Group,
  NavLink,
  Stack,
  Text,
  Tooltip,
} from '@mantine/core'
import { useDisclosure, useHash, useSessionStorage } from '@mantine/hooks'
import {
  BookOpen,
  ChevronRight,
  Folder,
  Languages,
  LogOut,
  MessageCircle,
  Sparkles,
} from 'lucide-react'
import { ApiError, request, showError } from './api'
import { AuthPage } from './pages/AuthPage'
import { WordsPage } from './pages/WordsPage'
import { CollectionsPage } from './pages/CollectionsPage'
import { ChatPage } from './pages/ChatPage'
import type { Api, Session } from './types'

const pages = [
  { id: 'dictionary', label: 'Мой словарь', icon: BookOpen },
  { id: 'collections', label: 'Подборки', icon: Folder },
  { id: 'chat', label: 'Разговорные комнаты', icon: MessageCircle },
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
          <Text className="eyebrow" c="dimmed" mb="md">
            ТВОЁ ПРОСТРАНСТВО
          </Text>
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
          <div className="sidebar-note">
            <Sparkles size={19} />
            <Text fw={650} size="sm" mt="sm">
              В своём ритме
            </Text>
            <Text size="xs" c="dimmed" mt={5}>
              Одно новое слово сегодня — чуть больше уверенности завтра.
            </Text>
          </div>
          <Group gap="xs" mt="xl" justify="space-between">
            <Group gap={7}>
              <Languages size={16} />
              <Text size="xs" fw={600}>
                English ↔ Русский
              </Text>
            </Group>
            <Badge size="xs" variant="light">
              RU / EN
            </Badge>
          </Group>
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
            <Text size="sm" c="dimmed" visibleFrom="sm">
              Личное пространство
            </Text>
            <ChevronRight size={13} className="breadcrumb-arrow" />
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
        <div className={`main-content ${current.id === 'chat' ? 'main-chat' : ''}`}>
          {current.id === 'dictionary' && <WordsPage api={api} />}
          {current.id === 'collections' && <CollectionsPage api={api} />}
          {current.id === 'chat' && <ChatPage api={api} session={session} />}
        </div>
      </AppShell.Main>
    </AppShell>
  )
}
