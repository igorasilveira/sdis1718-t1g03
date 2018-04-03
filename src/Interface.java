import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Interface extends Remote {
    void backupFile(String path, int repDeg) throws RemoteException, IOException, InterruptedException;
    void restoreFile(String filename) throws RemoteException, IOException;
    void deleteFile(String filename) throws RemoteException, IOException;
    void setIsInitiator(boolean isInitiator) throws RemoteException;
    void reclaimSpace(int maxSize) throws RemoteException, IOException;
}
