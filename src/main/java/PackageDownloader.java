import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;

public class PackageDownloader {

    private static final String MAVEN_CENTRAL = "https://maven-central.storage-download.googleapis.com/maven2/";

    public Path downloadPackage(String groupId, String artifactId, String version) throws IOException, InterruptedException {
        String groupPath = groupId.replace('.', '/');
        String jarName = artifactId + "-" + version + ".jar";
        String jarUrl = MAVEN_CENTRAL + groupPath + "/" + artifactId + "/" + version + "/" + jarName;

        System.out.println("Скачиваем пакет: " + jarUrl);

        Path targetPath = Paths.get(jarName);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jarUrl))
                .header("User-Agent", "Mozilla/5.0 (Java HttpClient)")
                .timeout(Duration.ofSeconds(120))
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IOException("Ошибка HTTP " + response.statusCode() + " при загрузке " + jarUrl);
        }

        long contentLength = response.headers()
                .firstValueAsLong("Content-Length")
                .orElse(-1);

        if (contentLength > 0)
            System.out.println("Размер файла: " + (contentLength / 1024) + " КБ");
        else
            System.out.println("Размер файла неизвестен, показываю примерный прогресс.");

        try (InputStream in = new BufferedInputStream(response.body());
             OutputStream out = new BufferedOutputStream(Files.newOutputStream(targetPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {

            byte[] buffer = new byte[8192];
            long totalRead = 0;
            int bytesRead;
            int lastPercent = 0;
            long start = System.currentTimeMillis();

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalRead += bytesRead;

                if (contentLength > 0) {
                    int percent = (int) ((totalRead * 100) / contentLength);
                    if (percent >= lastPercent + 5) {
                        System.out.printf("Прогресс: %d%%%n", percent);
                        lastPercent = percent;
                    }
                }
            }

            long elapsed = System.currentTimeMillis() - start;
            System.out.printf("Загрузка завершена за %.2f сек%n", elapsed / 1000.0);
        }

        System.out.println("Пакет сохранен: " + targetPath.toAbsolutePath());
        return targetPath;
    }
}
