package ua.shiningpr1sm;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ConfigManager {
    private static final String APP_NAME = "ShiningPr1sm/MediaDownloader";
    private static final String CONFIG_FILE = "config.properties";

    private static final String GITHUB_URL = "https://github.com/ShiningPr1sm/Media-Downloader/releases/latest/download/MediaDownloader.jar";

    public static Path getConfigPath() {
        return Paths.get(System.getenv("APPDATA"), APP_NAME, CONFIG_FILE);
    }

    public static void initConfig() {
        Path path = getConfigPath();
        try {
            if (Files.notExists(path.getParent())) Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Properties props = new Properties();
                props.setProperty("version", "1.0.0");
                try (OutputStream out = Files.newOutputStream(path)) {
                    props.store(out, null);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public record ReleaseInfo(String version, String releaseNotesHtml) {}

    public static ReleaseInfo getLatestReleaseInfo() {
        String apiUrl = "https://api.github.com/repos/ShiningPr1sm/Media-Downloader/releases/latest";
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/vnd.github+json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return null;
            }

            String body = response.body();

            String version = null;
            int tagIdx = body.indexOf("\"tag_name\":");
            if (tagIdx != -1) {
                int start = body.indexOf("\"", tagIdx + 11) + 1;
                int end = body.indexOf("\"", start);
                version = body.substring(start, end).replaceFirst("^v", "");
            }

            String notesHtml = null;
            int bodyIdx = body.indexOf("\"body\":");
            if (bodyIdx != -1) {
                int start = body.indexOf("\"", bodyIdx + 7) + 1;
                int end = body.indexOf("\"", start);
                String markdown = body.substring(start, end)
                        .replace("\\r\\n", "\n")
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"");

                Parser parser = Parser.builder().build();
                HtmlRenderer renderer = HtmlRenderer.builder().build();
                notesHtml = renderer.render(parser.parse(markdown));
            }

            if (version == null) return null;
            return new ReleaseInfo(version, notesHtml);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getJarVersion(Path jarPath) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry entry = jar.getJarEntry("project.properties");
            if (entry == null) return null;
            try (InputStream is = jar.getInputStream(entry)) {
                Properties props = new Properties();
                props.load(is);
                return props.getProperty("app.version");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int compareVersions(String v1, String v2) {
        String[] a = v1.split("\\.");
        String[] b = v2.split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? Integer.parseInt(a[i]) : 0;
            int y = i < b.length ? Integer.parseInt(b[i]) : 0;
            if (x != y) return Integer.compare(x, y);
        }
        return 0;
    }

    public static Properties loadConfig() {
        Properties props = new Properties();
        Path path = getConfigPath();
        try (InputStream is = Files.newInputStream(path)) {
            props.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props;
    }

    public static void saveConfig(Properties props) {
        Path path = getConfigPath();
        try (OutputStream out = Files.newOutputStream(path)) {
            props.store(out, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void recordFailedUpdate(String version) {
        Properties props = loadConfig();
        String failedVer = props.getProperty("lastFailedUpdateVersion", "");
        int attempts = Integer.parseInt(props.getProperty("updateAttempts", "0"));

        if (version.equals(failedVer)) {
            attempts++;
        } else {
            attempts = 1;
        }

        props.setProperty("lastFailedUpdateVersion", version);
        props.setProperty("updateAttempts", String.valueOf(attempts));
        saveConfig(props);
    }

    public static void resetUpdateAttempts() {
        Properties props = loadConfig();
        props.remove("lastFailedUpdateVersion");
        props.remove("updateAttempts");
        saveConfig(props);
    }

    public static void downloadNewVersion(Path target) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(GITHUB_URL)).build();

        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(target));

        if (response.statusCode() != 200) {
            throw new IOException("Server respond with code: " + response.statusCode());
        }
    }

    public static String getInternalVersion() {
        Properties props = new Properties();
        InputStream is = ConfigManager.class.getResourceAsStream("/project.properties");
        if (is == null) {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream("project.properties");
        }
        if (is == null) {
            is = ClassLoader.getSystemResourceAsStream("project.properties");
        }

        try {
            if (is != null) {
                try (InputStream input = is) {
                    props.load(input);
                    String version = props.getProperty("app.version");
                    if (version != null) return version.trim();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "UNKNOWN_VERSION";
    }
}