package ua.shiningpr1sm;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.util.Properties;

public class ConfigManager {
    private static final String APP_NAME = "ShiningPr1sm/MediaDownloader";
    private static final String CONFIG_FILE = "config.properties";

    private static final String GITHUB_URL = "https://github.com/ShiningPr1sm/media-downloader/releases/latest/download/MediaDownloader.jar";

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

    public static String getLatestVersion() {
        String versionUrl = "https://raw.githubusercontent.com/ShiningPr1sm/media-downloader/refs/heads/master/version.txt";
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(versionUrl)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body().trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
        try (InputStream is = ConfigManager.class.getResourceAsStream("/project.properties")) {
            if (is != null) {
                props.load(is);
                return props.getProperty("app.version");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "1.0.0";
    }
}