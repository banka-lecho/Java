package info.kgeorgiy.ja.Shpileva.hello;

import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class Utils {
    public static void shutdownWorkers(ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
                System.err.println("Executor service did not terminate within 60 seconds.");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static int getArgumentValue(String argument) {
        try {
            return Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("All arguments must be integer values:" + e.getMessage());
        }
    }

    public static void validate(String[] args, int argumentsNumber) {
        if (args == null || args.length != argumentsNumber) {
            throw new IllegalArgumentException(
                    "Invalid input arguments: there are should be " + argumentsNumber + " arguments"
            );
        }
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("None of the arguments must be null.");
        }
    }

    public static String getPacketData(DatagramPacket packet) {
        return "Hello, " + new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    public static void sendData(DatagramSocket socket, DatagramPacket packet, String request) throws IOException {
        final byte[] buffer = request.getBytes(StandardCharsets.UTF_8);
        setPacketData(packet, buffer);
        socket.send(packet);
    }

    public static void receiveData(DatagramSocket socket, DatagramPacket packet, byte[] buffer) throws IOException {
        setPacketData(packet, buffer);
        socket.receive(packet);
    }

    private static void setPacketData(DatagramPacket packet, byte[] buffer) {
        packet.setData(buffer);
        packet.setLength(buffer.length);
    }
}
