package com.lexify.service;

import com.github.luben.zstd.ZstdInputStream;
import com.lexify.dto.AnkiPreview;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.zip.ZipFile;
import org.jsoup.Jsoup;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class AnkiPackageReader {
    public static final long MAX_FILE = 100L * 1024 * 1024;
    private static final long MAX_DATABASE = 100L * 1024 * 1024;
    private static final int MAX_NOTES = 10000;
    private final JsonMapper json = JsonMapper.builder().build();

    public record Note(long id, long modelId, List<String> fields, boolean unsupported) {}
    public record PackageData(List<AnkiPreview.NoteType> types, List<Note> notes) {}

    public PackageData read(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > MAX_FILE || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".apkg")) {
            throw invalid("Загрузите файл .apkg размером до 100 МБ.");
        }
        Path archive = null;
        Path database = null;
        try {
            archive = Files.createTempFile("lexify-anki-", ".zip");
            database = Files.createTempFile("lexify-anki-", ".db");
            try (var input = file.getInputStream(); var output = Files.newOutputStream(archive)) {
                copyLimited(input, output, MAX_FILE);
            }
            try (var zip = new ZipFile(archive.toFile())) {
                var name = List.of("collection.anki21b", "collection.anki21", "collection.anki2").stream()
                        .filter(value -> zip.getEntry(value) != null).findFirst()
                        .orElseThrow(() -> invalid("В архиве не найдена база Anki."));
                try (var raw = zip.getInputStream(zip.getEntry(name));
                     var input = name.endsWith("21b") ? new ZstdInputStream(raw) : raw;
                     var output = Files.newOutputStream(database)) {
                    copyLimited(input, output, MAX_DATABASE);
                }
            }
            try (var connection = DriverManager.getConnection("jdbc:sqlite:file:" + database + "?mode=ro")) {
                try (var statement = connection.createStatement()) {
                    statement.execute("pragma query_only=on");
                    statement.execute("pragma trusted_schema=off");
                }
                var types = types(connection);
                var notes = new ArrayList<Note>();
                try (var statement = connection.createStatement()) {
                    statement.setQueryTimeout(5);
                    try (var rows = statement.executeQuery("select id, mid, substr(flds,1,20001) from notes order by id limit 10001")) {
                        while (rows.next()) {
                            if (notes.size() >= MAX_NOTES) throw invalid("В одном архиве допускается не более 10 000 заметок.");
                            var text = rows.getString(3);
                            var modelId = rows.getLong(2);
                            var unsupported = text == null || text.length() > 20000 || text.contains("{{c")
                                    || !types.containsKey(modelId) || types.get(modelId).fields().size() < 2;
                            var fields = unsupported ? List.<String>of() : Arrays.stream(text.split("\u001f", -1))
                                    .map(this::plainText).toList();
                            notes.add(new Note(rows.getLong(1), modelId, fields, unsupported));
                        }
                    }
                }
                var preview = new ArrayList<AnkiPreview.NoteType>();
                for (var type : types.values()) {
                    var matching = notes.stream().filter(n -> n.modelId() == type.id() && !n.unsupported()).toList();
                    if (!matching.isEmpty()) preview.add(new AnkiPreview.NoteType(type.id(), type.name(), type.fields(),
                            matching.size(), matching.stream().limit(3).map(Note::fields).toList()));
                }
                return new PackageData(preview, notes);
            }
        } catch (IOException | SQLException | tools.jackson.core.JacksonException exception) {
            throw invalid("Не удалось прочитать архив Anki. Проверьте, что это корректный .apkg.");
        } finally {
            delete(archive);
            delete(database);
        }
    }

    private Map<Long, AnkiPreview.NoteType> types(Connection connection) throws SQLException {
        var result = new LinkedHashMap<Long, AnkiPreview.NoteType>();
        boolean modern;
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("select count(*) from sqlite_master where type='table' and name='notetypes'")) {
            modern = rows.next() && rows.getInt(1) > 0;
        }
        if (modern) {
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("select id,name,config from notetypes limit 101")) {
                while (rows.next()) {
                    if (result.size() >= 100) throw invalid("В архиве слишком много типов заметок.");
                    var config = rows.getBytes(3);
                    if (config != null && config.length >= 2 && config[0] == 8 && config[1] == 1) continue;
                    var id = rows.getLong(1);
                    var fields = new ArrayList<String>();
                    try (var query = connection.prepareStatement("select name from fields where ntid=? order by ord limit 101")) {
                        query.setLong(1, id);
                        try (var fieldRows = query.executeQuery()) {
                            while (fieldRows.next()) fields.add(fieldRows.getString(1));
                        }
                    }
                    if (fields.size() > 100) throw invalid("В типе заметки слишком много полей.");
                    result.put(id, new AnkiPreview.NoteType(id, rows.getString(2), fields, 0, List.of()));
                }
            }
        } else {
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("select substr(models,1,1000001) from col limit 1")) {
                if (!rows.next()) throw invalid("В архиве отсутствуют типы заметок.");
                var text = rows.getString(1);
                if (text == null || text.length() > 1000000) throw invalid("Описание типов заметок слишком большое.");
                var models = json.readTree(text);
                for (var model : models) {
                    if (model.path("type").asInt() != 0) continue;
                    if (result.size() >= 100) throw invalid("В архиве слишком много типов заметок.");
                    var fields = new TreeMap<Integer, String>();
                    for (var field : model.path("flds")) fields.put(field.path("ord").asInt(), field.path("name").asText());
                    if (fields.size() > 100) throw invalid("В типе заметки слишком много полей.");
                    var id = model.path("id").asLong();
                    result.put(id, new AnkiPreview.NoteType(id, model.path("name").asText(),
                            List.copyOf(fields.values()), 0, List.of()));
                }
            }
        }
        return result;
    }

    private String plainText(String value) {
        return Jsoup.parse(value.replaceAll("(?i)\\[sound:[^\\]]*\\]", " ")).text().strip();
    }

    private void copyLimited(InputStream input, OutputStream output, long maximum) throws IOException {
        var buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > maximum) throw invalid("Превышен лимит размера архива или распакованной базы (100 МБ).");
            output.write(buffer, 0, read);
        }
    }

    private void delete(Path path) {
        if (path != null) {
            try { Files.deleteIfExists(path); }
            catch (IOException exception) { path.toFile().deleteOnExit(); }
        }
    }

    private ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
