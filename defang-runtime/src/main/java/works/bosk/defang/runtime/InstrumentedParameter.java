package works.bosk.defang.runtime;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(PARAMETER)
@Retention(RUNTIME)
public @interface InstrumentedParameter {
    /**
     * For classes that are not accessible from the {@code config} classes,
     * specify the name here, in the format returned by {@link Class#getName()}.
     * In that case, the first parameter of the instrumentation method should
     * be a superclass; {@code Object} is often a good choice.
     */
    String className() default "";
}
