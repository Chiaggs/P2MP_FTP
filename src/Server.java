import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Server {
    // variables
    public static final int PORT = 7735;
    public static int BUFFER_SIZE = 65535;
    public static byte[] buffer;
    public static DatagramSocket UDP_Socket;

    // Initialization functions
    public static void Initialize_Server() throws Exception{
        UDP_Socket = new DatagramSocket(PORT);
        buffer = new byte[BUFFER_SIZE];
    }

    // main
    public static void main(String args[]) throws Exception{
        if(args.length == 2) {
            String filename = args[0];
            float probabilistic_loss = Float.parseFloat(args[1]);
            Initialize_Server();
            while(true){
                DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
                UDP_Socket.receive(receivedPacket);
                System.out.println("Received data is: " + ConvertDataToString(buffer));
                buffer = new byte[BUFFER_SIZE];
            }
        }
        else{
            System.out.println("Input parameters incorrect, exiting.. ");
        }
    }

    // Helper functions
    public static StringBuilder ConvertDataToString(byte[] a)
    {
        if (a == null)
            return null;
        StringBuilder ret = new StringBuilder();
        int i = 0;
        while (a[i] != 0)
        {
            ret.append((char) a[i]);
            i++;
        }
        return ret;
    }
}
