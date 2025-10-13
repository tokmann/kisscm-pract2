import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

public class PackageAnalyzer {

    public void analyzePackage(Path jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry manifestEntry = jarFile.getJarEntry("META-INF/MANIFEST.MF");
            if (manifestEntry == null) {
                System.out.println("MANIFEST.MF не найден в архиве.");
                return;
            }

            System.out.println("=== Служебная информация из MANIFEST.MF ===");

            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                System.out.println("Ошибка: не удалось прочитать MANIFEST.");
                return;
            }

            Attributes attrs = manifest.getMainAttributes();

            int longestKey = attrs.keySet().stream()
                    .map(Object::toString)
                    .mapToInt(String::length)
                    .max()
                    .orElse(10);

            for (Map.Entry<Object, Object> entry : attrs.entrySet()) {
                String key = entry.getKey().toString();
                String value = entry.getValue().toString();
                System.out.printf("%-" + (longestKey + 2) + "s : %s%n", key, value);
            }

            System.out.println("\n=== Всего атрибутов: " + attrs.size() + " ===");
        }
    }
}
