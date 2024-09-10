package works.bosk.defang.instrumentation;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map;

import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Opcodes.ACC_NATIVE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public class Instrumenter {
    public static final String NATIVE_METHOD_PREFIX = "$DEFANG$_";
    /**
     * To avoid class name collisions during testing. Should be an empty string in production.
     */
    private final String classNameSuffix;
    private final Map<MethodKey, Method> instrumentationMethods;

    public Instrumenter(String classNameSuffix, Map<MethodKey, Method> instrumentationMethods) {
        this.classNameSuffix = classNameSuffix;
        this.instrumentationMethods = instrumentationMethods;
    }

    public ClassFileInfo instrumentClassFile(Class<?> clazz) throws IOException {
        ClassFileInfo initial = getClassFileInfo(clazz);
        return new ClassFileInfo(initial.fileName(), instrumentClass(Type.getInternalName(clazz), initial.bytecodes()));
    }

    public static ClassFileInfo getClassFileInfo(Class<?> clazz) throws IOException {
        String internalName = Type.getInternalName(clazz);
        String fileName = "/" + internalName + ".class";
        byte[] originalBytecodes;
        try (InputStream classStream = clazz.getResourceAsStream(fileName)) {
            if (classStream == null) {
                throw new IllegalStateException("Classfile not found in jar: " + fileName);
            }
            originalBytecodes = classStream.readAllBytes();
        }
        return new ClassFileInfo(fileName, originalBytecodes);
    }

    public byte[] instrumentClass(String className, byte[] classfileBuffer) {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(reader, COMPUTE_FRAMES | COMPUTE_MAXS);
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
            var targetMethod = new EntitlementMethodVisitor.MethodInfo(access, className, name, descriptor);
            if ((access & ACC_NATIVE) != 0) {
                throw new IllegalStateException("Cannot instrument native method: " + targetMethod);
            }
            boolean isStatic = (access & ACC_STATIC) != 0;
            var voidDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getArgumentTypes(descriptor));
            var key = new MethodKey(className, name, voidDescriptor, isStatic);
            var instrumentationMethod = instrumentationMethods.get(key);
            if (instrumentationMethod != null) {
                if ((access & ACC_NATIVE) != 0) {
                    LOGGER.debug("Will instrument native {}method {}", isStatic? "static ":"", key);
                    // We're effectively generating a method that wasn't there before, so we must take matters into our own hands
                    var mv = super.visitMethod(access & ~ACC_NATIVE, name, descriptor, signature, exceptions);
                    EntitlementMethodVisitor v = new EntitlementMethodVisitor(Opcodes.ASM9, mv, targetMethod, instrumentationMethod);
                    v.visitCode();
                    v.visitMaxs(0,0);

                    // Now generate the native method stub
                    // Note: to try without the stub, return null instead
                    return super.visitMethod(access, NATIVE_METHOD_PREFIX + name, descriptor, signature, exceptions);
                } else {
                    LOGGER.debug("Will instrument {}method {}", isStatic? "static ":"", key);
                    // Process the method normally, prepending to its bytecode
                    var mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    return new EntitlementMethodVisitor(Opcodes.ASM9, mv, targetMethod, instrumentationMethod);
                }
            } else {
                LOGGER.trace("Will not instrument method {}", key);
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }
    }

    static class EntitlementMethodVisitor extends MethodVisitor {
        private final MethodInfo targetMethod;
        private final Method instrumentationMethod;

        public record MethodInfo (
            int access,
            String className,
            String name,
            String descriptor
        ){}

        EntitlementMethodVisitor(int api, MethodVisitor methodVisitor, MethodInfo targetMethod, Method instrumentationMethod) {
            super(api, methodVisitor);
            this.targetMethod = targetMethod;
            this.instrumentationMethod = instrumentationMethod;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return super.visitAnnotation(descriptor, visible);
        }

        @Override
        public void visitCode() {
            forwardIncomingArguments();
            invokeInstrumentationMethod();
            if ((targetMethod.access & ACC_NATIVE) != 0) {
                forwardIncomingArguments();
                invokeNativeMethod();
                visitInsn(Type.getReturnType(targetMethod.descriptor).getOpcode(Opcodes.IRETURN));
            } else {
                super.visitCode();
            }
        }

        private void forwardIncomingArguments() {
            int localVarIndex = 0;
            if ((targetMethod.access & ACC_STATIC) != 0) {
                // To keep things consistent between static and virtual methods, we pass a null in here,
                // analogous to how Field and Method accept nulls for static fields/methods.
                mv.visitInsn(Opcodes.ACONST_NULL);
            } else {
                mv.visitVarInsn(Opcodes.ALOAD, localVarIndex++);
            }
            for (Type type : Type.getArgumentTypes(targetMethod.descriptor)) {
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

        private void invokeNativeMethod() {
            if ((targetMethod.access & ACC_STATIC) != 0) {
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        targetMethod.className,
                        NATIVE_METHOD_PREFIX + targetMethod.name,
                        targetMethod.descriptor,
                        false);
            } else {
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(
                        INVOKEVIRTUAL,
                        targetMethod.className,
                        NATIVE_METHOD_PREFIX + targetMethod.name,
                        targetMethod.descriptor,
                        false);
            }
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Instrumenter.class);

    public record ClassFileInfo(String fileName, byte[] bytecodes) { }
}
