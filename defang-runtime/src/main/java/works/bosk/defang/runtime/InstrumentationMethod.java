package works.bosk.defang.runtime;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a method that contains additional logic that should be called before a particular
 * instrumented method begins.
 * The marked method itself should be static, and should have the same name and signature as the
 * target instrumented method, with the following exceptions:
 * <ul>
 *     <li>
 *         The method should return {@code void}
 *     </li>
 *     <li>
 *         The first argument's type should be the target method's declaring class.
 *     </li>
 * </ul>
 * For a non-static method, the first argument will be the receiver object at runtime;
 * for a static method, the first argument will be null.
 *
 * @see #className()
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface InstrumentationMethod {
    /**
     * For classes that are not accessible from the {@code config} classes,
     * specify the name here, in the format returned by {@link Class#getName()}.
     * In that case, the first parameter of the instrumentation method should
     * be a superclass; {@code Object} is often a good choice.
     */
    String className() default "";

    boolean isStatic() default false;
}
