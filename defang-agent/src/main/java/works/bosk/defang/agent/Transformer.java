package works.bosk.defang.agent;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public class Transformer implements ClassFileTransformer {
    private final Set<String> classesToTransform;
    private final Map<MethodKey, Method> instrumentationMethods;

    public Transformer(Set<String> classesToTransform, Map<MethodKey, Method> instrumentationMethods) {
        this.classesToTransform = classesToTransform;
        // TODO: Should warn if any MethodKey doesn't match any methods
        this.instrumentationMethods = instrumentationMethods;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (classBeingRedefined != null) {
            System.out.println("Hey we're redefining " + classBeingRedefined);
        }
        if (!classesToTransform.contains(className)) {
            return classfileBuffer;
        }
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
            var mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            var voidDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getArgumentTypes(descriptor));
            var key = new MethodKey(className, name, voidDescriptor);
            var instrumentationMethod = instrumentationMethods.get(key);
            if (instrumentationMethod != null) {
                System.out.println("Matched key: " + key);
                return new EntitlementMethodVisitor(Opcodes.ASM9, mv, descriptor, instrumentationMethod);
            } else {
//                System.out.println("No match for key: " + key);
            }
            return mv;
        }
    }

    static class EntitlementMethodVisitor extends MethodVisitor {
        private final String instrumentedMethodDescriptor;
        private final Method instrumentationMethod;
        private boolean hasCallerSensitiveAnnotation = false;

        EntitlementMethodVisitor(int api, MethodVisitor methodVisitor, String instrumentedMethodDescriptor, Method instrumentationMethod) {
            super(api, methodVisitor);
            this.instrumentedMethodDescriptor = instrumentedMethodDescriptor;
            this.instrumentationMethod = instrumentationMethod;
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
            pushCallerClass();
            forwardIncomingArguments(false);
            invokeInstrumentationMethod();
            super.visitCode();
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

        private void forwardIncomingArguments(boolean isStatic) {
            int localVarIndex = isStatic ? 1 : 0;
            for (Type type : Type.getArgumentTypes(instrumentedMethodDescriptor)) {
                mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), localVarIndex);
                localVarIndex += type.getSize();
            }

        }

        private void invokeInstrumentationMethod() {
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    Type.getInternalName(instrumentationMethod.getDeclaringClass()),
                    instrumentationMethod.getName(),
                    Type.getMethodDescriptor(instrumentationMethod),
                    false);
        }
    }

}
