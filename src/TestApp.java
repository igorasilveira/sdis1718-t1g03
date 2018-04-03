import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class TestApp{

    public TestApp() {}

    public static void main(String args[]) {
        try{
            Registry registry = LocateRegistry.getRegistry(null);

            Interface remote_object_mdb = (Interface) registry.lookup(args[0] + 100);
            Interface remote_object_mc = (Interface) registry.lookup(args[0] + 1100);
            remote_object_mdb.setIsInitiator(true);
            remote_object_mc.setIsInitiator(true);

            switch (args[1]) {
                case "BACKUP":
                    remote_object_mdb.backupFile(args[2], Integer.parseInt(args[3]));
                    break;
                case "RESTORE":
                    remote_object_mc.restoreFile(args[2]);
                    break;
                case "DELETE":
                    remote_object_mc.deleteFile(args[2]);
                    break;
                case "RECLAIM":
                    remote_object_mc.reclaimSpace(Integer.parseInt(args[2]));
                    break;
                default:
                    System.out.println("Wrong usage, check project guidelines");
                    break;
            }

            remote_object_mdb.setIsInitiator(false);
            remote_object_mc.setIsInitiator(false);

            System.out.println("Finished...");

        } catch (RemoteException e) {

            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        } catch (Exception e) {

            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

    }
}
