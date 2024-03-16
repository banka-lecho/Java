package info.kgeorgiy.ja.Shpileva.bank;

import org.junit.jupiter.api.*;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;

@DisplayName("Bank's tests")
public class TestBank {
    private static final int PORT = 8819;
    private static Bank bank;
    final private String firstName = "anastasia";
    final private String lastName = "shpileva";
    final private String passportNum = "23123";
    private static Registry registry;

    @BeforeAll
    static void beforeAll() throws RemoteException {
        registry = LocateRegistry.createRegistry(PORT);
    }

    @BeforeEach
    void beforeEach() throws RemoteException {
        bank = new RemoteBank(PORT);
        Bank stub = (Bank) UnicastRemoteObject.exportObject(bank, PORT);
        registry.rebind("bank", stub);
    }

    @Test
    public void test_01_nullArguments() throws RemoteException {
        Assertions.assertNull(bank.createPerson(null, null, null));
        Assertions.assertNull(bank.createAccount(null, null));
    }

    @Test
    public void test_02_duplicateAccount() throws RemoteException {
        bank.createPerson(firstName, lastName, passportNum);

        bank.createAccount(passportNum, "1234");
        Account acc2 = bank.createAccount(passportNum, "1234");
        Assertions.assertNull(acc2);
    }

    @Test
    public void test_03_duplicatePerson() throws RemoteException {
        bank.createPerson(firstName, lastName, passportNum);
        Person person = bank.createPerson(firstName, lastName, passportNum);
        Assertions.assertNull(person);

        bank.createPerson("stas", "bareckiy", passportNum);
        Person person2 = bank.createPerson(firstName, lastName, passportNum);
        Assertions.assertNull(person2);
    }

    @Test
    public void test_04_checkArguments() throws RemoteException {
        bank.createPerson(firstName, lastName, passportNum);
        Assertions.assertNull(bank.createPerson("shpillleva", lastName, passportNum));
    }
    @Test
    public void test_05_createRemotePerson() throws RemoteException {
        for (int i = 0; i < 1000; i++) {
            String firstName = usingUUID();
            String lastName = usingUUID();
            String passportNum = String.valueOf(i + 2);
            bank.createPerson(firstName, lastName, passportNum);
            Person person = bank.getPerson(passportNum);
            Assertions.assertAll(
                    () -> Assertions.assertEquals(lastName, person.getFirstName()),
                    () -> Assertions.assertEquals(firstName, person.getLastName()),
                    () -> Assertions.assertEquals(passportNum, person.getPassportNumber())
            );
        }
    }

    @Test
    public void test_06_getWithoutCreation() throws RemoteException {
        Assertions.assertNull(bank.getPerson(passportNum));
        String subId = "23123:3423424";
        Assertions.assertNull(bank.getAccount(passportNum, subId));
    }

    @Test
    public void test_06_createLocalPerson() throws RemoteException {
        for (int i = 0; i < 1000; i++) {
            String firstName = usingUUID();
            String lastName = usingUUID();
            String passportNum = String.valueOf(i + 2);
            Assertions.assertNotNull(bank.createPerson(firstName, lastName, passportNum));
            Person localPerson = bank.getLocalPerson(passportNum);
            Assertions.assertAll(
                    () -> Assertions.assertEquals(lastName, localPerson.getFirstName()),
                    () -> Assertions.assertEquals(firstName, localPerson.getLastName()),
                    () -> Assertions.assertEquals(passportNum, localPerson.getPassportNumber())
            );
        }
    }

    @Test
    public void test_08_changingAmount() throws RemoteException {
        for (int i = 0; i < 1000; i++) {
            String firstName = usingUUID();
            String lastName = usingUUID();
            String passportNum = String.valueOf(i + 2);
            bank.createPerson(firstName, lastName, passportNum);
            for (int j = 0; j < 10; j++) {
                String agreementNum = String.valueOf(j + 1000);
                bank.createAccount(passportNum, passportNum + ":" + agreementNum);
            }
        }

        for (int i = 0; i < 1000; i++) {
            Person person = bank.getPerson(String.valueOf(i + 2));
            String passport = person.getPassportNumber();
            for (int j = 0; j < 10; j++) {
                String subId = passport + ":" + (j + 1000);
                Account account = bank.getAccount(passport, subId);
                System.out.println("Agreement Number:" + account.getSubId() +
                        ", amount of money:" + account.getAmount());
                account.setAmount(100);
                System.out.println("Change the amount of money: amount +100");
                Assertions.assertEquals(account.getAmount(), 100);

                account.setAmount(-100);
                System.out.println("Change the amount of money: amount -100");
                Assertions.assertEquals(account.getAmount(), 0);
            }
        }
    }

    @Test
    public void test_09_getAccounts() throws RemoteException {
        for (int i = 0; i < 100; i++) {
            String passportNum = String.valueOf(i + 2);
            bank.createPerson(firstName, lastName, passportNum);
            bank.getPerson(passportNum);
            for (int j = 0; j < 10; j++) {
                String agreementNum = String.valueOf(j + 1000);
                String subId = passportNum + ":" + agreementNum;
                bank.createAccount(passportNum, subId);
            }
            Assertions.assertEquals(bank.getAccounts(passportNum).size(), 10);
        }
    }


    static String usingUUID() {
        UUID randomUUID = UUID.randomUUID();
        return randomUUID.toString().replaceAll("-", "");
    }
}
