package de.cfranzen.benchmark.webfluxmvc.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Random;

@RestController
@RequestMapping("/blocking")
public class BlockingController {

    @GetMapping
    public StreamingResponseBody generateRandomNumbers(@RequestParam int numberCount, @RequestParam int delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted while simulating delay of " + delayMillis + " millis", e);
        }

        Random rand = new Random(numberCount);
        return outputStream -> {
            OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            for (int i = 0; i < numberCount; i++) {
                writer.append(String.valueOf(rand.nextInt())).append('\n');
            }
            writer.flush();
        };
    }
}
