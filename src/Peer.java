import javax.rmi.CORBA.Util;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Peer {

    private static Boolean running = true;
    static int peer_id = 0;
    private String version = "";

    public static MulticastSocket socket_mc, socket_mdb;
    public static InetAddress mc, mdb;

    FileClass receivedChunk;
    Message message;

    public Peer(int id, boolean isInitiatior) throws IOException {

        peer_id = id;
        this.version = version;

        System.out.println("Initializing Peer with ID " + id + ".");

        socket_mc = new MulticastSocket(4446);//mcast_port
        socket_mc.setSoTimeout(100);
        mc = InetAddress.getByName("224.0.0.1");//mcast_addr
        socket_mc.joinGroup(mc);

        socket_mdb = new MulticastSocket(4447);
        mdb = InetAddress.getByName("224.0.0.2");//mcast_addr
        socket_mdb.joinGroup(mdb);

        if (!isInitiatior) {
            while (running) {
              receiveMessages();
            }
        }
    }

    public void receiveMessages() throws IOException {
        /*String msg = "From peer_id<" + peer_id + ">";
                DatagramPacket test = new DatagramPacket(msg.getBytes(), msg.length(),
                        group, 4446);
                socket.send(test);*/

        //TODO
        byte[] buf = new byte[1024 * 60];

        DatagramPacket recvMDB = new DatagramPacket(buf, buf.length);
        socket_mdb.receive(recvMDB);

        String response = new String(recvMDB.getData(), recvMDB.getOffset(), recvMDB.getLength());
        System.out.println("Response: " + response);

        //TODO receber mensagem, fazer decode dela e chamar metodo correspondente
        Message messageReceivedMDB = new Message(response);

        if (messageReceivedMDB.getMessageType() == "PUTCHUNK"){

            int storedCount = 0;

            String dir = "../assets/Peer_" + peer_id + "/" + messageReceivedMDB.getFileId() + "/";
            String path = messageReceivedMDB.getChunkNo();
            File dirF = new File(dir);
            File file = new File(dir + path);

            if (!file.isFile()) {
              dirF.mkdirs();

                byte data[] = recvMDB.getData();
//                	String fileName = "./assets/id" + response_get[1];//fileName for chunk
                FileOutputStream out = new FileOutputStream(dir + path);//create file
                out.write(data);
                out.close();
                receivedChunk = new FileClass(dir + path, 1);

                message = new Message();
                message.setMessageType("STORED");
                message.setFileId(messageReceivedMDB.getFileId());
                message.setChunkNo(messageReceivedMDB.getChunkNo());

                receivedChunk.storeChunk(message);

                boolean toListen = true;
                while (toListen) {
                    DatagramPacket recvMC = new DatagramPacket(buf, buf.length);
                    socket_mc.receive(recvMC);

                    String responseMC = new String(recvMDB.getData(), recvMDB.getOffset(), recvMDB.getLength());
                    //System.out.println("Response: " + responseMC);

                    Message messageReceivedMC = new Message(responseMC);

                    storedCount++;

                    if (Integer.parseInt(messageReceivedMC.getSenderId()) == peer_id) {
                        toListen = false;
                        if (storedCount > Integer.parseInt(messageReceivedMDB.getReplicationDeg())) {
                          Files.delete(Paths.get(dir + path));
                          break;
                        }
                    }
                }
            }
        }

    }

    public void backupFile(String filePath, int replicationDeg) throws IOException, InterruptedException {
        FileClass fileClass = new FileClass(filePath, replicationDeg);

        if (fileClass.isValid()) {
            fileClass.putChunk();
        }
    }

    public MulticastSocket getSocket_mc() {
        return socket_mc;
    }

    public void setSocket_mc(MulticastSocket socket_mc) {
        this.socket_mc = socket_mc;
    }

    public MulticastSocket getSocket_mdb() {
        return socket_mdb;
    }

    public void setSocket_mdb(MulticastSocket socket_mdb) {
        this.socket_mdb = socket_mdb;
    }

    public InetAddress getMc() {
        return mc;
    }

    public void setMc(InetAddress mc) {
        this.mc = mc;
    }

    public InetAddress getMdb() {
        return mdb;
    }

    public void setMdb(InetAddress mdb) {
        this.mdb = mdb;
    }
}
