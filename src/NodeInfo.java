import java.io.*;
import java.net.InetAddress;

public class NodeInfo implements Serializable {

    private static final long serialVersionUID = 5249369646723187393L;
    enum nodeState {
        ON, OFF, BLACKLISTED       //TODO decidir os estados a termos
    }

    private InetAddress nodeIp;
    private Integer nodePort;
    private nodeState nodeState;

    public NodeInfo(InetAddress ip, int port, nodeState state){
        this.nodeIp = ip;
        this.nodePort = port;
        this.nodeState = state;
    }

    public NodeInfo(InetAddress ip, int port){
        this.nodeIp = ip;
        this.nodePort = port;
        this.nodeState = nodeState.ON;
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