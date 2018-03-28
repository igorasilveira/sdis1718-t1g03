import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;
import java.net.*;

public class FileClass implements Runnable{

    public enum Protocol {NONE, BACKUP, RESTORE}

    File file;
    private String id;
    private String path = "";
    private int replicationDeg = 0;
    private int numberChunks = -1;
    private String message;
    private Protocol currentProtocol = Protocol.NONE;

    private DatagramPacket packet;

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    public FileClass(String path, boolean restore) {

        file = new File(path);

        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);

        if (restore) {

        } else {

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

    }

    public void putChunk() throws IOException, InterruptedException {

        int sizeOfFiles = 1024 * 60;// 64KB
        byte[] buffer = new byte[sizeOfFiles];

        String fileName = file.getName();

        String dir = "C:\\Users\\up201505172\\IdeaProjects\\sdis1718-t1g03\\assets\\Initiator\\";
        //String dir = "../assets/Initiator/";
        String pathFolder = id;

        File dirF = new File(dir);
        path = dir + pathFolder + ".txt";
        File filePath = new File(path);

        dirF.mkdirs();

        FileWriter fileWriter = new FileWriter(dir + "backed_up_files.txt", true);

        PrintWriter out = new PrintWriter(path, "UTF-8");

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
            fileWriter.write(fileName + " " + id + System.lineSeparator());
            fileWriter.close();
        }
        out.close();
    }

    public void getChunk(int numberChunks) throws IOException {

        int readChunks = -1;

        int sizeOfFiles = 1024 * 60;// 64KB
        byte[] buffer = new byte[sizeOfFiles];

        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {


            int bytesAmount = 0;
            while (readChunks < numberChunks) {
                readChunks++;

                Message message = new Message();
                message.setMessageType("GETCHUNK");
                message.setFileId(id);
                message.setChunkNo(String.valueOf(readChunks));

                String msg = message.toString();

                DatagramPacket test = new DatagramPacket(msg.getBytes(), msg.length(),
                        Peer.mc, 4446);

                Peer.socket_mc.send(test);//Sends data chunk

                System.out.println("Requesting chunk #" + readChunks);

                while (true) {
                    DatagramPacket recv = new DatagramPacket(buffer, buffer.length);

                    try {
                        Peer.socket_mdr.receive(recv);//confirmation message from Peer
                        String response = new String(recv.getData(), recv.getOffset(), recv.getLength());

                        System.out.println("ANTES DA MSSG");
                        Message messageReceived = new Message(response);
                        System.out.println("Depois DA MSSG");

                        if (messageReceived.getMessageType() == "CHUNK"){
                           // System.out.println("Confirmation message: " + response);
                            bos.write(messageReceived.getBody().getBytes(), 0, messageReceived.getBody().getBytes().length);
                            continue;
                        }
                    } catch (SocketTimeoutException e) {
                    }
                }
            }
        }
    }

    public boolean storeChunk(Message message) throws IOException {

        Peer.socket_mc = new MulticastSocket(4446);//mcast_port
		Peer.mc = InetAddress.getByName("224.0.0.1");//mcast_addr
		Peer.socket_mc.joinGroup(Peer.mc);

		Peer.socket_mdb = new MulticastSocket(4447);
		Peer.mdb = InetAddress.getByName("224.0.0.2");//mcast_addr
		Peer.socket_mdb.joinGroup(Peer.mdb);

    	String msg = message.toString();

		packet = new DatagramPacket(msg.getBytes(), msg.length(),
					        Peer.mc, 4446);

        currentProtocol = Protocol.BACKUP;
        scheduledThreadPoolExecutor.schedule(this::run, Utilities.randomMiliseconds(), TimeUnit.MILLISECONDS);

	    return true;
    }

    public boolean sendChunk(Message message) throws IOException {

        Peer.socket_mc = new MulticastSocket(4446);//mcast_port
        Peer.mc = InetAddress.getByName("224.0.0.1");//mcast_addr
        Peer.socket_mc.joinGroup(Peer.mc);

        Peer.socket_mdr = new MulticastSocket(4448);
        Peer.mdr = InetAddress.getByName("224.0.0.3");//mcast_addr
        Peer.socket_mdr.joinGroup(Peer.mdr);

        String msg = message.toString();

        packet = new DatagramPacket(msg.getBytes(), msg.length(),
                Peer.mdr, 4448);

        currentProtocol = Protocol.RESTORE;
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

    public int getReplicationDeg() {
        return replicationDeg;
    }

    public void setReplicationDeg(int replicationDeg) {
        this.replicationDeg = replicationDeg;
    }

    public ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
      return scheduledThreadPoolExecutor;
    }

    @Override
    public void run() {
      try {
          switch (currentProtocol) {
              case BACKUP:
                Peer.socket_mc.send(packet);
                break;
              case RESTORE:
                Peer.socket_mdr.send(packet);
                  break;
              default:
                  break;
          }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    public void setId(String id) {
        this.id = id;
    }
}
