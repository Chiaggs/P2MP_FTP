import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

public class Client {
    public static class ServerMapping{
        InetAddress serverAddress;
        DatagramSocket socket;
        boolean ack_received;
        ServerMapping(String ServerIP) throws Exception{
            serverAddress = InetAddress.getByName(ServerIP);
            socket = new DatagramSocket();
            socket.setSoTimeout(60);
            ack_received = false;
        }
    }
    public static class DataHeader{
        public int headerSizeInBytes;
        char checksum;
        public char dataPacketField;
        int sequenceNumber;
        DataHeader(int sequenceNumber){
            headerSizeInBytes = 8;
            dataPacketField = 0b0101010101010101;
            this.sequenceNumber = sequenceNumber;
        }
        byte[] getHeader(){
            ByteBuffer buffer = ByteBuffer.allocate(headerSizeInBytes);
            buffer.putInt(sequenceNumber);
            buffer.putChar(checksum);
            buffer.putChar(dataPacketField);
            return buffer.array();
        }
        void calculateChecksum(byte[] data_block){
            int length = data_block.length;
            int i = 0;
            long sum = 0;
            long data;
            while (length > 1) {
                data = (((data_block[i] << 8) & 0xFF00) | ((data_block[i + 1]) & 0xFF));
                sum += data;
                if ((sum & 0xFFFF0000) > 0) {
                    sum = sum & 0xFFFF;
                    sum += 1;
                }
                i += 2;
                length -= 2;
            }
            if (length > 0) {
                sum += (data_block[i] << 8 & 0xFF00);
                if ((sum & 0xFFFF0000) > 0) {
                    sum = sum & 0xFFFF;
                    sum += 1;
                }
            }
            sum = ~sum;
            sum = sum & 0xFFFF;
            this.checksum = (char) sum;
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
            int sequenceNumber = 0;
            while (extractSize != 0){
                byte[] extractData = Arrays.copyOfRange(fileInBytes, (int) currentIndex, (int)(currentIndex + extractSize));
                System.out.println("Sending from " + currentIndex + " To " + (currentIndex + extractSize));
                currentIndex += extractSize;
                remainingTranferBytes = remainingTranferBytes - extractSize;
                extractSize = Math.min(remainingTranferBytes, MSS);
                rdt_send(extractData, sequenceNumber);
                sequenceNumber += extractData.length;
            }
        }
        else{
            System.out.println("Input parameters incorrect, exiting.. ");
        }
    }

    public static void rdt_send(byte[] data, int sequenceNumber) throws Exception{
        System.out.println("Call made to rdt send");
        DataHeader header = new DataHeader(sequenceNumber);
        header.calculateChecksum(data);
        ByteBuffer dataToSend = ByteBuffer.allocate(8 + data.length);
        dataToSend.put(header.getHeader());
        dataToSend.put(data);
        byte[] dataToSendInBytes = dataToSend.array();
        int ack_counter = ServerList.size();
        int expected_ack = sequenceNumber + data.length;
        for(int i = 0; i < ServerList.size(); i ++){
            DatagramPacket dpSend = new DatagramPacket(dataToSendInBytes, dataToSendInBytes.length, ServerList.get(i).serverAddress, SERVER_PORT);
            ServerList.get(i).socket.send(dpSend);
        }
        while(ack_counter != 0){
            for(int i = 0; i < ServerList.size(); i ++){
                byte[] serverResponse = new byte[8];
                DatagramPacket resPacket = new DatagramPacket(serverResponse, serverResponse.length);
                try{
                    if(ServerList.get(i).ack_received == true)
                        continue;
                    ServerList.get(i).socket.receive(resPacket);
                    byte[] recv_ack_bytes = Arrays.copyOfRange(resPacket.getData(), 0, 4);
                    int recv_ack = ByteBuffer.wrap(recv_ack_bytes).getInt();
                    if(recv_ack == expected_ack){
                        ack_counter--;
                        ServerList.get(i).ack_received = true;
                    }
                }
                catch(SocketTimeoutException ste){
                    DatagramPacket dpSend = new DatagramPacket(dataToSendInBytes, dataToSendInBytes.length, ServerList.get(i).serverAddress, SERVER_PORT);
                    System.out.println("Timeout occured at sequence: " + sequenceNumber);
                    ServerList.get(i).socket.send(dpSend);
                }
            }
        }
    }
}
