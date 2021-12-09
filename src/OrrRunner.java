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
                    String[] info = args[0].split(":");
                    Ott overlayNode = new Ott(Integer.parseInt(info[2]), InetAddress.getByName(info[0]), Integer.parseInt(info[1]),false);
                    System.out.println(overlayNode);
                    overlayNode.run();
                    break;

                case 2: // "ip:port:nodo_id" "c"
                    String[] infoo = args[0].split(":");
                    Ott overlayNodeCli = new Ott(Integer.parseInt(infoo[2]), InetAddress.getByName(infoo[0]), Integer.parseInt(infoo[1]),true);
                    System.out.println(overlayNodeCli);
                    overlayNodeCli.run();
                    break;

                default:
                    System.out.println("Looks like something is wrong...");
      }

        } catch (UnknownHostException e){
            e.printStackTrace();
            System.out.println("Invalid IP.");
        }
    }

}
