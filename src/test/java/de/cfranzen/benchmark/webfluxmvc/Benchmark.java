package de.cfranzen.benchmark.webfluxmvc;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

class Benchmark {

    private static final int PARALLELISM = 256;

    private final HttpClient httpClient = HttpClient.create(
            ConnectionProvider.builder("myConnectionProvider")
                    .maxConnections(PARALLELISM)
                    .pendingAcquireTimeout(Duration.ofSeconds(1))
                    .build());

    private final WebClient client = WebClient
            .builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .baseUrl("http://spring-webflux-mvc-benchmark.fly.dev/")
            .build();

    public static void main(final String[] args) {
        Benchmark benchmark = new Benchmark();
        benchmark.performWarmUp();
        benchmark.performMeasurements(PARALLELISM, 1);
    }

    private void performMeasurements(final int parallelism, final int measurementIterations) {
        final ExecutorService executorService = Executors.newFixedThreadPool(parallelism);
        final LongSupplier blockingCall = () -> callBlockingController(10, 1000);
        final LongSupplier reactiveCall = () -> callReactiveController(10, 1000);
        final int[] requestCounts = {parallelism, 2 * parallelism, 4 * parallelism, 8 * parallelism,
                16 * parallelism, 32 * parallelism};
        try {
            System.out.println("Blocking:");
            for (int requestCount : requestCounts) {
                performMeasurement(executorService, parallelism, measurementIterations, requestCount, blockingCall);
            }

            System.out.println("Reactive:");
            for (int requestCount : requestCounts) {
                performMeasurement(executorService, parallelism, measurementIterations, requestCount, reactiveCall);
            }
        } finally {
            executorService.shutdown();
        }
    }

    private void performMeasurement(final ExecutorService executorService,
                                    final int parallelism,
                                    final int measurementIterations,
                                    final int requestCount,
                                    final LongSupplier supplier) {
        final Supplier<Long> measurementTask = () -> {
            long start = System.nanoTime();
            supplier.getAsLong();
            long end = System.nanoTime();
            return end - start;
        };

        final long benchmarkStart = System.nanoTime();
        final long[] requestTimeSums = new long[measurementIterations];
        for (int i = 0; i < measurementIterations; i++) {
            final CompletableFuture<Long>[] futures = new CompletableFuture[requestCount];
            for (int j = 0; j < requestCount; j++) {
                futures[j] = CompletableFuture.supplyAsync(measurementTask, executorService);
            }
            CompletableFuture.allOf(futures).join();
            requestTimeSums[i] = Arrays.stream(futures).mapToLong(f -> f.join()).sum();
        }
        final long benchmarkEnd = System.nanoTime();
        final long benchmarkDuration = benchmarkEnd - benchmarkStart;

        final double avgRequestTimeSum = Arrays.stream(requestTimeSums).average().orElse(0.0);
        final double avgRequestsPerSecond = (double) requestCount / benchmarkDuration * 1e9;
        System.out.println("Measurement: parallelism=" + parallelism + ", requestCount="
                + requestCount + ", avgRequestsPerSecond=" + avgRequestsPerSecond);
    }

    private void performWarmUp() {
        final int iterations = 100;
        performWarmUp(iterations, () -> callBlockingController(10, 1));
        performWarmUp(iterations, () -> callReactiveController(10, 1));
    }

    private void performWarmUp(final int iterations, final LongSupplier supplier) {
        final List<Long> durations = new ArrayList<>(iterations);
        for (int i = 0; i < iterations; i++) {
            durations.add(supplier.getAsLong());
        }
    }

    private long callBlockingController(final int numberCount, final int delayMillis) {
        return callController("/blocking", numberCount, delayMillis);
    }

    private long callReactiveController(final int numberCount, final int delayMillis) {
        return callController("/reactive", numberCount, delayMillis);
    }

    private long callController(final String path, final int numberCount, final int delayMillis) {
        long start = System.nanoTime();
        final var result = Arrays.stream(client.get().uri(uriBuilder -> uriBuilder
                                .path(path)
                                .queryParam("numberCount", numberCount)
                                .queryParam("delayMillis", delayMillis)
                                .build()).retrieve()
                        .bodyToMono(String.class)
                        .block()
                        .split("\n"))
                .mapToInt(s -> Integer.valueOf(s).intValue())
                .toArray();
        long end = System.nanoTime();
        long duration = TimeUnit.NANOSECONDS.toMillis(end - start);

        if (duration < delayMillis) {
            throw new IllegalStateException("Request has been to fast (" + duration + "ms). " +
                    "Expecting at least " + delayMillis + "ms");
        }
        if (result.length != numberCount) {
            throw new IllegalStateException("Expecting exactly " + numberCount + " elements in " +
                    "result but found " + result.length);
        }
        return duration;
    }
}
