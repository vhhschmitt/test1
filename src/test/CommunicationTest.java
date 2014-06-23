package test;

/**
 *
 * @author jschmitt
 */
public class CommunicationTest {

    public static void main(String[] args) {
        // ServerTest mit Multiserver "ServerA" starten

        ServerTest serverTest = new ServerTest("ServerA");
        serverTest.gibInfo();
        ClientTest clTest1 = new ClientTest("Client1", "ServerA");
        clTest1.verbinde();
        serverTest.gibInfo();
        ClientTest clTest2 = new ClientTest("Client2", "ServerA");
        clTest2.verbinde();

        serverTest.gibInfo();

        clTest1.sendMessage("Hi");
        clTest1.sendMessage("#clientA");

        System.out.println("Nachricht 'antwort' an Client1, erfolgreich: "
                + serverTest.sendeNachricht("antwort", "Client1"));
        serverTest.sendeAnAlle("Nachricht an alle!");

        serverTest.gibInfo();
        clTest1.sendMessage("hi schluss");
        clTest1.trennen();
        clTest2.sendMessage("hallo von client2");
        serverTest.gibInfo();
        serverTest.stoppeServer();
        serverTest.gibInfo();
        clTest2.sendMessage("hi");
        serverTest.gibInfo();
        clTest2.trennen();

        serverTest.starteServer();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        clTest1 = new ClientTest("Client1", "ServerA");
        clTest1.verbinde();
        serverTest.gibInfo();
        clTest2 = new ClientTest("Client2", "ServerA");
        clTest2.verbinde();
       try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        serverTest.sendeAnAlle("nachricht an alle");
        serverTest.gibInfo();
        clTest1.sendMessage("Nachricht von client 1 nach Neuinstanziierung");
        serverTest.stoppeServer();
        clTest1.trennen();
        clTest2.trennen();

    }
}
