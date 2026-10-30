package com.lexify.client;

import com.lexify.dto.DictionaryData;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class DictionaryClientTest {

    private MockRestServiceServer server;
    private DictionaryClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("http://dictionary");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DictionaryClient(builder.build());
    }

    @Test
    void extractsExamplePhoneticMatchingAudioAndAttribution() {
        server.expect(requestTo("http://dictionary/api/v2/entries/en/hello"))
                .andRespond(withSuccess("""
                        [{
                          "word":"hello",
                          "phonetics":[
                            {"audio":"https://example.com/au.mp3"},
                            {"text":"/həˈləʊ/","audio":"//example.com/uk.mp3",
                             "sourceUrl":"https://commons.wikimedia.org/wiki/Audio",
                             "license":{"name":"CC BY","url":"https://creativecommons.org/licenses/by/4.0/"}}
                          ],
                          "meanings":[{"partOfSpeech":"noun","definitions":[{"definition":"A greeting"}]},
                                      {"definitions":[{"example":"Hello, everyone!"}]}],
                          "license":{"name":"CC BY-SA","url":"https://creativecommons.org/licenses/by-sa/4.0/"},
                          "sourceUrls":["https://en.wiktionary.org/wiki/hello"]
                        }]
                        """, MediaType.APPLICATION_JSON));

        var result = client.lookup("hello").orElseThrow();

        assertThat(result.example()).isEqualTo("Hello, everyone!");
        assertThat(result.phonetic()).isEqualTo("/həˈləʊ/");
        assertThat(result.audioUrl()).isEqualTo("https://example.com/uk.mp3");
        assertThat(result.sourceUrl()).isEqualTo("https://en.wiktionary.org/wiki/hello");
        assertThat(result.audioLicenseUrl()).isEqualTo("https://creativecommons.org/licenses/by/4.0/");
        server.verify();
    }

    @Test
    void missingWordIsAValidEmptyResult() {
        server.expect(requestTo("http://dictionary/api/v2/entries/en/unknown"))
                .andRespond(withResourceNotFound());
        assertThat(client.lookup("unknown")).contains(DictionaryData.empty());
        server.verify();
    }

    @Test
    void toleratesMissingOptionalFieldsAndNullArrayItems() {
        server.expect(requestTo("http://dictionary/api/v2/entries/en/hello"))
                .andRespond(withSuccess("""
                        [null,{"phonetics":[null,{}],"meanings":[null,{"definitions":[null,{}]}]}]
                        """, MediaType.APPLICATION_JSON));
        assertThat(client.lookup("hello")).contains(DictionaryData.empty());
        server.verify();
    }

    @Test
    void rejectsUnsafeMediaAndSourceUrls() {
        server.expect(requestTo("http://dictionary/api/v2/entries/en/hello"))
                .andRespond(withSuccess("""
                        [{"phonetic":"/hello/","phonetics":[{"audio":"javascript:alert(1)"}],
                          "sourceUrls":["http://example.com/word"],"license":{"url":"data:text/html,test"}}]
                        """, MediaType.APPLICATION_JSON));
        var result = client.lookup("hello").orElseThrow();
        assertThat(result.phonetic()).isEqualTo("/hello/");
        assertThat(result.audioUrl()).isNull();
        assertThat(result.sourceUrl()).isNull();
        assertThat(result.licenseUrl()).isNull();
        server.verify();
    }

    @Test
    void rateLimitPausesFurtherRequests() {
        server.expect(requestTo("http://dictionary/api/v2/entries/en/hello"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)
                        .header("Retry-After", "120"));
        assertThat(client.lookup("hello")).isEmpty();
        assertThat(client.lookup("world")).isEmpty();
        server.verify();
    }

    @Test
    void serverFailurePausesFurtherRequests() {
        server.expect(requestTo("http://dictionary/api/v2/entries/en/hello"))
                .andRespond(withServerError());
        assertThat(client.lookup("hello")).isEmpty();
        assertThat(client.lookup("world")).isEmpty();
        server.verify();
    }

    @Test
    void timeoutIsOptionalEnrichmentFailure() {
        server.expect(requestTo("http://dictionary/api/v2/entries/en/hello"))
                .andRespond(withException(new SocketTimeoutException("Timeout")));
        assertThat(client.lookup("hello")).isEmpty();
        server.verify();
    }

    @Test
    void malformedJsonDoesNotEscapeClient() {
        server.expect(requestTo("http://dictionary/api/v2/entries/en/hello"))
                .andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));
        assertThat(client.lookup("hello")).isEmpty();
        server.verify();
    }
}
