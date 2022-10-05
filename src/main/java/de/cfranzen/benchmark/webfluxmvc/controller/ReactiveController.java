package de.cfranzen.benchmark.webfluxmvc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.util.Random;

@RestController
@RequestMapping("/reactive")
public class ReactiveController {

    @GetMapping
    public Flux<String> generateRandomNumbers(@RequestParam int numberCount, @RequestParam int delayMillis) {
        return Flux.create((FluxSink<String> sink) -> {
                    Random rand = new Random(numberCount);
                    for (int i = 0; i < numberCount; i++) {
                        sink.next(rand.nextInt() + "\n");
                    }
                    sink.complete();
                })
                .delayElements(Duration.ofMillis(delayMillis));
    }
}
