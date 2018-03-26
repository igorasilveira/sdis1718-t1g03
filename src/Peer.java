import javax.rmi.CORBA.Util;
import java.io.*;
import java.net.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Peer implements Runnable {
    private static Boolean running = true;
    static int peer_id = 0;
    private String version = "";

    public static MulticastSocket socket_mc, socket_mdb;
    public static InetAddress mc, mdb;

    private static ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    public Peer(int id, boolean isInitiatior) throws IOException {

        peer_id = id;
        this.version = version;

        System.out.println("Initializing Peer with ID " + id + ".");

        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);

        socket_mc = new MulticastSocket(4446);//mcast_port
        socket_mc.setSoTimeout(Utilities.randomMiliseconds());
        mc = InetAddress.getByName("224.0.0.1");//mcast_addr
        socket_mc.joinGroup(mc);

        socket_mdb = new MulticastSocket(4447);
        mdb = InetAddress.getByName("224.0.0.2");//mcast_addr
        socket_mdb.joinGroup(mdb);

        if (!isInitiatior) {

            scheduledThreadPoolExecutor.scheduleWithFixedDelay(this::run, 0, Utilities.randomMiliseconds(), TimeUnit.MILLISECONDS);
        }
    }

    public void receiveMessages() throws IOException {
        /*String msg = "From peer_id<" + peer_id + ">";
                DatagramPacket test = new DatagramPacket(msg.getBytes(), msg.length(),
                        group, 4446);
                socket.send(test);*/

        byte[] buf = new byte[1000];

        DatagramPacket recv = new DatagramPacket(buf, buf.length);
        socket_mdb.receive(recv);

        String response = new String(recv.getData(), recv.getOffset(), recv.getLength());
        System.out.println("Response: " + response);

        //TODO receber mensagem, fazer decode dela e chamar metodo correspondente
        Message messageReceived = new Message(response);

        if (messageReceived.getMessageType() == "PUTCHUNK"){

            String dir = "../assets/Peer_" + peer_id + "/" + messageReceived.getFileId() + "/";
            String path = messageReceived.getChunkNo();
            File dirF = new File(dir);
            File file = new File(dir + path);

            if (!file.isFile()) {
              dirF.mkdirs();

                byte data[] = recv.getData();
//                	String fileName = "./assets/id" + response_get[1];//fileName for chunk
                System.out.println("antes");
                FileOutputStream out = new FileOutputStream(dir + path);//create file
                System.out.println("depois");
                out.write(data);
                out.close();
                FileClass receivedChunk = new FileClass(dir + path, 1);

                Message message = new Message();
                message.setMessageType("STORED");
                message.setFileId(messageReceived.getFileId());
                message.setChunkNo(messageReceived.getChunkNo());

                receivedChunk.storeChunk(message);
            }

        }
    }

    public void backupFile(String filePath, int replicationDeg) throws IOException, InterruptedException {
        FileClass fileClass = new FileClass(filePath, replicationDeg);

        if (fileClass.isValid()) {
            fileClass.putChunk();
        }
    }

    @Override
    public void run() {
        try {
            receiveMessages();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
