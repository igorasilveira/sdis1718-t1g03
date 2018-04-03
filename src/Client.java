import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.ConnectException;
import java.util.concurrent.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Client {

    public static String version = "1.0";
    private static Peer peer_mdb, peer_mc;

    public Client() {}

    public static void main(String args[]) throws IOException, InterruptedException {

        if (args.length != 1) {
            System.out.println("usage java Client <peer_id>");
            System.exit(1);
        }

        BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();
        try{
            peer_mdb = new Peer(Integer.parseInt(args[0]), 1, queue);
            peer_mc = new Peer(Integer.parseInt(args[0]), 2, queue);

            peer_mdb.setIsInitiator(true);
            peer_mc.setIsInitiator(true);

            peer_mdb.start();
            peer_mc.start();

            boolean connected = false;

            peer_mdb.setIsInitiator(false);
            peer_mc.setIsInitiator(false);

            Interface exported_mdb = (Interface) UnicastRemoteObject.exportObject(peer_mdb, Integer.parseInt(args[0]) + 100);
            Interface exported_mc = (Interface) UnicastRemoteObject.exportObject(peer_mc, Integer.parseInt(args[0]) + 1100);
            while (!connected) {

                try {

                    Registry registry = LocateRegistry.getRegistry();

                    registry.bind(args[0] + 100, exported_mdb);
                    registry.bind(args[0] + 1100, exported_mc);
                    System.err.println("Client ready");
                } catch (ConnectException e) {
                    System.out.println("Retrying...");
                } catch (AlreadyBoundException e1) {

                }
            }

//        registry.unbind(args[0]);
        } catch (Exception e) {

            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

    }

}
