import java.net.InetAddress;

public class NodeInfo {

    enum nodeState {
        READY, RUNNING, BLACKLISTED       //TODO decidir os estados a termos
    }

    private InetAddress nodeIp;
    private int nodePort;
    private nodeState nodeState;

    public NodeInfo(InetAddress ip, int port, nodeState state){
        this.nodeIp = ip;
        this.nodePort = port;
        this.nodeState = state;
    }

    public NodeInfo(InetAddress ip, int port){
        this.nodeIp = ip;
        this.nodePort = port;
        this.nodeState = nodeState.READY;
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