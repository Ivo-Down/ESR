import java.net.InetAddress;

public class PendingRequests {
    private Integer packetID;
    private OttPacket packet;
    private InetAddress ipDestinyNode;
    private Integer portDestinyNode;
    private Integer sentCounter;
    private Integer timeStamp;

    public PendingRequests(Integer packetID, OttPacket packet, InetAddress ipDestinyNode, Integer portDestinyNode, Integer sentCounter, Integer timeStamp) {
        this.packetID = packetID;
        this.packet = packet;
        this.ipDestinyNode = ipDestinyNode;
        this.portDestinyNode = portDestinyNode;
        this.sentCounter = sentCounter;
        this.timeStamp = timeStamp;
    }

    public OttPacket getPacket() {
        return packet;
    }

    public Integer getPacketID() {
        return packetID;
    }

    public void setPacketID(Integer packetID) {
        this.packetID = packetID;
    }

    public InetAddress getIpDestinyNode() {
        return ipDestinyNode;
    }

    public void setIpDestinyNode(InetAddress ipDestinyNode) {
        this.ipDestinyNode = ipDestinyNode;
    }

    public Integer getPortDestinyNode() {
        return portDestinyNode;
    }

    public void setPortDestinyNode(Integer portDestinyNode) {
        this.portDestinyNode = portDestinyNode;
    }

    public Integer getSentCounter() {
        return sentCounter;
    }

    public void setSentCounter(Integer sentCounter) {
        this.sentCounter = sentCounter;
    }

    public Integer getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Integer timeStamp) {
        this.timeStamp = timeStamp;
    }
}
