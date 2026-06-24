package ua.shiningpr1sm;

import com.formdev.flatlaf.FlatLightLaf;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JavaVideoDownloader {
    private static final String COMPANY_NAME = "ShiningPr1sm";
    private static final String APPDATA = System.getenv("APPDATA");
    private static final File SHARED_ROOT = new File(APPDATA, COMPANY_NAME);

    private static final File YTDLP_DIR = new File(SHARED_ROOT, "yt-dlp");
    private static final File YTDLP_EXE = new File(YTDLP_DIR, "yt-dlp.exe");
    private static final File YTDLP_VERSION_FILE = new File(YTDLP_DIR, "version.txt");
    private static final String YTDLP_URL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe";

    private static final File FFMPEG_DIR = new File(SHARED_ROOT, "ffmpeg");
    private static final File FFMPEG_EXE = new File(FFMPEG_DIR, "ffmpeg.exe");
    private static final String FFMPEG_ZIP_URL = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip";

    public JavaVideoDownloader() {
        System.out.println("Starting Social Media Video Downloader application.");
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException e) {
            System.err.println("Error setting LookAndFeel: " + e.getMessage());
            e.printStackTrace();
        }

        JFrame frame = new JFrame("Social Media Downloader");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(550, 300);
        frame.setLocation(
                (screenSize.width - frame.getWidth()) / 2,
                (screenSize.height - frame.getHeight()) / 2
        );
        frame.setLayout(new BorderLayout());
        frame.setResizable(false);

        try {
            Image icon = ImageIO.read(Objects.requireNonNull(JavaVideoDownloader.class.getResource("/project_icon.png")));
            frame.setIconImage(icon);
        } catch (IOException e) {
            System.err.println("Error loading icon: " + e.getMessage());
            e.printStackTrace();
        }

        final File[] downloadFolder = {new File(System.getProperty("user.home"), "Downloads/")};
        if (!downloadFolder[0].exists()) {
            downloadFolder[0].mkdirs();
            System.out.println("Created download directory: " + downloadFolder[0].getAbsolutePath());
        } else {
            System.out.println("Download directory exists: " + downloadFolder[0].getAbsolutePath());
        }

        JTextArea textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        String textarea_placeholder = "Paste or Enter links to social media here...";
        textArea.setForeground(Color.GRAY);
        textArea.setText(textarea_placeholder);
        textArea.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (textArea.getText().equals(textarea_placeholder)) {
                    textArea.setText("");
                    textArea.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (textArea.getText().isEmpty()) {
                    textArea.setForeground(Color.GRAY);
                    textArea.setText(textarea_placeholder);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 180));
        frame.add(scrollPane, BorderLayout.NORTH);

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(500, 20));
        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        progressPanel.add(progressBar);
        frame.add(progressPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] formats = {
                "Video + Audio",
                "Video only (muted)",
                "Audio only (mp3)"
        };

        JComboBox<String> formatBox = new JComboBox<>(formats);
        formatBox.setMaximumSize(new Dimension(200, 25));

        JButton downloadButton = new JButton("Download");
        downloadButton.setPreferredSize(new Dimension(320, 30));

        ImageIcon thumbIcon = new ImageIcon(
                Objects.requireNonNull(JavaVideoDownloader.class.getResource("/thumbnail_icon.png"))
        );
        Image scaled = thumbIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        JButton thumbnailButton = new JButton(new ImageIcon(scaled));
        thumbnailButton.setToolTipText("Download thumbnail");

        String[] browsers = {"None", "Firefox", "Chrome", "Edge", "Opera", "Brave"};
        JComboBox<String> browserComboBox = new JComboBox<>(browsers);

        bottomPanel.add(formatBox);
        bottomPanel.add(Box.createHorizontalStrut(10));
        bottomPanel.add(downloadButton);
        bottomPanel.add(Box.createHorizontalStrut(10));
        bottomPanel.add(thumbnailButton);
        bottomPanel.add(Box.createHorizontalStrut(10));
        bottomPanel.add(browserComboBox);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);

        downloadButton.addActionListener(e -> {
            String input = textArea.getText().trim();
            System.out.println("Download button clicked. Input: '" + input + "'");
            if (input.isEmpty() || input.equals(textarea_placeholder)) {
                JOptionPane.showMessageDialog(frame, "Please enter at least one video URL!");
                System.out.println("No URL entered.");
                return;
            }

            String[] urls = input.split("\\r?\\n");
            List<String> videoUrls = new ArrayList<>();
            for (String url : urls) {
                if (!url.trim().isEmpty()) {
                    videoUrls.add(url.trim());
                }
            }
            if (videoUrls.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No valid URLs found!");
                System.out.println("No valid URLs found after parsing.");
                return;
            }

            downloadButton.setEnabled(false);
            progressBar.setValue(0);
            progressBar.setString("Starting download...");
            System.out.println("Starting download process for " + videoUrls.size() + " URLs.");

            new Thread(() -> {
                try {
                    checkAndDownloadYTDLP();
                    checkAndDownloadFFMPEG();
                    String selectedFormat = (String) formatBox.getSelectedItem();
                    String browser = browserComboBox.getSelectedItem().toString().toLowerCase();
                    System.out.println("Selected format: " + selectedFormat + ", Browser for cookies: " + browser);

                    for (int i = 0; i < videoUrls.size(); i++) {
                        String videoUrl = videoUrls.get(i);
                        System.out.println("Processing URL " + (i + 1) + "/" + videoUrls.size() + ": " + videoUrl);
                        List<String> command = new ArrayList<>();
                        command.add(YTDLP_EXE.getAbsolutePath());

                        command.add("--remote-components");
                        command.add("ejs:github");
                        System.out.println("Adding --remote-components ejs:github for YouTube challenge solving.");

                        switch (selectedFormat) {
                            case "Video + Audio":
                                command.add("-f");
                                command.add("bestvideo[ext=mp4]+bestaudio[ext=m4a]/bestvideo+bestaudio/best");
                                command.add("--merge-output-format");
                                command.add("mp4");
                                command.add("--remux-video");
                                command.add("mp4");
                                command.add("--ffmpeg-location");
                                command.add(FFMPEG_EXE.getAbsolutePath());
                                if (!browser.isEmpty() && !browser.equals("none")) {
                                    command.add("--cookies-from-browser");
                                    command.add(browser);
                                    System.out.println("Adding --cookies-from-browser " + browser);
                                }
                                break;
                            case "Video only (muted)":
                                command.add("-f");
                                command.add("bestvideo[ext=mp4]/bestvideo/best");
                                command.add("--remux-video");
                                command.add("mp4");
                                command.add("--ffmpeg-location");
                                command.add(FFMPEG_EXE.getAbsolutePath());
                                if (!browser.isEmpty() && !browser.equals("none")) {
                                    command.add("--cookies-from-browser");
                                    command.add(browser);
                                    System.out.println("Adding --cookies-from-browser " + browser);
                                }
                                break;
                            case "Audio only (mp3)":
                                command.add("-f");
                                command.add("bestaudio/best");
                                command.add("--extract-audio");
                                command.add("--audio-format");
                                command.add("mp3");
                                command.add("--ffmpeg-location");
                                command.add(FFMPEG_EXE.getAbsolutePath());
                                if (!browser.isEmpty() && !browser.equals("none")) {
                                    command.add("--cookies-from-browser");
                                    command.add(browser);
                                    System.out.println("Adding --cookies-from-browser " + browser);
                                }
                                break;
                        }

                        command.add("--impersonate");
                        command.add("chrome");

                        String timeStamp = new java.text.SimpleDateFormat("dd-MM-yyyy_HH-mm-ss").format(new java.util.Date());
                        command.add("-o");
                        command.add(downloadFolder[0].getAbsolutePath() + "/%(title)s_%(id)s_" + timeStamp + ".%(ext)s");
                        command.add(videoUrl);

                        System.out.println("Executing command: " + String.join(" ", command));

                        ProcessBuilder pb = new ProcessBuilder(command);
                        pb.redirectErrorStream(true);
                        Process process = pb.start();

                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;
                        Pattern pattern = Pattern.compile("(\\d{1,3}\\.\\d)%");
                        int videoIndex = i + 1;
                        while ((line = reader.readLine()) != null) {
                            System.out.println("yt-dlp output: " + line);
                            Matcher matcher = pattern.matcher(line);
                            if (matcher.find()) {
                                int progress = (int) Float.parseFloat(matcher.group(1));
                                int totalProgress = (int) (((i + progress / 100.0) / videoUrls.size()) * 100);
                                SwingUtilities.invokeLater(() -> {
                                    progressBar.setValue(totalProgress);
                                    progressBar.setString("Video " + videoIndex + " of " + videoUrls.size() + " - " + progress + "%");
                                });
                            } else {
                                String finalLine = line;
                                SwingUtilities.invokeLater(() -> progressBar.setString("Video " + videoIndex + " of " + videoUrls.size() + " - " + finalLine.trim()));
                            }
                        }
                        int exitCode = process.waitFor();
                        System.out.println("yt-dlp process for " + videoUrl + " finished with exit code: " + exitCode);
                    }
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(100);
                        progressBar.setString("All downloads completed!");
                        downloadButton.setEnabled(true);
                        System.out.println("All downloads completed successfully!");
                    });
                } catch (IOException | InterruptedException ex) {
                    System.err.println("An error occurred during download: " + ex.getMessage());
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setString("Error: " + ex.getMessage());
                        downloadButton.setEnabled(true);
                        JOptionPane.showMessageDialog(frame, "An error occurred: " + ex.getMessage());
                    });
                }
            }).start();
        });

        thumbnailButton.addActionListener(e -> {
            String input = textArea.getText().trim();
            if (input.isEmpty() || input.equals(textarea_placeholder)) {
                JOptionPane.showMessageDialog(frame, "Please enter at least one video URL!");
                return;
            }

            String[] urls = input.split("\\r?\\n");
            List<String> videoUrls = new ArrayList<>();
            for (String url : urls) {
                if (!url.trim().isEmpty()) videoUrls.add(url.trim());
            }
            if (videoUrls.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No valid URLs found!");
                return;
            }

            thumbnailButton.setEnabled(false);
            progressBar.setValue(0);
            progressBar.setString("Starting thumbnail download...");
            System.out.println("Starting thumbnail download for " + videoUrls.size() + " URLs.");

            new Thread(() -> {
                try {
                    checkAndDownloadYTDLP();
                    String browser = browserComboBox.getSelectedItem().toString().toLowerCase();

                    for (int i = 0; i < videoUrls.size(); i++) {
                        String videoUrl = videoUrls.get(i);
                        int videoIndex = i + 1;
                        System.out.println("Downloading thumbnail for URL " + videoIndex + "/" + videoUrls.size() + ": " + videoUrl);

                        List<String> command = new ArrayList<>();
                        command.add(YTDLP_EXE.getAbsolutePath());
                        command.add("--write-thumbnail");
                        command.add("--skip-download");
                        command.add("--convert-thumbnails");
                        command.add("jpg");
                        command.add("--impersonate");
                        command.add("chrome");
                        if (!browser.isEmpty() && !browser.equals("none")) {
                            command.add("--cookies-from-browser");
                            command.add(browser);
                        }

                        String timeStamp = new java.text.SimpleDateFormat("dd-MM-yyyy_HH-mm-ss").format(new java.util.Date());
                        command.add("-o");
                        command.add(downloadFolder[0].getAbsolutePath() + "/%(title)s_%(id)s_" + timeStamp + ".%(ext)s");
                        command.add(videoUrl);

                        System.out.println("Executing thumbnail command: " + String.join(" ", command));

                        ProcessBuilder pb = new ProcessBuilder(command);
                        pb.redirectErrorStream(true);
                        Process process = pb.start();

                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;
                        int finalI = i;
                        while ((line = reader.readLine()) != null) {
                            System.out.println("yt-dlp thumbnail output: " + line);
                            String finalLine = line;
                            SwingUtilities.invokeLater(() -> progressBar.setString(
                                    "Thumbnail " + videoIndex + " of " + videoUrls.size() + " - " + finalLine.trim()));
                        }
                        int exitCode = process.waitFor();
                        System.out.println("Thumbnail download for " + videoUrl + " finished with exit code: " + exitCode);

                        int totalProgress = (int) (((double) (finalI + 1) / videoUrls.size()) * 100);
                        SwingUtilities.invokeLater(() -> progressBar.setValue(totalProgress));
                    }

                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(100);
                        progressBar.setString("All thumbnails downloaded!");
                        thumbnailButton.setEnabled(true);
                        System.out.println("All thumbnails downloaded successfully!");
                    });
                } catch (IOException | InterruptedException ex) {
                    System.err.println("Thumbnail download error: " + ex.getMessage());
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setString("Error: " + ex.getMessage());
                        thumbnailButton.setEnabled(true);
                        JOptionPane.showMessageDialog(frame, "An error occurred: " + ex.getMessage());
                    });
                }
            }).start();
        });
    }

    private static void checkAndDownloadYTDLP() throws IOException, InterruptedException {
        System.out.println("Checking yt-dlp existence and version...");
        if (!YTDLP_DIR.exists()) {
            YTDLP_DIR.mkdirs();
            System.out.println("Created yt-dlp directory: " + YTDLP_DIR.getAbsolutePath());
        }

        String storedVersion = readVersionFile();
        String latestVersion = getLatestYtDlpVersion();
        String currentInstalledVersion = null;

        boolean needsDownload = false;

        if (YTDLP_EXE.exists()) {
            try {
                Process process = new ProcessBuilder(YTDLP_EXE.getAbsolutePath(), "--version").start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                currentInstalledVersion = reader.readLine();
                process.waitFor();
                System.out.println("Currently installed yt-dlp version from executable: " + currentInstalledVersion);
            } catch (Exception e) {
                System.err.println("Failed to get current installed yt-dlp version from executable. Forcing download. Error: " + e.getMessage());
                needsDownload = true;
            }
        } else {
            System.out.println("yt-dlp.exe not found. Needs download.");
            needsDownload = true;
        }

        if (latestVersion == null) {
            System.err.println("Could not fetch latest yt-dlp version from GitHub. Skipping online version check.");
            if (!needsDownload && storedVersion != null && storedVersion.equals(currentInstalledVersion)) {
                System.out.println("yt-dlp is assumed to be up to date (cannot check online). Installed: " + currentInstalledVersion);
            } else if (!YTDLP_EXE.exists() && storedVersion == null) {
                needsDownload = true;
                System.out.println("No yt-dlp.exe and no stored version, and cannot get latest online. Attempting download.");
            }
        } else {
            System.out.println("Latest yt-dlp version from GitHub: " + latestVersion);

            if (currentInstalledVersion != null && currentInstalledVersion.equals(latestVersion) &&
                    storedVersion != null && storedVersion.equals(latestVersion)) {
                System.out.println("yt-dlp is up to date according to version file, installed version, and latest online (" + latestVersion + "). No download needed.");
                needsDownload = false;
            } else {
                System.out.println("yt-dlp update required: stored=" + storedVersion + ", installed=" + currentInstalledVersion + ", latest=" + latestVersion);
                needsDownload = true;
            }
        }

        if (needsDownload) {
            System.out.println("Downloading latest yt-dlp from: " + YTDLP_URL);
            try (InputStream in = new URL(YTDLP_URL).openStream();
                 FileOutputStream out = new FileOutputStream(YTDLP_EXE)) {
                in.transferTo(out);
            }
            YTDLP_EXE.setExecutable(true);
            System.out.println("yt-dlp updated successfully.");

            String versionToWrite = latestVersion;
            if (versionToWrite == null) {
                try {
                    Process process = new ProcessBuilder(YTDLP_EXE.getAbsolutePath(), "--version").start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    versionToWrite = reader.readLine();
                    process.waitFor();
                    System.out.println("Version from newly downloaded yt-dlp.exe: " + versionToWrite);
                } catch (Exception e) {
                    System.err.println("Could not get version from newly downloaded yt-dlp.exe: " + e.getMessage());
                    versionToWrite = "unknown_download";
                }
            }
            writeVersionFile(versionToWrite != null ? versionToWrite : "unknown_fallback");
        }
    }

    private static String getLatestYtDlpVersion() {
        System.out.println("Fetching latest yt-dlp version from GitHub API...");
        try (InputStream in = new URL("https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest").openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            StringBuilder jsonResponse = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonResponse.append(line);
            }
            Pattern pattern = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(jsonResponse.toString());
            if (matcher.find()) {
                String version = matcher.group(1);
                System.out.println("Latest yt-dlp version found on GitHub: " + version);
                return version;
            } else {
                System.err.println("Could not find 'tag_name' in GitHub API response.");
                return null;
            }
        } catch (IOException e) {
            System.err.println("Could not fetch latest yt-dlp version from GitHub API: " + e.getMessage());
        }
        return null;
    }

    private static String readVersionFile() {
        if (!YTDLP_VERSION_FILE.exists()) {
            System.out.println("yt-dlp version file not found.");
            return null;
        }
        try {
            String version = Files.readString(YTDLP_VERSION_FILE.toPath()).trim();
            System.out.println("Read yt-dlp version from file: " + version);
            return version;
        } catch (IOException e) {
            System.err.println("Error reading yt-dlp version file: " + e.getMessage());
            return null;
        }
    }

    private static void writeVersionFile(String version) {
        try {
            Files.writeString(YTDLP_VERSION_FILE.toPath(), version);
            System.out.println("Wrote yt-dlp version '" + version + "' to file: " + YTDLP_VERSION_FILE.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error writing yt-dlp version file: " + e.getMessage());
        }
    }

    private static void checkAndDownloadFFMPEG() throws IOException {
        System.out.println("Checking ffmpeg existence and version...");
        if (!FFMPEG_DIR.exists()) {
            FFMPEG_DIR.mkdirs();
            System.out.println("Created ffmpeg directory: " + FFMPEG_DIR.getAbsolutePath());
        }
        cleanupOldFfmpegExtracts();

        if (FFMPEG_EXE.exists()) {
            System.out.println("ffmpeg.exe already exists at: " + FFMPEG_EXE.getAbsolutePath());
            return;
        }

        System.out.println("ffmpeg not found, downloading zip from: " + FFMPEG_ZIP_URL);

        File zipFile = new File(FFMPEG_DIR, "ffmpeg.zip");
        try (InputStream in = new URL(FFMPEG_ZIP_URL).openStream();
             FileOutputStream out = new FileOutputStream(zipFile)) {
            in.transferTo(out);
            System.out.println("ffmpeg zip downloaded to: " + zipFile.getAbsolutePath());
        }

        System.out.println("Extracting ffmpeg from zip...");
        extractFfmpegFromZip(zipFile, FFMPEG_EXE);
        zipFile.delete();
        System.out.println("ffmpeg zip deleted.");

        if (!FFMPEG_EXE.exists()) {
            throw new IOException("Не найден ffmpeg.exe внутри архива.");
        }
        FFMPEG_EXE.setExecutable(true, false);

        System.out.println("ffmpeg installed to: " + FFMPEG_EXE.getAbsolutePath());
    }

    private static void extractFfmpegFromZip(File zipFile, File outFile) throws IOException {
        System.out.println("Attempting to extract ffmpeg.exe from " + zipFile.getName());
        try (ZipFile zf = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            ZipEntry candidate = null;

            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName().replace('\\','/').toLowerCase();
                if (!e.isDirectory() && name.endsWith("/bin/ffmpeg.exe")) {
                    candidate = e;
                    System.out.println("Found ffmpeg.exe at: " + e.getName());
                    break;
                }
            }

            if (candidate == null) {
                System.out.println("ffmpeg.exe not found in /bin/ path, searching root...");
                Enumeration<? extends ZipEntry> entries2 = zf.entries();
                while (entries2.hasMoreElements()) {
                    ZipEntry e = entries2.nextElement();
                    String name = e.getName().replace('\\','/').toLowerCase();
                    if (!e.isDirectory() && name.endsWith("ffmpeg.exe")) {
                        candidate = e;
                        System.out.println("Found ffmpeg.exe at: " + e.getName() + " (root level)");
                        break;
                    }
                }
            }

            if (candidate == null) {
                throw new IOException("В архиве не найден ffmpeg.exe");
            }

            try (InputStream is = zf.getInputStream(candidate);
                 FileOutputStream fos = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) > 0) fos.write(buffer, 0, len);
                System.out.println("ffmpeg.exe extracted successfully to: " + outFile.getAbsolutePath());
            }
        }
    }

    private static void cleanupOldFfmpegExtracts() {
        File dir = FFMPEG_DIR;
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        System.out.println("Cleaning up old ffmpeg extracts in: " + dir.getAbsolutePath());
        for (File f : files) {
            if (f.getName().equalsIgnoreCase("ffmpeg.exe") || f.getName().equalsIgnoreCase("ffmpeg.zip")) {
                continue;
            }
            System.out.println("Deleting old ffmpeg related file/directory: " + f.getAbsolutePath());
            deleteRecursivelyQuiet(f);
        }
    }

    private static void deleteRecursivelyQuiet(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) {
                    deleteRecursivelyQuiet(c);
                }
            }
        }
        try {
            f.delete();
            System.out.println("Successfully deleted: " + f.getAbsolutePath());
        } catch (Exception ignored) {
            System.err.println("Failed to delete: " + f.getAbsolutePath());
        }
    }
}