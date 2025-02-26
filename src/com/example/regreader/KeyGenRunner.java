package com.example.regreader;

import org.apache.log4j.Logger;
import java.io.*;

/**
 * KeyGenRunner - Utility class to execute the StandaloneKeyGen
 * while capturing its output
 * Optimized version with improved resource management and performance
 */
public class KeyGenRunner {
    private static final Logger log = Logger.getLogger(KeyGenRunner.class);

    public static String runKeyGen(String softwareKey, String requestId) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024); // Pre-allocate buffer size
        PrintStream capturingStream = null;

        try {
            log.info("Running KeyGen with software key: " + softwareKey);

            capturingStream = new PrintStream(outputStream);

            // Set output capture
            System.setOut(capturingStream);

            // Run StandaloneKeyGen
            String[] args = new String[]{softwareKey};
            StandaloneKeyGen.main(args);

            // Get output
            String output = outputStream.toString();
            if (log.isDebugEnabled()) {
                log.debug("Raw output from StandaloneKeyGen: " + output.replace("\n", " | "));
            }

            // Parse results
            return parseKeyGenOutput(output, requestId);

        } catch (Exception e) {
            log.error("Error running KeyGen", e);
            return "Error running KeyGen: " + e.getMessage();
        } finally {
            // Important: Always restore the original System.out
            System.setOut(originalOut);

            // Close streams
            if (capturingStream != null) {
                capturingStream.close();
            }
            try {
                outputStream.close();
            } catch (IOException e) {
                log.error("Error closing streams", e);
            }
        }
    }

    /**
     * Parse the raw output from StandaloneKeyGen into a formatted result
     */
    private static String parseKeyGenOutput(String output, String requestId) {
        String[] lines = output.split("\n");

        if (lines.length >= 7) {
            StringBuilder formattedOutput = new StringBuilder(256); // Pre-allocate for efficiency

            String subscriberId = lines[0].trim();
            String licenseKey = lines[1].trim();
            String keygen = lines[2].trim();
            String hwid = lines[3].trim();
            String installType = lines[4].trim();
            String users = lines[5].trim();
            String activation = lines[6].trim();

            // Validate that we got a proper Subscriber ID (should start with T)
            if (!subscriberId.startsWith("T")) {
                log.warn("Unexpected Subscriber ID format: " + subscriberId);
            }

            // Format the output to include all the information
            formattedOutput.append("Request ID: ").append(requestId);
            formattedOutput.append("\nSubscriber ID: ").append(subscriberId);
            formattedOutput.append("\n").append(licenseKey);
            formattedOutput.append("\n").append(keygen);
            formattedOutput.append("\n").append(hwid);
            formattedOutput.append("\n").append(installType);
            formattedOutput.append("\n").append(users);
            formattedOutput.append("\n").append(activation);

            return formattedOutput.toString();
        } else {
            log.error("Unexpected KeyGen output format: " + output);
            return "Error: Unexpected KeyGen output format";
        }
    }
}