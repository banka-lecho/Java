package info.kgeorgiy.ja.Shpileva.hello;

import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import java.util.concurrent.Executors;

import info.kgeorgiy.java.advanced.hello.HelloServer;

/**
 * @author Shpileva Anastasiia
 */
public class HelloUDPServer extends AbstractServer implements HelloServer {
    private int bufferSize;
    private DatagramSocket socket;

    @Override
    public void start(int port, int threads) {
        try {
            socket = new DatagramSocket(port);
            bufferSize = socket.getReceiveBufferSize();
        } catch (SocketException e) {
            System.err.println("Server startup failure: " + e.getMessage());
            return;
        }
        poolWorkers = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            poolWorkers.submit(this::task);
        }
    }

    private void task() {
        final byte[] buffer = new byte[bufferSize];
        final DatagramPacket packet = new DatagramPacket(buffer, bufferSize);
        while (!Thread.interrupted() && !socket.isClosed()) {
            try {
                Utils.receiveData(socket, packet, buffer);
                sendRequest(packet, Utils.getPacketData(packet));
            } catch (IOException e) {
                System.err.println("Receiving request failure: " + e.getMessage());
            }
        }
    }

    private void sendRequest(DatagramPacket packet, String request) {
        try {
            Utils.sendData(socket, packet, request);
        } catch (IOException e) {
            System.err.println("Sending response failure: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        socket.close();
        Utils.shutdownWorkers(poolWorkers);
    }

    public static void main(String[] args) {
        main(args, 0);
    }
}
