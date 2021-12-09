import java.net.InetAddress;
import java.net.UnknownHostException;


public class OrrRunner {
    public static void main(String[] args) {
        try{
            switch (args.length) {
                case 0: //bootstrapper
                    OttBootStrapper bootStrapper = new OttBootStrapper();
                    System.out.println(bootStrapper);
                    bootStrapper.run();
                    break;

                case 1: // "ip:port:node_id"
                    String[] info1 = args[0].split(":");
                    Ott overlayNode = new Ott(Integer.parseInt(info1[2]), InetAddress.getByName(info1[0]), Integer.parseInt(info1[1]),false);
                    System.out.println(overlayNode);
                    overlayNode.run();
                    break;

                case 2: // "ip:port:nodo_id" "-c"
                    String[] info2 = args[0].split(":");
                    Ott overlayNodeCli = new Ott(Integer.parseInt(info2[2]), InetAddress.getByName(info2[0]), Integer.parseInt(info2[1]),true);
                    System.out.println(overlayNodeCli);
                    overlayNodeCli.run();
                    break;

                default:
                    System.out.println("Looks like something is wrong!...");
      }

        } catch (UnknownHostException e){
            e.printStackTrace();
            System.out.println("Invalid IP.");
        }
    }

}
