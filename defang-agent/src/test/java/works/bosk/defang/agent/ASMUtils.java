package works.bosk.defang.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.Printer;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ASMUtils {
    public static String bytecode2text(byte[] classBytes) {
        ClassReader classReader = new ClassReader(classBytes);
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            Printer printer = new Textifier(); // For a textual representation
            TraceClassVisitor traceClassVisitor = new TraceClassVisitor(null, printer, printWriter);
            classReader.accept(traceClassVisitor, 0);
            return stringWriter.toString();
        }
    }
}
