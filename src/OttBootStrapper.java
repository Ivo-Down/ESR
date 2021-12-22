import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OttBootStrapper implements Runnable {

    private Integer id;
    private InetAddress ip;
    private Integer port;
    private boolean running;
    private Table overlayNodes;
    private Lock overlayNodesLock = new ReentrantLock();  //temos pelo menos duas threads a usar a Table 
    private DatagramSocket socket;
    private LinkedBlockingQueue<OttPacket> packetsQueue;
    private HashMap<Integer, NodeInfo> nodesToStreamTo;
    private Lock nodesToStreamToLock = new ReentrantLock();
    private byte[] buffer = new byte[Constants.DEFAULT_BUFFER_SIZE];
    private ConcurrentHashMap<Integer,PendingRequests> pendingRequestsTable;
    private AtomicInteger requestID;



    // ------------------------------ CONSTRUCTORS ------------------------------

    public OttBootStrapper(){
        this.id = 1;
        this.port = Constants.DEFAULT_PORT;
        this.overlayNodes = this.getAllNodes();
        this.packetsQueue = new LinkedBlockingQueue<>();
        this.nodesToStreamTo = new HashMap<>();
        this.pendingRequestsTable = new ConcurrentHashMap<>();
        this.requestID = new AtomicInteger(1);

        try{
            this.ip = InetAddress.getByName(Constants.SERVER_ADDRESS);
            this.socket = new DatagramSocket(this.port);

        } catch (IOException e){
            e.printStackTrace();
        }
    }


    // ------------------------------ OTHER METHODS ------------------------------


    public void run() {
        try {
            this.running = true;
            System.out.println("Bootstrapper is running!");



            new Thread(() -> {
                System.out.println("===> STREAMING VIDEO");

                    File f = new File("movie.Mjpeg");
                    if (f.exists()) {

                        Streamer s = new Streamer("movie.Mjpeg", this.nodesToStreamTo, this.nodesToStreamToLock, this.socket);
                    } else
                        System.out.println("Ficheiro de video não existe: " + "movie.Mjpeg");
                }).start();



            new Thread(() -> {
                System.out.println("===> CONSUMING PENDING REQUESTS");
                while(true)
                {
                    for (ConcurrentHashMap.Entry<Integer,PendingRequests> m : pendingRequestsTable.entrySet()){
                        int timeStamp = (int) (System.currentTimeMillis());
                        int lastSentTime = m.getValue().getTimeStamp();
                        int elapsedTime = timeStamp - lastSentTime;

                        if((m.getValue().getSentCounter() >= Constants.TRIES_UNTIL_TIMEOUT)){
                            //TODO por nodo a OFF
                            this.pendingRequestsTable.remove(m.getKey());
                            System.out.println("Packet " + m.getKey()+" timed out!");

                        }else if(elapsedTime > 100) {
                            int actualCounter = m.getValue().getSentCounter() +1;
                            m.getValue().setSentCounter(actualCounter);
                            timeStamp = (int) (System.currentTimeMillis());
                            m.getValue().setTimeStamp(timeStamp);

                            System.out.println("Sending repeated packet to: " + m.getValue().getIpDestinyNode() + "\ttype: " + m.getValue().getPacket().getPacketType());
                            sendRepeatedPacket(m.getValue().getPacket(),m.getValue().getIpDestinyNode(), m.getValue().getPortDestinyNode());

                        }
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

/*
            new Thread(() -> {
                // Esta thread vai periodicamente ver se os nodos ainda estão ativos e caso não estejam trata de os desligar
                System.out.println("===> CHECKING IF NODES ARE ALIVE");

                // De x em x segundos vai verificar se os servidores ainda estão vivos
                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.scheduleAtFixedRate(() -> {

                    try {
                        for(int i=0; i< Constants.TRIES_UNTIL_TIMEOUT; i++){
                            // Get nodes that are flagged with 'ON'

                            Table onlineNodes;
                            try{
                                this.overlayNodesLock.lock();
                                onlineNodes = this.overlayNodes.getNodesWithState(NodeInfo.nodeState.ON);
                            }
                            finally {
                                this.overlayNodesLock.unlock();
                            }


                            // For each online node, check if it is alive and change its state to UNKNOWN
                            for(Map.Entry<Integer, NodeInfo> e: onlineNodes.getMap().entrySet())
                                sendIsAliveCheck(e.getKey(), e.getValue());

                            // Waits for the nodes to respond
                            Thread.sleep(Constants.TIMEOUT_TIME);
                        }

                        // At this point the nodes who haven't replied have their state UNKNOWN, so they are changed to OFF
                        try{
                            this.overlayNodesLock.lock();
                            for(Map.Entry<Integer, NodeInfo> e: this.overlayNodes.getMap().entrySet()){
                                if(e.getValue().getNodeState().equals(NodeInfo.nodeState.UNKNOWN)){
                                    this.overlayNodes.setNodeState(e.getKey(), NodeInfo.nodeState.OFF);
                                    System.out.println("Node " + e.getKey() + " timed out.");
                                }
                                if(e.getValue().getNodeState().equals(NodeInfo.nodeState.CHECKED))
                                    this.overlayNodes.setNodeState(e.getKey(), NodeInfo.nodeState.ON);
                            }
                        }
                        finally {
                            this.overlayNodesLock.unlock();
                        }
                        this.overlayNodes.printTable();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }, 0, Constants.NODE_ALIVE_CHECKING_TIME, TimeUnit.SECONDS);

            }).start();
*/


            new Thread(() -> {
                System.out.println("===> LISTENING UDP");
                while(this.running)
                {
                    OttPacket receivePacket = receivePacket();
                    if(receivePacket!=null)
                        this.packetsQueue.add(receivePacket);
                }
            }).start();



            // ---> Main thread
            System.out.println("===> CONSUMING UDP");
            while(this.running){
                try{
                    OttPacket receivePacket = this.packetsQueue.take();
                    processPacket(receivePacket);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }


            System.out.println("Bootstrapper is shutting down.");
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /*public void sendIsAliveCheck (int id, NodeInfo nodeInfo){
        System.out.println("Checking if node "+ id+" is alive.");
        sendPacket(new byte[0], 5, 1, this.id, nodeInfo.getNodeIp(), nodeInfo.getNodePort());
        try{
            this.overlayNodesLock.lock();
            this.overlayNodes.setNodeState(id, NodeInfo.nodeState.UNKNOWN);
        }
        finally {
            this.overlayNodesLock.unlock();
        }
    }*/







    // Método para pedir ao servidor a lista de vizinhos (com a sua informação)
    public Table getNeighbors(int node_id){
        JSONParser parser = new JSONParser();
        ArrayList<Integer> neighbors = new ArrayList<>();
        Table res = new Table();

        // NOTA: ISTO ESTA MAL OTIMIZADO, PERCORRE 2 VEZES A ESTRUTURA PARA IR BUSCAR OS DADOS
        try {
            JSONArray jsonArray =  (JSONArray) parser.parse(new FileReader("overlay.json"));

            for (Object o: jsonArray){
                JSONObject node = (JSONObject) o;
                int id = Integer.parseInt((String) node.get("node"));

                if(node_id == id){
                    JSONArray neighbors_json = (JSONArray) node.get("neighbors");

                    for (Object obj : neighbors_json){
                        Integer i = ((Long) obj).intValue();
                        neighbors.add(i);
                    }
                    break;
                }
            }

            // Como a info de cada overlay node já está em memória
            try{
                this.overlayNodesLock.lock();
                for (Integer i: neighbors){
                    NodeInfo nodeInfo = this.overlayNodes.getNodeInfo(i);
                    res.addNode(i, nodeInfo);
                }
            }
            finally {
                this.overlayNodesLock.unlock();
            }


        } catch (IOException | ParseException e){
            e.printStackTrace();
        }

        return res;
    }

    public Table getAllNodes(){
        JSONParser parser = new JSONParser();
        Table res = new Table();
        try {
            JSONArray jsonArray =  (JSONArray) parser.parse(new FileReader("overlay.json"));

            for (Object o: jsonArray){
                JSONObject node = (JSONObject) o;
                int id = Integer.parseInt((String) node.get("node"));
                int node_port = Integer.parseInt((String) node.get("port"));;
                InetAddress node_ip = InetAddress.getByName((String) node.get("ip"));

                res.addNode(node_ip, node_port, id);

            }
        } catch (IOException | ParseException e){
            e.printStackTrace();
        }

        return res;
    }




    public void sendConfirmationPacket(int packetID, InetAddress IP, int port){
        try{
            byte[] payload = new byte[Constants.DEFAULT_BUFFER_SIZE];
            int timeStamp = (int) (System.currentTimeMillis());
            OttPacket newPacket = new OttPacket(payload, 7, packetID, this.id,timeStamp);
            DatagramPacket packet = new DatagramPacket(newPacket.getPacket(), newPacket.getPacketSize(), IP, port);
            //System.out.println(">> Sent confirmation packet to IP: " + IP + "  port: " + port + " type: " + newPacket.getPacketType());
            this.socket.send(packet);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void sendPacket(byte [] payload, int packetType, int senderId, InetAddress IP, int port){
        try{
            int timeStamp = (int) (System.currentTimeMillis());
            int seq = this.requestID.intValue();
            OttPacket newPacket = new OttPacket(payload, packetType, seq, senderId,timeStamp);
            DatagramPacket packet = new DatagramPacket(newPacket.getPacket(), newPacket.getPacketSize(), IP, port);
            System.out.println(">> Sent packet to IP: " + IP + "  port: " + port + " type: " + newPacket.getPacketType());
            this.socket.send(packet);

            // Se enviar um pacote de um destes tipos, vai ficar à espera de confirmção
            if(packetType==1){
                int request = this.requestID.intValue();
                PendingRequests pr = new PendingRequests(request,newPacket,IP,port,1,timeStamp);
                if(!this.pendingRequestsTable.containsKey(request)) {
                    pendingRequestsTable.put(request, pr);
                    this.requestID.getAndIncrement();
                }
            }

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void sendRepeatedPacket(OttPacket newPacket, InetAddress IP, int port){
        try{
            DatagramPacket packet = new DatagramPacket(newPacket.getPacket(), newPacket.getPacketSize(), IP, port);
            this.socket.send(packet);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public OttPacket receivePacket(){
        OttPacket ottPacket = new OttPacket();
        try{
            DatagramPacket packet = new DatagramPacket(this.buffer, this.buffer.length);
            socket.receive(packet);
            socket.setSoTimeout(0);  //removes any timeout existing
            InetAddress fromIp = packet.getAddress();
            Integer fromPort = packet.getPort();

            ottPacket = new OttPacket(this.buffer, fromIp, fromPort);
            System.out.println(">> Packet received from IP: " + fromIp + "\tPort: " + fromPort + "\tnode: " + ottPacket.getSenderId()+ "\tType: " + ottPacket.getPacketType());
            //rtpPacket.printPacketHeader();
        } catch (IOException e){
            e.printStackTrace();
        }
        return ottPacket;
    }



    public void processPacket(OttPacket packetReceived){
        switch (packetReceived.getPacketType()) {

            case 0: //Node wants to get its neighbors

                int nodeId = packetReceived.getSenderId();
                System.out.println("A new neighbors request has been made.\n");

                // Updating the state of the overlay to ready
                try{
                    this.overlayNodesLock.lock();
                    this.overlayNodes.setNodeState(nodeId, NodeInfo.nodeState.ON);
                }
                finally {
                    this.overlayNodesLock.unlock();
                }

                Table requestedNeighbors = getNeighbors(nodeId);

                byte[] data = StaticMethods.serialize(requestedNeighbors);

                // Sending the answer
                sendPacket(data, 1, this.id, packetReceived.getFromIp(), packetReceived.getFromPort());

                sendConfirmationPacket(packetReceived.getSequenceNumber(),packetReceived.getFromIp(),packetReceived.getFromPort());
                break;



            /*case 5: //Node receives a 'Is alive check'
                // Sends a Im alive signal
                sendPacket(new byte[0], 6,packetReceived.getSequenceNumber(), this.id, packetReceived.getFromIp(), packetReceived.getFromPort());
                break;
            */


            /*case 6: //IsAlive confirmation
                System.out.println("Node " + packetReceived.getSenderId() + " is alive.");
                try{
                    this.overlayNodesLock.lock();
                    this.overlayNodes.setNodeState(packetReceived.getSenderId(), NodeInfo.nodeState.CHECKED);
                }
                finally {
                    this.overlayNodesLock.unlock();
                }

                break;
            */


            case 6: //Bootstrapper receives a stream request

                // Ads requesting node to the list of nodes receiving the stream
                int streamRequestId = packetReceived.getSenderId();
                System.out.println("Received stream request from node " + packetReceived.getSenderId() + ".");
                try{
                    this.nodesToStreamToLock.lock();
                    this.nodesToStreamTo.put(streamRequestId, this.overlayNodes.getNodeInfo(streamRequestId));
                }
                finally {
                    this.nodesToStreamToLock.unlock();
                }
                sendConfirmationPacket(packetReceived.getSequenceNumber(),packetReceived.getFromIp(),packetReceived.getFromPort());
                break;



            case 7: //RECEIVING CONFIRMATION
                PendingRequests res = this.pendingRequestsTable.remove(packetReceived.getSequenceNumber());
                if(res!=null)
                    System.out.println("Received confirmation, removing request "+ packetReceived.getSequenceNumber()+"!");
                break;
        }
    }

}
