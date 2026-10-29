# Lexify

Приложение для изучения английского языка.

## Стек

Java 25, Spring Boot 4.1.2, Gradle, PostgreSQL, Spring Data JPA, Flyway,
Lombok, MapStruct. Локальный перевод — LibreTranslate.

## Запуск

Нужны Docker Compose и JDK 17 или новее для запуска Gradle.
Gradle автоматически загрузит JDK 25 для сборки и запуска приложения.

```sh
docker compose up -d --wait
./gradlew bootRun
```

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
`DB_PASSWORD` и `SERVER_PORT`. При смене учётных данных задавайте одинаковые
значения для приложения и Compose. Учётные данные существующей базы при смене
переменных Compose автоматически не обновляются.

## Сборка

```sh
./gradlew clean build
```

На первом этапе подготовлен каркас и инфраструктура. Пользовательские API,
миграции предметной области и тесты сервисов появятся на следующих этапах.

Проверено: сборка на JDK 25, запуск с PostgreSQL, ответ `UP` от
`/actuator/health` и перевод `привет` → `hello` через локальный LibreTranslate.
Автоматических тестов на этом этапе нет.
