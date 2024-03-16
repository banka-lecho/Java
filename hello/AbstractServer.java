package info.kgeorgiy.ja.Shpileva.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;

public abstract class AbstractServer {
    private static final int ARGUMENTS_NUMBER = 2;
    protected static ExecutorService poolWorkers;

    public static void main(String[] args, int mode) {
        Utils.validate(args, ARGUMENTS_NUMBER);
        int port = Utils.getArgumentValue(args[0]);
        int threads = Utils.getArgumentValue(args[1]);
        try (final HelloServer server = (mode == 0
                ? new HelloUDPServer()
                : new HelloUDPNonblockingServer())
        ) {
            server.start(port, threads);
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            reader.readLine();
        } catch (final IOException e) {
            System.err.println("Failure when reading input data: " + e.getMessage());
        }
    }
}
