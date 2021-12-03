import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Ott implements Runnable {

    private Integer id;
    private InetAddress ip;
    private Integer port;
    private boolean running;
    //private InetAddress lastReceivedIP;
    //private Integer lastReceivedPort;
    private Table neighbors;
    //private Boolean bootstrapper;
    private DatagramSocket socket;
    private Requests requests;

    private final Lock socketLock = new ReentrantLock();
    private byte[] buffer = new byte[Constants.DEFAULT_BUFFER_SIZE];


    // ------------------------------ CONSTRUCTORS ------------------------------

    public Ott(Integer id, InetAddress ip, Integer port){
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.neighbors = new Table();
        this.requests = new Requests();


        try{
            this.socket = new DatagramSocket(this.port);
        } catch (IOException e){
            e.printStackTrace();
        }
    }


    // ------------------------------ GETTERS AND SETTERS ------------------------------

    public InetAddress getIp() {
        return ip;
    }

    public void setIp(InetAddress ip) {
        this.ip = ip;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Table getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(Table neighbors) {
        this.neighbors = neighbors;
    }







    // ------------------------------ OTHER METHODS ------------------------------

    public void run() {
        try{
            this.running = false;
            this.socket = new DatagramSocket();
            //socket.connect(this.GatewayIP, this.GatewayPort);
            System.out.println("Node is running!");


            running = openConnection(); //Starts the connection with the bootstrapper and gets its neighbors

            while (running) {
                System.out.println("--------------------------------");
                RTPpacket receivePacket = receivePacket();

                processPacket(receivePacket);

                System.out.println("--------------------------------");
            }
            socket.close();
            System.out.println("Node is shutting down.");
        } catch (IOException e){
            e.printStackTrace();
        }
    }


    public boolean openConnection(){
        try{
            // Sends signal to start the connection
            sendPacket(new byte[0], 0, 1, this.id, InetAddress.getByName(Constants.SERVER_ADDRESS), Constants.DEFAULT_PORT);

            // Awaits until the connection is confirmed
            RTPpacket receivePacket = receivePacket();

            if( receivePacket!=null){
                processPacket(receivePacket);
                return true;
            }
            else
                return false;

        } catch (UnknownHostException e){
            e.printStackTrace();
            return false;
        }
    }


















    public void sendPacket(byte [] payload, int packetType, int sequenceNumber, int senderId, InetAddress IP, int port){
        try{
            int timeStamp = (int) (System.currentTimeMillis() / 1000);

            RTPpacket newPacket = new RTPpacket(payload, packetType, sequenceNumber, senderId, timeStamp);
            DatagramPacket packet = new DatagramPacket(newPacket.getPacket(), newPacket.getPacketSize(), IP, port);

            this.socket.send(packet);
            System.out.println(">> Sent packet:");

        } catch (IOException e){
            e.printStackTrace();
        }
    }


    public RTPpacket receivePacket(){
        RTPpacket rtpPacket = new RTPpacket();
        try{
            this.socketLock.lock();
            DatagramPacket packet = new DatagramPacket(this.buffer, this.buffer.length);
            socket.receive(packet);
            socket.setSoTimeout(0);  //removes any timeout existing
            //this.lastReceivedIP = packet.getAddress();
            //this.lastReceivedPort = packet.getPort();
            rtpPacket = new RTPpacket(this.buffer);
            System.out.println("Packet received.");
            rtpPacket.printPacketHeader();
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            this.socketLock.unlock();
        }
        return rtpPacket;
    }


    public void processPacket(RTPpacket packetReceived){

        switch (packetReceived.getPacketType()) {

            case 1: //Receives neighbors information

                byte[] data = packetReceived.getPayload();
                this.neighbors = (Table) Table.deserialize(data);
                System.out.println("Received neighbors information.");
                System.out.println(this.neighbors.toString());
                break;


            case 99: //IsAlive confirmation
                System.out.println("Node " + packetReceived.getSenderId() + " is alive.");
                break;


        }
    }

}
