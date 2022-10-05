package de.cfranzen.benchmark.webfluxmvc.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.nio.charset.StandardCharsets;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReactiveControllerTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    void receiveRandomNumbers() {
        var response = webClient
                .get()
                .uri(builder -> builder
                        .path("/reactive")
                        .queryParam("numberCount", 10)
                        .queryParam("delayMillis", 100)
                        .build())
                .exchange();

        response.expectStatus().isOk();
        response.expectBody().consumeWith(payload -> {
            String content = new String(requireNonNull(payload.getResponseBodyContent()), StandardCharsets.UTF_8);
            assertThat(content)
                    .isEqualToIgnoringNewLines("""
                            -1157793070
                            1913984760
                            1107254586
                            1773446580
                            254270492
                            -1408064384
                            1048475594
                            1581279777
                            -778209333
                            1532292428""");
        });
    }
}