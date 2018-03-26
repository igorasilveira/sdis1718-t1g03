import javax.rmi.CORBA.Util;
import java.io.*;
import java.net.*;

public class Peer {

    private static Boolean running = true;
    static int peer_id = 0;
    private String version = "";

    private MulticastSocket socket_mc, socket_mdb;
    private InetAddress mc, mdb;

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
        System.out.println("antes");
        socket_mdb.receive(recvMDB);
        System.out.println("depois");

        String response = new String(recvMDB.getData(), recvMDB.getOffset(), recvMDB.getLength());
        System.out.println("Response: " + response);

        //TODO receber mensagem, fazer decode dela e chamar metodo correspondente
        Message messageReceivedMDB = new Message(response);

        if (messageReceivedMDB.getMessageType() == "PUTCHUNK"){

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
                receivedChunk = new FileClass(dir + path, 1, this);

                message = new Message();
                message.setMessageType("STORED");
                message.setFileId(messageReceivedMDB.getFileId());
                message.setChunkNo(messageReceivedMDB.getChunkNo());

                receivedChunk.storeChunk(message, this);
            }
        }

    }

    public void backupFile(String filePath, int replicationDeg) throws IOException, InterruptedException {
        FileClass fileClass = new FileClass(filePath, replicationDeg, this);

        if (fileClass.isValid()) {
            fileClass.putChunk(this);
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
