import java.io.IOException;
import java.util.concurrent.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Client {

    public static String version = "1.0";
    private static Peer peer_mdb, peer_mc;

    public Client() {}

    public static void main(String args[]) throws IOException, InterruptedException {

        BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();
      try{

        peer_mdb = new Peer(Integer.parseInt(args[0]), 1, queue);
        peer_mc = new Peer(Integer.parseInt(args[0]), 2, queue);

        peer_mdb.start();
        peer_mc.start();

        Interface exported_mdb = (Interface) UnicastRemoteObject.exportObject(peer_mdb, Integer.parseInt(args[0]) + 100);
        Interface exported_mc = (Interface) UnicastRemoteObject.exportObject(peer_mc, Integer.parseInt(args[0]) + 1100);

        Registry registry = LocateRegistry.getRegistry();

        registry.bind(args[0] + 100, exported_mdb);
        registry.bind(args[0] + 1100, exported_mc);
        System.err.println("Client ready");


//        if (args.length != 2) {
//            System.out.println("usage <peer_id> <is_initiator>");
//            System.exit(1);
//        }
//        peer_mdb = new Peer(Integer.parseInt(args[0]),1,queue);
//        peer_mc = new Peer(Integer.parseInt(args[0]),2,queue);
//
//        peer_mdb.start();
//        peer_mc.start();
//
//        if (Boolean.valueOf(args[1])) {
//            peer_mdb.setIsInitiator(true);
//            peer_mc.setIsInitiator(true);
//        }
//
//    		if (Boolean.valueOf(args[1]))
//           	   peer_mdb.backupFile("D:\\Data\\GitHub\\sdis1718-t1g03\\assets\\TestFile.jpeg", 1);
////            	peer_mdb.backupFile("../assets/TestFile.pdf", 2);
////        	     peer_mc.restoreFile("TestFile.txt");
//              // peer_mc.deleteFile("../assets/TestFile.pdf");

//        registry.unbind(args[0]);
      } catch (Exception e) {

          System.err.println("Server exception: " + e.toString());
          e.printStackTrace();
      }

    }

}
