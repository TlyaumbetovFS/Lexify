import {
  Alert,
  Anchor,
  Button,
  Group,
  Paper,
  PasswordInput,
  Stack,
  Text,
  TextInput,
  ThemeIcon,
  Title,
} from '@mantine/core'
import { useForm } from '@mantine/form'
import { ArrowRight, BookOpen, Check, MessageCircle } from 'lucide-react'
import { useState } from 'react'
import { request } from '../api'
import type { Session } from '../types'

export function AuthPage({ onLogin }: { onLogin: (session: Session) => void }) {
  const [register, setRegister] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const form = useForm({
    initialValues: { email: '', password: '' },
    validate: {
      email: (value) => (/^\S+@\S+\.\S+$/.test(value) ? null : 'Введите корректный email'),
      password: (value) =>
        value.length < 8
          ? 'Не менее 8 символов'
          : new TextEncoder().encode(value).length > 72
            ? 'Пароль слишком длинный, сократите его.'
            : null,
    },
  })
  return (
    <main className="auth-layout">
      <section className="auth-story">
        <div className="brand">
          <span className="brand-mark">
            <BookOpen size={23} />
          </span>
          <span>
            lexify<span className="brand-dot">.</span>
          </span>
        </div>
        <div className="auth-pitch">
          <div className="eyebrow">ТВОЙ АНГЛИЙСКИЙ. ТВОЙ РИТМ.</div>
          <h1>
            Каждое слово —<br />
            новая возможность<span>.</span>
          </h1>
          <p>Собирай полезные слова, находи им место в своей жизни и общайся на английском.</p>
          <Stack gap="lg" mt={36}>
            {[
              'Личный словарь с переводом',
              'Подборки на любые темы',
              'Живое общение в комнатах',
            ].map((text) => (
              <Group key={text} gap="sm">
                <ThemeIcon variant="white" radius="xl" size={24}>
                  <Check size={14} />
                </ThemeIcon>
                <Text size="sm">{text}</Text>
              </Group>
            ))}
          </Stack>
        </div>
        <div className="auth-word">
          <span>serendipity</span>
          <Text size="sm">счастливая случайность</Text>
          <MessageCircle className="auth-word-icon" size={44} strokeWidth={1.3} />
        </div>
        <Text size="xs" className="auth-footer">
          Little by little, a little becomes a lot.
        </Text>
      </section>
      <section className="auth-form">
        <Paper w="100%" maw={390} p="xs">
          <div className="brand auth-mobile-brand">
            <span className="brand-mark">
              <BookOpen size={23} />
            </span>
            <span>
              lexify<span className="brand-dot">.</span>
            </span>
          </div>
          <Text size="sm" c="blue" fw={650} mb="xs">
            LET’S GET STARTED
          </Text>
          <Title order={2} mb="xs">
            {register ? 'Начнём знакомство' : 'С возвращением'}
          </Title>
          <Text c="dimmed" size="sm" mb={32}>
            {register
              ? 'Создай свой словарь — первое слово уже рядом.'
              : 'Твои слова и разговоры ждут тебя.'}
          </Text>
          <form
            onSubmit={form.onSubmit(async (values) => {
              setBusy(true)
              setError('')
              try {
                const result = await request<{ accessToken: string; expiresIn: number }>(
                  `/auth/${register ? 'register' : 'login'}`,
                  undefined,
                  'POST',
                  { ...values, email: values.email.trim() },
                )
                const payload = JSON.parse(
                  atob(result.accessToken.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')),
                )
                onLogin({
                  accessToken: result.accessToken,
                  expiresAt: Date.now() + result.expiresIn * 1000,
                  email: values.email.trim(),
                  userId: Number(payload.sub),
                })
              } catch (error) {
                setError(error instanceof Error ? error.message : 'Попробуйте ещё раз.')
              } finally {
                setBusy(false)
              }
            })}
          >
            <Stack gap="lg">
              {error && (
                <Alert color="red" role="alert">
                  {error}
                </Alert>
              )}
              <TextInput
                label="Email"
                placeholder="you@example.com"
                type="email"
                autoComplete="email"
                maxLength={254}
                required
                disabled={busy}
                {...form.getInputProps('email')}
              />
              <PasswordInput
                label="Пароль"
                placeholder="Не менее 8 символов"
                autoComplete={register ? 'new-password' : 'current-password'}
                maxLength={72}
                required
                disabled={busy}
                {...form.getInputProps('password')}
              />
              <Button
                type="submit"
                size="md"
                fullWidth
                loading={busy}
                rightSection={<ArrowRight size={17} />}
              >
                {register ? 'Создать аккаунт' : 'Войти'}
              </Button>
            </Stack>
          </form>
          <Text c="dimmed" size="sm" ta="center" mt="xl">
            {register ? 'Уже есть аккаунт?' : 'Пока нет аккаунта?'}{' '}
            <Anchor
              component="button"
              type="button"
              fw={600}
              disabled={busy}
              onClick={() => {
                setRegister(!register)
                setError('')
                form.clearErrors()
              }}
            >
              {register ? 'Войти' : 'Зарегистрироваться'}
            </Anchor>
          </Text>
        </Paper>
      </section>
    </main>
  )
}
