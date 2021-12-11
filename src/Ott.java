import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Ott implements Runnable {

    private Integer id;
    private InetAddress ip;
    private Integer port;
    private boolean running;

    private Table neighbors;
    private Lock neighborsLock = new ReentrantLock();

    private DatagramSocket socket;
    private LinkedBlockingQueue<RTPpacket> packetsQueue;
    private AddressingTable addressingTable;
    private boolean isClient;



    private byte[] buffer = new byte[Constants.DEFAULT_BUFFER_SIZE];


    // ------------------------------ CONSTRUCTORS ------------------------------

    public Ott(Integer id, InetAddress ip, Integer port, boolean isClient){
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.neighbors = new Table();
        this.packetsQueue = new LinkedBlockingQueue<>();
        this.addressingTable = new AddressingTable(this.id);
        this.isClient = isClient;
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
            this.socket = new DatagramSocket(this.port);
            System.out.println("Node is running!");

            running = openConnection(); //Starts the connection with the bootstrapper and gets its neighbors


            // If it is a client, only run the client thread, which will be the main thread
            if(this.isClient) {
                //new Thread(() -> {
                Client c = new Client(this.socket);
                //}).start();

                //TODO implementar o resto da logica do cliente (pedir ao servidor, receber ott node...)
            }


            // Normal overlay node, all overlay node code goes here
            else{
                new Thread(() -> {
                    System.out.println("===> LISTENING UDP");
                    while(this.running)
                    {
                        RTPpacket receivePacket = receivePacket();
                        if(receivePacket!=null)
                            this.packetsQueue.add(receivePacket);
                    }
                }).start();

                // ---> Main thread consuming the RTPpackets
                System.out.println("===> CONSUMING UDP");
                while(this.running){
                    try{
                        RTPpacket receivePacket = this.packetsQueue.take();
                        processPacket(receivePacket);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }


                socket.close();
                System.out.println("Node is shutting down.");
            }


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


    public void insertNeighborsInAddressingTable(){
        for (NodeInfo n : this.neighbors.getNeighborNodes())
            this.addressingTable.setDistance(n.getNodeId(), n.getNodeId(), 1);
    }


    // For each neighbor, sends the addressingTable
    public void sendAddressingTable(){
        byte[] data = StaticMethods.serialize(this.addressingTable);  //todo passar o serialize para fora do table

        try {
            neighborsLock.lock();
            for (NodeInfo n : this.neighbors.getNeighborNodes())
                sendPacket(data, 2, 1, this.id, n.getNodeIp(), n.getNodePort());
        }
        finally{
                neighborsLock.unlock();
        }
    }

    // For each neighbor except one, sends the addressingTable
    public void sendAddressingTable(Integer excludedNodeId){
        System.out.println("Sending updated table to neighbors.");
        byte[] data = StaticMethods.serialize(this.addressingTable);

        try {
            neighborsLock.lock();
            for (NodeInfo n : this.neighbors.getNeighborNodes()){
                if(n.getNodeId()!= excludedNodeId){
                    sendPacket(data, 2, 1, this.id, n.getNodeIp(), n.getNodePort());
                }
            }
        }
        finally{
            neighborsLock.unlock();
        }
    }



    // Receives the addressingTable and its owner id
    public boolean updateAddressingTable(AddressingTable at, Integer neighborId){
        boolean updated = false;

        for (Map.Entry<Integer, AddressingTable.MapValue> m : at.getDistanceVector().entrySet()){

            // Se o nodo já existe na tabela de endereçamento vai comparar as distâncias
            if(this.addressingTable.containsNode(m.getKey())){
                //  Se a distancia for menor, atualizar a tabela

                int actualDistance = this.addressingTable.getDistance(m.getKey());
                int neighborDistance = at.getDistance(m.getKey());

                if(neighborDistance + 1 < actualDistance){
                    // Atualizar a tabela
                    this.addressingTable.setDistance(m.getKey(), neighborId, neighborDistance+1);
                    updated = true;
                }
            }

            else{
                this.addressingTable.setDistance(m.getKey(), neighborId, at.getDistance(m.getKey())+1);
                updated = true;
            }
        }
        if(updated)
            System.out.println("Updated addressing table!");
        else
            System.out.println("Did not update addressing table.");
        return updated;
    }














    public void sendPacket(byte [] payload, int packetType, int sequenceNumber, int senderId, InetAddress IP, int port){
        try{
            int timeStamp = (int) (System.currentTimeMillis() / 1000);

            RTPpacket newPacket = new RTPpacket(payload, packetType, sequenceNumber, senderId, timeStamp);
            DatagramPacket packet = new DatagramPacket(newPacket.getPacket(), newPacket.getPacketSize(), IP, port);

            this.socket.send(packet);
            System.out.println(">> Sent packet to IP: " + IP + "  port: " + port);
            newPacket.printPacketHeader();
        } catch (IOException e){
            e.printStackTrace();
        }
    }


    public RTPpacket receivePacket(){
        RTPpacket rtpPacket = new RTPpacket();
        try{
            DatagramPacket packet = new DatagramPacket(this.buffer, this.buffer.length);
            socket.receive(packet);
            socket.setSoTimeout(0);  //removes any timeout existing
            InetAddress fromIp = packet.getAddress();
            Integer fromPort = packet.getPort();

            rtpPacket = new RTPpacket(this.buffer, fromIp, fromPort);
            System.out.println(">> Packet received from IP: " + fromIp + "\tPort: " + fromPort);
            rtpPacket.printPacketHeader();
        } catch (IOException e){
            e.printStackTrace();
        }
        return rtpPacket;
    }


    public void processPacket(RTPpacket packetReceived){

        switch (packetReceived.getPacketType()) {



            case 1: //Receives neighbors information

                byte[] data = packetReceived.getPayload();

                try {
                    neighborsLock.lock();
                    this.neighbors = (Table) StaticMethods.deserialize(data);
                }
                finally{
                    neighborsLock.unlock();
                }

                System.out.println("-> Received neighbors information.\n");

                // Inserts its neighbors in the addressing table
                insertNeighborsInAddressingTable();

                // Sends to each neighbor the addressing table
                sendAddressingTable();
                System.out.println(this.addressingTable.toString());
                break;



            case 2: //Receives addressingTable information from a neighbor and updated addressingTable

                byte[] addrdata = packetReceived.getPayload();
                AddressingTable at = (AddressingTable) StaticMethods.deserialize(addrdata);

                // Update addressing table
                boolean updated = updateAddressingTable(at, packetReceived.getSenderId());

                // If the addressing table changed, notify the neighbors
                if(updated){
                    //sendAddressingTable(packetReceived.getSenderId());   nota: assim resolve o problema de o último nó a entrar n ficar atualizado
                    sendAddressingTable();
                    System.out.println(this.addressingTable.toString());
                }
                break;



            case 5: //Node receives a 'Is alive check'
                // Sends a Im alive signal
                System.out.println("-> Received isAlive check. Replying.\n");
                sendPacket(new byte[0], 6, 1, this.id, packetReceived.getFromIp(), packetReceived.getFromPort());
                break;
        }
    }

    @Override
    public String toString() {
        return "Ott{" +
                "id=" + id +
                ", ip=" + ip +
                ", port=" + port +
                ", running=" + running +
                ", neighbors=" + neighbors.toString() +
                ", requests=" + packetsQueue +
                '}';
    }




}
