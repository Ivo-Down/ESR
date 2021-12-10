import java.io.*;
import java.net.InetAddress;

public class NodeInfo implements Serializable {

    private static final long serialVersionUID = 5249369646723187393L;
    enum nodeState {
        ON, OFF, UNKNOWN       //TODO decidir os estados a termos
    }

    private InetAddress nodeIp;
    private Integer nodePort;
    private nodeState nodeState;
    private Integer nodeId;

    public NodeInfo(InetAddress ip, int port, nodeState state, int nodeId){
        this.nodeIp = ip;
        this.nodePort = port;
        this.nodeState = state;
        this.nodeId = nodeId;
    }



    public InetAddress getNodeIp() {
        return nodeIp;
    }

    public void setNodeIp(InetAddress nodeIp) {
        this.nodeIp = nodeIp;
    }

    public int getNodePort() {
        return nodePort;
    }

    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodePort(int nodePort) {
        this.nodePort = nodePort;
    }

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
                '}';
    }
}