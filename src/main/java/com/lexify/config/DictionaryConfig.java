package com.lexify.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class DictionaryConfig {

    @Bean
    RestClient dictionaryRestClient(RestClient.Builder builder, @Value("${app.dictionary.url}") String url) {
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        var factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(5));
        return builder.baseUrl(url).requestFactory(factory).build();
    }
}
