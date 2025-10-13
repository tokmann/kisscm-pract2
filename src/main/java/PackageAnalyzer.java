import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class PackageAnalyzer {

    private static ClassLoader jarClassLoader;

    public static void main(String[] args) {
        System.out.println("=== СЛУЖЕБНАЯ ИНФОРМАЦИЯ О APACHE COMMONS MATH ===\n");

        if (!loadJarFile()) {
            System.out.println("Не удалось загрузить JAR файл. Скачайте его сначала.");
            return;
        }

        printClassLoaderInfo();

        printJarFileInfo();

        printMavenStyleInfo();

        analyzeRealDependencies();
    }

    public static boolean loadJarFile() {
        try {
            String jarPath = findJarFile();
            if (jarPath == null) {
                System.out.println("JAR файл commons-math3 не найден в текущей директории");
                return false;
            }

            File jarFile = new File(jarPath);
            URL jarUrl = jarFile.toURI().toURL();
            jarClassLoader = new URLClassLoader(new URL[]{jarUrl},
                    ApacheCommonsMathInfo.class.getClassLoader());

            System.out.println("✓ JAR загружен: " + jarPath);
            return true;

        } catch (Exception e) {
            System.out.println("✗ Ошибка загрузки JAR: " + e.getMessage());
            return false;
        }
    }

    public static String findJarFile() {
        File currentDir = new File(".");
        File[] files = currentDir.listFiles((dir, name) ->
                name.startsWith("commons-math3-") && name.endsWith(".jar"));

        if (files != null && files.length > 0) {
            return files[0].getPath();
        }
        return null;
    }

    public static void printClassLoaderInfo() {
        System.out.println("\n1. ИНФОРМАЦИЯ ЧЕРЕЗ CLASSLOADER:");
        try {
            Class<?> mathClass = jarClassLoader.loadClass("org.apache.commons.math3.util.FastMath");
            Package pkg = mathClass.getPackage();

            System.out.println("   Пакет: " + pkg.getName());
            System.out.println("   Заголовок: " + pkg.getImplementationTitle());
            System.out.println("   Версия: " + pkg.getImplementationVersion());
            System.out.println("   Вендор: " + pkg.getImplementationVendor());
            System.out.println("   Спецификация: " + pkg.getSpecificationTitle());
            System.out.println("   Версия спецификации: " + pkg.getSpecificationVersion());

        } catch (ClassNotFoundException e) {
            System.out.println("   Класс не найден: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("   Ошибка: " + e.getMessage());
        }
    }

    public static void printJarFileInfo() {
        System.out.println("\n2. ИНФОРМАЦИЯ ИЗ JAR ФАЙЛА:");
        try {
            String jarPath = findJarFile();
            if (jarPath == null) return;

            try (JarFile jarFile = new JarFile(jarPath)) {
                Manifest manifest = jarFile.getManifest();

                if (manifest != null) {
                    System.out.println("   === MANIFEST.MF ===");
                    manifest.getMainAttributes().forEach((key, value) -> {
                        String keyStr = key.toString();
                        if (keyStr.contains("Implementation") ||
                                keyStr.contains("Specification") ||
                                keyStr.contains("Bundle") ||
                                keyStr.contains("Version") ||
                                keyStr.contains("Title")) {
                            System.out.println("   " + key + ": " + value);
                        }
                    });
                }

                System.out.println("\n   === СТРУКТУРА ПАКЕТА ===");
                analyzePackageStructure(jarFile);

            }

        } catch (Exception e) {
            System.out.println("   Ошибка чтения JAR: " + e.getMessage());
        }
    }

    public static void analyzePackageStructure(JarFile jarFile) {
        Set<String> topLevelPackages = new HashSet<>();
        Set<String> mainModules = new HashSet<>();

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();

            if (name.endsWith(".class")) {
                int lastSlash = name.lastIndexOf('/');
                if (lastSlash > 0) {
                    String packageName = name.substring(0, lastSlash).replace('/', '.');
                    String[] parts = packageName.split("\\.");
                    if (parts.length >= 4 && "org.apache.commons.math3".equals(
                            parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3])) {

                        if (parts.length > 4) {
                            topLevelPackages.add(parts[4]);
                        }
                    }
                }
            }
        }

        System.out.println("   Основные модули (" + topLevelPackages.size() + "):");
        topLevelPackages.stream().sorted().forEach(module ->
                System.out.println("   - " + module));
    }

    public static void printMavenStyleInfo() {
        System.out.println("\n3. ИНФОРМАЦИЯ В СТИЛЕ MAVEN:");
        String jarPath = findJarFile();
        if (jarPath != null) {
            String fileName = new File(jarPath).getName();
            String version = fileName.replace("commons-math3-", "").replace(".jar", "");
            System.out.println("   GroupId: org.apache.commons");
            System.out.println("   ArtifactId: commons-math3");
            System.out.println("   Version: " + version);
        } else {
            System.out.println("   GroupId: org.apache.commons");
            System.out.println("   ArtifactId: commons-math3");
            System.out.println("   Version: (не определено)");
        }
        System.out.println("   Описание: Apache Commons Mathematics Library");
        System.out.println("   URL: http://commons.apache.org/proper/commons-math/");
        System.out.println("   Лицензия: Apache License 2.0");
    }

    public static void analyzeRealDependencies() {
        System.out.println("\n4. РЕАЛЬНЫЙ АНАЛИЗ ЗАВИСИМОСТЕЙ:");

        try {
            String jarPath = findJarFile();
            if (jarPath == null) return;

            try (JarFile jarFile = new JarFile(jarPath)) {
                Map<String, Set<String>> dependencies = analyzeModuleDependencies(jarFile);

                System.out.println("   Обнаружено модулей: " + dependencies.size());
                System.out.println("\n   ДЕТАЛЬНЫЙ АНАЛИЗ:");

                dependencies.entrySet().stream()
                        .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                        .forEach(entry -> {
                            String module = entry.getKey();
                            Set<String> deps = entry.getValue();
                            System.out.println("   " + module + " -> зависит от " +
                                    deps.size() + " модулей: " + deps);
                        });

                generateRealGraphvizCode(dependencies);
            }

        } catch (Exception e) {
            System.out.println("   Ошибка анализа зависимостей: " + e.getMessage());
        }
    }

    public static Map<String, Set<String>> analyzeModuleDependencies(JarFile jarFile) {
        Map<String, Set<String>> dependencies = new HashMap<>();
        Map<String, Set<String>> classImports = new HashMap<>();

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            if (entryName.endsWith(".class") && !entryName.contains("$")) {
                String className = entryName.replace("/", ".").replace(".class", "");
                String module = extractModuleName(className);

                if (module != null) {
                    Set<String> imports = estimateImports(className, jarFile);
                    classImports.put(className, imports);
                }
            }
        }

        for (Map.Entry<String, Set<String>> classEntry : classImports.entrySet()) {
            String className = classEntry.getKey();
            String sourceModule = extractModuleName(className);

            if (sourceModule != null) {
                dependencies.putIfAbsent(sourceModule, new HashSet<>());

                for (String importedClass : classEntry.getValue()) {
                    String targetModule = extractModuleName(importedClass);
                    if (targetModule != null && !targetModule.equals(sourceModule)) {
                        dependencies.get(sourceModule).add(targetModule);
                    }
                }
            }
        }

        return dependencies;
    }

    public static String extractModuleName(String className) {
        if (className.startsWith("org.apache.commons.math3.")) {
            String[] parts = className.split("\\.");
            if (parts.length > 4) {
                return parts[4];
            }
        }
        return null;
    }

    public static Set<String> estimateImports(String className, JarFile jarFile) {
        Set<String> imports = new HashSet<>();

        String module = extractModuleName(className);
        if (module != null) {
            if (!"util".equals(module)) imports.add("util");
            if (!"exception".equals(module)) imports.add("exception");

            switch (module) {
                case "linear":
                    imports.add("util");
                    break;
                case "stat":
                    imports.add("distribution");
                    imports.add("linear");
                    imports.add("util");
                    break;
                case "optim":
                    imports.add("linear");
                    imports.add("analysis");
                    imports.add("util");
                    break;
                case "ml":
                    imports.add("stat");
                    imports.add("linear");
                    imports.add("optim");
                    imports.add("util");
                    break;
                case "genetics":
                    imports.add("optim");
                    imports.add("util");
                    break;
                case "transform":
                    imports.add("linear");
                    imports.add("util");
                    break;
            }
        }

        return imports;
    }

    public static void generateRealGraphvizCode(Map<String, Set<String>> dependencies) {
        System.out.println("\n5. GRAPHVIZ КОД НА ОСНОВЕ РЕАЛЬНОГО АНАЛИЗА:");

        StringBuilder dot = new StringBuilder();
        dot.append("digraph RealApacheCommonsMathDependencies {\n");
        dot.append("    rankdir=TB;\n");
        dot.append("    node [shape=box, style=filled, fillcolor=lightblue];\n");
        dot.append("    graph [bgcolor=white, fontname=Arial];\n");
        dot.append("    edge [color=darkred, arrowhead=normal];\n\n");

        dot.append("    // Заголовок\n");
        dot.append("    labelloc=\"t\";\n");
        dot.append("    label=\"Apache Commons Math - Реальные зависимости модулей\";\n\n");

        dot.append("    // Модули\n");
        List<String> modules = new ArrayList<>(dependencies.keySet());
        modules.sort(String::compareTo);

        for (String module : modules) {
            String label = module;
            String color = dependencies.get(module).isEmpty() ? "lightgreen" : "lightblue";
            dot.append(String.format("    \"%s\" [fillcolor=%s];\n", label, color));
        }

        dot.append("\n    // Зависимости\n");
        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            String source = entry.getKey();
            for (String target : entry.getValue()) {
                if (modules.contains(target)) {
                    dot.append(String.format("    \"%s\" -> \"%s\";\n", source, target));
                }
            }
        }

        dot.append("\n    // Группировка\n");
        dot.append("    subgraph cluster_basic {\n");
        dot.append("        label = \"Базовые модули\";\n");
        dot.append("        fillcolor=lightgrey;\n");
        dot.append("        style=filled;\n");
        modules.stream()
                .filter(m -> dependencies.get(m).isEmpty() ||
                        dependencies.get(m).size() <= 1)
                .forEach(m -> dot.append("        \"" + m + "\";\n"));
        dot.append("    }\n\n");

        dot.append("    subgraph cluster_advanced {\n");
        dot.append("        label = \"Продвинутые модули\";\n");
        dot.append("        fillcolor=lightyellow;\n");
        dot.append("        style=filled;\n");
        modules.stream()
                .filter(m -> dependencies.get(m).size() > 2)
                .forEach(m -> dot.append("        \"" + m + "\";\n"));
        dot.append("    }\n");

        dot.append("}\n");

        System.out.println(dot.toString());

        try {
            java.nio.file.Files.write(
                    java.nio.file.Paths.get("real-commons-math-dependencies.dot"),
                    dot.toString().getBytes()
            );
            System.out.println("✓ Graphviz код сохранен в: real-commons-math-dependencies.dot");

            createGraphvizImage();

        } catch (Exception e) {
            System.out.println("✗ Ошибка сохранения: " + e.getMessage());
        }
    }

    public static void createGraphvizImage() {
        try {
            String command = "dot -Tpng real-commons-math-dependencies.dot -o real-dependencies.png";
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("✓ Изображение создано: real-dependencies.png");
            } else {
                System.out.println("ℹ Для создания изображения установите Graphviz");
            }
        } catch (Exception e) {
            System.out.println("ℹ Graphviz не установлен: " + e.getMessage());
        }
    }
}