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

public class OttBootStrapper {

    private Integer id;
    private InetAddress ip;
    private Integer port;
    //private InetAddress lastReceivedIP;
    //private Integer lastReceivedPort;
    private Table neighbors;
    //private Boolean bootstrapper;
    private DatagramSocket socket;
    private Requests requests;

    private final Lock socketLock = new ReentrantLock();
    private byte[] buffer = new byte[Constants.DEFAULT_BUFFER_SIZE];


    // ------------------------------ CONSTRUCTORS ------------------------------

    public OttBootStrapper(){
        this.id = 1;
        this.port = Constants.DEFAULT_PORT;
        this.neighbors = this.getNeighbors(this.id);
        this.requests = new Requests();

        try{
            this.ip = InetAddress.getByName(Constants.SERVER_ADDRESS);
            this.socket = new DatagramSocket(this.port);

        } catch (IOException e){
            e.printStackTrace();
        }
    }







    // ------------------------------ OTHER METHODS ------------------------------


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
            rtpPacket.printPacket();
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            this.socketLock.unlock();
        }
        return rtpPacket;
    }

}
