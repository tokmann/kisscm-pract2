import java.io.*;
import java.nio.file.*;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Введите groupId: ");
            String groupId = reader.readLine();
            System.out.print("Введите artifactId: ");
            String artifactId = reader.readLine();
            System.out.print("Введите version: ");
            String version = reader.readLine();

            PackageDownloader downloader = new PackageDownloader();
            Path jarPath = downloader.downloadPackage(groupId, artifactId, version);

            PackageAnalyzer analyzer = new PackageAnalyzer();
            Map<String, String> manifestData = analyzer.analyzePackage(jarPath);

            GraphBuilder graphBuilder = new GraphBuilder();
            String outputName = artifactId + "-" + version;
            graphBuilder.buildGraph(manifestData, outputName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
