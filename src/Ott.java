import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Ott implements Runnable {

    private Integer id;
    private InetAddress ip;
    private Integer port;
    private boolean running;
    private Integer streamerID;
    private Table neighbors;
    private Lock neighborsLock = new ReentrantLock();
    private DatagramSocket socket;
    private LinkedBlockingQueue<RTPpacket> packetsQueue;
    private AddressingTable addressingTable;
    private Lock addressingTableLock = new ReentrantLock();
    private boolean isClient;
    private LinkedBlockingQueue<RTPpacket> framesQueue;
    private boolean streaming;
    private HashSet<Integer> nodesToStreamTo;
    private Lock nodesToStreamToLock = new ReentrantLock();
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
        this.framesQueue = new LinkedBlockingQueue<>();
        this.streaming = false;
        this.nodesToStreamTo = new HashSet<>();
        this.streamerID=-1;
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


            // If it is a client, run thread to consume stream from special socket
            if(this.isClient) {
                System.out.println("===> WATCHING STREAM");
                new Thread(() -> {
                    Client c = new Client(this.framesQueue);
                    // Requesting stream to the nearest streaming node
                    requestStream();

                }).start();
            }

            // Thread to stream, consumes from framesQueue and sends to all its receivingStreamNodes
            if(!this.isClient) {

                new Thread(() -> {
                    System.out.println("===> STREAMING");
                    while(this.running){
                        //Gets the next packet to be streamed
                        RTPpacket rtp_packet = new RTPpacket();
                        try{
                            rtp_packet = framesQueue.take();
                            rtp_packet.setSenderId(this.id);
                        } catch (InterruptedException ex){
                            ex.printStackTrace();
                        }

                        streamPacket(rtp_packet);
                    }
                }).start();




                new Thread(() -> {
                    // Esta thread vai periodicamente ver se os nodos vizinhos ainda estão ativos e caso não estejam trata de os desligar
                    System.out.println("===> CHECKING IF NODES ARE ALIVE");

                    // De x em x segundos vai verificar se os servidores ainda estão vivos
                    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                    scheduler.scheduleAtFixedRate(() -> {

                        try {
                            for(int i=0; i< Constants.TRIES_UNTIL_TIMEOUT; i++){
                                // Get nodes that are flagged with 'ON'

                                Table onlineNodes;
                                try{
                                    this.neighborsLock.lock();
                                    onlineNodes = this.neighbors.getNodesWithState(NodeInfo.nodeState.ON);
                                }
                                finally {
                                    this.neighborsLock.unlock();
                                }


                                // For each online node, check if it is alive and change its state to UNKNOWN
                                for(Map.Entry<Integer, NodeInfo> e: onlineNodes.getMap().entrySet())
                                    if(e.getKey()!=Constants.SERVER_ID)
                                        sendIsAliveCheck(e.getKey(), e.getValue());

                                // Waits for the nodes to respond
                                Thread.sleep(Constants.TIMEOUT_TIME);
                            }

                            HashSet<Integer> nodesThatDied = new HashSet<>();

                            // At this point the nodes who haven't replied have their state UNKNOWN, so they are changed to OFF
                            try{
                                this.neighborsLock.lock();
                                for(Map.Entry<Integer, NodeInfo> e: this.neighbors.getMap().entrySet()){
                                    if(e.getValue().getNodeState().equals(NodeInfo.nodeState.UNKNOWN)){

                                        this.neighbors.setNodeState(e.getKey(), NodeInfo.nodeState.OFF);
                                        System.out.println("Node " + e.getKey() + " timed out.");
                                        nodesThatDied.add(e.getKey());

                                    }
                                    if(e.getValue().getNodeState().equals(NodeInfo.nodeState.CHECKED)){
                                        this.neighbors.setNodeState(e.getKey(), NodeInfo.nodeState.ON);
                                        System.out.println("Node " + e.getKey() + " is alive.");
                                    }
                                }
                            }
                            finally {
                                this.neighborsLock.unlock();
                            }

                            try{
                                this.addressingTableLock.lock();
                                for (Integer nodeId: nodesThatDied){
                                    // Verificar se o nodo era o melhor caminho para o servidor, se for tem de se pedir a stream a outro lado
                                    if( nodeId.equals(this.streamerID)){
                                        this.addressingTable.setNeighborState(nodeId, false);
                                        requestStream();
                                    }
                                    this.addressingTable.setNeighborState(nodeId, false);
                                }
                            }
                            finally {
                                this.addressingTableLock.unlock();
                            }


                            this.neighbors.printTable();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }, 0, Constants.NODE_ALIVE_CHECKING_TIME, TimeUnit.SECONDS);

                }).start();
            }


            new Thread(() -> {
                System.out.println("===> LISTENING UDP");
                while(this.running)
                {
                    RTPpacket receivePacket = receivePacket();
                    if(receivePacket!=null){

                        // Se for um pacote de streaming, nem vai processar
                        if(receivePacket.getPacketType()==26) {
                            this.streaming=true;  // it is streaming TODO POR ISTO DUMA MANEIRA MAIS ELEGANTE, VAI TAR SMP A DAR SET À  VAR
                            this.streamerID=receivePacket.getSenderId();
                            this.framesQueue.add(receivePacket);
                        }

                        else
                            this.packetsQueue.add(receivePacket);
                    }
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
        try{
            this.addressingTableLock.lock();
            for (NodeInfo n : this.neighbors.getNeighborNodes())
                this.addressingTable.setDistance(n.getNodeId(), n.getNodeId(), 1, true); //todo assumimos que começam a on?
        }
        finally {
            this.addressingTableLock.unlock();
        }
    }


    // For each neighbor, sends the addressingTable
    public void sendAddressingTable(){
        byte[] data;
        try{
            this.addressingTableLock.lock();
            data = StaticMethods.serialize(this.addressingTable);
        }
        finally {
            this.addressingTableLock.unlock();
        }

        try {
            neighborsLock.lock();
            int packetType = 2;
            // Neste caso específico se mandar um pedido tipo 2 nunca vai receber resposta, o tipo 22 exige resposta
            if(this.neighbors.getSize()==1)
                packetType = 22;

            for (NodeInfo n : this.neighbors.getNeighborNodes())
                if(n.getNodeId()!=Constants.SERVER_ID)
                    sendPacket(data, packetType, 1, this.id, n.getNodeIp(), n.getNodePort());
        }
        finally{
                neighborsLock.unlock();
        }
    }

    // Sends the addressingTable to that neighbor
    public void sendAddressingTable(Integer specificNodeId){
        System.out.println("Sending updated table to neighbors.");
        byte[] data;
        try{
            this.addressingTableLock.lock();
            data = StaticMethods.serialize(this.addressingTable);
        }
        finally {
            this.addressingTableLock.unlock();
        }

        try {
            neighborsLock.lock();
            for (NodeInfo n : this.neighbors.getNeighborNodes()){
                if(n.getNodeId()== specificNodeId){
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

        for (Map.Entry<Integer, ArrayList<AddressingTable.Value>> m : at.getDistanceVector().entrySet()){

            // Se o nodo já existe na tabela de endereçamento vai comparar as distâncias
            if(this.addressingTable.containsDestinyNode(m.getKey())){

                int actualDistance = this.addressingTable.getSpecificCost(m.getKey(), neighborId);
                int neighborDistance = at.getBestCost(m.getKey());

                // Adiciona uma nova entrada se ainda não tem nenhuma para aquele vizinho
                // ou se a distancia do vizinho + 1 é menor

                if(actualDistance<0 || neighborDistance + 1 < actualDistance){
                    // Atualizar a tabela
                    this.addressingTable.setDistance(m.getKey(), neighborId, neighborDistance+1, true);
                    updated = true;
                }
            }

            else{
                // Atualizar a tabela
                this.addressingTable.setDistance(m.getKey(), neighborId, at.getBestCost(m.getKey())+1, true);
                updated = true;
            }
        }
        if(updated)
            System.out.println("Updated addressing table!");
        else
            System.out.println("Did not update addressing table.");
        return updated;
    }


    public void updateNeighborState(int neighborId, NodeInfo.nodeState state){
        try {
            neighborsLock.lock();
            if(this.neighbors.getMap().containsKey(neighborId))
                this.neighbors.setNodeState(neighborId, state);
        }
        finally{
            neighborsLock.unlock();
        }

        boolean isOn = state.equals(NodeInfo.nodeState.ON);
        this.addressingTable.setNeighborState(neighborId, isOn);
    }


    public  void requestStream(){
        int nodeToRequestStream = this.addressingTable.getBestNextNode(Constants.SERVER_ID); // Node closest to the bootstrapper

        if(nodeToRequestStream<0)
            System.out.println("Impossible to request stream, there is no possible path available.");

        else{
            InetAddress closerNodeIp = this.neighbors.getNodeIP(nodeToRequestStream);
            int closerNodePort = this.neighbors.getNodePort(nodeToRequestStream);
            System.out.println("Requesting stream to node " + nodeToRequestStream);
            sendPacket(new byte[0], 7, 1, this.id, closerNodeIp, closerNodePort);
        }

    }

    public void streamPacket(RTPpacket rtPpacket){
        try {
            nodesToStreamToLock.lock();
            neighborsLock.lock();
            for(Integer nodeId: this.nodesToStreamTo){
                if(this.neighbors.getNodeState(nodeId) == NodeInfo.nodeState.ON){ // Only streams to alive nodes
                    InetAddress requestFromIp = this.neighbors.getNodeIP(nodeId);
                    int requestFromPort = this.neighbors.getNodePort(nodeId);
                    sendPacket(rtPpacket, requestFromIp, requestFromPort);
                    System.out.println("enviei stream............");
                }
            }
        }
        finally{
            neighborsLock.unlock();
            nodesToStreamToLock.unlock();
        }
    }


    public void sendIsAliveCheck (int id, NodeInfo nodeInfo){
        System.out.println("Checking if node "+ id+" is alive.");
        sendPacket(new byte[0], 5, 1, this.id, nodeInfo.getNodeIp(), nodeInfo.getNodePort());
        try{
            this.neighborsLock.lock();
            this.neighbors.setNodeState(id, NodeInfo.nodeState.UNKNOWN);
        }
        finally {
            this.neighborsLock.unlock();
        }
    }


    public void sendPacket(RTPpacket rtPpacket, InetAddress IP, int port){
        try{
            DatagramPacket packet = new DatagramPacket(rtPpacket.getPacket(), rtPpacket.getPacketSize(), IP, port);

            this.socket.send(packet);
            //System.out.println(">> Sent packet to IP: " + IP + "  port: " + port);
            //rtPpacket.printPacketHeader();
        } catch (IOException e){
            e.printStackTrace();
        }
    }


    public void sendPacket(byte [] payload, int packetType, int sequenceNumber, int senderId, InetAddress IP, int port){
        try{
            int timeStamp = (int) (System.currentTimeMillis() / 1000);

            RTPpacket newPacket = new RTPpacket(payload, packetType, sequenceNumber, senderId, timeStamp);
            DatagramPacket packet = new DatagramPacket(newPacket.getPacket(), newPacket.getPacketSize(), IP, port);

            this.socket.send(packet);
            //System.out.println(">> Sent packet to IP: " + IP + "  port: " + port);
            //newPacket.printPacketHeader();
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

            /*if(rtpPacket.getPacketType()!=26){
                System.out.println(">> Packet received from IP: " + fromIp + "\tPort: " + fromPort);
                rtpPacket.printPacketHeader();
            }*/

        } catch (IOException e){
            e.printStackTrace();
        }
        return rtpPacket;
    }


    public void processPacket(RTPpacket packetReceived){

        updateNeighborState(packetReceived.getSenderId(), NodeInfo.nodeState.ON);

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
                boolean updated;

                try{
                    this.addressingTableLock.lock();
                    updated = updateAddressingTable(at, packetReceived.getSenderId());
                    // If the addressing table changed, notify the neighbors
                    if(updated){
                        sendAddressingTable();
                        System.out.println(this.addressingTable.toString());
                    }
                }
                finally {
                    this.addressingTableLock.unlock();
                }
                break;



            case 22: // Isolated neighbor wants to be updated, mandatory request to ask for
                sendAddressingTable(packetReceived.getSenderId());
                try{
                    this.addressingTableLock.lock();
                    System.out.println(this.addressingTable.toString());
                }
                finally {
                    this.addressingTableLock.unlock();
                }

                break;



            case 5: //Node receives a 'Is alive check'
                // Sends a Im alive signal
                System.out.println("-> Received isAlive check. Replying.\n");
                sendPacket(new byte[0], 6, 1, this.id, packetReceived.getFromIp(), packetReceived.getFromPort());
                break;



            case 6: //IsAlive confirmation
                try{
                    this.neighborsLock.lock();
                    this.neighbors.setNodeState(packetReceived.getSenderId(), NodeInfo.nodeState.CHECKED);
                }
                finally {
                    this.neighborsLock.unlock();
                }

                break;



            case 7: //Node receives a stream request

                // Ads requesting node to the list of nodes receiving the stream
                try {
                    nodesToStreamToLock.lock();
                    this.nodesToStreamTo.add(packetReceived.getSenderId());
                }
                finally{
                    nodesToStreamToLock.unlock();
                }

                // If isn't receiving the stream, goes and asks for it
                if(!this.streaming){
                    requestStream();
                }
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
