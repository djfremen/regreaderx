package com.example.regreader;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;
import java.io.*;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.log4j.Logger;

/**
 * RegReaderX - Optimized version for Windows XP / Java 6
 * Application to extract registration data from PDF files
 * and generate license keys for SAAB software.
 */
public class RegReaderX extends JFrame {
    private static final Logger log = Logger.getLogger(RegReaderX.class);

    // Application version
    private static final String APP_VERSION = "1.5.4";

    // Cached UI components
    private final JButton selectPdfButton;
    private final JButton onlineMethodButton;
    private final JPanel outputPanel;
    private final JTextArea additionalInfoArea;
    private final JTextArea systemInfoArea;
    private final JLabel globalTisLabel;
    private final JLabel javaEnvLabel;

    // Cached patterns for better performance
    private static final String KEY_PATTERN = "[0-9A-Z]{4}-[0-9A-Z]{4}-[0-9A-Z]{4}-[0-9A-Z]{4}-" +
            "[0-9A-Z]{4}-[0-9A-Z]{4}-[0-9A-Z]{4}-[0-9A-Z]{4}";
    private static final Pattern KEY_REGEX = Pattern.compile("Software Key:\\s*(" + KEY_PATTERN + "(?:\\s*-\\s*" + KEY_PATTERN + ")?)");
    private static final Pattern PLAIN_KEY_REGEX = Pattern.compile("(" + KEY_PATTERN + "(?:\\s*-\\s*" + KEY_PATTERN + ")?)");
    private static final Pattern REQUEST_ID_REGEX = Pattern.compile("Request\\s*ID:\\s*(R\\d{9})", Pattern.CASE_INSENSITIVE);
    private static final Pattern REQUEST_ID_ALT_REGEX = Pattern.compile("RequestID:\\s*(R\\d{9})", Pattern.CASE_INSENSITIVE);

    // Thread pool for background tasks
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Constants - Increased height by 25%
    private static final int FRAME_WIDTH = 950;
    private static final int FRAME_HEIGHT = 725; // 580 * 1.25 = 725

    // Store current software key for online method
    private String currentSoftwareKey = null;

    public RegReaderX() {
        // Set up the window
        setTitle("RegReaderX v" + APP_VERSION);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setMinimumSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));

        log.info("Starting RegReaderX v" + APP_VERSION);

        // Top panel with search button and online method button
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 10, 0)); // Equal width buttons with 10px gap
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Add padding

        // Search PDF button
        selectPdfButton = new JButton("Search for REGISTRATION.PDF");
        selectPdfButton.setFont(new Font("Arial", Font.PLAIN, 16));
        selectPdfButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchForKey();
            }
        });

        // Online method button (initially disabled)
        onlineMethodButton = new JButton("Use Bojer Online Method");
        onlineMethodButton.setFont(new Font("Arial", Font.BOLD, 14));
        onlineMethodButton.setBackground(new Color(173, 216, 230)); // Light blue
        onlineMethodButton.setForeground(Color.BLACK);
        onlineMethodButton.setEnabled(false); // Initially disabled
        onlineMethodButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openOnlineMethod();
            }
        });

        topPanel.add(selectPdfButton);
        topPanel.add(onlineMethodButton);
        add(topPanel, BorderLayout.NORTH);

        // Main panel with output areas
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Panel for the key fields with copy buttons
        outputPanel = new JPanel();
        outputPanel.setLayout(new BoxLayout(outputPanel, BoxLayout.Y_AXIS));
        JPanel outputWrapperPanel = new JPanel(new BorderLayout());
        outputWrapperPanel.add(outputPanel, BorderLayout.NORTH);

        // Text areas for additional info and system info - custom weight distribution
        JPanel infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.weightx = 1.0;

        // Text area for additional information - 50% smaller
        JPanel additionalInfoPanel = new JPanel(new BorderLayout());
        additionalInfoPanel.setBorder(BorderFactory.createTitledBorder("Additional Information"));
        additionalInfoArea = new JTextArea(3, 50); // Height reduced by ~50%
        additionalInfoArea.setEditable(false);
        additionalInfoArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        additionalInfoArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        additionalInfoPanel.add(new JScrollPane(additionalInfoArea), BorderLayout.CENTER);

        // Text area for system information - 20% smaller
        JPanel systemInfoPanel = new JPanel(new BorderLayout());
        systemInfoPanel.setBorder(BorderFactory.createTitledBorder("System Information"));
        systemInfoArea = new JTextArea(5, 50); // Height reduced by ~20%
        systemInfoArea.setEditable(false);
        systemInfoArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        systemInfoArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        systemInfoPanel.add(new JScrollPane(systemInfoArea), BorderLayout.CENTER);

        // Add panels with weights
        gbc.weighty = 0.4; // Additional info takes 40% of the space
        gbc.gridx = 0;
        gbc.gridy = 0;
        infoPanel.add(additionalInfoPanel, gbc);

        gbc.weighty = 0.6; // System info takes 60% of the space
        gbc.gridy = 1;
        infoPanel.add(systemInfoPanel, gbc);

        mainPanel.add(outputWrapperPanel, BorderLayout.NORTH);
        mainPanel.add(infoPanel, BorderLayout.CENTER);

        // Add empty border around the main panel
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(mainPanel, BorderLayout.CENTER);

        // Bottom panel with environment info and edit button
        JPanel bottomPanel = new JPanel(new GridLayout(3, 1));
        globalTisLabel = new JLabel();
        globalTisLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        javaEnvLabel = new JLabel();
        javaEnvLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JButton editEnvButton = new JButton("Click here to edit variables");
        editEnvButton.setFont(new Font("Arial", Font.PLAIN, 16));
        editEnvButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                editEnvVars();
            }
        });
        bottomPanel.add(globalTisLabel);
        bottomPanel.add(javaEnvLabel);
        bottomPanel.add(editEnvButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // Credits label
        JLabel creditsLabel = new JLabel("compiled by djFremen SAABcentral, trionictuning & MHHauto. Special Thanks Bojer & Scarymistake", SwingConstants.CENTER);
        creditsLabel.setForeground(Color.BLUE);
        creditsLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        add(creditsLabel, BorderLayout.PAGE_END);

        refreshEnvInfo();
        updateSystemInfo();
        setLocationRelativeTo(null);

        // Add window listener to clean up resources
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                executorService.shutdown();
            }
        });
    }

    private void updateSystemInfo() {
        StringBuilder info = new StringBuilder();

        // Get GlobalTIS installation directory
        String globalTisDir = findGlobalTISDirectory();
        info.append("GlobalTIS Installation Directory:\n");
        info.append(globalTisDir != null ? globalTisDir : "Not found");

        // Get Java environment variables without extra space
        info.append("\nJava Environment Variables:\n");
        String javaHome = System.getenv("JAVA_HOME");
        info.append("JAVA_HOME: ").append(javaHome != null ? javaHome : "Not set").append("\n");

        String javaPath = System.getenv("PATH");
        if (javaPath != null && javaPath.toLowerCase().contains("java")) {
            info.append("Java in PATH: Yes");
        } else {
            info.append("Java in PATH: No or not detected");
        }

        // Java version without extra space
        info.append("\nJava version: ").append(System.getProperty("java.version"));

        systemInfoArea.setText(info.toString());
    }

    /**
     * Find GlobalTIS directory both in registry and Program Files
     */
    private String findGlobalTISDirectory() {
        // First try registry
        String regPath = getGlobalTISInstallDir();
        if (regPath != null) {
            return regPath;
        }

        // If not found in registry, check Program Files directories
        File programFiles = new File("C:\\Program Files\\GlobalTIS");
        if (programFiles.exists() && programFiles.isDirectory()) {
            return programFiles.getAbsolutePath();
        }

        File programFilesX86 = new File("C:\\Program Files (x86)\\GlobalTIS");
        if (programFilesX86.exists() && programFilesX86.isDirectory()) {
            return programFilesX86.getAbsolutePath();
        }

        return null;
    }

    private String getGlobalTISInstallDir() {
        BufferedReader reader = null;
        try {
            // Try primary registry path
            Process process = Runtime.getRuntime().exec("reg query \"HKEY_LOCAL_MACHINE\\SOFTWARE\\GlobalTIS\" /v InstallPath");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String result = findRegistryValue(reader);
            if (result != null) return result;

            // Try alternative registry path
            process = Runtime.getRuntime().exec("reg query \"HKEY_LOCAL_MACHINE\\SOFTWARE\\Wow6432Node\\GlobalTIS\" /v InstallPath");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return findRegistryValue(reader);
        } catch (Exception e) {
            log.error("Error reading registry for GlobalTIS install directory", e);
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.error("Error closing reader", e);
                }
            }
        }
    }

    private String findRegistryValue(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("InstallPath")) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 3) {
                    return parts[parts.length - 1];
                }
            }
        }
        return null;
    }

    private void refreshEnvInfo() {
        String globalTis = System.getenv("GlobalTIS");
        String javaEnv = System.getenv("Java");

        globalTisLabel.setText("Environment Variable GlobalTIS: " +
                (globalTis != null ? globalTis : "!! NOT SET !!"));
        globalTisLabel.setForeground(globalTis != null ? Color.BLACK : Color.RED);
        globalTisLabel.setFont(new Font("Arial", globalTis != null ? Font.PLAIN : Font.BOLD, 14));

        javaEnvLabel.setText("Environment Variable Java: " +
                (javaEnv != null ? javaEnv : "!! NOT SET !!"));
        javaEnvLabel.setForeground(javaEnv != null ? Color.BLACK : Color.RED);
        javaEnvLabel.setFont(new Font("Arial", javaEnv != null ? Font.PLAIN : Font.BOLD, 14));
    }

    private void editEnvVars() {
        final JDialog dialog = new JDialog(this, "Edit Environment Variables", true);
        dialog.setLayout(new GridLayout(3, 2));

        JLabel globalTisPrompt = new JLabel("GlobalTIS:");
        final JTextField globalTisField = new JTextField(System.getenv("GlobalTIS") != null ? System.getenv("GlobalTIS") : "", 40);
        JLabel javaPrompt = new JLabel("Java:");
        final JTextField javaField = new JTextField(System.getenv("Java") != null ? System.getenv("Java") : "", 40);
        JButton saveButton = new JButton("Save");

        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String globalTisVal = globalTisField.getText().trim();
                String javaVal = javaField.getText().trim();
                try {
                    if (!globalTisVal.isEmpty()) {
                        Runtime.getRuntime().exec("setx GlobalTIS \"" + globalTisVal + "\"");
                    }
                    if (!javaVal.isEmpty()) {
                        Runtime.getRuntime().exec("setx Java \"" + javaVal + "\"");
                    }
                    JOptionPane.showMessageDialog(dialog, "Environment variables set. Restart the app to apply changes.");
                    dialog.dispose();
                    refreshEnvInfo();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(dialog, "Error setting variables: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        dialog.add(globalTisPrompt);
        dialog.add(globalTisField);
        dialog.add(javaPrompt);
        dialog.add(javaField);
        dialog.add(new JLabel());
        dialog.add(saveButton);
        dialog.pack();
        dialog.setVisible(true);
    }

    private void searchForKey() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".pdf");
            }
            public String getDescription() {
                return "PDF Files";
            }
        });

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            final File selectedFile = fileChooser.getSelectedFile();
            try {
                // Clear previous results
                outputPanel.removeAll();
                JLabel processingLabel = new JLabel("Reading PDF file, please wait...");
                outputPanel.add(processingLabel);
                additionalInfoArea.setText("");
                onlineMethodButton.setEnabled(false);
                outputPanel.revalidate();
                outputPanel.repaint();

                log.info("Selected PDF: " + selectedFile.getAbsolutePath());

                // Use executor service for background processing
                executorService.submit(new Runnable() {
                    public void run() {
                        try {
                            processPdf(selectedFile);
                        } catch (final Exception ex) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    showErrorMessage("Error: " + ex.getMessage());
                                    log.error("Error processing PDF", ex);
                                }
                            });
                        }
                    }
                });
            } catch (Exception e) {
                showErrorMessage("Error: " + e.getMessage());
                log.error("Error processing PDF", e);
            }
        }
    }

    private void processPdf(File pdfFile) {
        try {
            String text = extractTextFromPdf(pdfFile);
            if (text == null) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        showErrorMessage("Registration PDF format is invalid. Please regenerate new Registration.PDF and try again.");
                    }
                });
                return;
            }

            // Extract software key
            final String softwareKey = extractSoftwareKey(text);
            if (softwareKey == null || softwareKey.length() != 64) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        showErrorMessage("Invalid software key" + (softwareKey != null ? " length: " + softwareKey.length() + " (expected 64)" : "") +
                                ". Please regenerate new Registration.PDF and try again.");
                    }
                });
                return;
            }

            // Store the current software key for online method
            currentSoftwareKey = formatKeyWithDashes(softwareKey);

            // Extract request ID
            final String requestId = extractRequestId(text);

            // Generate key
            final String generatedKey = KeyGenRunner.runKeyGen(softwareKey, requestId);

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    displayResults(generatedKey);

                    // Enable the online method button
                    onlineMethodButton.setEnabled(true);
                }
            });

        } catch (Exception e) {
            final String errorMsg = e.getMessage();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    showErrorMessage("Error processing PDF: " + errorMsg);
                }
            });
            log.error("Error processing PDF", e);
        }
    }

    /**
     * Format the key with dashes for display (XXXX-XXXX-...)
     */
    private String formatKeyWithDashes(String rawKey) {
        StringBuilder formattedKey = new StringBuilder();
        for (int i = 0; i < rawKey.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                formattedKey.append('-');
            }
            formattedKey.append(rawKey.charAt(i));
        }
        return formattedKey.toString();
    }

    /**
     * Open the browser with the online method URL
     */
    private void openOnlineMethod() {
        if (currentSoftwareKey == null || currentSoftwareKey.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No software key available. Please process a PDF file first.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String url = "https://z90.pl/s_aab/gtis.php?x=" + currentSoftwareKey;
            log.info("Opening browser with URL: " + url);

            // Try to open the default browser
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // Alternative for Windows if Desktop is not supported
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            }
        } catch (Exception ex) {
            log.error("Error opening browser", ex);
            JOptionPane.showMessageDialog(this,
                    "Could not open browser. Please manually visit:\n" +
                            "https://z90.pl/s_aab/gtis.php?x=" + currentSoftwareKey,
                    "Browser Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String extractSoftwareKey(String text) {
        // Try with label first
        Matcher keyMatcher = KEY_REGEX.matcher(text);
        if (keyMatcher.find()) {
            String key = keyMatcher.group(1).replaceAll("[^0-9A-Z]", "");
            log.info("Extracted Software Key with label: " + key);
            return key;
        }

        // Try without label
        Matcher plainKeyMatcher = PLAIN_KEY_REGEX.matcher(text);
        if (plainKeyMatcher.find()) {
            String key = plainKeyMatcher.group(1).replaceAll("[^0-9A-Z]", "");
            log.info("Extracted Software Key without label: " + key);
            return key;
        }

        return null;
    }

    private String extractRequestId(String text) {
        // Try standard format
        Matcher requestIdMatcher = REQUEST_ID_REGEX.matcher(text);
        if (requestIdMatcher.find()) {
            String requestId = requestIdMatcher.group(1);
            log.info("Extracted Request ID: " + requestId);
            return requestId;
        }

        // Try alternative format
        requestIdMatcher = REQUEST_ID_ALT_REGEX.matcher(text);
        if (requestIdMatcher.find()) {
            String requestId = requestIdMatcher.group(1);
            log.info("Extracted Request ID (alt pattern): " + requestId);
            return requestId;
        }

        log.info("No Request ID found, using default");
        return "(leave default)";
    }

    private void displayResults(String generatedKey) {
        // Parse the returned output to format it properly
        String[] lines = generatedKey.split("\n");
        outputPanel.removeAll();

        if (lines.length >= 8) {  // Expecting 8 lines with all information
            String requestId = lines[0].trim();
            String subscriberId = lines[1].trim();
            String licenseKey = lines[2].trim();
            // Skip the "Standalone KeyGen" line
            String hwid = lines[4].trim();
            String installType = lines[5].trim();
            String users = lines[6].trim();
            String activation = lines[7].trim();

            // Extract the actual values without labels
            String requestIdValue = requestId.replace("Request ID:", "").trim();
            String subscriberIdValue = subscriberId.replace("Subscriber ID:", "").trim();

            // Add each field with a copy button for the first three lines
            addFieldWithCopyButton("Request ID:", requestIdValue);
            addFieldWithCopyButton("Subscriber ID:", subscriberIdValue);
            addFieldWithCopyButton("License Key:", licenseKey);

            // Add the additional information to the text area
            StringBuilder additionalInfo = new StringBuilder();
            additionalInfo.append(hwid).append("\n");
            additionalInfo.append(installType).append("\n");
            additionalInfo.append(users).append("\n");
            additionalInfo.append(activation);

            additionalInfoArea.setText(additionalInfo.toString());
        } else {
            // If output format is unexpected, just show the raw output
            outputPanel.add(new JLabel("Unexpected output format:"));
            additionalInfoArea.setText(generatedKey);
        }

        outputPanel.revalidate();
        outputPanel.repaint();
    }

    private void addFieldWithCopyButton(String label, final String value) {
        JPanel fieldPanel = new JPanel(new BorderLayout(10, 0));
        fieldPanel.setMaximumSize(new Dimension(FRAME_WIDTH - 50, 35));

        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setFont(new Font("Arial", Font.BOLD, 14));
        fieldLabel.setPreferredSize(new Dimension(140, 25));

        JTextField fieldValue = new JTextField(value);
        fieldValue.setEditable(false);
        fieldValue.setFont(new Font("Arial", Font.PLAIN, 14));

        JButton copyButton = new JButton("Copy");
        copyButton.setPreferredSize(new Dimension(100, 30));
        copyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                StringSelection stringSelection = new StringSelection(value);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
                JOptionPane.showMessageDialog(RegReaderX.this, "Copied to clipboard", "Copy Success", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        fieldPanel.add(fieldLabel, BorderLayout.WEST);
        fieldPanel.add(fieldValue, BorderLayout.CENTER);
        fieldPanel.add(copyButton, BorderLayout.EAST);

        // Add this field panel to the output panel
        outputPanel.add(fieldPanel);
        outputPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    }

    private void showErrorMessage(String message) {
        outputPanel.removeAll();

        JPanel errorPanel = new JPanel();
        errorPanel.setLayout(new BoxLayout(errorPanel, BoxLayout.Y_AXIS));

        JLabel errorIcon = new JLabel(UIManager.getIcon("OptionPane.errorIcon"));
        errorIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel errorLabel = new JLabel(message);
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(new Font("Arial", Font.BOLD, 14));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        errorPanel.add(Box.createVerticalStrut(20));
        errorPanel.add(errorIcon);
        errorPanel.add(Box.createVerticalStrut(10));
        errorPanel.add(errorLabel);
        errorPanel.add(Box.createVerticalStrut(20));

        outputPanel.add(errorPanel);
        additionalInfoArea.setText("");
        outputPanel.revalidate();
        outputPanel.repaint();
    }

    private String extractTextFromPdf(File file) {
        PDDocument document = null;
        try {
            document = PDDocument.load(file);
            PDFTextStripper stripper = new PDFTextStripper();
            // Limit to first few pages for better performance (key info should be at start)
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(3, document.getNumberOfPages()));
            return stripper.getText(document);
        } catch (Exception e) {
            log.error("Error extracting text from PDF", e);
            return null;
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    log.error("Error closing PDF document", e);
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            // Set Nimbus look and feel
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, fall back to the Windows look and feel
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                // If Windows L&F fails, let it use the default look and feel
                log.error("Could not set look and feel", ex);
            }
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    // Set system properties to ensure proper font rendering in Java 6
                    System.setProperty("awt.useSystemAAFontSettings", "on");
                    System.setProperty("swing.aatext", "true");

                    new RegReaderX().setVisible(true);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null,
                            "Error starting application: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        });
    }
}