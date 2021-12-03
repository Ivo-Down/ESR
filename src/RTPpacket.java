import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class RTPpacket{

      /**private int Version;
      private int Padding;
      private int Extension;
      private int CC;
      private int Marker;
      private int PayloadType;
      private int SequenceNumber;
      private int TimeStamp;
      private int Ssrc;
      private int senderId; */

      private byte[] header;
      private byte[] payload;
      private InetAddress fromIp;
      private Integer fromPort;


    /** Payload Types:
     *  0 -> asking the server for the neighbors
     *
     */

    /**
     *  Header:
     *      int packetType
     *      int sequenceNumber
     *      int senderId
     *      int timeStamp
     *      int payloadSize
     */


    // ------------------------------ CONSTRUCTORS ------------------------------

    public RTPpacket(){
        this.payload = new byte[Constants.DEFAULT_BUFFER_SIZE];
    }

    public RTPpacket (byte[] packet){
        this.header = Arrays.copyOfRange(packet,0, Constants.HEADER_SIZE);
        int dataSize = getPayloadSize();
        this.payload = Arrays.copyOfRange(packet,Constants.HEADER_SIZE, Constants.HEADER_SIZE + dataSize);

    }

    public RTPpacket (byte[] packet, InetAddress fromIp, Integer fromPort){
        this.header = Arrays.copyOfRange(packet,0, Constants.HEADER_SIZE);
        int dataSize = getPayloadSize();
        this.payload = Arrays.copyOfRange(packet,Constants.HEADER_SIZE, Constants.HEADER_SIZE + dataSize);
        this.fromIp = fromIp;
        this.fromPort = fromPort;

    }

    public RTPpacket (byte[] payload, int packetType, int sequenceNumber, int senderId, int timeStamp){
        this.payload = payload;
        this.header = new byte[Constants.HEADER_SIZE];
        setPacketType(packetType);
        setSequenceNumber(sequenceNumber);
        setSenderId(senderId);
        setTimeStamp(timeStamp);
        setPayloadSize(payload.length);
    }



    // ------------------------------ GETTERS AND SETTERS ------------------------------

    public void setPacketType(int type){
        byte[] Bytes = ByteBuffer.allocate(4).putInt(type).array();
        int i = 0;
        for(byte b : Bytes){
            this.header[i++] = b;
        }
    }
    public int getPacketType(){
        return ByteBuffer.wrap(this.header,0,4).getInt();
    }


    public void setSequenceNumber(int chunkId){
        byte[] chunkIdBytes = ByteBuffer.allocate(4).putInt(chunkId).array();
        int i = 4;
        for(byte b : chunkIdBytes){
            this.header[i++] = b;
        }
    }
    public int getSequenceNumber(){
        return ByteBuffer.wrap(this.header,4,4).getInt();
    }


    public void setSenderId(int chunkId){
        byte[] chunkIdBytes = ByteBuffer.allocate(4).putInt(chunkId).array();
        int i = 8;
        for(byte b : chunkIdBytes){
            this.header[i++] = b;
        }
    }
    public int getSenderId(){
        return ByteBuffer.wrap(this.header,8,4).getInt();
    }


    public void setTimeStamp(int chunkId){
        byte[] chunkIdBytes = ByteBuffer.allocate(4).putInt(chunkId).array();
        int i = 12;
        for(byte b : chunkIdBytes){
            this.header[i++] = b;
        }
    }
    public int getTimeStamp(){
        return ByteBuffer.wrap(this.header,12,4).getInt();
    }


    public void setPayloadSize(int chunkId){
        byte[] chunkIdBytes = ByteBuffer.allocate(4).putInt(chunkId).array();
        int i = 16;
        for(byte b : chunkIdBytes){
            this.header[i++] = b;
        }
    }
    public int getPayloadSize(){
        return ByteBuffer.wrap(this.header,16,4).getInt();
    }


    public void setPayload(byte[] d, int size){
        if (size >= 0) System.arraycopy(d, 0, this.payload, 0, size);
    }
    public byte[] getPayload(){
        return Arrays.copyOfRange(this.payload, 0, this.getPayloadSize());
    }


    public byte[] getHeader(){
        return Arrays.copyOfRange(this.header, 0, Constants.HEADER_SIZE);
    }


    public byte[] getPacket(){
        byte [] res = new byte [this.header.length + this.payload.length];
        System.arraycopy(this.header,0, res, 0, this.header.length);
        System.arraycopy(this.payload,0, res, this.header.length, this.payload.length);
        return res;
    }
    public int getPacketSize(){
        return this.header.length + this.payload.length;
    }

    public InetAddress getFromIp() {
        return fromIp;
    }
    public void setFromIp(InetAddress fromIp) {
        this.fromIp = fromIp;
    }


    public Integer getFromPort() {
        return fromPort;
    }
    public void setFromPort(Integer fromPort) {
        this.fromPort = fromPort;
    }


    // ------------------------------ OTHER METHODS ------------------------------

    public void printPacket(){
        System.out.println("    PacketType " + this.getPacketType());
        System.out.println("    SequenceNumber " + this.getSequenceNumber());
        System.out.println("    SenderId " + this.getSenderId());
        System.out.println("    TimeStamp " + this.getTimeStamp());
        System.out.println("    PayloadSize " + this.getPayloadSize());
        System.out.println("    Payload " + new String(this.getPayload()));
    }

    public void printPacketHeader(){
        System.out.println("    PacketType " + this.getPacketType());
        System.out.println("    SequenceNumber " + this.getSequenceNumber());
        System.out.println("    SenderId " + this.getSenderId());
        System.out.println("    TimeStamp " + this.getTimeStamp());
        System.out.println("    PayloadSize " + this.getPayloadSize());
    }








}
