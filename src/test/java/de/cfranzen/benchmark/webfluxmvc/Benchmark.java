package de.cfranzen.benchmark.webfluxmvc;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static java.util.stream.Collectors.joining;

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
        final BenchmarkResult<Integer, Double> result = benchmark.performMeasurements(PARALLELISM);
        writeResultToCsv("benchmark.csv", result);
    }

    private BenchmarkResult<Integer, Double> performMeasurements(final int parallelism) {
        final ExecutorService executorService = Executors.newFixedThreadPool(parallelism);
        final LongSupplier blockingCall = () -> callBlockingController(10, 1000);
        final LongSupplier reactiveCall = () -> callReactiveController(10, 1000);

        final int[] multipliers = {1, 2, 4, 8, 16, 32, 64, 128};
        final int[] requestCounts = Arrays.stream(multipliers).map(m -> m * parallelism).toArray();

        BenchmarkResult<Integer, Double> result = new BenchmarkResult<>(parallelism, "requests");
        try {
            System.out.println("Blocking:");
            for (int requestCount : requestCounts) {
                final double avgRequestsPerSecond = performMeasurement(executorService, parallelism, requestCount, blockingCall);
                result.addMeasurement("blocking", requestCount, avgRequestsPerSecond);
            }

            System.out.println("Reactive:");
            for (int requestCount : requestCounts) {
                final double avgRequestsPerSecond = performMeasurement(executorService, parallelism, requestCount, reactiveCall);
                result.addMeasurement("reactive", requestCount, avgRequestsPerSecond);
            }
        } finally {
            executorService.shutdown();
        }
        return result;
    }

    private double performMeasurement(final ExecutorService executorService,
                                      final int parallelism,
                                      final int requestCount,
                                      final LongSupplier supplier) {
        final Supplier<Long> measurementTask = () -> {
            long start = System.nanoTime();
            supplier.getAsLong();
            long end = System.nanoTime();
            return end - start;
        };

        final long benchmarkStart = System.nanoTime();

        final CompletableFuture<Long>[] futures = new CompletableFuture[requestCount];
        for (int j = 0; j < requestCount; j++) {
            futures[j] = CompletableFuture.supplyAsync(measurementTask, executorService);
        }
        CompletableFuture.allOf(futures).join();

        final long benchmarkEnd = System.nanoTime();
        final long benchmarkDuration = benchmarkEnd - benchmarkStart;

        final double avgRequestsPerSecond = (double) requestCount / benchmarkDuration * 1e9;
        System.out.println("Measurement: parallelism=" + parallelism + ", requestCount="
                + requestCount + ", avgRequestsPerSecond=" + avgRequestsPerSecond);
        return avgRequestsPerSecond;
    }

    private void performWarmUp() {
        final int iterations = 100;
        performWarmUp(iterations, () -> callBlockingController(10, 1));
        performWarmUp(iterations, () -> callReactiveController(10, 1));
    }

    private void performWarmUp(final int iterations, final LongSupplier supplier) {
        for (int i = 0; i < iterations; i++) {
            supplier.getAsLong();
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
                .mapToInt(Integer::valueOf)
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

    private static void writeResultToCsv(final String filename, final BenchmarkResult<Integer, Double> result) {
        final Path csvFile = Paths.get("src/test/resources", filename);
        try (final BufferedWriter writer = Files.newBufferedWriter(csvFile)) {
            final String rowIndexName = result.getRowIndexName();
            final List<String> columnNames = result.getColumnNames();
            final List<Integer> rowIndex = result.getRowIndex();

            writer.append(rowIndexName).append(";").append(String.join(";", columnNames));

            for (Integer indexValue : rowIndex) {
                writer.newLine();
                writer.append(String.valueOf(indexValue))
                        .append(";")
                        .append(columnNames.stream().map(c -> String.valueOf(result.getValue(c, indexValue)))
                                .collect(joining(";")));
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to write CSV file", e);
        }
    }
}
