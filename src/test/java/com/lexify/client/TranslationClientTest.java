package com.lexify.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class TranslationClientTest {

    private MockRestServiceServer server;
    private TranslationClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("http://translation");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TranslationClient(builder.build());
    }

    @Test
    void sendsRussianToEnglishTranslationRequest() {
        server.expect(requestTo("http://translation/translate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"q":"привет","source":"ru","target":"en","format":"text"}
                        """))
                .andRespond(withSuccess("""
                        {"translatedText":"hello"}
                        """, MediaType.APPLICATION_JSON));
        assertThat(client.translate("привет", "ru", "en")).isEqualTo("hello");
        server.verify();
    }

    @Test
    void mapsUnavailableServiceToBadGateway() {
        server.expect(requestTo("http://translation/translate")).andRespond(withServerError());
        assertBadGateway();
    }

    @Test
    void rejectsEmptyTranslation() {
        server.expect(requestTo("http://translation/translate"))
                .andRespond(withSuccess("""
                        {"translatedText":" "}
                        """, MediaType.APPLICATION_JSON));
        assertBadGateway();
    }

    @Test
    void rejectsMalformedResponse() {
        server.expect(requestTo("http://translation/translate"))
                .andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));
        assertBadGateway();
    }

    @Test
    void sendsEnglishToRussianTranslationRequest() {
        server.expect(requestTo("http://translation/translate"))
                .andExpect(content().json("""
                        {"q":"hello","source":"en","target":"ru","format":"text"}
                        """))
                .andRespond(withSuccess("""
                        {"translatedText":"привет"}
                        """, MediaType.APPLICATION_JSON));
        assertThat(client.translate("hello", "en", "ru")).isEqualTo("привет");
        server.verify();
    }

    private void assertBadGateway() {
        assertThatThrownBy(() -> client.translate("привет", "ru", "en"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(502));
        server.verify();
    }
}
