import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Ott {

    public Integer id;
    public InetAddress ip;
    public Integer port;
    //private InetAddress lastReceivedIP;
    //private Integer lastReceivedPort;
    private Table neighbors;
    private Boolean bootstrapper;
    private DatagramSocket socket;
    private final Lock socketLock = new ReentrantLock();
    private byte[] buffer = new byte[Constants.DEFAULT_BUFFER_SIZE];


    // TODO criar construtores
    // ------------------------------ CONSTRUCTORS ------------------------------

    public Ott(){
        try{
            this.socket = new DatagramSocket(Constants.DEFAULT_PORT);
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

    public Boolean getBootstrapper() {
        return bootstrapper;
    }

    public void setBootstrapper(Boolean bootstrapper) {
        this.bootstrapper = bootstrapper;
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


    // Método para pedir ao servidor a lista de vizinhos (com a sua informação)  --> vai ser usado pelo bootstrapper apenas
    public Table getNeighbors(int node_id){
        JSONParser parser = new JSONParser();
        ArrayList<Integer> neighbors = new ArrayList<>();
        Table res = new Table();

        // NOTA: ISTO ESTA MAL OTIMIZADO, PERCORRE 2 VEZES A ESTRUTURA PARA IR BUSCAR OS DADOS
        try {
            JSONArray jsonArray =  (JSONArray) parser.parse(new FileReader("overlay.json"));

            for (Object o: jsonArray){
                JSONObject node = (JSONObject) o;
                Integer id = (Integer) node.get("node");

                if(node_id == id){
                    JSONArray neighbors_json = (JSONArray) node.get("neighbors");
                    for (Object obj : neighbors_json){
                        Integer i = (Integer) obj;
                        neighbors.add(i);
                    }
                    break;
                }
            }

            for (Object o: jsonArray){
                JSONObject node = (JSONObject) o;
                Integer id = (Integer) node.get("node");

                if (neighbors.contains(id)){
                    Integer node_port = (Integer) node.get("port");
                    InetAddress node_ip = (InetAddress) node.get("ip");

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

    @Override
    public String toString() {
        return "Ott{" +
                "id=" + id +
                ", ip=" + ip +
                ", port=" + port +
                ", neighbors=" + neighbors +
                ", bootstrapper=" + bootstrapper +
                ", socket=" + socket +
                ", socketLock=" + socketLock +
                ", buffer=" + Arrays.toString(buffer) +
                '}';
    }
}
