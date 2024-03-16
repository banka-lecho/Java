package info.kgeorgiy.ja.Shpileva.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import static java.util.Arrays.asList;

public class RemoteBank implements Bank {
    private final int port;
    private final Map<String, Person> persons = new HashMap<>();
    private final Map<String, Map<String, Account>> accounts = new HashMap<>();

    public RemoteBank(final int port) {
        this.port = port;
    }

    @Override
    public Account createAccount(String passportNum, String subId) throws RemoteException {
        if (checkArguments(passportNum, subId)) {
            System.err.println("Null arguments");
            return null;
        }

        if (persons.get(passportNum) == null) {
            System.err.println("This person does not exist");
            return null;
        }

        Account acc = new RemoteAccount(passportNum, subId, 0);
        Map<String, Account> accsOfPerson = new HashMap<>();
        if (accounts.get(passportNum) != null) {
            accsOfPerson = accounts.get(passportNum);
        }
        if (accsOfPerson.containsKey(subId)) {
            System.err.println("This account already exists");
            return null;
        }
        accsOfPerson.put(subId, acc);
        accounts.put(passportNum, accsOfPerson);
        UnicastRemoteObject.exportObject(acc, port);
        System.out.println("Account of person was successfully created");
        return acc;
    }

    @Override
    public Account getAccount(String passportNum, String subId) throws RemoteException {
        if (checkArguments(passportNum, subId)) {
            System.err.println("Null arguments");
            return null;
        }
        Map<String, Account> accs = accounts.get(passportNum);
        if (accs != null) {
            return accs.get(subId);
        }
        return null;
    }

    @Override
    public Person getPerson(String passportNum) throws RemoteException {
        if (checkArguments(passportNum)) {
            System.err.println("Null arguments");
            return null;
        }
        Person person = persons.get(passportNum);
        if (person == null) {
            System.out.println("This person does not exist in the database of bank");
            return null;
        } else {
            return person;
        }
    }

    @Override
    public Person getLocalPerson(String passportNum) throws RemoteException {
        Person person = persons.get(passportNum);
        if (person == null) {
            return null;
        }
        String firstName = person.getFirstName();
        String lastName = person.getLastName();
        return new LocalPerson(firstName, lastName, passportNum);
    }

    @Override
    public Person createPerson(String firstName, String lastName, String passportNum) throws RemoteException {
        Person person = new RemotePerson(firstName, lastName, passportNum);
        if (checkArguments(firstName, lastName, passportNum)) {
            System.err.println("Wrong arguments");
            return null;
        }
        if (persons.putIfAbsent(passportNum, person) != null) {
            System.out.println("This person already exists");
            if (firstName.equals(person.getFirstName()) && lastName.equals(person.getLastName())) {
                return person;
            }
            return null;
        }
        persons.put(passportNum, person);
        System.out.println("Person was successfully created");
        return person;
    }

    @Override
    public Map<String, Account> getAccounts(String passportNum) throws RemoteException {
        System.out.println("Account of person were successfully found");
        return accounts.get(passportNum);
    }

    private boolean checkArguments(String... args) {
        return asList(args).contains(null);
    }
}
