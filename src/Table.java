import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Table implements Serializable {
    private static final long serialVersionUID = 1249369646723187393L;
    private HashMap<Integer, NodeInfo> neighborNodes;  // id node -> nodeInfo

    public Table(){
        this.neighborNodes = new HashMap<>();
    }



    public void addNode(InetAddress ip, int port, int nodeId){
        NodeInfo nodeInfo = new NodeInfo(ip, port, NodeInfo.nodeState.OFF);
        neighborNodes.put(nodeId, nodeInfo);
    }


    public InetAddress getNodeIP(int nodeId){ return neighborNodes.get(nodeId).getNodeIp(); }


    public int getNodePort(int nodeId){
        return neighborNodes.get(nodeId).getNodePort();
    }


    public void setNodeState(int nodeId, NodeInfo.nodeState state){
        neighborNodes.get(nodeId).setNodeState(state);
    }


    public ArrayList<Integer> getNodesWithState(NodeInfo.nodeState state){
        ArrayList<Integer> res = new ArrayList();
        for(Map.Entry<Integer, NodeInfo> e: neighborNodes.entrySet()){
            if(e.getValue().getNodeState().equals(state))
                res.add(e.getKey());
        }
        return res;
    }

    public int getNodeReady(){
        for(Map.Entry<Integer, NodeInfo> e: neighborNodes.entrySet()){
            if(e.getValue().getNodeState().equals(NodeInfo.nodeState.READY))
                return e.getKey();
        }
        return -1;
    }

    public ArrayList<Integer> getNeighborNodeIds(){
        ArrayList<Integer> res = new ArrayList();
        for(Map.Entry<Integer, NodeInfo> e: neighborNodes.entrySet()){
            res.add(e.getKey());
        }
        return res;
    }

    public ArrayList<NodeInfo> getNeighborNodes(){
        ArrayList<NodeInfo> res = new ArrayList();
        for(Map.Entry<Integer, NodeInfo> e: neighborNodes.entrySet()){
            res.add(e.getValue());
        }
        return res;
    }


    public void printTable(){
        System.out.println("Table: ");
        for(Map.Entry<Integer, NodeInfo> e: neighborNodes.entrySet()){
            System.out.println("Node " + e.getKey() +":\n" +e.getValue().toString());
        }
    }


    public void removeNode(int nodeId){
        this.neighborNodes.remove(nodeId);
    }





    public static byte[] serialize(Object obj) {
        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(boas)) {
            oos.writeObject(obj);
            oos.flush();
            oos.close();
            return boas.toByteArray();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        throw new RuntimeException();
    }


    public static Object deserialize(byte[] bytes) {
        InputStream is = new ByteArrayInputStream(bytes);
        try (ObjectInputStream ois = new ObjectInputStream(is)) {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException ioe) {
            ioe.printStackTrace();
        }
        throw new RuntimeException();
    }
}