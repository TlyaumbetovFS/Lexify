# Lexify

Приложение для изучения английского языка.

## Стек

Java 25, Spring Boot 4.1.2, Gradle, PostgreSQL, Spring Data JPA, Flyway,
Spring Security, JWT, Lombok, MapStruct. Локальный перевод — LibreTranslate.

## Структура

Код в `src/main/java/com/lexify` разделён по слоям:

```text
client       — клиент LibreTranslate
config       — конфигурация Spring
controller   — REST-контроллеры
dto          — запросы и ответы
exception    — обработка ошибок API
mapper       — преобразование моделей в DTO
model        — JPA-сущности
repository   — доступ к базе данных
service      — бизнес-логика
```

Тесты в `src/test/java/com/lexify` повторяют структуру основных пакетов.

## Запуск

Нужны Docker Compose и JDK 17 или новее для запуска Gradle.
Gradle автоматически загрузит JDK 25 для сборки и запуска приложения.

```sh
docker compose up -d --wait
```

Один раз создайте локальный JWT-ключ:

```sh
echo "JWT_SECRET=$(openssl rand -base64 32)" > .env
chmod 600 .env
```

Загрузите переменные и запустите приложение:

```sh
set -a
. ./.env
set +a
./gradlew bootRun
```

Файл `.env` исключён из Git. Используйте один и тот же ключ между запусками;
после смены ключа ранее выданные токены перестанут работать.

При первом запуске LibreTranslate скачивает модели русского и английского языков.
Если загрузка моделей занимает больше пяти минут, дождитесь её завершения:

```sh
docker compose logs -f libretranslate
```

Проверка приложения:

```sh
curl http://localhost:8080/actuator/health
```

Проверка перевода:

```sh
curl http://localhost:5000/translate \
  -H 'Content-Type: application/json' \
  -d '{"q":"привет","source":"ru","target":"en","format":"text"}'
```

PostgreSQL доступен на `localhost:5432`, база, пользователь и пароль для локальной
разработки — `lexify`. Данные и модели сохраняются в Docker volumes.

Приложение поддерживает переменные окружения `DB_URL`, `DB_USERNAME`,
`DB_PASSWORD`, `SERVER_PORT`, `TRANSLATION_URL` и обязательную `JWT_SECRET`
(случайный ключ минимум 32 байта в Base64). При смене учётных данных задавайте одинаковые
значения для приложения и Compose. Учётные данные существующей базы при смене
переменных Compose автоматически не обновляются.

## Сборка

```sh
./gradlew clean build
```

## API

Регистрация и вход возвращают `accessToken`, `tokenType` и `expiresIn` в секундах.
JWT действует один час. Пароль — от 8 символов, не более 72 байт в UTF-8.

```sh
curl http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@example.com","password":"password123"}'

curl http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@example.com","password":"password123"}'
```

Скопируйте `accessToken` в переменную `TOKEN`. Для запросов к словарю передавайте
`Authorization: Bearer $TOKEN`.

```sh
curl http://localhost:8080/api/words \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"russianText":"привет","example":"Hello, friend!"}'

curl http://localhost:8080/api/words -H "Authorization: Bearer $TOKEN"
```

| Метод | Путь | Действие |
| --- | --- | --- |
| POST | `/api/auth/register` | Регистрация и выдача JWT |
| POST | `/api/auth/login` | Вход и выдача JWT |
| GET | `/api/words` | Список своих слов |
| GET | `/api/words/{id}` | Своя карточка слова |
| POST | `/api/words` | Добавление слова с переводом |
| PUT | `/api/words/{id}` | Замена русского текста и примера |
| DELETE | `/api/words/{id}` | Удаление слова |

`PUT` принимает тот же JSON, что `POST`: `russianText` и необязательный `example`.
Если пример не указан, он очищается. При изменении русского текста перевод
обновляется автоматически. Слова сохраняются без пробелов по краям и в нижнем
регистре; одно слово нельзя добавить себе дважды. Примеры указываются вручную.

Ошибки: `400` — неверный запрос, `401` — неверные учётные данные или JWT,
`404` — слово не найдено в вашем словаре, `409` — дубликат,
`502` — переводчик недоступен или вернул некорректный ответ.
При ошибке перевода новое слово не сохраняется, существующее не изменяется.

### Подборки

Подборки доступны только владельцу. Они содержат ссылки на уже добавленные
слова личного словаря. Одно слово можно включить в несколько подборок.

| Метод | Путь | Действие |
| --- | --- | --- |
| GET | `/api/collections` | Список своих подборок со словами |
| GET | `/api/collections/{id}` | Подборка со словами |
| POST | `/api/collections` | Создание подборки |
| PUT | `/api/collections/{id}` | Изменение названия и описания |
| DELETE | `/api/collections/{id}` | Удаление подборки |
| PUT | `/api/collections/{id}/words/{wordId}` | Добавление своего слова в подборку |
| DELETE | `/api/collections/{id}/words/{wordId}` | Удаление слова из подборки |

```sh
curl http://localhost:8080/api/collections \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Путешествия","description":"Слова для поездок"}'

curl -X PUT http://localhost:8080/api/collections/1/words/1 \
  -H "Authorization: Bearer $TOKEN"
```

В последнем примере замените идентификаторы подборки и слова на свои.
Название — от 1 до 100 символов, описание необязательно, до 1000 символов.
Пробелы по краям названия удаляются. `PUT /api/collections/{id}` принимает
тот же JSON, что создание; пропущенное описание очищается, состав слов сохраняется.

Повторное добавление слова не создаёт дубль. Удаление слова из подборки или
самой подборки сохраняет слово в словаре. Удаление слова из личного словаря
также удаляет его из всех подборок. Обращение к чужой подборке или попытка
добавить чужое слово возвращает `404`.

Тесты сервисов и JWT выполняются без Docker через `./gradlew test`.
