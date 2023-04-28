# Spring Webflux vs. MVC Benchmark
Performance benchmark comparing Spring Webflux with Spring Mvc

The following figure shows the throughput in requests per second when triggering 
the total amount of requests with 256 threads in a blocking or reactive way. Each
request takes one second on the server side to get processed, so the maximum throughput
can be 256 requests per second. The reactive benchmark results come quite close to that value.
At least they show up to 25% higher throughput compared to the blocking benchmark results.

![Figure showing Throughput over Total Requests](src/test/resources/benchmark.svg)