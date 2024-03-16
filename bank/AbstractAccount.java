package info.kgeorgiy.ja.Shpileva.bank;

import java.rmi.RemoteException;

public abstract class AbstractAccount implements Account {
    String subId;
    String passportNum;
    Integer amountOfSubId;

    protected AbstractAccount(String passportNum, String subId, Integer amountOfSubId) {
        this.passportNum = passportNum;
        this.subId = subId;
        this.amountOfSubId = amountOfSubId;
    }

    @Override
    public String getSubId() throws RemoteException {
        return subId;
    }

    @Override
    public int getAmount() throws RemoteException {
        return amountOfSubId;
    }

    @Override
    public void setAmount(int amount) throws RemoteException {
        amountOfSubId += amount;
    }
}
