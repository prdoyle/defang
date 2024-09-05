package works.bosk.defang.scan;

import org.objectweb.asm.Type;
import works.bosk.defang.instrumentation.ConfigScanner;
import works.bosk.defang.instrumentation.Instrumenter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static works.bosk.defang.runtime.Config.CONFIG_CLASSES;

public class JarMaker {

    public static void main(String[] args) throws IOException {
        Path outputJarPath = Path.of("instrumented.jar");
        var scanResults = ConfigScanner.scanConfig(CONFIG_CLASSES);
        var instrumenter = new Instrumenter("", scanResults.instrumentationMethods());

        // URI for the JAR file, with "jar:" prefix
        Files.deleteIfExists(outputJarPath);
        URI jarURI = URI.create("jar:" + outputJarPath.toUri());
        try (FileSystem jar = FileSystems.newFileSystem(jarURI, Map.of("create", "true"))) {
            for (var clazz: scanResults.classesToInstrument()) {
                String internalName = Type.getInternalName(clazz);
                String fileName = "/" + internalName + ".class";
                try (InputStream classStream = clazz.getResourceAsStream(fileName)) {
                    if (classStream == null) {
                        throw new IllegalStateException("Classfile not found in jar: " + fileName);
                    }
                    byte[] modifiedClass = instrumenter.instrumentClass(internalName, classStream.readAllBytes());

                    Path filePath = jar.getPath(fileName);
                    Files.createDirectories(filePath.getParent());
                    Files.write(filePath, modifiedClass);
                }
            }
        }
    }
}
