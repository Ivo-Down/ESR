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
        NodeInfo nodeInfo = new NodeInfo(ip, port, NodeInfo.nodeState.OFF, nodeId);
        neighborNodes.put(nodeId, nodeInfo);
    }

    public void addNode(int nodeId, NodeInfo nodeInfo){
        neighborNodes.put(nodeId, nodeInfo);
    }

    public int getNodeByIpAndPort(InetAddress ip, int port){
        for(Map.Entry<Integer, NodeInfo> e: neighborNodes.entrySet()){
            if(e.getValue().getNodeIp().equals(ip) && e.getValue().getNodePort()==port)
                return e.getKey();
        }
        return -1;
    }



    public InetAddress getNodeIP(int nodeId){ return neighborNodes.get(nodeId).getNodeIp(); }


    public int getNodePort(int nodeId){
        return neighborNodes.get(nodeId).getNodePort();
    }

    public int getDeathCount(int nodeId){
        return neighborNodes.get(nodeId).getDeathCount();
    }

    public NodeInfo.nodeState getNodeState(int nodeId){
        return neighborNodes.get(nodeId).getNodeState();
    }


    public void setNodeState(int nodeId, NodeInfo.nodeState state){
        neighborNodes.get(nodeId).setNodeState(state);
    }

    public void incDeathCount(int nodeId){
        neighborNodes.get(nodeId).incDeathCount();
    }

    public void decDeathCount(int nodeId){
        neighborNodes.get(nodeId).decDeathCount();
    }

    public int getSize(){
        return this.neighborNodes.size();
    }


    public Table getNodesWithState(NodeInfo.nodeState state){
        Table res = new Table();

        for(Map.Entry<Integer, NodeInfo> e: this.neighborNodes.entrySet()){
            if(e.getValue().getNodeState().equals(state))
                res.addNode(e.getKey(), e.getValue());
        }
        return res;
    }



    public ArrayList<NodeInfo> getNeighborNodes(){
        ArrayList<NodeInfo> res = new ArrayList<>();
        for(Map.Entry<Integer, NodeInfo> e: neighborNodes.entrySet()){
            res.add(e.getValue());
        }
        return res;
    }

    public HashMap<Integer, NodeInfo> getMap(){
        return this.neighborNodes;
    }


    public void printTable(){
        System.out.println("Table: ");
        for(Map.Entry<Integer, NodeInfo> e: neighborNodes.entrySet()){
            System.out.println("Node " + e.getKey() +":\n" +e.getValue().toString());
        }
    }

    public NodeInfo getNodeInfo(Integer nodeId){
        return this.neighborNodes.get(nodeId);
    }


    @Override
    public String toString() {
        return "Table{" +
                "neighborNodes=" + neighborNodes +
                '}';
    }

}