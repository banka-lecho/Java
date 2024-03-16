package info.kgeorgiy.ja.Shpileva.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Walk {
    private static final String ZERO_HASH = "0".repeat(64);

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("There are should be 2 arguments");
            return;
        }
        if (args[0] == null || args[1] == null) {
            System.err.println("Input/Output path should not be null");
            return;
        }
        try {
            Path outputPath = Path.of(args[1]);
            if (Files.notExists(outputPath)) {
                Path outputParent = outputPath.getParent();
                try {
                    if (outputParent != null) {
                        Files.createDirectories(outputParent);
                    }
                    Files.createFile(outputPath);
                } catch (IOException | InvalidPathException | SecurityException | FileSystemNotFoundException e) {
                    System.err.println("Failed to create output file " + e.getMessage());
                    return;
                }
            }
            try (BufferedReader br = Files.newBufferedReader(Path.of(args[0]), StandardCharsets.UTF_8)) {
                try (BufferedWriter wr = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
                    String nextFile = br.readLine();
                    try {
                        MessageDigest md = MessageDigest.getInstance("SHA-256");
                        while (nextFile != null) {
                            String hash = getSHA(new File(nextFile), md);
                            wr.write(hash + " " + nextFile);
                            wr.newLine();
                            nextFile = br.readLine();
                        }
                    } catch (NoSuchAlgorithmException | InvalidPathException | SecurityException |
                             FileSystemNotFoundException e) {
                        System.err.println("Oops, wrong algorithm name. " + e);
                    }
                } catch (IOException | InvalidPathException | SecurityException | FileSystemNotFoundException e) {
                    System.err.println("Can not write result to the file. " + e);
                }
            } catch (IOException | InvalidPathException | SecurityException | FileSystemNotFoundException e) {
                System.err.println("Can not read input list of files. " + e);
            }
        } catch (InvalidPathException | SecurityException | FileSystemNotFoundException e) {
            System.err.println("Incorrect path to the output file: " + args[1]);
        }
    }

    public static String getSHA(File file, MessageDigest digest) {
        digest.reset();
        // :NOTE: nio and io
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[1024];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        } catch (IOException | InvalidPathException | SecurityException | FileSystemNotFoundException e) {
            System.err.println("Failed to process file \"" + file + "\". Error_msg: " + e.getMessage());
            return ZERO_HASH;
        }
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }
}