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
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
    private LinkedBlockingQueue<RTPpacket> packetsQueue;


    private byte[] buffer = new byte[Constants.DEFAULT_BUFFER_SIZE];


    // ------------------------------ CONSTRUCTORS ------------------------------

    public OttBootStrapper(){
        this.id = 1;
        this.port = Constants.DEFAULT_PORT;
        this.overlayNodes = this.getAllNodes();
        this.packetsQueue = new LinkedBlockingQueue<>();


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
                System.out.println("===> SHARING VIDEO");

                    File f = new File("src/movie.Mjpeg");
                    if (f.exists()) {
                        // TODO MUDAR -> SO STREAMA PARA O CLIENT 11
                        Streamer s = new Streamer("src/movie.Mjpeg","127.0.0.1", 8097);
                    } else
                        System.out.println("Ficheiro de video não existe: " + "src/movie.Mjpeg");
                }).start();


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



            new Thread(() -> {
                System.out.println("===> LISTENING UDP");
                while(this.running)
                {
                    RTPpacket receivePacket = receivePacket();
                    if(receivePacket!=null)
                        this.packetsQueue.add(receivePacket);
                }
            }).start();



            // ---> Main thread
            System.out.println("===> CONSUMING UDP");
            while(this.running){
                try{
                    RTPpacket receivePacket = this.packetsQueue.take();
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



    public void sendIsAliveCheck (int id, NodeInfo nodeInfo){
        System.out.println("Checking if node "+ id+" is alive.");
        sendPacket(new byte[0], 5, 1, this.id, nodeInfo.getNodeIp(), nodeInfo.getNodePort());
        try{
            this.overlayNodesLock.lock();
            this.overlayNodes.setNodeState(id, NodeInfo.nodeState.UNKNOWN);
        }
        finally {
            this.overlayNodesLock.unlock();
        }
    }







    // Método para pedir ao servidor a lista de vizinhos (com a sua informação)
    public Table getNeighbors(int node_id){
        JSONParser parser = new JSONParser();
        ArrayList<Integer> neighbors = new ArrayList<>();
        Table res = new Table();

        // NOTA: ISTO ESTA MAL OTIMIZADO, PERCORRE 2 VEZES A ESTRUTURA PARA IR BUSCAR OS DADOS
        try {
            JSONArray jsonArray =  (JSONArray) parser.parse(new FileReader("src\\overlay.json"));

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
            JSONArray jsonArray =  (JSONArray) parser.parse(new FileReader("src\\overlay.json"));

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



    public void sendPacket(byte [] payload, int packetType, int sequenceNumber, int senderId, InetAddress IP, int port){
        try{
            int timeStamp = (int) (System.currentTimeMillis() / 1000);
            RTPpacket newPacket = new RTPpacket(payload, packetType, sequenceNumber, senderId, timeStamp);
            DatagramPacket packet = new DatagramPacket(newPacket.getPacket(), newPacket.getPacketSize(), IP, port);
            this.socket.send(packet);
            System.out.println(">> Sent packet to IP: " + IP + "  port: " + port);
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
            System.out.println(">> Packet received.");
            //rtpPacket.printPacketHeader();
        } catch (IOException e){
            e.printStackTrace();
        }
        return rtpPacket;
    }

    public RTPpacket receivePacket(int timeout){
        RTPpacket rtpPacket;
        try{
            socket.setSoTimeout(timeout);
            DatagramPacket packet = new DatagramPacket(this.buffer, this.buffer.length);
            socket.receive(packet);
            socket.setSoTimeout(0);
            InetAddress fromIp = packet.getAddress();
            Integer fromPort = packet.getPort();

            rtpPacket = new RTPpacket(this.buffer, fromIp, fromPort);
            System.out.println(">> Packet received.");
           // rtpPacket.printPacket();
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
        return rtpPacket;
    }


    public void processPacket(RTPpacket packetReceived){
        switch (packetReceived.getPacketType()) {

            case 0: //Node wants to get its neighbors

                int nodeId = packetReceived.getSenderId();
                System.out.println("A new neighbors request has been made.\n");
                Table requestedNeighbors = getNeighbors(nodeId);

                byte[] data = Table.serialize(requestedNeighbors);

                // Updating the state of the overlay to ready
                try{
                    this.overlayNodesLock.lock();
                    this.overlayNodes.setNodeState(nodeId, NodeInfo.nodeState.ON);
                }
                finally {
                    this.overlayNodesLock.unlock();
                }


                // Sending the answer
                sendPacket(data, 1, 1, this.id, packetReceived.getFromIp(), packetReceived.getFromPort());
                break;




            case 6: //IsAlive confirmation
                System.out.println("Node " + packetReceived.getSenderId() + " is alive.");
                try{
                    this.overlayNodesLock.lock();
                    this.overlayNodes.setNodeState(packetReceived.getSenderId(), NodeInfo.nodeState.CHECKED);
                }
                finally {
                    this.overlayNodesLock.unlock();
                }

                break;
        }
    }

}
