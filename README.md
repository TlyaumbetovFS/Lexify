# Lexify

Приложение для изучения английского языка.

## Стек

Java 25, Spring Boot 4.1.2, Gradle, PostgreSQL, Spring Data JPA, Flyway,
Spring Security, JWT, WebSocket + STOMP, Lombok, MapStruct. Локальный перевод — LibreTranslate.
Примеры и транскрипция — Dictionary API, озвучка — Web Speech API браузера.

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

Клиент находится в `frontend`: React, TypeScript, Vite, готовые компоненты Mantine
и `@stomp/stompjs`. Страницы — в `src/pages`, общие формы — в `src/components`,
загрузка данных и подключение чата — в `src/hooks`. Запросы и типы DTO находятся
в `src/api.ts` и `src/types.ts`, оформление — в `src/styles.css`.

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
`DB_PASSWORD`, `SERVER_PORT`, `TRANSLATION_URL`, `DICTIONARY_URL` и обязательную `JWT_SECRET`
(случайный ключ минимум 32 байта в Base64). При смене учётных данных задавайте одинаковые
значения для приложения и Compose. Учётные данные существующей базы при смене
переменных Compose автоматически не обновляются.

## Сборка

```sh
./gradlew clean build
```

## Клиент

Для разработки клиента нужен Node.js 22.12 или новее и npm. Сначала запустите
бекенд и контейнеры по инструкции выше, затем в другом терминале:

```sh
cd frontend
npm ci
npm run dev
```

Интерфейс откроется на `http://127.0.0.1:5173`. Vite проксирует `/api` и `/ws`
на `localhost:8080`; отдельная настройка CORS не требуется.

В интерфейсе доступны регистрация и вход, все операции со словарём и подборками,
комнаты, история сообщений, отправка через STOMP, редактирование и удаление
сообщений, управление модераторами. Сессия хранится в sessionStorage текущей
вкладки и очищается при выходе или истечении срока JWT.

Чтобы собрать клиент и бекенд в один JAR:

```sh
npm --prefix frontend ci
npm --prefix frontend run build
./gradlew clean bootJar
```

Gradle включает готовый `frontend/dist` в статические ресурсы приложения.
Для запуска JAR нужен JDK 25 или новее. В терминале с загруженными переменными
из `.env` выполните:

```sh
java -jar build/libs/lexify-0.0.1-SNAPSHOT.jar
```

Интерфейс доступен на `http://localhost:8080`. Перед пересборкой JAR после изменения клиента
снова выполняйте `npm --prefix frontend run build`.

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
  -d '{"text":"hello"}'

curl http://localhost:8080/api/words -H "Authorization: Bearer $TOKEN"
```

| Метод | Путь | Действие |
| --- | --- | --- |
| POST | `/api/auth/register` | Регистрация и выдача JWT |
| POST | `/api/auth/login` | Вход и выдача JWT |
| GET | `/api/words` | Список своих слов |
| GET | `/api/words/{id}` | Своя карточка слова |
| POST | `/api/words` | Добавление слова с переводом |
| PUT | `/api/words/{id}` | Изменение слова и своего примера |
| POST | `/api/words/{id}/enrich` | Дополнение карточки словарными данными |
| DELETE | `/api/words/{id}` | Удаление слова |
| POST | `/api/translations` | Перевод английского слова на русский без сохранения |

`POST` и `PUT` принимают `text` (до 100 символов) и необязательный `example`.
Язык определяется по буквам: английское слово переводится на русский,
русское — на английский. Смешанный ввод и строки без букв возвращают `400`.
Например, можно добавить `{"text":"hello"}` или `{"text":"возможность"}`.

Пример, IPA-транскрипция и ссылка на аудио автоматически берутся из
[Dictionary API](https://dictionaryapi.dev/) для английского слова, если данные есть.
Свой `example` имеет приоритет; пустое поле включает автоматический пример.
При изменении слова перевод и словарные данные обновляются.

В ответе есть `sourceLanguage` (`en` или `ru`) и `dictionary`: транскрипция,
ссылка на аудио, источники и лицензии. Для старых карточек используйте
`POST /api/words/{id}/enrich` или пункт «Дополнить карточку» в интерфейсе.
Это действие сохраняет свой пример и не меняет перевод.

В чате можно нажать на английское слово и увидеть русский перевод во всплывающем окне.
`POST /api/translations` принимает `{"text":"hello"}` и возвращает
`{"translatedText":"привет"}`. Нужен JWT. Слово ограничено 100 символами;
допускаются английские буквы, внутренние апострофы и дефисы.
Используется локальный LibreTranslate. Перевод не добавляет слово в личный словарь.
Клиент запоминает переводы до выхода со страницы чата; при ошибке можно повторить запрос.

Данные словаря сохраняются в PostgreSQL отдельно от личных словарей и используются
всеми пользователями. Успешный результат кешируется на 30 дней, отсутствие данных —
на 5 минут. Открытие словаря не вызывает внешних запросов. Приложение делает не
более одного словарного запроса в секунду; при ошибке временно приостанавливает
запросы, учитывая `Retry-After`. Если обновление не удалось, ранее сохранённые
словарные данные остаются доступны.

Пример или транскрипция могут отсутствовать: это не мешает сохранить перевод.
Кнопка произношения использует `speechSynthesis` браузера и работает даже без
словарных данных. Выбирается английский голос, предпочтительно `en-US`, со скоростью
`0.9`. Следующее нажатие прерывает предыдущую озвучку. Доступные голоса и звучание
зависят от браузера и устройства. Аудиофайлы Dictionary API не загружаются.
Источники и лицензии словарных данных доступны по кнопке информации рядом с произношением.

Перевод остаётся локальным: `TRANSLATION_URL` по умолчанию равен `http://localhost:5000`.
Словарный сервис задаётся через `DICTIONARY_URL`, по умолчанию `https://api.dictionaryapi.dev`.

Ошибки: `400` — неверный запрос, `401` — неверные учётные данные или JWT,
`403` — недостаточно прав,
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

### Чаты

Комнаты доступны всем авторизованным пользователям. Создатель комнаты становится
её владельцем и модератором. Только владелец может менять название и описание,
удалять комнату, назначать и снимать модераторов. Владельца нельзя убрать из
модераторов. Модераторы действуют только в своей комнате.

Сообщения редактирует только автор. Удалить сообщение может автор или модератор
комнаты. Автор и время создания назначаются сервером. Удаление комнаты удаляет
её сообщения и список модераторов.

| Метод | Путь | Действие |
| --- | --- | --- |
| GET | `/api/rooms` | Список комнат |
| GET | `/api/rooms/{id}` | Комната и идентификаторы модераторов |
| POST | `/api/rooms` | Создание комнаты |
| PUT | `/api/rooms/{id}` | Изменение комнаты |
| DELETE | `/api/rooms/{id}` | Удаление комнаты |
| PUT | `/api/rooms/{id}/moderators/{userId}` | Назначение модератора |
| DELETE | `/api/rooms/{id}/moderators/{userId}` | Снятие модератора |
| GET | `/api/rooms/{roomId}/messages` | История сообщений |
| GET | `/api/rooms/{roomId}/messages/{id}` | Одно сообщение |
| POST | `/api/rooms/{roomId}/messages` | Отправка сообщения |
| PUT | `/api/rooms/{roomId}/messages/{id}` | Редактирование сообщения |
| DELETE | `/api/rooms/{roomId}/messages/{id}` | Удаление сообщения |

Создание и изменение комнаты принимают `{"name":"English practice","description":"Travel"}`.
Название — до 100 символов, описание необязательно, до 1000 символов.
Сообщения принимают `{"text":"Hello!"}`, текст — от 1 до 2000 символов.
Пробелы по краям названия и текста удаляются.

История возвращает сообщения от новых к старым. Параметры `page` и `size`
по умолчанию равны `0` и `50`, максимальный `size` — `100`.

### WebSocket

Подключение: `ws://localhost:8080/ws`, протокол `v12.stomp`.
JWT передаётся в заголовке `Authorization: Bearer <accessToken>` кадра `CONNECT`.
Неверный JWT или запрещённый адрес приводит к STOMP `ERROR`; клиенту нужно
переподключиться. JWT также проверяется при отправке и подписке.
Соединения из браузера принимаются с того же origin, что и сервер.

| Действие | Адрес | Данные |
| --- | --- | --- |
| SUBSCRIBE | `/topic/rooms` | События создания, изменения и удаления комнат |
| SUBSCRIBE | `/topic/rooms/{id}` | События конкретной комнаты и её сообщений |
| SUBSCRIBE | `/user/queue/errors` | Ошибки запросов текущего соединения |
| SEND | `/app/rooms/{id}/messages` | `{"text":"Hello!"}` |

Сообщения из REST и STOMP сохраняются одинаково. События отправляются после
фиксации транзакции в БД. Прямые отправки в `/topic` и `/queue`, подписки с
шаблонами и подписки на чужие очереди запрещены.

Событие сообщения:

```json
{
  "type": "MESSAGE_CREATED",
  "roomId": 1,
  "message": {
    "id": 1,
    "roomId": 1,
    "authorId": 1,
    "text": "Hello!",
    "createdAt": "2026-10-30T00:00:00Z"
  }
}
```

Типы: `MESSAGE_CREATED`, `MESSAGE_UPDATED`, `MESSAGE_DELETED`,
`ROOM_CREATED`, `ROOM_UPDATED`, `ROOM_DELETED`. Событие удаления сообщения
содержит удалённое сообщение. У событий комнаты `message` равен `null`;
при `ROOM_CREATED` и `ROOM_UPDATED` клиент запрашивает актуальную комнату через REST.
Изменение списка модераторов также отправляет `ROOM_UPDATED`.

Ошибки отправки сообщения приходят только в `/user/queue/errors` текущего
соединения, например `{"status":400,"message":"Message must not be blank"}`.

Пример подключения в браузере после получения JWT:

```js
const token = "<accessToken>";
const roomId = 1;
const socket = new WebSocket("ws://localhost:8080/ws", "v12.stomp");

socket.onopen = () => socket.send(
  `CONNECT\naccept-version:1.2\nhost:localhost\nAuthorization:Bearer ${token}\n\n\0`
);

socket.onmessage = ({ data }) => {
  if (data.startsWith("CONNECTED")) {
    socket.send(`SUBSCRIBE\nid:room\ndestination:/topic/rooms/${roomId}\n\n\0`);
    socket.send("SUBSCRIBE\nid:errors\ndestination:/user/queue/errors\n\n\0");
  }
  console.log(data);
};

function sendMessage(text) {
  socket.send(
    `SEND\ndestination:/app/rooms/${roomId}/messages\ncontent-type:application/json\n\n${JSON.stringify({ text })}\0`
  );
}
```

Вызывайте `sendMessage` после получения `CONNECTED`.

Тесты сервисов, JWT и проверки STOMP выполняются без Docker через `./gradlew test`.
