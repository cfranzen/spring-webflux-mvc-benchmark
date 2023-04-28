package de.cfranzen.benchmark.webfluxmvc;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class BenchmarkResult<R, V> {
    private final int parallelism;

    private final String rowIndexName;

    private final Set<String> columnNames = new HashSet<>();

    private final Map<R, Map<String, V>> values = new HashMap<>();

    public BenchmarkResult(final int parallelism, final String rowIndexName) {
        this.parallelism = parallelism;
        this.rowIndexName = rowIndexName;
    }

    public int getParallelism() {
        return parallelism;
    }

    public void addMeasurement(final String name, final R indexValue, final V value) {
        columnNames.add(name);
        values.computeIfAbsent(indexValue, k -> new HashMap<>()).put(name, value);
    }

    public V getValue(final String columnName, final R indexValue) {
        return values.getOrDefault(indexValue, Collections.emptyMap()).getOrDefault(columnName, null);
    }

    public String getRowIndexName() {
        return rowIndexName;
    }

    public List<String> getColumnNames() {
        return columnNames.stream().sorted().collect(toList());
    }

    public List<R> getRowIndex() {
        return values.keySet().stream().sorted().toList();
    }
}
