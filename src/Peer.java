import java.io.*;
import java.net.*;

public class Peer{
    private static Boolean running = true;
    private static int peer_id = 0;

    public Peer(int id) throws IOException {

        peer_id = id;

        System.out.println("Initializing Peer with ID 1...");

        /*MulticastSocket socket = new MulticastSocket(4446*//*Integer.parseInt(args[1])*//*);//mcast_port
        InetAddress group = InetAddress.getByName("227.0.0.2");//mcast_addr
        socket.joinGroup(group);
        while(running){
            String msg = "From peer_id<" + peer_id+">" ;
            DatagramPacket test = new DatagramPacket(msg.getBytes(), msg.length(),
                    group, 4446);
            socket.send(test);

            byte[] buf = new byte[1000];

            DatagramPacket recv = new DatagramPacket(buf, buf.length);
            socket.receive(recv);
            String response = new String(recv.getData(), recv.getOffset(), recv.getLength());
            if (!response.equals(msg))
                System.out.println("Response: " + response);

        }*/
    }

    public void backupFile(String filePath) throws IOException {
        FileClass fileClass = new FileClass(filePath);

        if (fileClass.isValid()) {
            fileClass.backupFile();
        }
    }
}

