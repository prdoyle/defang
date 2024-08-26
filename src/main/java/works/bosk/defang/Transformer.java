
package works.bosk.defang;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

public class Transformer implements ClassFileTransformer {

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
							ProtectionDomain protectionDomain, byte[] classfileBuffer) {
		if (!className.startsWith("works/bosk")) {
			return classfileBuffer;
		}
		ClassReader reader = new ClassReader(classfileBuffer);
		ClassWriter writer = new ClassWriter(reader, COMPUTE_FRAMES | COMPUTE_MAXS);
		ClassVisitor visitor = new HashCodeClassVisitor(Opcodes.ASM9, writer);
		reader.accept(visitor, 0);
		return writer.toByteArray();
	}

	static class HashCodeClassVisitor extends ClassVisitor {
		HashCodeClassVisitor(int api, ClassVisitor classVisitor) {
			super(api, classVisitor);
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
			if (name.equals("hashCode") && descriptor.equals("()I")) {
				return new HashCodeMethodVisitor(Opcodes.ASM9, mv);
			}
			return mv;
		}
	}

	static class HashCodeMethodVisitor extends MethodVisitor {
		HashCodeMethodVisitor(int api, MethodVisitor methodVisitor) {
			super(api, methodVisitor);
		}

		@Override
		public void visitCode() {
			super.visitCode();
			mv.visitVarInsn(Opcodes.ALOAD, 0); // Load "this" onto the stack
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I", false);
			mv.visitInsn(Opcodes.IRETURN);
		}
	}
}
