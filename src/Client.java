import java.io.IOException;

public class Client {

    public static String version = "1.0";

    public static void main(String args[]) throws IOException, InterruptedException {

        if (args.length != 2) {
            System.out.println("usage <peer_id> <is_initiator>");
            System.exit(1);
        }

        Peer peer = new Peer(Integer.parseInt(args[0]), Boolean.valueOf(args[1]),1);


		if (Boolean.valueOf(args[1]))
       	  // peer.backupFile("D:\\Data\\GitHub\\sdis1718-t1g03\\assets\\TestFile.jpeg", 2);
        	// peer.backupFile("../assets/TestFile.pdf", 1);
    	    peer.restoreFile("TestFile.pdf");


    }
}
