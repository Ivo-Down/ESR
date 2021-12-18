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

public class Ott implements Runnable {

    private Integer id;
    private InetAddress ip;
    private Integer port;
    private boolean running;
    private Integer streamerId;
    private Table neighbors;
    private DatagramSocket socket;
    private LinkedBlockingQueue<RTPpacket> packetsQueue;
    private AddressingTable addressingTable;
    private boolean isClient;
    private LinkedBlockingQueue<RTPpacket> framesQueue;
    private boolean streaming;
    private HashSet<Integer> nodesToStreamTo;
    private byte[] buffer = new byte[Constants.DEFAULT_BUFFER_SIZE];
    private boolean receivingStream;


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
        this.receivingStream = false;
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
            }


            new Thread(() -> {
                // Esta thread vai periodicamente ver se os nodos vizinhos ainda estão ativos e caso não estejam trata de os desligar
                System.out.println("===> CHECKING IF NEIGHBORS ARE ALIVE");

                // De x em x segundos vai verificar se os vizinhos ainda estão vivos
                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.scheduleAtFixedRate(() -> {

                    try {
                        for(int i=0; i< Constants.TRIES_UNTIL_TIMEOUT; i++){
                            // Get nodes that are flagged with 'ON'

                            Table onlineNodes;
                            synchronized (this.neighbors){
                                onlineNodes = this.neighbors.getNodesWithState(NodeInfo.nodeState.ON);
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
                        synchronized (this.neighbors){
                            for(Map.Entry<Integer, NodeInfo> e: this.neighbors.getMap().entrySet()){
                                if(e.getValue().getNodeState().equals(NodeInfo.nodeState.UNKNOWN)){

                                    this.neighbors.setNodeState(e.getKey(), NodeInfo.nodeState.OFF);
                                    this.neighbors.incDeathCount(e.getKey());
                                    System.out.println("Node " + e.getKey() + " timed out.");
                                    nodesThatDied.add(e.getKey());

                                }
                                if(e.getValue().getNodeState().equals(NodeInfo.nodeState.CHECKED)){
                                    this.neighbors.setNodeState(e.getKey(), NodeInfo.nodeState.ON);
                                    //System.out.println("Node " + e.getKey() + " is alive.");
                                }
                            }

                            //this.neighbors.printTable();
                        }

                        synchronized (this.addressingTable){
                            for (Integer nodeId: nodesThatDied){
                                // Verificar se o nodo era o melhor caminho para o servidor, se for tem de se pedir a stream a outro lado
                                this.addressingTable.setNeighborState(nodeId, false);
                                if(nodeId.equals(this.streamerId))
                                    requestStream();


                            }
                        }

                        // Deixa de enviar a stream para os nodos que morreram
                        synchronized (this.nodesToStreamTo){
                            for (Integer nodeId: nodesThatDied)
                                this.nodesToStreamTo.remove(nodeId);
                        }


                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }, 0, Constants.NODE_ALIVE_CHECKING_TIME, TimeUnit.SECONDS);

            }).start();


            new Thread(() -> {
                System.out.println("===> LISTENING UDP");
                while(this.running)
                {
                    RTPpacket receivePacket = receivePacket();
                    if(receivePacket!=null){

                        // Se for um pacote de streaming, nem vai processar
                        if(receivePacket.getPacketType()==26) {
                            this.streaming=true;
                            this.streamerId=receivePacket.getSenderId();
                            this.receivingStream=true;
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


    public synchronized void insertNeighborsInAddressingTable(){
        for (NodeInfo n : this.neighbors.getNeighborNodes())
            this.addressingTable.setDistance(n.getNodeId(), n.getNodeId(), 1, true); //todo assumimos que começam a on?
    }


    // For each neighbor, sends the addressingTable
    public synchronized void sendAddressingTable(){
        byte[] data = StaticMethods.serialize(this.addressingTable);

        int packetType = 2;
        // Neste caso específico se mandar um pedido tipo 2 nunca vai receber resposta, o tipo 22 exige resposta
        if(this.neighbors.getSize()==1)
            packetType = 22;

        for (NodeInfo n : this.neighbors.getNeighborNodes())
            if(n.getNodeId()!=Constants.SERVER_ID)
                sendPacket(data, packetType, 1, this.id, n.getNodeIp(), n.getNodePort());
    }

    // Sends the addressingTable to that neighbor
    public synchronized void sendAddressingTable(Integer specificNodeId){
        System.out.println("Sending updated table to neighbors.");
        byte[] data = StaticMethods.serialize(this.addressingTable);

        for (NodeInfo n : this.neighbors.getNeighborNodes()){
            if(n.getNodeId()== specificNodeId){
                sendPacket(data, 2, 1, this.id, n.getNodeIp(), n.getNodePort());
            }
        }
    }



    // Receives the addressingTable and its owner id
    public synchronized boolean updateAddressingTable(AddressingTable at, Integer neighborId){
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


    public synchronized void updateNeighborState(int neighborId, NodeInfo.nodeState state){
        if(this.neighbors.getMap().containsKey(neighborId))
            this.neighbors.setNodeState(neighborId, state);

        boolean isOn = state.equals(NodeInfo.nodeState.ON);
        this.addressingTable.setNeighborState(neighborId, isOn);
    }


    public synchronized void requestStream(){
        int nodeToRequestStream = this.addressingTable.getBestNextNode(Constants.SERVER_ID); // Node closest to the bootstrapper

        if(nodeToRequestStream<0){
            System.out.println("Impossible to request stream, there is no possible path available.");
            this.receivingStream=false;
        }

        else{
            InetAddress closerNodeIp = this.neighbors.getNodeIP(nodeToRequestStream);
            int closerNodePort = this.neighbors.getNodePort(nodeToRequestStream);
            System.out.println("Requesting stream to node " + nodeToRequestStream);
            sendPacket(new byte[0], 7, 1, this.id, closerNodeIp, closerNodePort);
        }

    }

    public synchronized void streamPacket(RTPpacket rtPpacket){
        for(Integer nodeId: this.nodesToStreamTo){
            if(this.neighbors.getNodeState(nodeId) == NodeInfo.nodeState.ON){ // Only streams to alive nodes
                InetAddress requestFromIp = this.neighbors.getNodeIP(nodeId);
                int requestFromPort = this.neighbors.getNodePort(nodeId);
                sendPacket(rtPpacket, requestFromIp, requestFromPort);
            }
        }
    }


    public synchronized void sendIsAliveCheck (int id, NodeInfo nodeInfo){
        //System.out.println("Checking if node "+ id+" is alive.");
        sendPacket(new byte[0], 5, 1, this.id, nodeInfo.getNodeIp(), nodeInfo.getNodePort());
        this.neighbors.setNodeState(id, NodeInfo.nodeState.UNKNOWN);
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


    public synchronized void processPacket(RTPpacket packetReceived){

        updateNeighborState(packetReceived.getSenderId(), NodeInfo.nodeState.ON);

        switch (packetReceived.getPacketType()) {

            case 1: //Receives neighbors information

                byte[] data = packetReceived.getPayload();

                this.neighbors = (Table) StaticMethods.deserialize(data);

                System.out.println("-> Received neighbors information from bootstrapper.\n");

                // Inserts its neighbors in the addressing table
                insertNeighborsInAddressingTable();

                // Sends to each neighbor the addressing table
                sendAddressingTable();
                System.out.println(this.addressingTable.toString());
                break;



            case 2: //Receives addressingTable information from a neighbor and updated addressingTable
                // Se o nodo já tinha estado online e morreu, enviar tabela por solidariedade
                if(this.neighbors.getDeathCount(packetReceived.getSenderId())>0){
                    sendAddressingTable(packetReceived.getSenderId());
                    System.out.println("Sent addressing table to reborn node.");
                }

                else{
                    byte[] addrdata = packetReceived.getPayload();
                    AddressingTable at = (AddressingTable) StaticMethods.deserialize(addrdata);

                    // Update addressing table
                    boolean updated = updateAddressingTable(at, packetReceived.getSenderId());
                    // If the addressing table changed, notify the neighbors
                    if(updated){
                        sendAddressingTable();
                        System.out.println(this.addressingTable.toString());
                    }
                }

                // Se for cliente e não estiver a receber a stream
                if(this.isClient && !this.receivingStream){
                    requestStream();
                    System.out.println("SOU LINDO FDS");
                }

                break;



            case 22: // Isolated neighbor wants to be updated, mandatory request to ask for
                sendAddressingTable(packetReceived.getSenderId());
                System.out.println(this.addressingTable.toString());

                break;



            case 5: //Node receives a 'Is alive check'
                // Sends a Im alive signal
                //System.out.println("-> Received isAlive check. Replying.\n");
                sendPacket(new byte[0], 6, 1, this.id, packetReceived.getFromIp(), packetReceived.getFromPort());
                break;



            case 6: //IsAlive confirmation
                this.neighbors.setNodeState(packetReceived.getSenderId(), NodeInfo.nodeState.CHECKED);
                break;



            case 7: //Node receives a stream request

                if(!this.isClient){

                    // Ads requesting node to the list of nodes receiving the stream
                    this.nodesToStreamTo.add(packetReceived.getSenderId());

                    // If isn't receiving the stream, goes and asks for it
                    if(!this.streaming){
                        requestStream();

                        if(!this.receivingStream)
                            sendPacket(new byte[0], 8, 1, this.id, packetReceived.getFromIp(), packetReceived.getFromPort());
                    }

                }
                break;



            case 8: //Nodo a quem pedimos a stream nao a consegue obter
                this.receivingStream=false;
                System.out.println("RECEBI PACOTE TIPO 8 CRL");
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
