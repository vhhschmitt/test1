package netzwerkkommunikation;

import java.net.*;
import java.io.*;

/**
 * Client, der mit einem Multiserver Textnachrichten austauschen kann. Der
 * Client schickt beim Verbindungsaufbau eine Broadcastnachricht mit dem Namen
 * des gewünschten Servers auf Port 3333. Bei existierendem Server wird der
 * Server eine Antwort schicken. Der Client baut mithilfe der nun bekannten
 * IP-Adresse des Servers eine TCP-Verbindung auf.
 *
 * @author Jochen Schmitt
 */
public class NetworkClient {

    private final String clientName;
    private String serverName;
    private Socket server;
    private Thread serverHandler;

    /**
     * Konstruktor zum Erstellen eines Clients\br Die Verbindung muss mit der
     * Methode verbindemitServer() aufgebaut werden
     *
     * @param clientName frei gewählter Name des Clients
     * @param serverName Name des zu verbindenden Servers
     */
    public NetworkClient(String clientName, String serverName) {
        this.clientName = clientName.toLowerCase();
        this.serverName = serverName.toLowerCase();
    }

    /**
     * eine Verbindung mit dem Server wird hergestellt\br Falls der Server nicht
     * existiert, wird die Methode nicht verlassen!
     *
     * @return true, falls Verbindung erfolgreich aufgebaut wurde, andernfalls
     * false
     */
    public boolean verbindeMitServer() {
        // UDP-Broadcastnachricht senden und InetAddress des Servers ermitteln
        InetAddress serverAddress = sendBroadcast(serverName);
        // Methode verlassen, falls kein UDP-Server erreichbar
        if (serverAddress == null) {
            return false;
        }
        server = null;

        try {
            server = new Socket(serverAddress.getHostName(), 3333);
            // neuen Thread für den Serverhandler erzeugen
            serverHandler = new ServerHandler(server);
            // Thread starten 
            serverHandler.start();

            // System.out.println("Verbindung mit " + serverAddress.getHostAddress() + " " + "hergestellt!");
        } catch (Exception e) {
            System.out.println("Kein Kontakt zum Server");
            return true;
        }

        // Eigenen Namen an den Server schicken
        sendeNachricht("#" + clientName);
        return true;
    }

    public String gibInfo() {
        String info = "--------------------------------------------\n";
        info += "Client: " + clientName;
        info += "verbunden mit " + serverName + "\n";
        info += "--------------------------------------------";
        return info;
    }

    /**
     * Eine Nachricht wird an den Server geschickt.
     *
     * @param message zu verschickende Nachricht
     * @return true beim erfolgreichen Versand der Nachricht, andernfalls false
     */
    public boolean sendeNachricht(String message) {
        OutputStream out;
        try {
            out = server.getOutputStream();
            PrintWriter writer = new PrintWriter(out);
            writer.println(message);
            writer.flush(); // Nachricht an den Server schicken
        } catch (IOException ex) {
            System.out.println("Fehler beim Versand einer Nachricht an " + serverName);
            return false;
        }
        return true;
    }

    /**
     * Verbindung zum Server wird getrennt
     */
    public void trenneServer() {
        // ServerHandler beenden
        serverHandler.interrupt();
        // Client beim Serverabmelden
        sendeNachricht("#STOP" + clientName);
        try {
            // Serversocket schließen
            server.close();

        } catch (IOException ex) {
            //Logger.getLogger(NetworkClient.class.getName()).log(Level.SEVERE, null, ex);
        }
  
    }

    /**
     * Die Methode sendet eine Broadcastnachricht mit dem Namen eines Servers
     * und wartet auf eine Antwort
     *
     * @param destinationServer selbstgewählter Name des Servers
     * @return InetAddress des Servers
     */
    private static InetAddress sendBroadcast(String destinationServer) {
        InetAddress serverAddress = null;
        try {
            serverAddress = null;
            DatagramSocket clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(1500);
            InetAddress IPAddress = InetAddress.getByName("255.255.255.255");
            byte[] sendData;
            byte[] receiveData = new byte[1024];
            // Bytearray erzeugen
            sendData = destinationServer.getBytes();

            //Datagramm senden (Name des Zielservers, Länge des Namens, eigene IP-Adresse, fester Port)
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9999);
            clientSocket.setBroadcast(true);
            clientSocket.send(sendPacket);

            //Antwort erhalten (
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            String modifiedSentence = new String(receivePacket.getData());
            serverAddress = receivePacket.getAddress();
            clientSocket.close();

        } catch (IOException ex) {
            return null;
        }
        return serverAddress;

    }

    private class ServerHandler extends Thread {

        private final Socket server;

        private ServerHandler(Socket c) {
            server = c;

        }

        @Override
        public void run() {
            // Eingabestrom vom Server
            InputStream in;
            try {
                in = server.getInputStream();

                InputStreamReader streamReader = new InputStreamReader(in);
                BufferedReader reader = new BufferedReader(streamReader);

                // Schleife für Textempfang und -versand
                String message; //Nachricht vom Server
                do {
                    message = reader.readLine();
                } while (!interrupted());

            } catch (IOException ex) {
                // Fehler beim Socket bzw. Socket von außen geschlossen
            }

        }
    }

    /**
     * Interface zum Erhalt von Nachrichten des Servers
     */
    public interface ClientListener {

        /**
         * Methode wird bei Erhalt einer Nachricht aufgerufen
         *
         * @param message übermittelte Nachricht vom Server
         */
        public void getMessage(String message);
    }
}
