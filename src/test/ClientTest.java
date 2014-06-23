/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import netzwerkkommunikation.NetworkClient;
import netzwerkkommunikation.NetworkClient.ClientListener;
import netzwerkkommunikation.MultiServer;

/**
 *
 * @author jschmitt
 */
public class ClientTest implements ClientListener {

    NetworkClient client;

    public ClientTest(String clientName, String serverName) {
        client = new NetworkClient(clientName, serverName);

    }

    public void verbinde() {
        if (client.verbindeMitServer()) {
            System.out.println("Verbindungsaufbau erfolgreich");
        } else {
            System.out.println("Verbindungsaufbau nicht erfolgreich");
        }
    }

    @Override
    public void getMessage(String message) {
        System.out.println("Nachricht vom Server: " + message);
    }

    public void sendMessage(String message) {
        client.sendeNachricht(message);

    }
    
    public void trennen(){
        client.trenneServer();
    }
}
