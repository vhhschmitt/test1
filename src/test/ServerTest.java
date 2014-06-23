package test;

import netzwerkkommunikation.MultiServer;
import netzwerkkommunikation.MultiServer.ServerListener;

/**
 *
 * @author jschmitt
 */
public class ServerTest implements ServerListener {

    private MultiServer multiServer;
    private String name;

    public ServerTest(String serverName) {
        multiServer = new MultiServer(serverName, this);
        name=serverName;
        multiServer.starteServer();
    }

    @Override
    public void getMessage(String clientName, String message) {
        System.out.println("ServerTest: Nachricht von " + clientName + ": " + message);
        multiServer.sendeAnAlle("Server "+name+" "+message);
    }

    public boolean sendeNachricht(String message, String client) {
        return multiServer.sendeNachricht(message, client);
    }

    void sendeAnAlle(String message) {
        multiServer.sendeAnAlle(message);
    }

    public void gibInfo() {
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
        }
        System.out.println(multiServer.gibInfo());
    }

    public void stoppeServer() {
        multiServer.stoppeServer();
    }
    
    public void starteServer(){
        multiServer.starteServer();
    }
}
