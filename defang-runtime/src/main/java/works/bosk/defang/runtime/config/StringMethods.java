package works.bosk.defang.runtime.config;

import works.bosk.defang.runtime.InstanceMethod;

public class StringMethods {
    @InstanceMethod
    public static void isEmpty(Class<?> callerClass, String str) {
        if (callerClass == null) {
            throw new NullPointerException("No caller class??");
        }
    }
}
