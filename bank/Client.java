package info.kgeorgiy.ja.Shpileva.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;


public class Client {
    public static void main(String... args) throws RemoteException {
        Bank bank;
        try {
            bank = (Bank) Naming.lookup("//localhost/bank");
        } catch (NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        } catch (MalformedURLException e) {
            System.out.println("Bank URL is invalid");
            return;
        }

        final String firstName = args[0];
        final String lastName = args[1];
        final String passportNum = args[2];
        final String agreementNum = args[3];
        int valueOfChange = Integer.parseInt(args[4]);

        Person person = bank.getLocalPerson(passportNum);
        if (person == null) {
            bank.createPerson(firstName, lastName, passportNum);
            System.out.println("Person was created");
        } else {
            System.out.println("Person exists");
        }

        String subId = passportNum + ":" + agreementNum;
        Account account = bank.getAccount(passportNum, subId);
        if (account == null) {
            System.out.println("Creating account");
            account = bank.createAccount(passportNum, subId);
        } else {
            System.out.println("Account already exists");
        }

        System.out.println("Account id: " + account.getSubId());
        System.out.println("Money of one certain agreement: " + account.getAmount());
        System.out.println("Adding money");
        account.setAmount(account.getAmount() + valueOfChange);
        System.out.println("Money: " + account.getAmount());
    }
}
