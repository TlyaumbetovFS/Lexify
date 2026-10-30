import { notifications } from '@mantine/notifications'

const messages: Record<string, string> = {
  'Use a word in Russian or English, without mixing languages':
    'Введите слово на русском или английском, не смешивая языки.',
  'Email already registered': 'Этот email уже зарегистрирован.',
  'Invalid email or password': 'Неверный email или пароль.',
  'Word already in your vocabulary': 'Это слово уже есть в вашем словаре.',
  'Room owner must remain a moderator': 'Создатель комнаты остаётся модератором.',
  'User not found': 'Пользователь с таким ID не найден.',
  'Message must not be blank': 'Напишите текст сообщения.',
  'Message text must contain between 1 and 2000 characters':
    'Сообщение должно содержать от 1 до 2000 символов.',
  'Room not found': 'Комната больше не доступна.',
}

export class ApiError extends Error {
  status: number

  constructor(status: number, detail?: string) {
    super(
      messages[detail ?? ''] ??
        {
          400: 'Проверьте заполнение полей.',
          401: 'Сессия завершена. Войдите ещё раз.',
          403: 'У вас нет прав на это действие.',
          404: 'Запись больше не доступна.',
          409: 'Такая запись уже существует.',
          502: 'Перевод временно недоступен. Попробуйте ещё раз.',
        }[status] ??
        'Не удалось выполнить запрос. Попробуйте ещё раз.',
    )
    this.status = status
  }
}

export async function request<T>(
  path: string,
  token?: string,
  method = 'GET',
  body?: unknown,
): Promise<T> {
  let response: Response
  try {
    response = await fetch(`/api${path}`, {
      method,
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new Error('Нет связи с сервером. Проверьте подключение и попробуйте ещё раз.')
  }
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}))
    throw new ApiError(response.status, problem.detail)
  }
  return response.status === 204 ? (undefined as T) : response.json()
}

export function showError(error: unknown) {
  notifications.show({
    color: 'red',
    title: 'Не получилось',
    message: error instanceof Error ? error.message : 'Попробуйте ещё раз.',
  })
}
