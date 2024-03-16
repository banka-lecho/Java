package info.kgeorgiy.ja.Shpileva.bank;

public class RemoteAccount extends AbstractAccount {
    protected RemoteAccount(String passportNum, String subId, int amount) {
        super(passportNum, subId, amount);
    }
}
