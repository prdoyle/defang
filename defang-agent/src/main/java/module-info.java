module defang.agent {
    requires java.instrument;
    requires defang.runtime;
    requires org.objectweb.asm;
    requires org.objectweb.asm.util;
}