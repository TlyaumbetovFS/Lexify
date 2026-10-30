package com.lexify.client;

import com.lexify.dto.DictionaryData;
import com.lexify.dto.DictionaryResponse;
import java.net.URI;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class DictionaryClient {

    private final RestClient dictionaryRestClient;
    private Instant nextRequestAt = Instant.EPOCH;
    private Instant blockedUntil = Instant.EPOCH;

    public synchronized Optional<DictionaryData> lookup(String word) {
        if (Instant.now().isBefore(blockedUntil)) {
            return Optional.empty();
        }
        try {
            var delay = Duration.between(Instant.now(), nextRequestAt).toMillis();
            if (delay > 0) {
                Thread.sleep(delay);
            }
            nextRequestAt = Instant.now().plusSeconds(1);
            var response = dictionaryRestClient.get()
                    .uri("/api/v2/entries/en/{word}", word)
                    .retrieve()
                    .body(DictionaryResponse[].class);
            return Optional.of(extract(response));
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                return Optional.of(DictionaryData.empty());
            }
            var headers = exception.getResponseHeaders();
            blockedUntil = retryAt(headers == null ? null : headers.getFirst("Retry-After"));
            return Optional.empty();
        } catch (RestClientException exception) {
            blockedUntil = Instant.now().plusSeconds(60);
            return Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private DictionaryData extract(DictionaryResponse[] response) {
        if (response == null) {
            return DictionaryData.empty();
        }
        var entries = Arrays.stream(response).filter(Objects::nonNull).toList();
        var phonetics = entries.stream().flatMap(entry -> stream(entry.phonetics())).toList();
        var pronunciation = phonetics.stream()
                .filter(value -> text(value.text(), 200) != null && httpsUrl(value.audio()) != null)
                .findFirst()
                .orElseGet(() -> phonetics.stream().filter(value -> httpsUrl(value.audio()) != null)
                        .findFirst().orElse(null));
        var phonetic = pronunciation == null ? null : text(pronunciation.text(), 200);
        if (phonetic == null) {
            phonetic = phonetics.stream().map(value -> text(value.text(), 200)).filter(Objects::nonNull)
                    .findFirst().orElseGet(() -> entries.stream().map(value -> text(value.phonetic(), 200))
                            .filter(Objects::nonNull).findFirst().orElse(null));
        }
        var example = entries.stream().flatMap(entry -> stream(entry.meanings()))
                .flatMap(meaning -> stream(meaning.definitions()))
                .map(definition -> text(definition.example(), 1000)).filter(Objects::nonNull)
                .findFirst().orElse(null);
        var source = entries.stream().flatMap(entry -> stream(entry.sourceUrls()))
                .map(this::httpsUrl).filter(Objects::nonNull).findFirst().orElse(null);
        var license = entries.stream().map(DictionaryResponse::license).filter(Objects::nonNull)
                .map(value -> httpsUrl(value.url())).filter(Objects::nonNull).findFirst().orElse(null);
        return new DictionaryData(example, phonetic,
                pronunciation == null ? null : httpsUrl(pronunciation.audio()), source, license,
                pronunciation == null ? null : httpsUrl(pronunciation.sourceUrl()),
                pronunciation == null || pronunciation.license() == null ? null : httpsUrl(pronunciation.license().url()));
    }

    private <T> Stream<T> stream(List<T> values) {
        return values == null ? Stream.empty() : values.stream().filter(Objects::nonNull);
    }

    private String text(String value, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            return null;
        }
        return value.strip();
    }

    private String httpsUrl(String value) {
        if (value == null || value.isBlank() || value.length() > 2000) {
            return null;
        }
        try {
            var url = URI.create(value.startsWith("//") ? "https:" + value : value);
            return "https".equalsIgnoreCase(url.getScheme()) && url.getHost() != null && url.getUserInfo() == null
                    ? url.toASCIIString() : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private Instant retryAt(String header) {
        var fallback = Instant.now().plusSeconds(60);
        if (header == null) {
            return fallback;
        }
        try {
            var retry = Instant.now().plusSeconds(Long.parseLong(header));
            return retry.isAfter(fallback) ? retry : fallback;
        } catch (NumberFormatException | DateTimeException | ArithmeticException exception) {
            try {
                var retry = ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
                return retry.isAfter(fallback) ? retry : fallback;
            } catch (DateTimeParseException invalidDate) {
                return fallback;
            }
        }
    }
}
