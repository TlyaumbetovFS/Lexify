# Lexify — клиент

React, TypeScript, Vite, Mantine и STOMP.

```sh
npm ci
npm run dev
```

Клиент: http://127.0.0.1:5173. Бекенд должен работать на localhost:8080.
Vite проксирует `/api` и `/ws` на бекенд.

```sh
npm run build
```

Результат — `dist/`. Корневая инструкция описывает сборку вместе со Spring Boot.

`src/pages` — три основных раздела и вход, `src/components` — общие формы,
`src/hooks` — загрузка данных и подключение чата. Запросы — в `src/api.ts`,
DTO — в `src/types.ts`, стили — в `src/styles.css`.

Сессия хранится в sessionStorage текущей вкладки и очищается при выходе
или истечении срока токена.
