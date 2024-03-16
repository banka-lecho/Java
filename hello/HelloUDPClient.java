package info.kgeorgiy.ja.Shpileva.hello;

import java.io.IOException;

import java.net.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

import info.kgeorgiy.java.advanced.hello.HelloClient;

// :NOTE: javadoc и scripts должны лежать в корне репозитории, а не в директории java-advanced
// :NOTE: названия пакетов в java должны быть с маленькой буквы

/**
 * @author Shpileva Anastasiia
 */
public class HelloUDPClient extends AbstractClient implements HelloClient {
    private static final int SOCKET_TIMEOUT = 100;
    private static final String REQUEST_FORMAT = "%s%d_%d";

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            SocketAddress address = new InetSocketAddress(inetAddress, port);
            ExecutorService executorService = Executors.newFixedThreadPool(threads);
            IntStream.range(1, threads + 1).forEach(
                    i -> executorService.submit(() -> sendRequests(prefix, i, requests, address))
            );
            Utils.shutdownWorkers(executorService);
        } catch (UnknownHostException e) {
            System.err.println("Fail to find host IP address. Exception: " + e.getMessage());
        }
    }

    private Boolean sendOneRequest(DatagramSocket socket, DatagramPacket packet,
                                   String request, byte[] buffer) throws IOException {
        try {
            Utils.sendData(socket, packet, request);
        } catch (IOException e) {
            System.err.println("Sending request failure. Exception: " + e.getMessage());
        }
        try {
            Utils.receiveData(socket, packet, buffer);
            String response = Utils.getPacketData(packet);
            System.out.println("Received response: " + response);
            if (response.contains(request)) {
                return true;
            }
        } catch (SocketTimeoutException e) {
        } catch (IOException e) {
            System.err.println("Receiving response failure. Exception: " + e.getMessage());
        }
        return false;
    }

    private void sendRequests(String prefix, int thread, int requests, SocketAddress address) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(SOCKET_TIMEOUT);
            final int bufferSize = socket.getReceiveBufferSize();
            final byte[] buffer = new byte[bufferSize];
            final DatagramPacket packet = new DatagramPacket(buffer, bufferSize, address);
            for (int i = 1; i < requests + 1; i++) {
                final String request = String.format(REQUEST_FORMAT, prefix, thread, i);
                boolean result = false;
                while (!Thread.interrupted() && !socket.isClosed() && !result) {
                    try {
                        result = sendOneRequest(socket, packet, request, buffer);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Socket exception was caught: " + e.getMessage() + ". The thread is: " + thread);
        }
    }

    public static void main(String[] args) {
        main(args, 0);
    }
}
