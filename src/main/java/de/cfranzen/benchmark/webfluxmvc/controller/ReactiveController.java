package de.cfranzen.benchmark.webfluxmvc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Random;

@RestController
@RequestMapping("/reactive")
public class ReactiveController {

    @GetMapping
    public Flux<String> generateRandomNumbers(@RequestParam int numberCount, @RequestParam int delayMillis) {
        Random rand = new Random(numberCount);
        return Flux.concat(
                Mono.delay(Duration.ofMillis(delayMillis))
                        .thenReturn(nextElement(rand)),
                Flux.create((FluxSink<String> sink) -> {
                    for (int i = 1; i < numberCount; i++) {
                        sink.next(nextElement(rand));
                    }
                    sink.complete();
                }));
    }

    private String nextElement(final Random rand) {
        return rand.nextInt() + "\n";
    }
}
