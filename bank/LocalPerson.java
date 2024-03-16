package info.kgeorgiy.ja.Shpileva.bank;

import java.io.Serializable;

public class LocalPerson extends Personality implements Serializable {
    public LocalPerson(String firstName, String lastName, String passportNum) {
        super(firstName, lastName, passportNum);
    }
}