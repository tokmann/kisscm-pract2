import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

public class PackageAnalyzer {

    public Map<String, String> analyzePackage(Path jarPath) throws IOException {
        Map<String, String> manifestData = new LinkedHashMap<>();

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                System.out.println("MANIFEST.MF не найден.");
                return manifestData;
            }

            Attributes attrs = manifest.getMainAttributes();

            System.out.println("=== Служебная информация из MANIFEST.MF ===");

            int longestKey = attrs.keySet().stream()
                    .map(Object::toString)
                    .mapToInt(String::length)
                    .max()
                    .orElse(10);

            for (Map.Entry<Object, Object> entry : attrs.entrySet()) {
                String key = entry.getKey().toString();
                String value = entry.getValue().toString();
                manifestData.put(key, value);
                System.out.printf("%-" + (longestKey + 2) + "s : %s%n", key, value);
            }

            System.out.println("=== Всего атрибутов: " + attrs.size() + " ===");
        }

        return manifestData;
    }
}
