package works.bosk.defang.runtime;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Same as {@link InstanceMethod} except for static methods.
 * The requirements on the instrumentation method's parameters are the same;
 * the value of the "receiver object" argument will be null.
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface StaticMethod {
}
