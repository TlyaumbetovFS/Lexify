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
  Title,
} from '@mantine/core'
import { useForm } from '@mantine/form'
import { ArrowRight, BookOpen } from 'lucide-react'
import { useState } from 'react'
import { request } from '../api'
import { ThemeToggle } from '../components/ThemeToggle'
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
  async function submit(values: typeof form.values) {
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
        expiresAt: Number(payload.exp) * 1000,
        email: values.email.trim(),
        userId: Number(payload.sub),
      })
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Попробуйте ещё раз.')
    } finally {
      setBusy(false)
    }
  }
  return (
    <main className="auth-layout">
      <section className="auth-form">
        <Paper w="100%" maw={390} p="xs">
          <Group justify="space-between" wrap="nowrap" mb={28}>
            <div className="brand">
              <span className="brand-mark">
                <BookOpen size={23} />
              </span>
              <span>
                lexify<span className="brand-dot">.</span>
              </span>
            </div>
            <ThemeToggle />
          </Group>
          <Title order={2} mb="xs">
            {register ? 'Регистрация' : 'Вход'}
          </Title>
          <Text c="dimmed" size="sm" mb={32}>
            {register
              ? 'Создай аккаунт для словаря и чатов.'
              : 'Войди, чтобы продолжить учить слова.'}
          </Text>
          <form onSubmit={form.onSubmit(submit)}>
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
