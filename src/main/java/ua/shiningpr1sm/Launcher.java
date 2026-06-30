package ua.shiningpr1sm;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Launcher {
    public static void main(String[] args) {
        ConfigManager.initConfig();
        String currentVer = ConfigManager.getInternalVersion();
        String latestVer = ConfigManager.getLatestVersion();

        if (latestVer != null && !latestVer.equals(currentVer)) {
            try {
                String releaseNotes = ConfigManager.getLatestReleaseNotes();

                showUpdateDialog(currentVer, latestVer, releaseNotes);

                Path tempJar = Paths.get("TestField_new.jar");
                ConfigManager.downloadNewVersion(tempJar);
                restartAndApply(tempJar);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        SwingUtilities.invokeLater(JavaVideoDownloader::new);
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

    private static void showUpdateDialog(String currentVer, String latestVer, String releaseNotes) {
        String notesHtml = (releaseNotes != null && !releaseNotes.isBlank())
                ? releaseNotes
                : "<p>No release notes available.</p>";

        String fullHtml = "<html><head><style>" +
                "body { font-family: Segoe UI, sans-serif; font-size: 13px; margin: 10px; color: #222; }" +
                "h2 { color: #1a73e8; margin-top: 0; margin-bottom: 4px; }" +
                "h1, h3 { color: #1a73e8; margin-top: 8px; }" +
                "ul { padding-left: 18px; margin: 4px 0; }" +
                "li { margin-bottom: 2px; }" +
                "code { background: #f0f0f0; padding: 1px 4px; border-radius: 3px; font-family: Consolas, monospace; }" +
                "blockquote { border-left: 3px solid #1a73e8; margin: 6px 0; padding-left: 10px; color: #555; }" +
                "strong { font-weight: bold; }" +
                "em { font-style: italic; }" +
                "</style></head><body>" +
                "<h2>New Update is here!</h2>" +
                "<p><b>Version:</b> " + currentVer + " → " + latestVer + "</p>" +
                "<hr style='border: none; border-top: 1px solid #ddd; margin: 8px 0;'/>" +
                "<b>What's new:</b><br/>" +
                notesHtml +
                "</body></html>";

        JEditorPane editorPane = new JEditorPane("text/html", fullHtml);
        editorPane.setCaret(new javax.swing.text.DefaultCaret() {
            @Override
            public void paint(Graphics g) {}

            @Override
            public boolean isVisible() { return false; }
        });
        editorPane.setEditable(false);
        editorPane.setOpaque(false);
        editorPane.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        editorPane.setHighlighter(null);

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(450, 400));

        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(26, 115, 232);
                this.trackColor = new Color(240, 240, 240);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return invisibleButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return invisibleButton();
            }

            private JButton invisibleButton() {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(0, 0));
                btn.setMinimumSize(new Dimension(0, 0));
                btn.setMaximumSize(new Dimension(0, 0));
                return btn;
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(thumbColor);
                g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
                        thumbBounds.width - 4, thumbBounds.height - 4, 8, 8);
                g2.dispose();
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(trackColor);
                g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
                g2.dispose();
            }
        });
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JButton okButton = new JButton("Update Now");
        okButton.setBackground(new java.awt.Color(101, 153, 243));
        okButton.setForeground(Color.WHITE);
        okButton.setFocusPainted(false);
        okButton.setBorderPainted(false);
        okButton.setOpaque(true);
        okButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JDialog dialog = new JDialog();
        dialog.setTitle("Update Available");
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);

        okButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPanel.add(okButton);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);


        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                scrollPane.getVerticalScrollBar().setValue(0);
            }
        });
        dialog.setVisible(true);
    }
}