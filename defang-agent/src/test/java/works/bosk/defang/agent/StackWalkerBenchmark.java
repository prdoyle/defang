package works.bosk.defang.agent;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;

public class StackWalkerBenchmark {

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
    @Fork(1)
    public Class<?> benchmarkGetCallerClass() {
        return StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass();  // Adjust the stack depth as needed
    }
}