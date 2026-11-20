package com.lexify.service;

import com.lexify.dto.AnkiImportResult;
import com.lexify.dto.AnkiPreview;
import com.lexify.model.Word;
import com.lexify.model.WordCollection;
import com.lexify.repository.CollectionRepository;
import com.lexify.repository.UserRepository;
import com.lexify.repository.WordRepository;
import java.nio.charset.StandardCharsets;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AnkiImportService {
    private final AnkiPackageReader reader;
    private final UserRepository users;
    private final WordRepository words;
    private final CollectionRepository collections;

    public AnkiPreview preview(MultipartFile file) {
        var data = reader.read(file);
        return new AnkiPreview(data.types(), data.notes().size(),
                (int) data.notes().stream().filter(AnkiPackageReader.Note::unsupported).count());
    }

    @Transactional
    public AnkiImportResult importWords(Long userId, MultipartFile file, long modelId, int englishField,
                                        int russianField, Integer exampleField, Integer hintField, String name) {
        var data = reader.read(file);
        var type = data.types().stream().filter(value -> value.id() == modelId).findFirst()
                .orElseThrow(() -> invalid("Выберите поддерживаемый тип заметки."));
        if (name == null || name.isBlank() || name.strip().length() > 100) throw invalid("Название подборки: от 1 до 100 символов.");
        var fields = Arrays.asList(englishField, russianField, exampleField, hintField)
                .stream().filter(Objects::nonNull).toList();
        if (fields.stream().anyMatch(index -> index < 0 || index >= type.fields().size())
                || new HashSet<>(fields).size() != fields.size()) {
            throw invalid("Выберите отдельное поле для каждого значения.");
        }
        var user = users.lockById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var existing = words.findByUserIdOrderByIdDesc(userId);
        var byPair = new HashMap<String, Word>();
        long bytes = 0;
        for (var word : existing) {
            byPair.put(key(word.getEnglishText(), word.getRussianText()), word);
            bytes += size(word.getEnglishText(), word.getRussianText(), word.getExample(), word.getRussianHint());
        }
        var collection = new WordCollection();
        collection.setUser(user);
        collection.setName(name.strip());
        collection.setDescription("Импортировано из Anki. Медиа и история повторений не перенесены.");
        int created = 0;
        int reused = 0;
        int skipped = 0;
        var warnings = new ArrayList<String>();
        for (var note : data.notes()) {
            if (note.unsupported() || note.modelId() != modelId || note.fields().size() != type.fields().size()) {
                skipped++;
                warn(warnings, "Заметка " + note.id() + ": другой или неподдерживаемый тип.");
                continue;
            }
            var english = note.fields().get(englishField).toLowerCase(Locale.ROOT);
            var russian = note.fields().get(russianField).toLowerCase(Locale.ROOT);
            var example = exampleField == null ? null : note.fields().get(exampleField);
            var hint = hintField == null ? null : note.fields().get(hintField);
            if (hint != null && hint.isBlank()) hint = null;
            if (english.isBlank() || russian.isBlank() || english.length() > 500 || russian.length() > 500
                    || example != null && example.length() > 1000 || hint != null && hint.length() > 1000) {
                skipped++;
                warn(warnings, "Заметка " + note.id() + ": пустые или слишком длинные поля.");
                continue;
            }
            var pair = key(english, russian);
            var word = byPair.get(pair);
            if (word == null) {
                bytes += size(english, russian, example, hint);
                if (existing.size() + created >= VocabularyQuota.MAX_WORDS || bytes > VocabularyQuota.MAX_TEXT_BYTES) {
                    throw invalid("Лимит словаря: 10 000 слов и 30 МБ текста на пользователя. Импорт отменён целиком.");
                }
                word = new Word();
                word.setUser(user);
                word.setEnglishText(english);
                word.setRussianText(russian);
                word.setExample(example);
                word.setRussianHint(hint);
                word.setSourceLanguage("en");
                words.save(word);
                byPair.put(pair, word);
                created++;
            } else {
                if (word.getRussianHint() == null && hint != null) {
                    bytes += size(hint);
                    if (bytes > VocabularyQuota.MAX_TEXT_BYTES) {
                        throw invalid("Лимит словаря: 30 МБ текста на пользователя. Импорт отменён целиком.");
                    }
                    word.setRussianHint(hint);
                }
                reused++;
            }
            collection.getWords().add(word);
        }
        if (collection.getWords().isEmpty()) throw invalid("Нет подходящих слов для импорта. В архиве должны быть заполнены поля английского слова и русского перевода.");
        collections.saveAndFlush(collection);
        return new AnkiImportResult(collection.getId(), created, reused, skipped, warnings);
    }

    private String key(String english, String russian) {
        return english.toLowerCase(Locale.ROOT) + "\u001f" + russian.toLowerCase(Locale.ROOT);
    }

    private long size(String... fields) {
        long total = 0;
        for (var field : fields) if (field != null) total += field.getBytes(StandardCharsets.UTF_8).length;
        return total;
    }

    private void warn(List<String> warnings, String text) {
        if (warnings.size() < 20) warnings.add(text);
    }

    private ResponseStatusException invalid(String text) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, text);
    }
}
