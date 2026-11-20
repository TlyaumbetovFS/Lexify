package com.lexify.service;

import com.github.luben.zstd.ZstdOutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.sql.DriverManager;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;

class AnkiPackageReaderTest {
    private final AnkiPackageReader reader = new AnkiPackageReader();

    @Test
    void readsLegacyHtmlAndDropsMediaAndCloze() throws Exception {
        var result = reader.read(fixture(false));
        assertThat(result.types()).hasSize(1);
        assertThat(result.types().getFirst().fields()).containsExactly("English", "Russian", "Example");
        assertThat(result.types().getFirst().notes()).isEqualTo(1);
        assertThat(result.notes().getFirst().fields()).containsExactly("hello", "привет", "Hello there");
        assertThat(result.notes().get(1).unsupported()).isTrue();
    }

    @Test
    void readsModernZstdAndNormalizedFields() throws Exception {
        var result = reader.read(fixture(true));
        assertThat(result.types()).hasSize(1);
        assertThat(result.notes().getFirst().fields()).containsExactly("hello", "привет", "Hello there");
    }

    @Test
    void rejectsInvalidFileAndMissingDatabase() throws Exception {
        assertThatThrownBy(() -> reader.read(new MockMultipartFile("file", "words.txt", "text/plain", new byte[]{1})))
                .isInstanceOf(ResponseStatusException.class);
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("../../outside"));
            zip.write(1);
        }
        assertThatThrownBy(() -> reader.read(new MockMultipartFile("file", "words.apkg", "application/zip", bytes.toByteArray())))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("не найдена база");
    }

    @Test
    void rejectsOversizedUploadBeforeParsing() {
        var file = new MockMultipartFile("file", "words.apkg", "application/zip", new byte[1]) {
            @Override public long getSize() { return AnkiPackageReader.MAX_FILE + 1; }
        };
        assertThatThrownBy(() -> reader.read(file)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("100 МБ");
    }

    @Test
    void rejectsDatabaseThatExpandsBeyondLimit() throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("collection.anki2"));
            var chunk = new byte[1024 * 1024];
            for (int i = 0; i < 101; i++) zip.write(chunk);
        }
        assertThatThrownBy(() -> reader.read(new MockMultipartFile("file", "bomb.apkg", "application/zip", bytes.toByteArray())))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("100 МБ");
    }

    private MockMultipartFile fixture(boolean modern) throws Exception {
        var database = Files.createTempFile("anki-fixture", ".db");
        try {
            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                 var sql = connection.createStatement()) {
                sql.execute("create table notes (id integer, mid integer, flds text)");
                if (modern) {
                    sql.execute("create table notetypes(id integer, name text, config blob)");
                    sql.execute("insert into notetypes values (1,'Basic',X''),(2,'Cloze',X'0801')");
                    sql.execute("create table fields(ntid integer, ord integer, name text)");
                    sql.execute("insert into fields values (1,0,'English'),(1,1,'Russian'),(1,2,'Example')");
                } else {
                    sql.execute("create table col(models text)");
                    sql.execute("""
                            insert into col values ('{"1":{"id":1,"name":"Basic","type":0,"flds":[{"ord":0,"name":"English"},{"ord":1,"name":"Russian"},{"ord":2,"name":"Example"}]},"2":{"id":2,"name":"Cloze","type":1,"flds":[]}}')
                            """);
                }
                try (var query = connection.prepareStatement("insert into notes values (?,?,?)")) {
                    query.setLong(1, 1);
                    query.setLong(2, 1);
                    query.setString(3, "<b>hello</b>[sound:hello.mp3]\u001fпривет<img src='a.jpg'>\u001fHello there");
                    query.executeUpdate();
                    query.setLong(1, 2);
                    query.setLong(2, 2);
                    query.setString(3, "{{c1::cloze}}");
                    query.executeUpdate();
                }
            }
            var bytes = new ByteArrayOutputStream();
            try (var zip = new ZipOutputStream(bytes)) {
                zip.putNextEntry(new ZipEntry(modern ? "collection.anki21b" : "collection.anki2"));
                if (modern) {
                    var compressed = new ByteArrayOutputStream();
                    try (var zstd = new ZstdOutputStream(compressed)) { zstd.write(Files.readAllBytes(database)); }
                    zip.write(compressed.toByteArray());
                } else zip.write(Files.readAllBytes(database));
            }
            return new MockMultipartFile("file", "words.apkg", "application/zip", bytes.toByteArray());
        } finally { Files.deleteIfExists(database); }
    }
}
