package com.example.regreader;

import java.util.Scanner;

public class KeyGenCLI {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("===== TIS2WEB Key Generator =====");

        try {
            System.out.print("Enter request key: ");
            String requestKey = scanner.nextLine().trim();

            if (requestKey.isEmpty()) {
                System.out.println("Error: Request key is required.");
                return;
            }

            System.out.print("Enter subscriber ID (or leave blank for random): ");
            String subscriberId = scanner.nextLine().trim();

            String[] keygenArgs;
            if (subscriberId.isEmpty()) {
                keygenArgs = new String[] { requestKey };
            } else {
                keygenArgs = new String[] { requestKey, subscriberId };
            }

            // Call our standalone keygen
            StandaloneKeyGen.main(keygenArgs);

        } catch (Exception e) {
            System.out.println("Error processing key: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}