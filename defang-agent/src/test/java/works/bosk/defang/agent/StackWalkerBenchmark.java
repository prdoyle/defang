package works.bosk.defang.agent;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class StackWalkerBenchmark {

    public static final StackWalker WALKER = StackWalker.getInstance(RETAIN_CLASS_REFERENCE);
    public static final StackWalker WALKER_2_FRAMES = StackWalker.getInstance(EnumSet.of(RETAIN_CLASS_REFERENCE), 2);

    @Benchmark
    public void baseline() {

    }

    @Benchmark
    public Class<?> getCallerClass() {
        return WALKER.getCallerClass();
    }

    @Benchmark
    public Class<?> getCallerClass2Frames() {
        return WALKER_2_FRAMES.getCallerClass();
    }

    @Benchmark
    public Class<?> inlinedGetCallerClass(Blackhole blackhole) {
        for (int i = 0; i < 999; i++) {
            blackhole.consume(methodThatCallsGetCallerClass());
        }
        return methodThatCallsGetCallerClass();
    }

    private static Class<?> methodThatCallsGetCallerClass() {
        return WALKER.getCallerClass();
    }

    @Benchmark
    public Class<?> walkOneStackFrame() {
        return WALKER.walk(s -> s.skip(1).findFirst().get()).getClass();
    }

    @Benchmark
    public Class<?> walkAllStackFrames() {
        List<StackWalker.StackFrame> frames = WALKER.walk(Stream::toList);
        return frames.get(1).getClass();
    }

    @Benchmark
    public Class<?> createException() {
        Exception e = new Exception();
        return e.getStackTrace()[1].getClass();
    }
}
