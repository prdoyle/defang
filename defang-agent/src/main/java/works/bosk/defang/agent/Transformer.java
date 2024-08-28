package works.bosk.defang.agent;

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

public class Transformer implements ClassFileTransformer {
    private final Map<MethodKey, Entitlement> entitlements;

    public Transformer(Map<MethodKey, Entitlement> entitlements) {
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

        EntitlementMethodVisitor(int api, MethodVisitor methodVisitor, Entitlement requirement) {
            super(api, methodVisitor);
            this.requirement = requirement;
        }

        @Override
        public void visitCode() {
            mv.visitFieldInsn(
                    Opcodes.GETSTATIC,
                    Type.getInternalName(Entitlement.class),
                    requirement.toString(),
                    Type.getDescriptor(Entitlement.class));

            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    Type.getInternalName(CHECK_ENTITLEMENT.getDeclaringClass()),
                    CHECK_ENTITLEMENT.getName(),
                    Type.getMethodDescriptor(CHECK_ENTITLEMENT),
                    false);

            super.visitCode();
        }
    }

    private static final Method CHECK_ENTITLEMENT;

    static {
        try {
            CHECK_ENTITLEMENT = EntitlementChecking.class.getDeclaredMethod("checkEntitlement", Entitlement.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
