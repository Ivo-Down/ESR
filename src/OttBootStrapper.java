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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OttBootStrapper implements Runnable {

    private Integer id;
    private InetAddress ip;
    private Integer port;
    private boolean running;
    //private InetAddress lastReceivedIP;
    //private Integer lastReceivedPort;
    private Table overlayNodes;
    //private Boolean bootstrapper;
    private DatagramSocket socket;
    private Requests requests;

    private final Lock socketLock = new ReentrantLock();
    private byte[] buffer = new byte[Constants.DEFAULT_BUFFER_SIZE];


    // ------------------------------ CONSTRUCTORS ------------------------------

    public OttBootStrapper(){
        this.id = 1;
        this.port = Constants.DEFAULT_PORT;
        this.overlayNodes = this.getAllNodes();
        this.requests = new Requests();

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


            // Thread para responder a pedidos
            /**new Thread(() -> {
                //Quando um pedido é adicionado ao clientRequests, a thread acorda e trata de pedir o ficheiro e devolvê-lo

                while(this.running){
                    Client_Requests.Client_Request client_request = this.clientRequests.getNextRequest();  //Fica bloqueado até conseguir um request
                    System.out.println("Client request found!");
                    String filename = client_request.getFilename();
                    Socket clientSocket = client_request.getSocket();


                    try{
                        DataOutputStream clientOut = new DataOutputStream(clientSocket.getOutputStream());
                        if(table.getServersWithState(ServerInfo.serverState.READY).size()>0) {    //se tem pelo menos um server disponivel pede o ficheiro
                            // Get the file's data
                            byte [] data = getFile(filename);

                            // Create a new reply with the data
                            if (data!=null){

                                System.out.println("Sending file " + filename + " to client.");
                                String contentLen = "Content-Length: "+data.length +"\r\n";
                                String contentDisposition = "Content-Disposition: attachment; filename=\"" + filename + "\"";
                                clientOut.write("HTTP/1.1 200 OK\r\n".getBytes(StandardCharsets.UTF_8));
                                clientOut.write(contentLen.getBytes(StandardCharsets.UTF_8));
                                clientOut.write(contentDisposition.getBytes(StandardCharsets.UTF_8));
                                clientOut.write("Content-Type: text/plain\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                                clientOut.write(data);
                                clientOut.flush();
                                clientSocket.close();
                            }


                            else{
                                clientOut.write("HTTP/1.1 404 Not Found Error\r\n".getBytes(StandardCharsets.UTF_8));
                                clientOut.write("Content-Length: 0 \r\n".getBytes(StandardCharsets.UTF_8));
                                clientOut.write("Content-Type: text/plain\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                                clientOut.flush();
                                clientSocket.close();
                            }

                        }
                        else{
                            clientOut.write("HTTP/1.1 500 Internal Server Error\r\n".getBytes(StandardCharsets.UTF_8));
                            clientOut.write("Content-Length: 0 \r\n".getBytes(StandardCharsets.UTF_8));
                            clientOut.write("Content-Type: text/plain\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                            clientOut.flush();
                            clientSocket.close();
                        }

                    } catch (IOException e){
                        e.printStackTrace();
                    }
                }


                System.out.println(Thread.currentThread().getName() + " is ending.");
            }).start(); */


            // Main thread listening to node requests
            while(this.running){
                RTPpacket receivePacket = receivePacket();
                if(receivePacket!=null)
                    processPacket(receivePacket);


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

            for (Object o: jsonArray){
                JSONObject node = (JSONObject) o;
                int id = Integer.parseInt((String) node.get("node"));

                if (neighbors.contains(id)){
                    int node_port = Integer.parseInt((String) node.get("port"));;
                    InetAddress node_ip = InetAddress.getByName((String) node.get("ip"));

                    res.addNode(node_ip, node_port, id);
                }
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
            InetAddress fromIp = packet.getAddress();
            Integer fromPort = packet.getPort();

            rtpPacket = new RTPpacket(this.buffer, fromIp, fromPort);
            System.out.println(">> Packet received.");
            rtpPacket.printPacket();
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            this.socketLock.unlock();
        }
        return rtpPacket;
    }


    public void processPacket(RTPpacket packetReceived){

        switch (packetReceived.getPacketType()) {

            case 0: //Node wants to get its neighbors

                int nodeId = packetReceived.getSenderId();
                System.out.println("A new neighbors request has been made.");
                Table requestedNeighbors = getNeighbors(nodeId);

                byte[] data = Table.serialize(requestedNeighbors);

                // Updating the state of the overlay to ready
                this.overlayNodes.setNodeState(nodeId, NodeInfo.nodeState.READY);

                // Sending the answer
                sendPacket(data, 1, 1, nodeId, packetReceived.getFromIp(), packetReceived.getFromPort());
                break;


            case 99: //IsAlive confirmation
                System.out.println("Node " + packetReceived.getSenderId() + " is alive.");
                break;


        }
    }

}
