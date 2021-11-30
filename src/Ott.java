import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

public class Ott {

    public InetAddress ip;
    private HashMap<InetAddress,Integer> neighbors_table;    //key: IP vizinho  |  value: distancia


    public InetAddress getIp() {
        return ip;
    }

    public void setIp(InetAddress ip) {
        this.ip = ip;
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


    // metodo para escutar trafego

    // metodo para enviar trafego
}
