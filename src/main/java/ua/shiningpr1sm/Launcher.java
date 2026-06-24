package ua.shiningpr1sm;

import javax.swing.*;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Launcher {
    public static void main(String[] args) {
        try {
            PrintStream log = new PrintStream(
                    new java.io.FileOutputStream(
                            System.getenv("APPDATA") + "\\ShiningPr1sm\\MediaDownloader\\app.log", true));
            System.setOut(log);
            System.setErr(log);
            System.out.println("=== App started ===");
        } catch (Exception ignored) {}

        try {
            System.out.println("Step 1: initConfig");
            ConfigManager.initConfig();

            System.out.println("Step 2: getInternalVersion");
            String currentVer = ConfigManager.getInternalVersion();

            System.out.println("Step 3: getLatestVersion");
            String latestVer = ConfigManager.getLatestVersion();

            System.out.println("currentVer=" + currentVer + " latestVer=" + latestVer);

            if (latestVer != null && !latestVer.equals(currentVer)) {
                try {
                    Path tempJar = Paths.get("MediaDownloader_new.jar");
                    ConfigManager.downloadNewVersion(tempJar);
                    restartAndApply(tempJar);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            System.out.println("Step 4: launching UI");
            SwingUtilities.invokeLater(JavaVideoDownloader::new);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void restartAndApply(Path tempJar) {
        try {
            Path currentJarPath = Paths.get(Launcher.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).toAbsolutePath();
            Path tempJarPath = tempJar.toAbsolutePath();
            Path scriptPath = currentJarPath.getParent().resolve("update.bat");

            String scriptContent = String.format(
                    "@echo off\n" +
                            "echo Starting update process...\n" +
                            "timeout /t 2 /nobreak > nul\n" +
                            ":loop\n" +
                            "echo Trying to delete old JAR: %s\n" +
                            "del /f \"%s\"\n" +
                            "if exist \"%s\" (\n" +
                            "  echo File is still locked, retrying in 1s...\n" +
                            "  timeout /t 1 > nul\n" +
                            "  goto loop\n" +
                            ")\n" +
                            "echo Moving new JAR to destination...\n" +
                            "move /y \"%s\" \"%s\"\n" +
                            "echo Starting new version...\n" +
                            "start javaw -jar \"%s\"\n" +
                            "echo Update complete. Closing.\n",
                    currentJarPath, currentJarPath, currentJarPath, tempJarPath, currentJarPath, currentJarPath
            );

            java.nio.file.Files.writeString(scriptPath, scriptContent);

            new ProcessBuilder("cmd", "/c", "start", scriptPath.toString()).start();

            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}