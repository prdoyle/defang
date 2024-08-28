
package works.bosk.defang.runtime;

public record MethodKey(
	String className,
	String methodName,
	String descriptor
) { }
