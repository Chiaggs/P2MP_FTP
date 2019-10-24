public class Client {
    public static void main(String args[]){
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
        }
        else{
            System.out.println("Input parameters incorrect, exiting.. ");
        }
    }
}
