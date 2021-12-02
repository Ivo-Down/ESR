import java.net.InetAddress;
import java.net.UnknownHostException;


public class OrrRunner {

    public static void main(String[] args) {
        try{

            if (args.length == 0){  // Se não tem argumentos então é o boostrapper
                OttBootStrapper bootStrapper = new OttBootStrapper();
                System.out.println(bootStrapper);
                System.out.println("Bootstrapper is running!");

                // TODO FICAR Á ESCUTA DE PEDIDOS
            }

            else{  // ip:port:node_id
                String[] info = args[0].split(":");
                Ott overlayNode = new Ott(Integer.parseInt(info[2]), InetAddress.getByName(info[0]), Integer.parseInt(info[1]));
                System.out.println(overlayNode);
                System.out.println("Overlay node is running!");

                //TODO ENVIAR PEDIDO AO BOOTSTRAPPER PARA PREENCHER VIZINHOS
            }


        } catch (UnknownHostException e){
            e.printStackTrace();
            System.out.println("Invalid IP.");
        }
    }

}
