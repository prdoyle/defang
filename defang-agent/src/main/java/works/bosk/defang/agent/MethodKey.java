package works.bosk.defang.agent;

public record MethodKey(
        String className,
        String methodName,
        String descriptor
) {
}
