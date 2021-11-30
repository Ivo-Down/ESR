import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class OrrRunner {

    public static void main(String[] args) {
        Ott ott = new Ott();
        try{

            ott.setIp(InetAddress.getByName(args[0]));
            args = Arrays.copyOfRange(args, 1, args.length);

            for (String arg : args){
                InetAddress neighbor_ip = InetAddress.getByName(arg);
                ott.setNeighbors_table(neighbor_ip, 1);
            }

            //int serverPort = Integer.parseInt(args[1]);      --> guardar portas?

        } catch (UnknownHostException e){
            e.printStackTrace();
            System.out.println("Invalid IP.");
        }
    }

}
