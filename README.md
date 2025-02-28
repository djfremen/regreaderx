# RegReaderX

![RegReaderX](https://img.shields.io/badge/Version-1.5.0-blue)
![Java](https://img.shields.io/badge/Java-1.6-orange)
![Platform](https://img.shields.io/badge/Platform-Windows_XP-green)

RegReaderX is a specialized Java application for extracting registration data from PDF files and generating license keys for SAAB GlobalTIS software. It's designed to be compatible with Windows XP and Java 6 environments.

## Features

- Extract software ID and request IDs from REGISTRATION.PDF files
- Generate ID keys using standalone key generation
- Support for the Bojer Online Method via browser integration
- User-friendly interface with copy-to-clipboard functionality
- Environment variable management for GlobalTIS and Java paths
- System information display for troubleshooting

## Requirements

- Windows XP or later
- Java 6 (JRE 1.6.0_45 recommended)
- GlobalTIS installation (optional)

## Installation

1. Download the latest release JAR file from the [Releases](https://github.com/YourUsername/RegReaderX/releases) page
2. Ensure Java 6 is installed on your system
3. Run the application by double-clicking the JAR file or using:
   ```
   java -jar RegReaderX.jar
   ```

## Usage

### Generating a License Key

1. Click "Search for REGISTRATION.PDF" and select your registration PDF file
2. The application will extract the software key and request ID automatically
3. A license key will be generated and displayed
4. Click "Copy" next to any field to copy the value to your clipboard

### Using the Bojer Online Method

1. Load a registration PDF file as described above
2. Click the "Use Bojer Online Method" button
3. Your default web browser will open with the software key pre-loaded in the URL
4. Follow the instructions on the website to complete key generation

### Environment Variables

The application checks for two environment variables:
- `GlobalTIS`: Path to your GlobalTIS installation
- `Java`: Path to your Java installation

You can set these variables by clicking "Click here to edit variables" at the bottom of the application.

## Building from Source

### Prerequisites
- Java Development Kit (JDK) 1.6.0_45
- IntelliJ IDEA (recommended) or any Java IDE
- Apache PDFBox 1.8.16 and its dependencies

### Build Steps

1. Clone the repository:
   ```
   git clone https://github.com/djfremen/RegReaderX.git
   ```

2. Open the project in IntelliJ IDEA or your preferred IDE

3. Add the following dependencies to your classpath:
   - pdfbox-1.8.16.jar
   - fontbox-1.8.16.jar
   - commons-logging-1.1.1.jar
   - log4j-1.2.15.jar

4. Compile and build the project

5. Create a JAR file with dependencies included

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Compiled by djFremen SAABcentral, trionictuning & MHHauto
- Special thanks to Bojer & Scarymistake for their contributions

## Disclaimer

This software is provided as-is with no warranty. It is designed for legitimate users who have purchased the SAAB software and need to generate license keys for their registered copies. Use of this software to bypass licensing for software you have not purchased may violate copyright laws and is not the intended purpose of this application.
