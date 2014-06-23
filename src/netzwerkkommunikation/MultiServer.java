package netzwerkkommunikation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasse zur Bereitstellung eines Servers, der mit mehreren Clients eine
 * Verbindung halten kann. Der Server hört auf UDP-Nachrichten auf dem Port 9999
 * und übermittelt als Antwort seine IP-Adresse, die zum Aufbau eine
 * TCP-Verbindung auf dem Port 3333 genutzt werden kann. Intern werden die
 * übermittelten Namen der Clients zum Versand von Nachrichten verwaltet.
 *
 * @author Jochen Schmitt
 */
public class MultiServer {

    private final String serverName;
    private final ServerListener serverListener;
    private ArrayList<Thread> clientList = null;
    private UDPServer udpServer;
    private TCPServer tcpServer;

    /**
     * Konstruktor zum Erzeugen eines Multiservers
     *
     * @param serverName frei gewählter Name des Servers, unveränderbar
     * @param serverListener zu benachrichtigender Listener bei einkommenden
     * Nachrichten
     */
    public MultiServer(String serverName, ServerListener serverListener) {
        this.serverName = serverName.toLowerCase();
        this.serverListener = serverListener;
        clientList = new ArrayList<Thread>();

    }

    /**
     * Der UDP-Server zum Empfang von Broadcastnachrichten und der TCP-Server
     * zum Aufbau von TCP-Verbindungen werden gestartet
     */
    public synchronized void starteServer() {
        udpServer = new UDPServer();
        tcpServer = new TCPServer();
        //UDP-Server starten
        udpServer.start();
        // TCP-Server starten
        tcpServer.start();
    }

    public synchronized void stoppeServer() {
        // UDP- und TCP-Server stoppen
        udpServer.interrupt();
        tcpServer.interrupt();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        for (Thread clientH : clientList) {
            clientH.interrupt();
        }

        // zwei Sekunden warten, damit evt. ein interrupted Server sich beenden kann
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        clientList.clear();
    }

    /**
     * Sendet an einen Client eine Nachricht
     *
     * @param message Nachricht
     * @param clientName Name des Clients
     * @return true, wenn der Client existiert, andernfalls false
     */
    public synchronized boolean sendeNachricht(String message, String clientName) {
        for (Thread clientH : clientList) {
            if (((ClientHandler) clientH).getClientName().equalsIgnoreCase(clientName)) {
                ((ClientHandler) clientH).sendMessage(message);
                return true;
            }
        }
        // Client existiert nicht
        return false;
    }

    /**
     * Entfernt einen Client aus der Liste
     *
     * @param clientName
     */
    private synchronized void removeClient(String clientName) {
        int i = 0;
        while (i < clientList.size()) {
            Thread clientH = clientList.get(i);
            clientH.interrupt();
            if (((ClientHandler) clientH).getClientName().equalsIgnoreCase(clientName)) {
                clientList.remove(i);
            }
            i++;
        }

    }

    /**
     * Methode zum Versenden von Nachrichten an alle verbundenen Clients
     *
     * @param message zu verschickende Nachricht
     */
    public synchronized void sendeAnAlle(String message) {
        for (Thread clientH : clientList) {
            ((ClientHandler) clientH).sendMessage(message);
        }
    }

    /**
     * gibt eine Information über den eigenen Namen sowie die Namen der
     * verbundenen Clients zurück
     *
     * @return Infotext
     */
    public synchronized String gibInfo() {
        String info = "--------------------------------------------\n";
        info += "Servername: " + serverName + "\nClients:\n";
        for (Thread clientH : clientList) {
            info += ((ClientHandler) clientH).getClientName() + "\n";
        }
        info += "--------------------------------------------";
        return info;

    }

    public class ClientHandler extends Thread {

        private String clientName;
        Socket client;
        InputStream in;
        OutputStream out;
        InputStreamReader streamReader;
        BufferedReader reader;
        PrintWriter writer;

        private ClientHandler(String clName, Socket c) {
            clientName = clName;
            client = c;

            try {

                in = client.getInputStream();
                streamReader = new InputStreamReader(in);
                reader = new BufferedReader(streamReader);
                out = client.getOutputStream();
                writer = new PrintWriter(out);

            } catch (IOException ex) {
                System.out.println("Fehler beim ClientHandler");
                System.out.println(ex);
            }
        }

        private String getClientName() {
            return clientName;
        }

        @Override
        public void run() {

            // Eingabestrom vom Client
            try {
                // Schleife für Textempfang 
                String message; //Nachricht vom Client
                do {
                    message = reader.readLine();
                    if (message.startsWith("#STOP")) {
                        // Client entfernen
                        removeClient(clientName);
                        // Thread beenden
                        interrupt();
                    } else if (message.startsWith("#")) {
                        // Clientname wird übermittelt
                        clientName = message.substring(1);
                    } else {
                        // Listener benachrichtigen
                        serverListener.getMessage(clientName, message);
                    }
                } while (!isInterrupted());

                client.close(); //Socket schließen
            } catch (Exception ex) {
                System.out.println("Fehler beim Client-Handler");

            }
        }

        private boolean sendMessage(String message) {
            try {
                // Ausgabestrom zum Client   
                writer.println(message);
                writer.flush();
            } catch (Exception ex) {
                System.out.println("Fehler beim Senden an: " + clientName);
                return false;
            }
            return true;
        }
    }

    /**
     * Interface zum Erhalt von Nachrichten, die von Clients an den Server
     * geschickt werden
     *
     */
    public interface ServerListener {

        /**
         * Methode wird bei Erhalt einer Nachricht aufgerufen
         *
         * @param clientName Name des Client-Rechners
         * @param message Nachricht des Clients
         */
        public void getMessage(String clientName, String message);
    }

    private class TCPServer extends Thread {

        @Override
        public void run() {

            ServerSocket server = null;
            try {
                server = new ServerSocket(3333);
                server.setSoTimeout(1000);
                while (!interrupted()) { //Server ständig lauschen lassen
                    Socket client;
                    // System.out.println("TCP-Server: Warte auf einen Client...");
                    try {
                        client = server.accept(); //Client erhält eine Verbindung
                        // System.out.println("Neuer Client: " + client.getInetAddress().getHostAddress());
                        // neuen Prozess starten, der sich um den Client kümmert
                        Thread clientHandler = new ClientHandler("ClientName", client);
                        clientHandler.start();
                        // ClientHandler in der clientList aufnehmen
                        clientList.add(clientHandler);
                    } catch (SocketTimeoutException e) {
                        // Timeout
                    }
                }

            } catch (Exception e) {

                System.out.println("Fehler beim Serverstart!");
                System.out.println(e);

            }
            if (server != null) {
                try {
                    server.close();
                    System.out.println("TCP-Server closed");
                } catch (IOException ex) {

                }
            }

        }
    }

    private class UDPServer extends Thread {

        private UDPServer() {
        }

        @Override
        public void run() {

            // System.out.println("UDP-Server: " + serverName + " gestartet.");
            DatagramSocket serverSocket = null;
            try {
                serverSocket = new DatagramSocket(9999);
                byte[] receiveData = new byte[1024];
                byte[] sendData = new byte[1024];
                while (!isInterrupted()) {
                    // System.out.println("UDP-Server wartet auf Broadcastnachricht.");
                    // Timeout nach 1s

                    serverSocket.setSoTimeout(1000);
                    //Datagramm erhalten und IP-Adresse des Hosts ermitteln
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    try {
                        serverSocket.receive(receivePacket);
                        String incomingMessage = new String(receivePacket.getData());
                        incomingMessage = incomingMessage.substring(0, receivePacket.getLength());
                    // System.out.println("RECEIVED: " + incomingMessage);

                        // Bei passendem Servernamen Antwort senden
                        if (incomingMessage.equalsIgnoreCase(serverName)) {
                            InetAddress ipAddress = receivePacket.getAddress();
                            // System.out.println("Broadcast Nachrichte von :" + ipAddress.getHostAddress());
                            int port = receivePacket.getPort();
                            //Eigenen Servername als Antwort senden
                            sendData = serverName.getBytes();
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, port);
                            serverSocket.send(sendPacket);
                        }
                    } catch (SocketTimeoutException e) {
                        // Timeout
                    }
                }

            } catch (Exception ex) {
                interrupt();

            }
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }
}
