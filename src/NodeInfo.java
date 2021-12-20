import java.io.*;
import java.net.InetAddress;

public class NodeInfo implements Serializable {

    private static final long serialVersionUID = 5249369646723187393L;
    enum nodeState {
        ON, OFF, UNKNOWN, CHECKED
    }

    private InetAddress nodeIp;
    private Integer nodePort;
    private nodeState nodeState;
    private Integer nodeId;
    private Integer deathCount;

    public NodeInfo(InetAddress ip, int port, nodeState state, int nodeId){
        this.nodeIp = ip;
        this.nodePort = port;
        this.nodeState = state;
        this.nodeId = nodeId;
        this.deathCount = 0;
    }



    public InetAddress getNodeIp() {
        return nodeIp;
    }

    public int getNodePort() {
        return nodePort;
    }

    public int getNodeId() {
        return nodeId;
    }

    public int getDeathCount() {
        return deathCount;
    }

    public void incDeathCount(){this.deathCount+=1;}

    public NodeInfo.nodeState getNodeState() {
        return nodeState;
    }

    public void setNodeState(NodeInfo.nodeState nodeState) {
        this.nodeState = nodeState;
    }

    @Override
    public String toString() {
        return "NodeInfo{" +
                "nodeIp=" + nodeIp +
                ", nodePort=" + nodePort +
                ", nodeState=" + nodeState +
                ", nodeId=" + nodeId +
                ", deathCount=" + deathCount +
                '}';
    }
}