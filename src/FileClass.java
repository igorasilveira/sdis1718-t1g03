import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.*;
import java.net.*;
import java.nio.file.Path;

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

    public void putChunk(int peer_id) throws IOException {

        int sizeOfFiles = 1024 * 60;// 64KB
        byte[] buffer = new byte[sizeOfFiles];

        String fileName = file.getName();

        String dir = "D:\\Data\\GitHub\\sdis1718-t1g03\\assets\\Peer_" + peer_id + "\\";
        // String dir = "../assets/Initiator/";
        String pathFolder = id;

        File dirF = new File(dir);
        path = dir + pathFolder + ".txt";
        File filePath = new File(path);

        dirF.mkdirs();

        FileWriter fileWriter = new FileWriter(dir + "backed_up_files.txt", true);

        PrintWriter out = new PrintWriter(path, "UTF-8");

        //try-with-resources to ensure closing stream
        try (FileInputStream fis = new FileInputStream(file)) {

            int bytesAmount = 0;
            while ((bytesAmount = fis.read(buffer)) > 0) {
                numberChunks++;
                int currentRepDegree = 0;
                long timeToWait = 500;
                int tries = 0;

                ByteArrayOutputStream baoos = new ByteArrayOutputStream();

                Message message = new Message();
                message.setMessageType("PUTCHUNK");
                message.setFileId(id);
                message.setReplicationDeg(replicationDeg);
                message.setChunkNo(String.valueOf(numberChunks));

                baoos.write(buffer, 0, bytesAmount);
                baoos.close();

                message.setBody(baoos.toByteArray());

                ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 64);
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(message);
                byte[] data = baos.toByteArray();

                DatagramPacket test = new DatagramPacket(data, data.length,
                        Peer.mdb, 4447);

                while (currentRepDegree < replicationDeg) {

                    timeToWait *= 2;
                    tries++;

                    if (tries == 6) {
                        System.out.println("Exceed maximum PUTCHUNK tries for chunk #" + numberChunks);
                        out.close();
                        fileWriter.close();
                        return;
                    }

                    //System.out.println("@@@@@@@@@@@@  Byte Array length " + data.length);

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

                          ByteArrayInputStream bais = new ByteArrayInputStream(buf);
                          ObjectInputStream ois = new ObjectInputStream(bais);
                          Message messageReceived = (Message) ois.readObject();

                          System.out.println("******************" + messageReceived.getMessageType());

                          if (messageReceived.getMessageType().equals("STORED")){
                            out.print(messageReceived.getSenderId() + " ");
                            //System.out.println("Confirmation message: " + messageReceived.getMessageType());
                            System.out.println("Received: " + messageReceived.getMessageType() + " from Peer " + messageReceived.getSenderId() + " for file with id " + messageReceived.getFileId() + ".");

                            currentRepDegree++;
                          }
                      } catch (SocketTimeoutException e) {
                          System.out.println("Waiting for chunk storing confirmations.");
                      } catch (ClassNotFoundException e) {
                          e.printStackTrace();
                      }

                        long elapsedWhile = System.currentTimeMillis() - start;
                      elapsedTime = elapsedTime + elapsedWhile;
                    }
                    out.println();
                    oos.close();
                }
            }
            fileWriter.write(fileName + " " + id + " " + replicationDeg + System.lineSeparator());
            fileWriter.close();
        }
        out.close();
    }

    public void getChunk(int numberChunks) throws IOException {

        int readChunks = -1;

        int sizeOfFiles = 1024 * 64;// 64KB
        byte[] buffer = new byte[sizeOfFiles];
        System.out.println("CHUNKS " + numberChunks);

        try (
            ByteArrayOutputStream baios = new ByteArrayOutputStream()) {

            while (readChunks < numberChunks - 1) {
                readChunks++;

                Message message = new Message();
                message.setMessageType("GETCHUNK");
                message.setFileId(id);
                message.setChunkNo(String.valueOf(readChunks));

                ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 64);
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(message);
                byte[] data = baos.toByteArray();

                DatagramPacket test = new DatagramPacket(data, data.length,
                        Peer.mc, 4446);

                Peer.socket_mc.send(test);//Sends data chunk

                System.out.println("Requesting chunk #" + readChunks);

                boolean waitChunk = true;
                while (waitChunk) {
            FileOutputStream fos = new FileOutputStream(file, true);
                    DatagramPacket recv = new DatagramPacket(buffer, buffer.length);

                    try {
                        Peer.socket_mdr.receive(recv);//confirmation message from Peer

                        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
                        ObjectInputStream ois = new ObjectInputStream(bais);
                        Message messageReceived = (Message) ois.readObject();

                        if (messageReceived.getMessageType().equals("CHUNK")){
                            if (messageReceived.getFileId().equals(id) && Integer.parseInt(messageReceived.getChunkNo()) == readChunks) {
                                //System.out.println("Confirmation response: received CHUNK from " + messageReceived.getSenderId());
                                System.out.println("Received: " + messageReceived.getMessageType() + " from Peer " + messageReceived.getSenderId() + " for file with id " + messageReceived.getFileId() + ".");
                                fos.write(messageReceived.getBody(), 0, messageReceived.getBody().length);
                                fos.close();
                                waitChunk = false;
                            }
                        }
                    } catch (SocketTimeoutException e) {
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void deleteChunk(int peer_id) throws IOException {

        String fileName = file.getName();

        Path storedPeerChunks  = Paths.get("D:\\Data\\GitHub\\sdis1718-t1g03\\assets\\Peer_" + peer_id + "\\" + getId()+ ".txt");

        if (storedPeerChunks == null) return;

        Files.delete(file.toPath());//deletes file
        Files.delete(storedPeerChunks);//deletes file with where the chunks were saved in peers

        /*removes info about this file from backed up files*/
        File backed_up_files = new File("D:\\Data\\GitHub\\sdis1718-t1g03\\assets\\Peer_" + peer_id + "\\backed_up_files.txt");
        File tempFile = new File("temp.txt");

        BufferedReader reader = new BufferedReader(new FileReader(backed_up_files));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

        String lineToRemove = file.getName() + " " + id;
        String currentLine;

        while((currentLine = reader.readLine()) != null) {
            if(!currentLine.equals(lineToRemove))
              writer.write(currentLine + System.getProperty("line.separator"));
        }

        writer.close();
        reader.close();

        tempFile.renameTo(backed_up_files);


        //creates DELETE packet to be sent from the Initiator peer
        int sizeOfFiles = 1024 * 60;// 64KB
        byte[] buffer = new byte[sizeOfFiles];

        ByteArrayOutputStream baoos = new ByteArrayOutputStream();

        Message message = new Message();
        message.setMessageType("DELETE");
        message.setFileId(id);

        baoos.write(buffer, 0, buffer.length);
        baoos.close();

        message.setBody(baoos.toByteArray());

        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 64);
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        byte[] data = baos.toByteArray();

        DatagramPacket test = new DatagramPacket(data, data.length,
                Peer.mc, 4446);

        int tries = 0;//sends delete request 3 times
        while (tries != 3 ) {
            tries++;
            Peer.socket_mc.send(test);//Sends data chunk
            System.out.println("Requesting deletion of chunks of file id " + id + ".");

        }

  }

    public boolean storeChunk(Message message) throws IOException {

        Peer.socket_mc = new MulticastSocket(4446);//mcast_port
		Peer.mc = InetAddress.getByName("224.0.0.1");//mcast_addr
		Peer.socket_mc.joinGroup(Peer.mc);

		Peer.socket_mdb = new MulticastSocket(4447);
		Peer.mdb = InetAddress.getByName("224.0.0.2");//mcast_addr
		Peer.socket_mdb.joinGroup(Peer.mdb);

        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 64);
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        byte[] data = baos.toByteArray();

        packet = new DatagramPacket(data, data.length,
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
                System.out.println("@@@@@ SENT STORED");
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
