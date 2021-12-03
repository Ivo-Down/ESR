import java.net.InetAddress;
import java.net.UnknownHostException;


public class OrrRunner {

    public static void main(String[] args) {
        try{

            if (args.length == 0){  // Se não tem argumentos então é o boostrapper
                OttBootStrapper bootStrapper = new OttBootStrapper();
                System.out.println(bootStrapper);
                bootStrapper.run();


            }

            else{  // ip:port:node_id
                String[] info = args[0].split(":");
                Ott overlayNode = new Ott(Integer.parseInt(info[2]), InetAddress.getByName(info[0]), Integer.parseInt(info[1]));
                System.out.println(overlayNode);
                overlayNode.run();


            }


        } catch (UnknownHostException e){
            e.printStackTrace();
            System.out.println("Invalid IP.");
        }
    }

}
