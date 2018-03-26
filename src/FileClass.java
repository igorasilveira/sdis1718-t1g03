import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.net.*;

public class FileClass {

    File file;
    private String id;
    private int replicationDeg = 0;
    private int numberChunks = 0;
    private String message;

    public FileClass(String path, int repDegree) {

        file = new File(path);
        replicationDeg = repDegree;

        if (!file.isFile()) {
            System.out.println("[ERROR] No valid file found from path " + path + ".");
            file = null;
        } else {
            System.out.println("Processing file...");

            String fileName = file.getName();
            String owner = "", lastModified = "";

            try {
                owner = String.valueOf(Files.getOwner(Paths.get(file.getPath())));
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("[ERROR] Could not get file owner successfully.");
                file = null;
            }

            try {
                lastModified = String.valueOf(Files.getLastModifiedTime(Paths.get(file.getPath())));
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("[ERROR] Could not get last modified date successfully.");
                file = null;
            }

            try {
                id = Utilities.encodeSHA256(fileName + owner + lastModified);
                System.out.println("Encoded ID: " + id);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            System.out.println("ID retrieved Successfully");
        }
    }

    public void putChunk() throws IOException, InterruptedException {
        //TODO verificar tamanho

        int sizeOfFiles = 1024 * 60;// 64KB
        byte[] buffer = new byte[sizeOfFiles];

        String fileName = file.getName();

        //try-with-resources to ensure closing stream
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {


            int bytesAmount = 0;
            while ((bytesAmount = bis.read(buffer)) > 0) {
                numberChunks++;
                int currentRepDegree = 0;
                long timeToWait = 500;
                int tries = 0;

                while (currentRepDegree != replicationDeg) {

                    timeToWait *= 2;
                    tries++;

                    if (tries == 6) {
                        System.out.println("Exceed maximum PUTCHUNK tries for chunk #" + numberChunks);
                        System.exit(1);
                    }

                    Message message = new Message();
                    message.setMessageType("PUTCHUNK");
                    message.setFileId(id);
                    message.setReplicationDeg(replicationDeg);
                    message.setChunkNo(String.valueOf(numberChunks));

                    String v = new String( buffer, Charset.forName("UTF-8") );
                    message.setBody(v);

                    String msg = message.toString();

                    DatagramPacket test = new DatagramPacket(msg.getBytes(), msg.length(),
                            Peer.mdb, 4447);

                //TODO send PUTCHUNK message

                    Peer.socket_mdb.send(test);//Sends data chunk

                    System.out.println("Sending chunk #" + numberChunks);

                    long elapsedTime = 0;
                    while (elapsedTime < timeToWait) {
                      long start = System.currentTimeMillis();

                      byte[] buf = new byte[1000];

                      DatagramPacket recv = new DatagramPacket(buf, buf.length);
                      Peer.socket_mc.receive(recv);//confirmation message from peer

                      String response = new String(recv.getData(), recv.getOffset(), recv.getLength());

                      Message messageReceived = new Message(response);

                      if (messageReceived.getMessageType() == "STORED"){
                        System.out.println("Confirmation message: " + response);
                        currentRepDegree++;
                      }

                      long elapsedWhile = System.currentTimeMillis() - start;
                      elapsedTime =+ elapsedWhile;
                      System.out.println("elapsedTime: " + elapsedTime);
                    }
                }
            }
        }
    }

    public boolean storeChunk(Message message) throws IOException {
        //TODO reply to PUTCHUNK message with STORED

    	Peer.socket_mc = new MulticastSocket(4446);//mcast_port
		InetAddress mc = InetAddress.getByName("224.0.0.1");//mcast_addr
		Peer.socket_mc.joinGroup(mc);

		Peer.socket_mdb = new MulticastSocket(4447);
		Peer.mdb = InetAddress.getByName("224.0.0.2");//mcast_addr
		Peer.socket_mdb.joinGroup(Peer.mdb);

    	String msg = message.toString();

		DatagramPacket test = new DatagramPacket(msg.getBytes(), msg.length(),
					        mc, 4446);
		Peer.socket_mc.send(test);

	    return true;
    }

    public String getId() {
        return id;
    }

    public boolean isValid() {
        return file != null;
    }

    public int getNumberChunks() {
        return numberChunks;
    }
}
