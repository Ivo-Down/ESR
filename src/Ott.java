import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

public class Ott {

    public Integer id;
    public InetAddress ip;
    public Integer port;
    private HashMap<InetAddress,Integer> neighbors_table;    //key: IP vizinho  |  value: port
    private Boolean bootstrapper;


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

    public HashMap<InetAddress, Integer> getNeighbors_table() {
        return neighbors_table;
    }

    public void setNeighbors_table(HashMap<InetAddress, Integer> neighbors_table) {
        this.neighbors_table = neighbors_table;
    }

    public void setNeighbors_table(InetAddress neighbor_ip, int cost) {
        this.neighbors_table.put(neighbor_ip, cost);
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


    // metodo para escutar trafego

    // metodo para enviar trafego


}
