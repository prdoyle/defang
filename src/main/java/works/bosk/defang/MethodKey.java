
package works.bosk.defang;

public record MethodKey(
	String className,
	String methodName,
	String descriptor
) { }
