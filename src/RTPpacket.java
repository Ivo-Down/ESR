import java.nio.ByteBuffer;
import java.util.Arrays;

public class RTPpacket{

  //Fields that compose the RTP header
  public int Version;
  public int Padding;
  public int Extension;
  public int CC;
  public int Marker;
  public int PayloadType;
  public int SequenceNumber;
  public int TimeStamp;
  public int Ssrc;
  public int senderId;
  
  //Bitstream of the RTP header
  public byte[] header;

  //size of the RTP payload
  public int payload_size;
  //Bitstream of the RTP payload
  public byte[] payload;


    /** Payload Types:
     *  0 -> asking the server for the neighbors
     *
     */



  //--------------------------
  //Constructor of an RTPpacket object from header fields and payload bitstream
  //--------------------------
  public RTPpacket(int PType, int Framenb, int senderId, int Time, byte[] data, int data_length){
    //fill by default header fields:
    this.Version = 2;
    this.Padding = 0;
    this.Extension = 0;
    this.CC = 0;
    this.Marker = 0;
    this.Ssrc = 0;

    //fill changing header fields:
    this.SequenceNumber = Framenb;
    this.TimeStamp = Time;
    this.PayloadType = PType;
    this.senderId = senderId;
    
    //build the header bistream:
    //--------------------------
    this.header = new byte[Constants.HEADER_SIZE];

    //.............
    //TO COMPLETE
    //.............

    //fill the header array of byte with RTP header fields
    header[0] = (byte)(Version << 6 | Padding << 5 | Extension << 4 | CC);
    header[1] = (byte)(Marker << 7 | PayloadType & 0x000000FF);
    header[2] = (byte)(SequenceNumber >> 8);
    header[3] = (byte)(SequenceNumber & 0xFF);
    header[4] = (byte)(TimeStamp >> 24);
    header[5] = (byte)(TimeStamp >> 16);
    header[6] = (byte)(TimeStamp >> 8);
    header[7] = (byte)(TimeStamp & 0xFF);
    header[8] = (byte)(Ssrc >> 24);
    header[9] = (byte)(Ssrc >> 16);
    header[10] = (byte)(Ssrc >> 8);
    header[11] = (byte)(Ssrc & 0xFF);
    this.setSender(senderId);

    //fill the payload bitstream:
    //--------------------------
    payload_size = data_length;
    payload = new byte[data_length];

    //fill payload array of byte from data (given in parameter of the constructor)
    this.payload = Arrays.copyOfRange(data,Constants.HEADER_SIZE, Constants.HEADER_SIZE + data_length);
    this.printheader();

  }
    
  //--------------------------
  //Constructor of an RTPpacket object from the packet bitstream
  //--------------------------
  public RTPpacket(byte[] packet, int packet_size)
  {
    //fill default fields:
    Version = 2;
    Padding = 0;
    Extension = 0;
    CC = 0;
    Marker = 0;
    Ssrc = 0;

    //check if total packet size is lower than the header size
    if (packet_size >= Constants.HEADER_SIZE)
      {
	//get the header bitsream:
	header = new byte[Constants.HEADER_SIZE];
	for (int i=0; i < Constants.HEADER_SIZE; i++)
	  header[i] = packet[i];

	//get the payload bitstream:
	payload_size = packet_size - Constants.HEADER_SIZE;
	payload = new byte[payload_size];
	for (int i=Constants.HEADER_SIZE; i < packet_size; i++)
	  payload[i-Constants.HEADER_SIZE] = packet[i];

	//interpret the changing fields of the header:
	PayloadType = header[1] & 127;
	SequenceNumber = unsigned_int(header[3]) + 256*unsigned_int(header[2]);
	TimeStamp = unsigned_int(header[7]) + 256*unsigned_int(header[6]) + 65536*unsigned_int(header[5]) + 16777216*unsigned_int(header[4]);
	this.senderId = this.getSender();
      }
 }

  //--------------------------
  //getpayload: return the payload bistream of the RTPpacket and its size
  //--------------------------
  public int getpayload(byte[] data) {

    for (int i=0; i < payload_size; i++)
      data[i] = payload[i];

    return(payload_size);
  }

  //--------------------------
  //getpayload_length: return the length of the payload
  //--------------------------
  public int getpayload_length() {
    return(payload_size);
  }

  //--------------------------
  //getlength: return the total length of the RTP packet
  //--------------------------
  public int getlength() {
    return(payload_size + Constants.HEADER_SIZE);
  }

  //--------------------------
  //getpacket: returns the packet bitstream and its length
  //--------------------------
  /*public int getpacket(byte[] packet)
  {
    //construct the packet = header + payload
    for (int i=0; i < Constants.HEADER_SIZE; i++)
	packet[i] = header[i];
    for (int i=0; i < payload_size; i++)
	packet[i+Constants.HEADER_SIZE] = payload[i];

    //return total size of the packet
    return(payload_size + Constants.HEADER_SIZE);
  }*/

    public byte[] getPacket(){
        byte [] res = new byte [this.header.length + this.payload.length];
        System.arraycopy(this.header,0, res, 0, this.header.length);
        System.arraycopy(this.payload,0, res, this.header.length, this.payload.length);
        return res;
    }

    public int getPacketSize(){
        return this.header.length + this.payload.length;
    }

  //--------------------------
  //gettimestamp
  //--------------------------

  public int gettimestamp() {
    return(TimeStamp);
  }

  //--------------------------
  //getsequencenumber
  //--------------------------
  public int getsequencenumber() {
    return(SequenceNumber);
  }

  //--------------------------
  //getpayloadtype
  //--------------------------
  public int getpayloadtype() {
    return(PayloadType);
  }


  //--------------------------
  //print headers without the SSRC
  //--------------------------
  public void printheader()
  {
        System.out.print("[RTP-Header] ");
        System.out.println("Version: " + Version 
                           + ", Padding: " + Padding
                           + ", Extension: " + Extension 
                           + ", CC: " + CC
                           + ", Marker: " + Marker 
                           + ", PayloadType: " + PayloadType
                           + ", SequenceNumber: " + SequenceNumber
                           + ", TimeStamp: " + TimeStamp);
  }

  //return the unsigned value of 8-bit integer nb
  static int unsigned_int(int nb) {
    if (nb >= 0)
      return(nb);
    else
      return(256+nb);
  }

  public void setSender(int senderId){
      byte[] Bytes = ByteBuffer.allocate(4).putInt(senderId).array();
      int i = 0;
      for(byte b : Bytes){
          this.header[i++] = b;
      }
  }

    public int getSender(){
        return ByteBuffer.wrap(this.header,12,4).getInt();
    }

}
