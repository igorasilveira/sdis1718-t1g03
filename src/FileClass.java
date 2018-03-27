import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.net.*;
import java.util.concurrent.TimeUnit;

public class FileClass implements Runnable{

    File file;
    private String id;
    private int replicationDeg = 0;
    private int numberChunks = 0;
    private String message;

    private DatagramPacket packet;

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    public FileClass(String path, int repDegree) {

        file = new File(path);
        replicationDeg = repDegree;

        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);

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

        String dir = "D:\\Data\\GitHub\\sdis1718-t1g03\\assets\\Initiator\\";
        String path = id;

        File dirF = new File(dir);
        File filePath = new File(dir + path + ".txt");

        dirF.mkdirs();

        PrintWriter out = new PrintWriter(dir + path + ".txt", "UTF-8");

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
                            Peer.mdb, 4447);

                //TODO send PUTCHUNK message

                    Peer.socket_mdb.send(test);//Sends data chunk

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
                          Peer.socket_mc.receive(recv);//confirmation message from Peer
                          String response = new String(recv.getData(), recv.getOffset(), recv.getLength());

                          Message messageReceived = new Message(response);

                          if (messageReceived.getMessageType() == "STORED"){
                            out.print(messageReceived.getSenderId() + " ");
                            System.out.println("Confirmation message: " + response);
                            currentRepDegree++;
                          }
                      } catch (SocketTimeoutException e) {
                      }

                      long elapsedWhile = System.currentTimeMillis() - start;
                      elapsedTime = elapsedTime + elapsedWhile;
                    }
                    out.println();
                }
            }
        }
        out.close();
    }

    public boolean storeChunk(Message message) throws IOException {
        //TODO reply to PUTCHUNK message with STORED

        Peer.socket_mc = new MulticastSocket(4446);//mcast_port
		Peer.mc = InetAddress.getByName("224.0.0.1");//mcast_addr
		Peer.socket_mc.joinGroup(Peer.mc);

		Peer.socket_mdb = new MulticastSocket(4447);
		Peer.mdb = InetAddress.getByName("224.0.0.2");//mcast_addr
		Peer.socket_mdb.joinGroup(Peer.mdb);

    	String msg = message.toString();

		packet = new DatagramPacket(msg.getBytes(), msg.length(),
					        Peer.mc, 4446);

    scheduledThreadPoolExecutor.schedule(this::run, Utilities.randomMiliseconds(), TimeUnit.MILLISECONDS);

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

    public ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
      return scheduledThreadPoolExecutor;
    }

    @Override
    public void run() {
      try {
        Peer.socket_mc.send(packet);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
}
