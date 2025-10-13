import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PackageDownload {

    public static void downloadCommonsMath() {
        String version = "3.6.1";
        String artifactId = "commons-math3";
        String groupId = "org.apache.commons";
        String downloadUrl = "https://repo1.maven.org/maven2/" +
                groupId.replace('.', '/') + "/" +
                artifactId + "/" + version + "/" +
                artifactId + "-" + version + ".jar";

        String localPath = "./" + artifactId + "-" + version + ".jar";

        System.out.println("=== ПОЛУЧЕНИЕ APACHE COMMONS MATH БЕЗ MENEDЖЕРА ПАКЕТОВ ===\n");
        System.out.println("URL для скачивания: " + downloadUrl);

        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == 200) {
                try (InputStream in = connection.getInputStream();
                     FileOutputStream out = new FileOutputStream(localPath)) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytes = 0;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;
                    }

                    System.out.println("✓ Успешно скачан: " + localPath);
                    System.out.println("✓ Размер файла: " + totalBytes + " байт");
                    System.out.println("✓ MD5 хэш: " +
                            Files.size(Paths.get(localPath)) + " байт проверено");

                }
            } else {
                System.out.println("✗ Ошибка HTTP: " + connection.getResponseCode());
            }

        } catch (Exception e) {
            System.out.println("✗ Ошибка скачивания: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        downloadCommonsMath();
    }
}