import java.io.IOException;
import java.util.concurrent.*;

public class Client {

    public static String version = "1.0";

    public static void main(String args[]) throws IOException, InterruptedException {

        if (args.length != 2) {
            System.out.println("usage <peer_id> <is_initiator>");
            System.exit(1);
        }

        BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();
        Peer peer_mdb = new Peer(Integer.parseInt(args[0]), Boolean.valueOf(args[1]),1,queue);
        Peer peer_mc = new Peer(Integer.parseInt(args[0]), Boolean.valueOf(args[1]),2,queue);

        peer_mdb.start();
        peer_mc.start();

		if (Boolean.valueOf(args[1]))
       	  // peer.backupFile("D:\\Data\\GitHub\\sdis1718-t1g03\\assets\\TestFile.jpeg", 2);
        	// peer_mdb.backupFile("../assets/TestFile.pdf", 2);
    	    // peer_mc.restoreFile("TestFile.pdf");
          // peer_mc.deleteFile("../assets/TestFile.pdf");

    }
}
