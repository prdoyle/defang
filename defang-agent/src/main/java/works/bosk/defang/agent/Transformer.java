package works.bosk.defang.agent;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * This is to avoid name collisions during testing. In prod this should be an empty string.
     */
    private final String classNameSuffix;

    public Transformer(Set<String> classesToTransform, Map<MethodKey, Method> instrumentationMethods) {
        this(classesToTransform, instrumentationMethods, "");
    }

    public Transformer(Set<String> classesToTransform, Map<MethodKey, Method> instrumentationMethods, String classNameSuffix) {
        this.classesToTransform = classesToTransform;
        // TODO: Should warn if any MethodKey doesn't match any methods
        this.instrumentationMethods = instrumentationMethods;
        this.classNameSuffix = classNameSuffix;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!classesToTransform.contains(className)) {
            LOGGER.trace("Not transforming {}", className);
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
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name + classNameSuffix, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            var mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            var voidDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getArgumentTypes(descriptor));
            var key = new MethodKey(className, name, voidDescriptor);
            var instrumentationMethod = instrumentationMethods.get(key);
            if (instrumentationMethod != null) {
                LOGGER.debug("Will instrument method {}", key);
                return new EntitlementMethodVisitor(Opcodes.ASM9, mv, descriptor, instrumentationMethod);
            } else {
                LOGGER.trace("Will not instrument method {}", key);
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
            int localVarIndex = 0;
            if (!isStatic) {
                mv.visitVarInsn(Opcodes.ALOAD, localVarIndex++);
            }
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

    private static final Logger LOGGER = LoggerFactory.getLogger(Transformer.class);
}
