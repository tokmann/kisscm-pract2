import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class GraphBuilder {

    public void buildGraph(Map<String, String> manifestData, String outputName) throws IOException, InterruptedException {
        String dotFileName = outputName + ".dot";
        String pngFileName = outputName + ".png";

        StringBuilder dot = new StringBuilder();
        dot.append("digraph Manifest {\n");
        dot.append("  rankdir=TB;\n");
        dot.append("  size=\"11,8.5\";\n"); // Формат A4
        dot.append("  ratio=fill;\n");
        dot.append("  dpi=150;\n"); // Умеренное качество
        dot.append("  ranksep=0.5;\n");
        dot.append("  nodesep=0.3;\n");
        dot.append("  node [shape=box, style=rounded, fontname=\"Arial\", fontsize=14];\n"); // Крупные шрифты
        dot.append("  edge [fontsize=12];\n\n");

        // Главный узел - крупный и читаемый
        String bundleName = manifestData.getOrDefault("Bundle-Name", outputName);
        String version = manifestData.getOrDefault("Bundle-Version", "");
        dot.append("  main [label=\"")
                .append(escape(bundleName))
                .append("\\n")
                .append(escape(outputName))
                .append("\\nVersion: ")
                .append(escape(version))
                .append("\", shape=ellipse, style=filled, fillcolor=lightblue, fontsize=16, width=3, height=1.5];\n\n");

        // Извлекаем пакеты
        List<String> exportPackages = new ArrayList<>();
        List<String> privatePackages = new ArrayList<>();

        for (Map.Entry<String, String> entry : manifestData.entrySet()) {
            String key = entry.getKey();
            if (key.contains("Export-Package")) {
                exportPackages = splitPackages(entry.getValue());
            } else if (key.contains("Private-Package")) {
                privatePackages = splitPackages(entry.getValue());
            }
        }

        // Служебная информация - компактно в одном узле с крупным шрифтом
        dot.append("  // ========== СЛУЖЕБНАЯ ИНФОРМАЦИЯ ==========\n");
        StringBuilder infoLabel = new StringBuilder();
        infoLabel.append("СЛУЖЕБНАЯ ИНФОРМАЦИЯ\\n");
        infoLabel.append("─────────────────────\\n");

        int infoCount = 0;
        for (Map.Entry<String, String> entry : manifestData.entrySet()) {
            String key = entry.getKey();
            if (isImportantInfo(key) && infoCount < 8) { // Ограничиваем количество
                infoLabel.append(escape(shorten(key, 25)))
                        .append(": ")
                        .append(escape(shorten(entry.getValue(), 20)))
                        .append("\\n");
                infoCount++;
            }
        }

        dot.append("  info [label=\"")
                .append(infoLabel.toString())
                .append("\", fillcolor=lightyellow, style=filled, fontsize=12, width=3, height=2];\n");
        dot.append("  main -> info [color=blue];\n\n");

        // Export-Package - один узел со списком
        if (!exportPackages.isEmpty()) {
            dot.append("  // ========== EXPORT PACKAGES ==========\n");
            StringBuilder exportLabel = new StringBuilder();
            exportLabel.append("EXPORT PACKAGES\\n");
            exportLabel.append("(").append(exportPackages.size()).append(" packages)\\n");
            exportLabel.append("─────────────────\\n");

            // Показываем только первые 15 пакетов для читаемости
            int maxToShow = Math.min(exportPackages.size(), 15);
            for (int i = 0; i < maxToShow; i++) {
                String shortName = getVeryShortPackageName(exportPackages.get(i));
                exportLabel.append("• ").append(escape(shortName)).append("\\n");
            }

            if (exportPackages.size() > maxToShow) {
                exportLabel.append("• ... and ").append(exportPackages.size() - maxToShow).append(" more");
            }

            dot.append("  export [label=\"")
                    .append(exportLabel.toString())
                    .append("\", fillcolor=lightgreen, style=filled, fontsize=12, width=3.5, height=3];\n");
            dot.append("  main -> export [color=green];\n\n");
        }

        // Private-Package - один узел со списком
        if (!privatePackages.isEmpty()) {
            dot.append("  // ========== PRIVATE PACKAGES ==========\n");
            StringBuilder privateLabel = new StringBuilder();
            privateLabel.append("PRIVATE PACKAGES\\n");
            privateLabel.append("(").append(privatePackages.size()).append(" packages)\\n");
            privateLabel.append("──────────────────\\n");

            for (int i = 0; i < privatePackages.size(); i++) {
                String shortName = getVeryShortPackageName(privatePackages.get(i));
                privateLabel.append("• ").append(escape(shortName)).append("\\n");
            }

            dot.append("  private [label=\"")
                    .append(privateLabel.toString())
                    .append("\", fillcolor=lightcoral, style=filled, fontsize=12, width=3, height=1.5];\n");
            dot.append("  main -> private [color=red];\n\n");
        }

        // Версия и лицензия - компактно в одном узле
        dot.append("  // ========== ВЕРСИЯ И ЛИЦЕНЗИЯ ==========\n");
        StringBuilder versionLabel = new StringBuilder();
        versionLabel.append("ВЕРСИЯ И ЛИЦЕНЗИЯ\\n");
        versionLabel.append("─────────────────\\n");

        int versionCount = 0;
        for (Map.Entry<String, String> entry : manifestData.entrySet()) {
            String key = entry.getKey();
            if ((key.toLowerCase().contains("version") || key.toLowerCase().contains("license")) && versionCount < 5) {
                versionLabel.append(escape(shorten(key, 20)))
                        .append(": ")
                        .append(escape(shorten(entry.getValue(), 25)))
                        .append("\\n");
                versionCount++;
            }
        }

        dot.append("  version [label=\"")
                .append(versionLabel.toString())
                .append("\", fillcolor=orange, style=filled, fontsize=12, width=3, height=2];\n");
        dot.append("  main -> version [color=orange];\n");

        dot.append("}\n");

        Files.write(Paths.get(dotFileName), dot.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("Файл DOT сохранён: " + dotFileName);

        if (!isGraphvizInstalled()) {
            System.out.println("Graphviz (dot) не найден в PATH. Установите его, чтобы получить картинку.\n" +
                    "Можно установить через: sudo apt install graphviz");
            return;
        }

        // Простые настройки для читаемого графа
        ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", "-Gsize=11,8.5", "-Gdpi=150",
                dotFileName, "-o", pngFileName);
        pb.inheritIO();
        Process process = pb.start();
        process.waitFor();

        System.out.println("Граф успешно создан: " + pngFileName);
    }

    private String getVeryShortPackageName(String fullPackageName) {
        // Максимально сокращаем имена пакетов для читаемости
        String[] parts = fullPackageName.split("\\.");
        if (parts.length > 4) {
            // Показываем только последние 2 компонента
            return parts[parts.length - 2] + "." + parts[parts.length - 1];
        } else if (parts.length > 2) {
            // Показываем только последний компонент
            return parts[parts.length - 1];
        }
        return fullPackageName.length() > 20 ?
                fullPackageName.substring(0, 17) + "..." :
                fullPackageName;
    }

    private List<String> splitPackages(String packageString) {
        List<String> packages = new ArrayList<>();
        if (packageString == null || packageString.trim().isEmpty()) {
            return packages;
        }

        String[] parts = packageString.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                String cleanPackage = trimmed.replaceAll(";version=\"[^\"]*\"", "")
                        .replaceAll(";.*", "");
                packages.add(cleanPackage);
            }
        }
        return packages;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private boolean isGraphvizInstalled() {
        try {
            Process process = new ProcessBuilder("dot", "-V").start();
            process.waitFor();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String shorten(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private boolean isImportantInfo(String key) {
        return key.equals("Manifest-Version") ||
                key.equals("Implementation-Title") ||
                key.equals("Built-By") ||
                key.equals("Implementation-Vendor") ||
                key.equals("Specification-Title") ||
                key.equals("Bundle-Name") ||
                key.equals("Created-By") ||
                key.equals("Build-Jdk") ||
                key.equals("Bundle-SymbolicName") ||
                key.equals("Bundle-Description");
    }
}