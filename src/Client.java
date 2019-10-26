import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

public class Client {
    public static class ServerMapping{
        InetAddress serverAddress;
        DatagramSocket socket;
        ServerMapping(String ServerIP) throws Exception{
            serverAddress = InetAddress.getByName(ServerIP);
            socket = new DatagramSocket();
            socket.setSoTimeout(500);
        }
    }
    public static final int SERVER_PORT = 7735;
    public static ArrayList<ServerMapping> ServerList = new ArrayList<ServerMapping>();
    public static void main(String args[]) throws Exception{
        int argLength = args.length;
        if(argLength > 2) {
            // Commandline Parameters
            String filename = args[argLength - 2];
            int MSS = Integer.parseInt(args[argLength - 1]);
            String[] servers = new String[argLength - 2];
            for (int i = 0; i < argLength - 2; i++){
                servers[i] = args[i];
            }

            // Logic
            System.out.println("FileName: " + filename);
            System.out.println("MSS: " + MSS);
            for (int i = 0; i < argLength - 2; i++){
                System.out.println("Servers: " + servers[i]);
            }

            // Fetching the file
            File temp = new File("src/"+filename);
            if(temp.exists()) {
                BufferedReader br1 = new BufferedReader(new FileReader(temp));
                System.out.println("File exists, and added to buffered reader");
            }

            // Initialise server list
            for(int i = 0; i < servers.length; i ++){
                ServerMapping sm = new ServerMapping(servers[i]);
                ServerList.add(sm);
            }
        }
        else{
            System.out.println("Input parameters incorrect, exiting.. ");
        }
    }

    public static void rdt_send(byte[] data){

    }
}
