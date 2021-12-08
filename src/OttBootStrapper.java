import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

public class OttBootStrapper implements Runnable {

    private Integer id;
    private InetAddress ip;
    private Integer port;
    private boolean running;
    private Table overlayNodes;
    private DatagramSocket socket;


    private byte[] buffer = new byte[Constants.DEFAULT_BUFFER_SIZE];


    // ------------------------------ CONSTRUCTORS ------------------------------

    public OttBootStrapper(){
        this.id = 1;
        this.port = Constants.DEFAULT_PORT;
        this.overlayNodes = this.getAllNodes();


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



            // ---> Esta thread vai periodicamente ver se os nodos ainda estão ativos e caso não estejam trata de os desligar
            /**new Thread(() -> {


                // De x em x segundos vai verificar se os servidores ainda estão vivos
                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.scheduleAtFixedRate(() -> {
                    try {
                        // Get nodes that are flagged with 'ON'
                        Table onlineNodes = this.overlayNodes.getNodesWithState(NodeInfo.nodeState.ON);

                        // For each online node, check if it is alive
                        for(Map.Entry<Integer, NodeInfo> e: onlineNodes.getMap().entrySet())
                            checkIfServerAlive(e.getKey(), e.getValue());  //todo aqui dentro só se vai mandar o isAliveCheck, n se vai ouvir

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, 0, Constants.NODE_ALIVE_CHECKING_TIME, TimeUnit.MILLISECONDS);

            }).start();*/

            // TODO  -- ter apenas uma thread a receber do socket
            // TODO -- ter uma thread a tratar do processamento dos pacotes


            // ---> Main thread listening to node requests and replying
            while(this.running){
                RTPpacket receivePacket = receivePacket();
                if(receivePacket!=null)
                    processPacket(receivePacket);

                // Check if there are any packets to send and send them
                // Processa chunks que tenham sido ignorados, como novos servidores
                //processStoredChunks();
                //Thread.sleep(100);
                //table.printTable();
            }


            System.out.println("Bootstrapper is shutting down.");
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }







    public boolean checkIfServerAlive (int id, NodeInfo nodeInfo){
        System.out.println("Checking if node "+ id+" is alive.");

        for(int i=1; i<=Constants.TRIES_UNTIL_TIMEOUT; i++){

            sendPacket(new byte[0], 5, 1, this.id, nodeInfo.getNodeIp(), nodeInfo.getNodePort());
            RTPpacket receivedPacket = receivePacket(i*1000);  //TODO TIRAR ISTO DAQUI OU USAR OUTRO SOCKET OU ESTRUTURA E UM TIMER

            if(receivedPacket!=null){
                System.out.println("Reached node "+ id+" with success.");
                processPacket(receivedPacket);

                this.overlayNodes.setNodeState(id, NodeInfo.nodeState.ON);
                return true;
            }
            else{
                System.out.println("Failed to reach node "+ id+". Tentative number "+i+".");
                this.overlayNodes.setNodeState(id, NodeInfo.nodeState.OFF);
            }
        }
        this.overlayNodes.setNodeState(id, NodeInfo.nodeState.OFF);
        System.out.println("Server "+id+ " timed out.");
        return false;
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
            for (Integer i: neighbors){
                NodeInfo nodeInfo = this.overlayNodes.getNodeInfo(i);
                res.addNode(i, nodeInfo);
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
            rtpPacket.printPacket();
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
                this.overlayNodes.setNodeState(nodeId, NodeInfo.nodeState.ON);

                // Sending the answer
                sendPacket(data, 1, 1, this.id, packetReceived.getFromIp(), packetReceived.getFromPort());
                break;

            case 6: //IsAlive confirmation
                System.out.println("Node " + packetReceived.getSenderId() + " is alive.");
                break;
        }
    }

}
