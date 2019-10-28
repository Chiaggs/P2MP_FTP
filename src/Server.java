import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Server {

    public static final int PORT = 7735;
    public static int BUFFER_SIZE = 65535;
    public static byte[] buffer;
    public static DatagramSocket UDP_Socket;
    public static final int HEADER_SIZE = 8;
    public static int ACK_1 = 0x0000;
    public static int ACK_2 = 0xAAAA;
    public static double probabilistic_loss;
    public static ArrayList<Integer> listSeqNum;
    public static int oldSequenceNum;
    public static String fileName;

    public static void Initialize_Server() throws Exception{
        UDP_Socket = new DatagramSocket(PORT);
        buffer = new byte[BUFFER_SIZE];
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
                //System.out.println("Received data is: " + ConvertDataToString(buffer));
                buffer = new byte[BUFFER_SIZE];

                System.out.print("Server waiting for receiving files");

                int seqNum = processRequest(Arrays.copyOfRange(receivedPacket.getData(), 0, receivedPacket.getLength()));
                System.out.println("SeqNum is: " + seqNum);

                int size = ((Integer.SIZE)/8) + ((Character.SIZE * 2)/8);
                System.out.println("Size is: " + size);
                ByteBuffer bf = ByteBuffer.allocate(size);
                bf.putInt(seqNum);
                bf.putChar((char)ACK_1);
                bf.putChar((char)ACK_2);
                byte[] segmentAck = bf.array();

                receivedPacket.setData(segmentAck);
                UDP_Socket.send(receivedPacket);
                buffer = new byte[BUFFER_SIZE];
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

    public static int processRequest(byte[] data) {
        byte[] sequenceBytes = Arrays.copyOfRange(data, 0, 4);
        byte[] checksumBytes = new byte[4];
        byte[] _checksumBytes = Arrays.copyOfRange(data, 4, 6);
        checksumBytes[0] = 0;
        checksumBytes[1] = 0;
        checksumBytes[2] = _checksumBytes[0];
        checksumBytes[3] = _checksumBytes[1];

        byte[] dataBytes = Arrays.copyOfRange(data, HEADER_SIZE, data.length);

        int sequenceNum = ByteBuffer.wrap(sequenceBytes).getInt();
        int mss = data.length - HEADER_SIZE;

        if(!checkProbValue(sequenceNum)) {
            return -1;
        }

        int checksum = ByteBuffer.wrap(checksumBytes).getInt();
        if(!checkCheckSum(dataBytes, checksum)) {
            return -1;
        }

        int ackSequenceNum = getSequenceNumber(sequenceNum, mss);
        System.out.println("\nAck num: " + ackSequenceNum);

        if (ackSequenceNum != -1) {
            saveToFile(dataBytes);
        }

        oldSequenceNum = sequenceNum;
        //oldMSS = mss;

        return ackSequenceNum;
    }

    public static Boolean checkProbValue(int sequenceNum) {
        double r = new Random().nextDouble();
        if(r <= probabilistic_loss) {
            System.out.print("\nPacket discarded hence packet loss where sequence number = "+sequenceNum);
            return false;
        }
        return true;
    }

    public static Boolean checkCheckSum(byte[] data, int checksum) {

        ByteBuffer ba = ByteBuffer.allocate(data.length + (Integer.SIZE)/8);
        ba.putInt(checksum);
        ba.put(data);
        byte [] input = ba.array();

        long dataSum = generateSumofDataandCheckum(input);
        boolean isVerified = (dataSum == 0xFFFF);

        return isVerified;
    }

    public static long generateSumofDataandCheckum(byte[] input) {
        int bufferLength = input.length;
        int i = 0;
        long addition = 0;
        long data;

        while (bufferLength > 1) {
            data = (((input[i] << 8) & 0xFF00) | ((input[i + 1]) & 0xFF));
            addition += data;
            if ((addition & 0xFFFF0000) > 0) {
                addition = addition & 0xFFFF;
                addition += 1;
            }
            i += 2;
            bufferLength -= 2;
        }
        if (bufferLength > 0) {
            addition += (input[i] << 8 & 0xFF00);
            if ((addition & 0xFFFF0000) > 0) {
                addition = addition & 0xFFFF;
                addition += 1;
            }
        }
        return addition;
    }

    public static int getSequenceNumber(int seqNum, int mss) {
        int temp = seqNum + mss;
        int size = listSeqNum.size();

        if (size == 0) {
            listSeqNum.add(temp);
            return temp;
        }

        int prevSeqNum = listSeqNum.get(size - 1);

        if(seqNum == oldSequenceNum) {
            System.out.print("\nDuplicate Seq # " + seqNum + " received. " + "Resending previous ACK number # " + prevSeqNum);
            return prevSeqNum;
        }

        if((prevSeqNum + mss) == temp) {
            listSeqNum.add(temp);
            return temp;
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
