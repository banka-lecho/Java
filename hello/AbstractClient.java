package info.kgeorgiy.ja.Shpileva.hello;

public class AbstractClient {
    private static final int ARGUMENTS_NUMBER = 5;

    public static void main(String[] args, int mode) {
        Utils.validate(args, ARGUMENTS_NUMBER);
        int threads = Utils.getArgumentValue(args[3]);
        int requests = Utils.getArgumentValue(args[4]);
        int port = Utils.getArgumentValue(args[1]);
        if (mode == 0) {
            new HelloUDPClient().run(args[0], port, args[2], threads, requests);
        } else {
            new HelloUDPNonblockingClient().run(args[0], port, args[2], threads, requests);
        }
    }
}
