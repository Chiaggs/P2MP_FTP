public class Server {
    public static final int PORT = 7735;
    public static void main(String args[]){
        if(args.length == 2) {
            String filename = args[0];
            float probabilistic_loss = Float.parseFloat(args[1]);
        }
        else{
            System.out.println("Input parameters incorrect, exiting.. ");
        }
    }
}
