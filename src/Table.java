import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Table {

    private ConcurrentHashMap<Integer, NodeInfo> neighborNodes;  // id node -> nodeInfo

    public Table(){
        this.neighborNodes = new ConcurrentHashMap<>();
    }



    public void addNode(InetAddress ip, int port, int nodeId){
        NodeInfo nodeInfo = new NodeInfo(ip, port, NodeInfo.nodeState.READY);
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

    public ArrayList<Integer> getNeighborNodes(){
        ArrayList<Integer> res = new ArrayList();
        for(Map.Entry<Integer, NodeInfo> e: neighborNodes.entrySet()){
            res.add(e.getKey());
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
}