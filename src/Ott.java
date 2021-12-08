import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

public class Ott implements Runnable {

    private Integer id;
    private InetAddress ip;
    private Integer port;
    private boolean running;
    private Table neighbors;
    private DatagramSocket socket;
    private Requests requests;
    private AddressingTable addressingTable;

    private byte[] buffer = new byte[Constants.DEFAULT_BUFFER_SIZE];


    // ------------------------------ CONSTRUCTORS ------------------------------

    public Ott(Integer id, InetAddress ip, Integer port){
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.neighbors = new Table();
        this.requests = new Requests();
        this.addressingTable = new AddressingTable();
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

            // Sending it's addressingTable info to it's neighbors - DVA

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


    public void insertNeighborsInAddressingTable(){
        for (NodeInfo n : this.neighbors.getNeighborNodes()){
            this.addressingTable.setDistance(n.getNodeId(), n.getNodeId(), 1);
        }
    }


    // For each neighbor, sends the addressingTable
    public void sendAddressingTable(){
        byte[] data = Table.serialize(this.addressingTable);  //todo passar o serialize para fora do table

        for (NodeInfo n : this.neighbors.getNeighborNodes()){
            sendPacket(data, 2, 1, this.id, n.getNodeIp() , n.getNodePort());
        }
    }

    // For each neighbor except one, sends the addressingTable
    public void sendAddressingTable(Integer excludedNodeId){
        byte[] data = Table.serialize(this.addressingTable);

        for (NodeInfo n : this.neighbors.getNeighborNodes()){
            if(n.getNodeId()!= excludedNodeId)
                sendPacket(data, 2, 1, this.id, n.getNodeIp() , n.getNodePort());
        }
    }



    // Receives the addressingTable and its owner id
    public boolean updateAddressingTable(AddressingTable at, Integer neighborId){
        boolean updated = false;

        for (Map.Entry<Integer, AddressingTable.MapValue> m : at.getDistanceVector().entrySet()){

            // Se o nodo já existe na tabela de endereçamento
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
            System.out.println(">> Packet received.");
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
                this.neighbors = (Table) Table.deserialize(data);
                System.out.println("Received neighbors information.");

                // Inserts its neighbors in the addressing table
                insertNeighborsInAddressingTable();

                // Sends to each neighbor the addressing table
                sendAddressingTable();

                break;

            case 2: //Receives addressingTable information from a neighbor

                byte[] addrdata = packetReceived.getPayload();
                AddressingTable at = (AddressingTable) Table.deserialize(addrdata);

                // Update addressing table
                boolean updated = updateAddressingTable(at, packetReceived.getSenderId());

                // If the addressing table changed, notify the neighbors
                if(updated)
                    sendAddressingTable(packetReceived.getSenderId());

                break;

            case 5: //Node receives a 'Is alive check'
                // Sends a Im alive signal
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
                ", requests=" + requests +
                '}';
    }
}
