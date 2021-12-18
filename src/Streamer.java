/* ------------------
   Servidor
   usage: java Servidor [Video file]
   adaptado dos originais pela equipa docente de ESR (nenhumas garantias)
   colocar primeiro o cliente a correr, porque este dispara logo
   ---------------------- */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;


public class Streamer extends JFrame implements ActionListener {

  //------------------------------------GUI-------------------------------
  JLabel label;


  
  //-------------------------------RTP VARIABLES--------------------------
    DatagramSocket RTPsocket; //socket to be used to send and receive UDP packet
    HashMap<Integer, NodeInfo> nodesToStreamTo;
    Lock nodesToStreamToLock;


    static String VideoFileName; //video file to request to the server


      //----------------------------VIDEO CONSTANTS-------------------------------
      int imagenb = 0; //image nb of the image currently transmitted
      VideoStream video; //VideoStream object used to access video frames
      static int FRAME_PERIOD = 40; //Frame period of the video to stream, in ms
      static int VIDEO_LENGTH = 500; //length of the video in frames

      Timer sTimer; //timer used to send the images at the video frame rate
      byte[] sBuf; //buffer used to store the images to send to the client


  //-------------------------------CONSTRUCTOR-------------------------------
  public Streamer(String Video, HashMap<Integer, NodeInfo> nodesToStreamTo, Lock nodesToStreamToLock, DatagramSocket RTPsocket) {

      //init Frame
      super("Streamer");

      VideoFileName=Video;

      //init para a parte do servidor
      sTimer = new Timer(FRAME_PERIOD, this); //init Timer para servidor
      sTimer.setInitialDelay(0);
      sTimer.setCoalesce(true);
      sBuf = new byte[15000]; //allocate memory for the sending buffer
      this.nodesToStreamTo = nodesToStreamTo;
      this.nodesToStreamToLock = nodesToStreamToLock;
      this.RTPsocket = RTPsocket;

      try {


	    video = new VideoStream(VideoFileName); //init the VideoStream object:
        System.out.println("Servidor: vai enviar video do file " + VideoFileName);

      } catch (SocketException e) {
        System.out.println("Servidor: erro no socket: " + e.getMessage());
      } catch (Exception e) {
        System.out.println("Servidor: erro no video: " + e.getMessage());
      }

      //Handler to close the main window
      addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) { //stop the timer and exit
          sTimer.stop();
          System.exit(0);
        }});

      //GUI:
      pack();
      setVisible(true);
      setSize(new Dimension(400, 400));
      label = new JLabel("Send frame #        ", JLabel.CENTER);
      getContentPane().add(label, BorderLayout.CENTER);

      sTimer.start();
  }



  //-------------------------------HANDLERS----------------------------------

  //-------------------------------FOR TIMER---------------------------------
  public void actionPerformed(ActionEvent e) {
          //if the current image nb is less than the length of the video
            if (imagenb < VIDEO_LENGTH) {

                //update current imagenb
                imagenb++;
                try {
                    //get next frame to send from the video, as well as its size
                    int image_length = video.getnextframe(sBuf);


                    //For each node sends stream frame
                    try{
                        this.nodesToStreamToLock.lock();
                        for(Map.Entry<Integer, NodeInfo> nodeToStream : this.nodesToStreamTo.entrySet()){

                            RTPpacket rtp_packet = new RTPpacket(sBuf, 26, imagenb, Constants.SERVER_ID, image_length);
                            InetAddress ip = nodeToStream.getValue().getNodeIp();
                            int port = nodeToStream.getValue().getNodePort();

                            DatagramPacket packet = new DatagramPacket(rtp_packet.getPacket(), rtp_packet.getPacketSize(), ip, port);
                            RTPsocket.send(packet);
                        }
                    }
                    finally {
                        this.nodesToStreamToLock.unlock();
                    }



                   // System.out.println("Send frame #" + imagenb);
                    //print the header bitstream
                    //rtp_packet.printPacketHeader();

                    //update GUI
                    label.setText("Send frame #" + imagenb);
                } catch (Exception ex) {
                    System.out.println("Exception caught: " + ex);
                    System.exit(0);
                }
            } else {
                //if we have reached the end of the video file, stop the timer
                //sTimer.stop();
                try{
                    video = new VideoStream(VideoFileName);
                    imagenb = 0;
                } catch (Exception exception){
                    exception.printStackTrace();
                }
            }

  }
}
