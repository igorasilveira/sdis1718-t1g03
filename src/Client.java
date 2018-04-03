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

            peer_mdb.setIsInitiator(true);
            peer_mc.setIsInitiator(true);

            peer_mdb.start();
            peer_mc.start();

            if (args.length > 1) {
                switch (args[1]) {
                    case "BACKUP":
                        peer_mdb.backupFile(args[2],Integer.parseInt(args[3]));
                        break;
                    case "RESTORE":
                        peer_mc.restoreFile(args[2]);
                        break;
                    case "DELETE":
                        peer_mc.deleteFile(args[2]);
                        break;
                    case "RECLAIM":
                        peer_mc.reclaimSpace(Integer.parseInt(args[2]));
                        break;
                    case "RMI":

                        peer_mdb.setIsInitiator(false);
                        peer_mc.setIsInitiator(false);

                        Interface exported_mdb = (Interface) UnicastRemoteObject.exportObject(peer_mdb, Integer.parseInt(args[0]) + 100);
                        Interface exported_mc = (Interface) UnicastRemoteObject.exportObject(peer_mc, Integer.parseInt(args[0]) + 1100);

                        Registry registry = LocateRegistry.getRegistry();

                        registry.bind(args[0] + 100, exported_mdb);
                        registry.bind(args[0] + 1100, exported_mc);
                        System.err.println("Client ready");
                        break;
                }
            } else {
                peer_mdb.setIsInitiator(false);
                peer_mc.setIsInitiator(false);
            }

//        registry.unbind(args[0]);
        } catch (Exception e) {

            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

    }

}
