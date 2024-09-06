package works.bosk.defang.runtime;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a method that contains additional logic that should be called before a particular
 * non-static instrumented method begins.
 * The marked method should be static, and should have the same name and signature as the
 * instrumented method, with the following exceptions:
 * <ul>
 *     <li>
 *         The method should return {@code void}
 *     </li>
 *     <li>
 *         The first argument should be {@code Class<?> callingClass}
 *     </li>
 *     <li>
 *         The second argument's type should be the target method's declaring class;
 *         this is the receiver object of the nonstatic call.
 *     </li>
 * </ul>
 * The annotated method should mimic the actual target method to be instrumented,
 * with the same name and return type, and the same parameters plus an additional
 * parameter at the start whose type is the owning class of the target method.
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface InstanceMethod {
}
