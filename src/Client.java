import java.io.File;
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
        Boolean ack_received;
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
        void checksumProcessor(byte[] data_block){
            int size = data_block.length, iterator = 0;
            long addition = 0;
            while (size > 1) {
                addition += (((data_block[iterator] << 8) & 0xFF00) | ((data_block[iterator + 1]) & 0xFF));
                if ((addition & 0xFFFF0000) > 0) {
                    addition = addition & 0xFFFF;
                    addition += 1;
                }
                iterator += 2;
                size -= 2;
            }
            if (size > 0) {
                addition += (data_block[iterator] << 8 & 0xFF00);
                if ((addition & 0xFFFF0000) > 0) {
                    addition = addition & 0xFFFF;
                    addition += 1;
                }
            }
            addition = ~addition;
            addition = addition & 0xFFFF;
            this.checksum = (char) addition;
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

            long startTime = System.currentTimeMillis();
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
                System.out.println("\nInside...");
                byte[] extractData = Arrays.copyOfRange(fileInBytes, (int) currentIndex, (int)(currentIndex + extractSize));
                System.out.println("Sending from " + currentIndex + " To " + (currentIndex + extractSize));
                currentIndex += extractSize;
                remainingTranferBytes = remainingTranferBytes - extractSize;
                extractSize = Math.min(remainingTranferBytes, MSS);
                try{
                    rdt_send(extractData, sequenceNumber);
                }
                catch(Exception e){
                    e.printStackTrace();
                }
                sequenceNumber += extractData.length;
            }
            System.out.println("\nTotal time taken: " + (System.currentTimeMillis() - startTime));
        }
        else{
            System.out.println("Input parameters incorrect, exiting.. ");
        }
    }

    public static void rdt_send(byte[] data, int sequenceNumber) throws Exception{
        System.out.println("Call made to rdt send");
        DataHeader header = new DataHeader(sequenceNumber);
        header.checksumProcessor(data);
        ByteBuffer dataToSend = ByteBuffer.allocate(8 + data.length);
        dataToSend.put(header.getHeader());
        dataToSend.put(data);
        byte[] dataToSendInBytes = dataToSend.array();
        int ack_counter = ServerList.size();
        int expected_ack = sequenceNumber + data.length;
        System.out.print("\nExpected ack from server: "+expected_ack);
        for(int i = 0; i < ServerList.size(); i ++){
            DatagramPacket dpSend = new DatagramPacket(dataToSendInBytes, dataToSendInBytes.length, ServerList.get(i).serverAddress, SERVER_PORT);
            ServerList.get(i).ack_received = false;
            ServerList.get(i).socket.send(dpSend);
        }
        while(ack_counter > 0){
            //System.out.print(ack_counter);
            for(int i = 0; i < ServerList.size(); i++){
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
                    System.out.println("\nTimeout occured at sequence: " + sequenceNumber);
                    ServerList.get(i).socket.send(dpSend);
                }
            }
        }
    }
}
