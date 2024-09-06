package works.bosk.defang.scan;

import works.bosk.defang.instrumentation.ConfigScanner;
import works.bosk.defang.instrumentation.Instrumenter;

import java.io.IOException;
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
                Instrumenter.ClassFileInfo i = instrumenter.instrumentClassFile(clazz);
                Path filePath = jar.getPath(i.fileName());
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, i.bytecodes());
            }
        }
    }

}
