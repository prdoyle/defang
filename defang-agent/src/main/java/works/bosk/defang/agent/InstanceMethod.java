package works.bosk.defang.agent;

import works.bosk.defang.api.Entitlement;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates that a non-static method should be instrumented to check
 * for the given {@link Entitlement}.
 * The annotated method should mimic the actual target method to be instrumented,
 * with the same name and return type, and the same parameters plus an additional
 * parameter at the start whose type is the owning class of the target method.
 */
@Target({METHOD, TYPE})
@Retention(RUNTIME)
public @interface InstanceMethod {
    Entitlement value();
}
