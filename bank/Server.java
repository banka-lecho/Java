package info.kgeorgiy.ja.Shpileva.bank;


import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Server {
    private final static int DEFAULT_PORT = 8819;

    public static void main(String[] args) {
        final int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        Bank bank;
        try {
            bank = new RemoteBank(port);
            Bank stub = (Bank) UnicastRemoteObject.exportObject(bank, port);
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind("bank", stub);
            System.out.println("Bank starts");
        } catch (RemoteException e) {
            System.out.println("Cannot export object: " + e.getMessage());
        }
    }
}
