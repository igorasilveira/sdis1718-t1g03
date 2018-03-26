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

    public void putChunk(Peer peer) throws IOException, InterruptedException {
        //TODO verificar tamanho

        int sizeOfFiles = 1024 * 60;// 64KB
        byte[] buffer = new byte[sizeOfFiles];

        String fileName = file.getName();

        FileOutputStream out = new FileOutputStream("../assets/initiator" + Peer.peer_id + "/" + id);

        //try-with-resources to ensure closing stream
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {


            int bytesAmount = 0;
            while ((bytesAmount = bis.read(buffer)) > 0) {
                numberChunks++;
                int currentRepDegree = 0;
                long timeToWait = 500;
                int tries = 0;

                while (currentRepDegree < replicationDeg) {

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
                            peer.getMdb(), 4447);

                //TODO send PUTCHUNK message

                    peer.getSocket_mdb().send(test);//Sends data chunk

                    System.out.println("Sending chunk #" + numberChunks);

                    long elapsedTime = 0;
                    boolean print = true;
                    while (elapsedTime < timeToWait) {
                      if (print) {
                        System.out.println("Listening for: " + (timeToWait/1000.0) + " seconds");
                        print = false;
                      }
                      long start = System.currentTimeMillis();

                      byte[] buf = new byte[sizeOfFiles];

                      DatagramPacket recv = new DatagramPacket(buf, buf.length);

                      try {
                          peer.getSocket_mc().receive(recv);//confirmation message from peer
                          String response = new String(recv.getData(), recv.getOffset(), recv.getLength());

                          Message messageReceived = new Message(response);

                          if (messageReceived.getMessageType() == "STORED"){
                            System.out.println("Confirmation message: " + response);
                            currentRepDegree++;
                          }
                      } catch (SocketTimeoutException e) {
                      }

                      long elapsedWhile = System.currentTimeMillis() - start;
                      elapsedTime = elapsedTime + elapsedWhile;
                    }
                }
            }
        }
    }

    public boolean storeChunk(Message message, Peer peer) throws IOException {
        //TODO reply to PUTCHUNK message with STORED

        peer.setSocket_mc(new MulticastSocket(4446));//mcast_port
		peer.setMc(InetAddress.getByName("224.0.0.1"));//mcast_addr
		peer.getSocket_mc().joinGroup(peer.getMc());

		peer.setSocket_mdb(new MulticastSocket(4447));
		peer.setMdb(InetAddress.getByName("224.0.0.2"));//mcast_addr
		peer.getSocket_mdb().joinGroup(peer.getMdb());

    	String msg = message.toString();

		DatagramPacket test = new DatagramPacket(msg.getBytes(), msg.length(),
					        peer.getMc(), 4446);
		peer.getSocket_mc().send(test);

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
