/* ------------------
   Cliente
   usage: java Cliente
   adaptado dos originais pela equipa docente de ESR (nenhumas garantias)
   colocar o cliente primeiro a correr que o servidor dispara logo!
   ---------------------- */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {

  //------------------------------------GUI-------------------------------
  JFrame f = new JFrame("Cliente Teste");
  JButton setupButton = new JButton("Setup");
  JButton playButton = new JButton("Play");
  JButton pauseButton = new JButton("Pause");
  JButton tearButton = new JButton("Close");
  JPanel mainPanel = new JPanel();
  JPanel buttonPanel = new JPanel();
  JLabel statLabel1 = new JLabel();
  JLabel statLabel2 = new JLabel();
  JLabel statLabel3 = new JLabel();
  JLabel iconLabel = new JLabel();
  ImageIcon icon;


  //-------------------------------RTP VARIABLES--------------------------
  //DatagramPacket rcvdp; //UDP packet received from the server (to receive)
  //DatagramSocket RTPsocket; //socket to be used to send and receive UDP packet

  static int RTP_RCV_PORT = 25001; //port where the client will receive the RTP packets
  Timer cTimer; //timer used to receive data from the UDP socket
  //byte[] cBuf; //buffer used to store data received from the server

  //---------------------------STATISTICS VARIABLES-----------------------
  //Em desenvolvimento, a ignorar
  double statDataRate;        //Rate of video data received in bytes/s
  int statTotalBytes;         //Total number of bytes received in a session
  double statStartTime;       //Time in milliseconds when start is pressed
  double statTotalPlayTime;   //Time in milliseconds of video playing since beginning
  float statFractionLost;     //Fraction of RTP data packets from sender lost since the prev packet was sent
  int statCumLost;            //Number of packets lost
  int statExpRtpNb;           //Expected Sequence number of RTP messages within the session
  int statHighSeqNb;          //Highest sequence number received in session
  LinkedBlockingQueue<RTPpacket> framesQueue;

  //-------------------------------CONSTRUCTOR-------------------------------
  public Client(LinkedBlockingQueue<RTPpacket> framesQueue) {

    //------------------------------BUILD GUI-------------------------------
    //Frame
    f.addWindowListener(new WindowAdapter() {
       public void windowClosing(WindowEvent e) {
	 System.exit(0);
       }
    });

    //Buttons
    buttonPanel.setLayout(new GridLayout(1,0));
    buttonPanel.add(setupButton);
    buttonPanel.add(playButton);
    buttonPanel.add(pauseButton);
    buttonPanel.add(tearButton);

    //Handlers
    playButton.addActionListener(new playButtonListener());
    tearButton.addActionListener(new tearButtonListener());

    //Statistics
    statLabel1.setText("Total Bytes Received: 0");
    statLabel2.setText("Packets Lost: 0");
    statLabel3.setText("Data Rate (bytes/sec): 0");

    //Image display label
    iconLabel.setIcon(null);

    //Frame layout
    mainPanel.setLayout(null);
    mainPanel.add(iconLabel);
    mainPanel.add(buttonPanel);
    mainPanel.add(statLabel1);
    mainPanel.add(statLabel2);
    mainPanel.add(statLabel3);
    iconLabel.setBounds(0,0,380,280);
    buttonPanel.setBounds(0,280,380,50);
    statLabel1.setBounds(0,330,380,20);
    statLabel2.setBounds(0,350,380,20);
    statLabel3.setBounds(0,370,380,20);

    //Box
    f.getContentPane().add(mainPanel, BorderLayout.CENTER);
    f.setSize(new Dimension(390,430));
    f.setVisible(true);


    //Init para a parte do cliente
    //--------------------------
    cTimer = new Timer(20, new clientTimerListener());
    cTimer.setInitialDelay(0);
    cTimer.setCoalesce(true);
    //cBuf = new byte[15000]; //allocate enough memory for the buffer used to receive data from the server
    this.framesQueue=framesQueue;

    /*try {
      // Socket e Video
	  RTPsocket = socket; //new DatagramSocket(RTP_RCV_PORT); //init RTP socket (o mesmo para o cliente e servidor)
      RTPsocket.setSoTimeout(5000); // setimeout to 5s
    } catch (SocketException e) {
        System.out.println("Cliente: erro no socket: " + e.getMessage());
    }*/
  }

  //Para fins estatísticos. Ignorar para já
  private void updateStatsLabel() {
    DecimalFormat formatter = new DecimalFormat("###,###.##");
    statLabel1.setText("Total Bytes Received: " + statTotalBytes);
    statLabel2.setText("Packet Lost Rate: " + formatter.format(statFractionLost));
    statLabel3.setText("Data Rate: " + formatter.format(statDataRate) + " bytes/s");
  }


  //-------------------------------HANDLERS----------------------------------


  //-----------------------------FOR PLAY BUTTON------------------------------
  class playButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e){

    System.out.println("Play Button pressed !"); 
	      //start the timers ... 
	      cTimer.start();
	    }
  }

  //-----------------------------FOR TEAR BUTTON------------------------------
  class tearButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e){

      System.out.println("Teardown Button pressed !");  
	  //stop the timer
	  cTimer.stop();
	  //exit
	  System.exit(0);
	}
    }

  //---------------------------FOR TIMER (CLIENTE)--------------------------
  class clientTimerListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      
      //Construct a DatagramPacket to receive data from the UDP socket
      //rcvdp = new DatagramPacket(cBuf, cBuf.length);

      //receive the DP from the socket:
      //RTPsocket.receive(rcvdp);

      double curTime = System.currentTimeMillis();
      statTotalPlayTime += curTime - statStartTime;
      statStartTime = curTime;

      //create an RTPpacket object from the DP
      RTPpacket rtp_packet = new RTPpacket();
      try{
        rtp_packet = framesQueue.take();
      } catch (InterruptedException ex){
        ex.printStackTrace();
      }
      int seqNb = rtp_packet.getSequenceNumber();

      //print header bitstream:
      //rtp_packet.printPacketHeader();

      //get the payload bitstream from the RTPpacket object
      int payload_length = rtp_packet.getPayloadSize();
      byte [] payload = new byte[payload_length];
      payload = rtp_packet.getPayload();

      //compute stats and update the label in GUI
      statExpRtpNb++;
      if (seqNb > statHighSeqNb) {
        statHighSeqNb = seqNb;
      }
      if (statExpRtpNb != seqNb) {
        statCumLost++;
      }

      //Ignorar, estatísticas...
      statDataRate = statTotalPlayTime == 0 ? 0 : (statTotalBytes / (statTotalPlayTime / 1000.0));
      statFractionLost = (float)statCumLost / statHighSeqNb;
      statTotalBytes += payload_length;
      updateStatsLabel();


      //get an Image object from the payload bitstream
      Toolkit toolkit = Toolkit.getDefaultToolkit();
      Image image = toolkit.createImage(payload, 0, payload_length);

      //display the image as an ImageIcon object
      icon = new ImageIcon(image);
      iconLabel.setIcon(icon);
    }
  }

}

