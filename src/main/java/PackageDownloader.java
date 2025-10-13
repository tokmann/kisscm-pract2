import java.io.*;
import java.net.*;
import java.nio.file.*;

public class PackageDownloader {

    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2/";

    public Path downloadPackage(String groupId, String artifactId, String version) throws IOException {
        String groupPath = groupId.replace('.', '/');
        String jarName = artifactId + "-" + version + ".jar";
        String jarUrl = MAVEN_CENTRAL + groupPath + "/" + artifactId + "/" + version + "/" + jarName;

        System.out.println("Скачиваем пакет: " + jarUrl);

        Path targetPath = Paths.get(jarName);
        try (InputStream in = new URL(jarUrl).openStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("Пакет сохранен: " + targetPath.toAbsolutePath());
        return targetPath;
    }
}
