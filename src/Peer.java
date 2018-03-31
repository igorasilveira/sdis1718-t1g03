import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.*;


public class Peer extends Thread{

    private static Boolean running = true;
    static int peer_id = 0;
    private int socket = 0;
    private String version = "";
    private boolean isInitiatior = false;
    private final BlockingQueue<Message> queue;

    public static MulticastSocket socket_mc, socket_mdb, socket_mdr;
    public static InetAddress mc, mdb, mdr;
    public static Message sync;

    FileClass receivedChunk;
    Message message;

    public Peer(int id, boolean isInitiatior, int socket, BlockingQueue<Message> q) throws IOException{

        peer_id = id;
        this.version = version;
        this.socket = socket;
        this.isInitiatior = isInitiatior;
        sync = new Message();
        sync.setMessageType("PUTCHUNK");
        queue = q;

        System.out.println("Initializing Peer with ID " + id + ".");

        socket_mc = new MulticastSocket(4446);//mcast_port
        socket_mc.setSoTimeout(100);
        mc = InetAddress.getByName("224.0.0.1");//mcast_addr
        socket_mc.joinGroup(mc);

        socket_mdb = new MulticastSocket(4447);
        socket_mdb.setSoTimeout(100);
        mdb = InetAddress.getByName("224.0.0.2");//mcast_addr
        socket_mdb.joinGroup(mdb);

        socket_mdr = new MulticastSocket(4448);
        mdr = InetAddress.getByName("224.0.0.3");//mcast_addr
        socket_mdr.joinGroup(mdr);

        //if (!isInitiatior) {
          /*  while (running) {
                run();
            }*/
        //}
    }

    public void run(){
        /*String msg = "From peer_id<" + peer_id + ">";
                DatagramPacket test = new DatagramPacket(msg.getBytes(), msg.length(),
                        group, 4446);
                socket.send(test);*/
      if (!isInitiatior){
        while (running){
          if (socket == 1){
            try {
                byte[] buf = new byte[1024 * 64];
                DatagramPacket recvMDB = new DatagramPacket(buf, buf.length);
                socket_mdb.receive(recvMDB);

                ByteArrayInputStream baos = new ByteArrayInputStream(buf);
                ObjectInputStream oos = new ObjectInputStream(baos);
                Message messageReceivedMDB = (Message) oos.readObject();

                //System.out.println("/////////////// --" + messageReceivedMDB.getMessageType() + "--");

                if (messageReceivedMDB.getMessageType().equals("PUTCHUNK")) {
                    int storedCount = 0;
                    System.out.println("Received: " + messageReceivedMDB.getMessageType() + " from Peer " + messageReceivedMDB.getSenderId() + " for file with id " + messageReceivedMDB.getFileId() + " for chunk nr" + messageReceivedMDB.getChunkNo() + ".");

                    // String dir = "../assets/Peer_" + peer_id + "/" + messageReceivedMDB.getFileId() + "/";
                    String dir = "D:\\Data\\GitHub\\sdis1718-t1g03\\assets\\Peer_" + peer_id + "\\" + messageReceivedMDB.getFileId() + "\\";
                    String path = messageReceivedMDB.getChunkNo();
                    File dirF = new File(dir);
                    File file = new File(dir + path);

                    if (!file.isFile()) {

                        dirF.mkdirs();
                        //                	String fileName = "./assets/id" + response_get[1];//fileName for chunk

                        FileOutputStream fileOutputStream = new FileOutputStream(dir + path);
                        BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream);
                        bos.write(messageReceivedMDB.getBody());
                        bos.close();

                        receivedChunk = new FileClass(dir + path, false);

                        message = new Message();
                        message.setMessageType("STORED");
                        message.setFileId(messageReceivedMDB.getFileId());
                        message.setChunkNo(messageReceivedMDB.getChunkNo());

                        queue.add(sync);
                        receivedChunk.storeChunk(message);
                        boolean toListen = true;
                        boolean idListened = false;

                        while (toListen) {

                            DatagramPacket recvMC = new DatagramPacket(buf, buf.length);

                            try {
                                socket_mc.receive(recvMC);
                            } catch (SocketTimeoutException e) {
                            }
                            ByteArrayInputStream baois = new ByteArrayInputStream(buf);
                            ObjectInputStream oois = new ObjectInputStream(baois);
                            Message messageReceivedMC = (Message) oois.readObject();

                            if (messageReceivedMC.getMessageType().equals("STORED")) {
                                storedCount++;

                                if (Integer.parseInt(messageReceivedMC.getSenderId()) == peer_id)
                                    idListened = true;
                            /*if (storedCount > Integer.parseInt(messageReceivedMDB.getReplicationDeg())) {
                              Files.delete(Paths.get(dir + path));
                              break;
                            }*/



                                if (storedCount == Integer.parseInt(messageReceivedMDB.getReplicationDeg())) {
                                    toListen = false;
                                    boolean hasSent = receivedChunk.getScheduledThreadPoolExecutor().shutdownNow().isEmpty();
                                    if (!idListened && !hasSent) {
                                        Files.delete(Paths.get(dir + path));
                                    }
                                    //break;
                                }
                            }
                        }
                        queue.remove(sync);
                        //System.out.println("not stuck");
                    }
                }

            } catch (SocketTimeoutException e) {
                //System.out.println("Awaiting message on socket mdb.");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (EOFException e) {
                //System.out.println("@@@@@@@@@@@@@ WAITING FOR NEXT");
            } catch (IOException e) {

            }
          }
          if (socket == 2){
            try {
                if (queue.contains(sync)){//In case protocol BACKUP is using the mc channel
                  //System.out.println("MC Channel in use.");
                  Thread.sleep(50);
                } else {

                byte[] buf = new byte[1024 * 64];
                DatagramPacket recvMC = new DatagramPacket(buf, buf.length);
                socket_mc.receive(recvMC);

                ByteArrayInputStream bais = new ByteArrayInputStream(buf);
                ObjectInputStream ois = new ObjectInputStream(bais);
                Message messageReceivedMC = (Message) ois.readObject();

                //System.out.println("/////////////// --" + messageReceivedMC.getMessageType() + "--");

                if (messageReceivedMC.getMessageType().equals("GETCHUNK")) {
                    File dir = new File("D:\\Data\\GitHub\\sdis1718-t1g03\\assets\\Peer_" + peer_id + "\\" + messageReceivedMC.getFileId());
                    // System.out.println("Received: " + messageReceivedMC.getMessageType() + " from Peer " + messageReceivedMC.getSenderId() + " for file with id " + messageReceivedMC.getFileId() + ".");

                    // File dir =  new File("../assets/Peer_" + peer_id + "/" + messageReceivedMC.getFileId());
                    if (!dir.exists()) {
                        System.out.println("Did not store this file");
                    }

                    File file = new File("D:\\Data\\GitHub\\sdis1718-t1g03\\assets\\Peer_" + peer_id + "\\" + messageReceivedMC.getFileId() + "\\" + messageReceivedMC.getChunkNo());
                    // File file = new File("../assets/Peer_" + peer_id + "/" + messageReceivedMC.getFileId() + "/" + messageReceivedMC.getChunkNo());

                    if (file.exists()) {

                        int sizeOfFiles = 1024 * 60;// 64KB
                        byte[] buffer = new byte[sizeOfFiles];

                        Message message = new Message();
                        message.setMessageType("CHUNK");
                        message.setFileId(messageReceivedMC.getFileId());
                        message.setChunkNo(messageReceivedMC.getChunkNo());

                        try (FileInputStream fis = new FileInputStream(file)) {

                            // ByteArrayOutputStream bioos = new D:\\Data\\GitHub\\sdis1718-t1g03\\ByteArrayOutputStream();
                            ByteArrayOutputStream bioos = new ByteArrayOutputStream();
                            int bytesRead = fis.read(buffer);

                            bioos.write(buffer, 0, bytesRead);
                            bioos.close();

                            message.setBody(bioos.toByteArray());

                        } catch (FileNotFoundException e) {

                        }

                        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 64);
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(message);
                        byte[] data = baos.toByteArray();
                        baos.close();

                        DatagramPacket send = new DatagramPacket(data, data.length, Peer.mdr, 4448);

                        Peer.socket_mdr.send(send);
                    }

                } else if (messageReceivedMC.getMessageType().equals("DELETE")){

                  boolean chunksDeleted = false;

                  System.out.println("Received: " + messageReceivedMC.getMessageType() + " from Peer " + messageReceivedMC.getSenderId() + " for file with id " + messageReceivedMC.getFileId() + ".");

                  File toDelete = new File("../assets/Peer_" + peer_id + "/" + messageReceivedMC.getFileId());
                  /*deletes chunks inside folder*/
                  if (toDelete.exists()){
                    File[] files = toDelete.listFiles();
                    if (files != null){
                      for(File f: files)
                          f.delete();
                    }
                    chunksDeleted = toDelete.delete();/*deletes directory*/
                  }

                  if (chunksDeleted == true)
                    System.out.println("Chunks for this file id have been deleted.");
                  else
                    System.out.println("Peer does not contain chunks for this file id.");


                }
              }
            } catch (SocketTimeoutException e1) {
              //System.out.println("Awaiting message on socket mc.");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {

            }catch (InterruptedException e) {

            }
          }
        }
        }
      }


      /*  try{
          byte[] buf = new byte[1024 * 64];
          DatagramPacket recvMC = new DatagramPacket(buf, buf.length);
          socket_mc.receive(recvMC);

          ByteArrayInputStream bais = new ByteArrayInputStream(buf);
          ObjectInputStream ois = new ObjectInputStream(bais);
          Message messageReceivedMC = (Message) ois.readObject();


        } catch (ClassNotFoundException e) {

        } catch (SocketTimeoutException e1) {

        }*/




    public void backupFile(String filePath, int replicationDeg) throws IOException, InterruptedException {
        FileClass fileClass = new FileClass(filePath, false);
        fileClass.setReplicationDeg(replicationDeg);

        if (fileClass.isValid()) {
            fileClass.putChunk();
        }
    }

    public void restoreFile(String fileName) throws IOException {

        String readId = "";

        File backupUpFiles = new File("D:\\Data\\GitHub\\sdis1718-t1g03\\assets\\Initiator\\backed_up_files.txt");
        // File backupUpFiles = new File("../assets/Initiator/backed_up_files.txt");

        if (!backupUpFiles.exists()) {
            System.out.println("No backed up file found for Initiator");
            System.exit(1);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(backupUpFiles))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] fields = line.split(" ");
                if (fields[0].equals(fileName)){
                    readId = fields[1];
                    break;
                }
            }
        }

        if (readId != "") {

            int countLines = 0;
            String dir = "D:\\Data\\GitHub\\sdis1718-t1g03\\assets\\Initiator\\";
            // String dir = "../assets/Initiator/";
            File fileChunks = new File(dir + readId + ".txt");

            try (BufferedReader br = new BufferedReader(new FileReader(fileChunks))) {
                String line;
                while ((line = br.readLine()) != null) {
                    countLines++;
                }
            }

            // File dirF = new File(dir + "restoredFiles\\");
            File dirF = new File(dir + "restoredFiles/");
            dirF.mkdirs();
            // FileWriter fileWriter = new FileWriter(dir + "restoredFiles\\" + fileName);
            FileWriter fileWriter = new FileWriter(dir + "restoredFiles/" + fileName);

            // FileClass fileClass = new FileClass(dir + "restoredFiles\\" + fileName, true);
            FileClass fileClass = new FileClass(dir + "restoredFiles/" + fileName, true);
            fileClass.setId(readId);
            fileClass.getChunk(countLines);
        } else {
            System.out.println("No backed up file found for " + fileName);
            System.exit(1);
        }
    }

    public void deleteFile(String filePath) throws IOException{
      try{
      FileClass fileClass = new FileClass(filePath, false);

      if (fileClass.isValid()) {
        fileClass.deleteChunk();
        }

      }catch (FileNotFoundException e) {
        System.out.println("File doesn't exist.");
      }

    }

    public void changeFileLines(String[] lines) {
        try {
            //TODO change
            PrintWriter printWriter = new PrintWriter("TODO", "UTF-8");

            for (String line : lines) {
                printWriter.println(line);
            }

            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getFileLines(String path) {

        String line = "", oldtext = "";
        int count = 0;

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(path));
            try
            {
                while((line = reader.readLine()) != null)
                {
                    oldtext += line + "\r\n";
                }
                reader.close();

                return oldtext;
            }
            catch (IOException ioe)
            {
                ioe.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return "";
    }

    public void setProtocol(int socket) {
        this.socket = socket;
    }
}
