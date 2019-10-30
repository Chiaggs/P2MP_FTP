import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Server {

    public static final int PORT = 7735;
    public static byte[] buffer;
    public static DatagramSocket UDP_Socket;
    public static double probabilistic_loss;
    public static ArrayList<Integer> listSeqNum;
    public static int oldSequenceNum;
    public static String fileName;

    public static void Initialize_Server() throws Exception{
        UDP_Socket = new DatagramSocket(PORT);
        buffer = new byte[65535];
        listSeqNum = new ArrayList<>();
    }

    public static void main(String args[]) throws Exception{
        if(args.length == 2) {
            fileName = args[0];
            probabilistic_loss = Double.parseDouble(args[1]);
            Initialize_Server();
            while(true){
                DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
                UDP_Socket.receive(receivedPacket);
                buffer = new byte[65535];
                System.out.print("Server waiting for receiving files");
                int seqNum = requestProcessor(Arrays.copyOfRange(receivedPacket.getData(), 0, receivedPacket.getLength()));
                System.out.println("SeqNum is: " + seqNum);
                StringBuilder convertedData = ConvertDataToString(receivedPacket.getData());
                if(convertedData != null){
                    ByteBuffer bf = ByteBuffer.allocate(8);
                    bf.putInt(seqNum);
                    bf.putChar((char)0x0000);
                    bf.putChar((char)0xAAAA);
                    byte[] segmentAck = bf.array();
                    receivedPacket.setData(segmentAck);
                    UDP_Socket.send(receivedPacket);
                    buffer = new byte[65535];
                }
            }
        }
        else{
            System.out.println("Input parameters incorrect, exiting.. ");
        }
    }

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

    public static int requestProcessor(byte[] data) {
        byte[] checksumProcessorByteArray = new byte[4];
        byte[] hashedProcessorHelper = Arrays.copyOfRange(data, 4, 6);
        for(int i = 0; i < 4; i ++ ){
            if(i == 0 || i == 1)
                checksumProcessorByteArray[i] = 0;
            else
                checksumProcessorByteArray[i] = hashedProcessorHelper[i-2];
        }
        int sequenceNum = ByteBuffer.wrap(Arrays.copyOfRange(data, 0, 4)).getInt();
        if(!checkProbValue(sequenceNum)) {
            return -1;
        }
        int checksum = ByteBuffer.wrap(checksumProcessorByteArray).getInt();
        if(!checkCheckSum(Arrays.copyOfRange(data, 8, data.length), checksum)) {
            return -1;
        }
        int ackSequenceNum = getSequenceNumber(sequenceNum, data.length - 8);
        System.out.println("\nAck num: " + ackSequenceNum);
        if (ackSequenceNum != -1) {
            saveToFile(Arrays.copyOfRange(data, 8, data.length));
        }
        oldSequenceNum = sequenceNum;
        return ackSequenceNum;
    }

    public static Boolean checkProbValue(int sequenceNum) {
        double r = new Random().nextDouble();
        boolean shouldReturn = true;
        if(r <= probabilistic_loss) {
            System.out.print("\nPacket discarded hence packet loss where sequence number = "+sequenceNum);
            shouldReturn = false;
        }
        return shouldReturn;
    }

    public static Boolean checkCheckSum(byte[] data, int checksum) {
        ByteBuffer ba = ByteBuffer.allocate(data.length + (Integer.SIZE)/8);
        ba.putInt(checksum);
        ba.put(data);
        return (generateSumofDataandCheckum(ba.array()) == 0xFFFF);
    }

    public static long generateSumofDataandCheckum(byte[] input) {
        int bufferLength = input.length;
        int i = 0;
        long addition = 0;

        while (bufferLength > 1) {
            addition += (((input[i] << 8) & 0xFF00) | ((input[i + 1]) & 0xFF));
            if ((addition & 0xFFFF0000) > 0) {
                addition = addition & 0xFFFF;
                addition++;
            }
            i += 2;
            bufferLength -= 2;
        }
        if (bufferLength > 0) {
            addition += (input[i] << 8 & 0xFF00);
            if ((addition & 0xFFFF0000) > 0) {
                addition = addition & 0xFFFF;
                addition++;
            }
        }
        return addition;
    }

    public static int getSequenceNumber(int seqNum, int mss) {
        int size = listSeqNum.size();
        if (size == 0) {
            listSeqNum.add(seqNum + mss);
            return seqNum + mss;
        }
        int prevSeqNum = listSeqNum.get(size - 1);
        if(seqNum == oldSequenceNum) {
            System.out.print("\nDuplicate Seq # " + seqNum + " received. " + "Resending previous ACK number # " + prevSeqNum);
            return prevSeqNum;
        }
        else if((prevSeqNum + mss) == seqNum + mss) {
            listSeqNum.add(seqNum + mss);
            return seqNum + mss;
        }
        else {
            System.out.print("\nSequence number from client # " + seqNum + " is wrong. " + "Requiring # " + prevSeqNum );
            return -1;
        }
    }

    public static void saveToFile(byte[] data) {
        try {
            System.out.println("Data length is: " + data.length);
            File file = new File(fileName);
            FileOutputStream fileOutStream = new FileOutputStream(file, true);
            fileOutStream.write(data);
            fileOutStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
