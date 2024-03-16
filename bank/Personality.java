package info.kgeorgiy.ja.Shpileva.bank;

import java.rmi.RemoteException;

public abstract class Personality implements Person {
    private final String firstName;
    private final String lastName;
    private final String passportNum;

    protected Personality(String firstName, String lastName, String passportNum) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.passportNum = passportNum;
    }

    public String getFirstName() throws RemoteException {
        return firstName;
    }

    public String getLastName() throws RemoteException {
        return lastName;
    }

    public String getPassportNumber() throws RemoteException {
        return passportNum;
    }
}
