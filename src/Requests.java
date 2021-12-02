import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Requests {

    public class Request {
        private InetAddress requestIP;
        private int requestPort;
        private String filename;   // esta variável pode não vir preenchida
        private Socket socket;

        public Request(InetAddress IP, int port, String filename, Socket socket){
            this.requestIP = IP;
            this.requestPort = port;
            this.filename = filename;
            this.socket = socket;
        }

        public String getFilename(){
            return this.filename;
        }

        public InetAddress getClientIP() {
            return requestIP;
        }

        public int getClientPort() {
            return requestPort;
        }

        public Socket getSocket() {
            return socket;
        }


    }

    private LinkedBlockingQueue<Request> requestList;

    public Requests(){
        this.requestList = new LinkedBlockingQueue<>(); //thread safe
    }

    public void addRequest(InetAddress IP, int port, String filename, Socket socket){
        Request cr = new Request(IP, port, filename, socket);
        this.requestList.add(cr);
    }

    public boolean isEmpty(){
        return this.requestList.isEmpty();
    }


    public Request getNextRequest(long timeout){
        try{
            return this.requestList.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e){
            return null;
        }
    }

    public Request getNextRequest(){
        try{
            return this.requestList.take();
        } catch (InterruptedException e){
            return null;
        }
    }

    @Override
    public String toString() {
        return "Client_Requests{" +
                "requestList=" + requestList +
                '}';
    }
}