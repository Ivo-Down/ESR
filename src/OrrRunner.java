import java.net.InetAddress;
import java.net.UnknownHostException;


public class OrrRunner {

    public static void main(String[] args) {
        Ott ott = new Ott();
        try{

            if (args.length == 0){  // Se não tem argumentos então é o boostrapper
                ott.setIp(InetAddress.getByName(Constants.SERVER_ADDRESS));
                ott.setPort(Constants.DEFAULT_PORT);
                ott.setId(1);
                ott.setBootstrapper(true);
                System.out.println("Bootstrapper is running!");
            }

            else{  // ip:port:node_id
                String[] info = args[0].split(":");
                ott.setIp(InetAddress.getByName(info[0]));
                ott.setPort(Integer.parseInt(info[1]));
                ott.setId(Integer.parseInt(info[2]));
                ott.setBootstrapper(false);
            }


        } catch (UnknownHostException e){
            e.printStackTrace();
            System.out.println("Invalid IP.");
        }
    }

}
