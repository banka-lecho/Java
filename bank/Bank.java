package info.kgeorgiy.ja.Shpileva.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

interface Bank extends Remote {
    Account createAccount(String passportNum, String subId) throws RemoteException;

    Account getAccount(String passportNum, String subId) throws RemoteException;

    Map<String, Account> getAccounts(String id) throws RemoteException;

    Person getPerson(String passportNum) throws RemoteException;

    Person getLocalPerson(String passportNum) throws RemoteException;

    Person createPerson(String firstName, String lastName, String passportNum) throws RemoteException;
}
