package works.bosk.defang.agent;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import works.bosk.defang.api.Entitlement;
import works.bosk.defang.runtime.EntitlementChecking;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Map;

import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public class Transformer implements ClassFileTransformer {
    private final Map<MethodKey, Entitlement> entitlements;

    public Transformer(Map<MethodKey, Entitlement> entitlements) {
        // TODO: Should warn if any MethodKey doesn't match any methods
        this.entitlements = entitlements;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new EntitlementClassVisitor(Opcodes.ASM9, writer, className);
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    class EntitlementClassVisitor extends ClassVisitor {
        final String className;

        EntitlementClassVisitor(int api, ClassVisitor classVisitor, String className) {
            super(api, classVisitor);
            this.className = className;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            MethodKey key = new MethodKey(className, name, descriptor);
            Entitlement requirement = entitlements.get(key);
            if (requirement != null) {
                return new EntitlementMethodVisitor(Opcodes.ASM9, mv, requirement);
            }
            return mv;
        }
    }

    static class EntitlementMethodVisitor extends MethodVisitor {
        private final Entitlement requirement;
        private boolean hasCallerSensitiveAnnotation = false;

        EntitlementMethodVisitor(int api, MethodVisitor methodVisitor, Entitlement requirement) {
            super(api, methodVisitor);
            this.requirement = requirement;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (visible && descriptor.endsWith("CallerSensitive;")) {
                hasCallerSensitiveAnnotation = true;
            }
            return super.visitAnnotation(descriptor, visible);
        }

        @Override
        public void visitCode() {
            pushEntitlement();
            pushCallerClass();
            invokeCheckEntitlement();
            super.visitCode();
        }

        private void pushEntitlement() {
            mv.visitFieldInsn(
                    GETSTATIC,
                    Type.getInternalName(Entitlement.class),
                    requirement.toString(),
                    Type.getDescriptor(Entitlement.class));
        }

        private void pushCallerClass() {
            if (hasCallerSensitiveAnnotation) {
                mv.visitMethodInsn(INVOKESTATIC, "jdk/internal/reflect/Reflection", "getCallerClass", "()Ljava/lang/Class;", false);
            } else {
                mv.visitFieldInsn(GETSTATIC, "java/lang/StackWalker$Option", "RETAIN_CLASS_REFERENCE", "Ljava/lang/StackWalker$Option;");
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/StackWalker", "getInstance", "(Ljava/lang/StackWalker$Option;)Ljava/lang/StackWalker;", false);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StackWalker", "getCallerClass", "()Ljava/lang/Class;", false);
            }
        }

        private void invokeCheckEntitlement() {
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    Type.getInternalName(CHECK_ENTITLEMENT.getDeclaringClass()),
                    CHECK_ENTITLEMENT.getName(),
                    Type.getMethodDescriptor(CHECK_ENTITLEMENT),
                    false);
        }
    }

    private static final Method CHECK_ENTITLEMENT;

    static {
        try {
            CHECK_ENTITLEMENT = EntitlementChecking.class.getDeclaredMethod("checkEntitlement", Entitlement.class, Class.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
