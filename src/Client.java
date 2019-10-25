import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class Client {
    public static final int SERVER_PORT = 7735;
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
        }
        else{
            System.out.println("Input parameters incorrect, exiting.. ");
        }
    }

    public static void rdt_send(byte[] data){

    }
}
