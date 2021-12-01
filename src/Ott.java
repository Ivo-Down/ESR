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
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Ott {

    public Integer id;
    public InetAddress ip;
    public Integer port;
    private HashMap<InetAddress,Integer> neighborsTable;    //key: IP vizinho  |  value: port
    private Boolean bootstrapper;
    private DatagramSocket socket;
    private final Lock socketLock = new ReentrantLock();


    // TODO criar construtores
    public Ott(){
        try{
            this.socket = new DatagramSocket(Constants.DEFAULT_PORT);
        } catch (IOException e){
            e.printStackTrace();
        }
    }


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

    public HashMap<InetAddress, Integer> getNeighborsTable() {
        return neighborsTable;
    }

    public void setNeighborsTable(HashMap<InetAddress, Integer> neighborsTable) {
        this.neighborsTable = neighborsTable;
    }

    public void setNeighbors_table(InetAddress neighbor_ip, int cost) {
        this.neighborsTable.put(neighbor_ip, cost);
    }


    // Método para pedir ao servidor a lista de vizinhos (com a sua informação)  --> vai ser usado pelo bootstrapper apenas
    public HashMap<Integer,HashMap<InetAddress, Integer>> getNeighbors(int node_id){
        JSONParser parser = new JSONParser();
        ArrayList<Integer> neighbors = new ArrayList<>();
        HashMap<Integer, HashMap<InetAddress, Integer>> res = new HashMap<>(); //key: node_id   value: pair ip port

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
                    HashMap<InetAddress, Integer> aux = new HashMap<>();
                    aux.put(node_ip, node_port);
                    res.put(id, aux);
                }
            }
        } catch (IOException | ParseException e){
            e.printStackTrace();
        }

        return res;
    }



    // Método para enviar um pacote de trafego
    public void sendPacket(byte [] payload, int type, int frameNb, int senderId, InetAddress IP, int port){
        try{
            int timestamp = (int) (System.currentTimeMillis() / 1000);

            RTPpacket newPacket = new RTPpacket(type, frameNb, senderId, timestamp, payload,0);
            DatagramPacket packet = new DatagramPacket(newPacket.getPacket(), newPacket.getPacketSize(), IP, port);

            this.socket.send(packet);
            System.out.println(">> Sent packet:");

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    // Método para receber pacotes
    public RTPpacket receivePacket(){
        RTPpacket fsChunk = new RTPpacket();
        try{
            this.socketLock.lock();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            socket.setSoTimeout(0);  //removes any timeout existing
            this.lastReceivedIP = packet.getAddress();
            this.lastReceivedPort = packet.getPort();

            fsChunk = new FS_Chunk_Protocol(this.buffer);

            System.out.println("Packet received.");
            //fsChunk.printFsChunk();
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            this.socketLock.unlock();
        }
        return fsChunk;
    }


}
