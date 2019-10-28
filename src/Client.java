import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

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

    // Main method
    public static void main(String args[]) throws Exception{
        int argLength = args.length;
        if(argLength > 2) {
            // Commandline Parameters
            String filename = args[argLength - 2];
            long MSS = Integer.parseInt(args[argLength - 1]);
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
            byte[] fileInBytes = Files.readAllBytes(temp.toPath());
            if(temp.exists()) {
                System.out.println("File exists");
            }

            // Initialise server list
            for(int i = 0; i < servers.length; i ++){
                ServerMapping sm = new ServerMapping(servers[i]);
                ServerList.add(sm);
            }

            long extractSize = Math.min(Math.abs(temp.length() - MSS), MSS);
            long remainingTranferBytes = temp.length();
            long currentIndex = 0;
            while (extractSize != 0){
                byte[] extractData = Arrays.copyOfRange(fileInBytes, (int) currentIndex, (int)(currentIndex + extractSize));
                currentIndex += extractSize;
                remainingTranferBytes = remainingTranferBytes - extractSize;
                extractSize = Math.min(remainingTranferBytes, MSS);
                rdt_send(extractData);
            }
            rdt_send(null);
        }
        else{
            System.out.println("Input parameters incorrect, exiting.. ");
        }
    }

    public static void rdt_send(byte[] data) throws Exception{
        System.out.println("Call made to rdt send");
        String temp_data = "Hello";
        byte buf[] = temp_data.getBytes();
        DatagramPacket dpSend = new DatagramPacket(buf, buf.length, ServerList.get(0).serverAddress, SERVER_PORT);
        ServerList.get(0).socket.send(dpSend);
    }
}
