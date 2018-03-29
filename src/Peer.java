import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class Peer {

    private static Boolean running = true;
    static int peer_id = 0;
    private int protocol = 1;
    private String version = "";

    public static MulticastSocket socket_mc, socket_mdb, socket_mdr;
    public static InetAddress mc, mdb, mdr;

    FileClass receivedChunk;
    Message message;

    public Peer(int id, boolean isInitiatior, int protocol) throws IOException {

        peer_id = id;
        this.version = version;
        this.protocol = protocol;

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

        if (protocol == 0) {
            byte[] buf = new byte[1024 * 64];

            try {
                DatagramPacket recvMDB = new DatagramPacket(buf, buf.length);
                socket_mdb.receive(recvMDB);

                ByteArrayInputStream baos = new ByteArrayInputStream(buf);
                ObjectInputStream oos = new ObjectInputStream(baos);
                Message messageReceivedMDB = (Message) oos.readObject();

                System.out.println("/////////////// --" + messageReceivedMDB.getMessageType() + "--");

                if (messageReceivedMDB.getMessageType().equals("PUTCHUNK")) {
                    int storedCount = 0;

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
                    }
                }

            } catch (SocketTimeoutException e) {
                System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@ could not receive PUTCHUNK");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (EOFException e) {
                System.out.println("@@@@@@@@@@@@@ WAITING FOR NEXT");
            }
        }
        if (protocol == 1) {
            try {
                byte[] buf = new byte[1024 * 64];
                DatagramPacket recvMC = new DatagramPacket(buf, buf.length);
                socket_mc.receive(recvMC);

                ByteArrayInputStream bais = new ByteArrayInputStream(buf);
                ObjectInputStream ois = new ObjectInputStream(bais);
                Message messageReceivedMC = (Message) ois.readObject();

                System.out.println("/////////////// --" + messageReceivedMC.getMessageType() + "--");

                if (messageReceivedMC.getMessageType().equals("GETCHUNK")) {
                    File dir = new File("D:\\Data\\GitHub\\sdis1718-t1g03\\assets\\Peer_" + peer_id + "\\" + messageReceivedMC.getFileId());
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

                            ByteArrayOutputStream bioos = new D:\\Data\\GitHub\\sdis1718-t1g03\\ByteArrayOutputStream();
                            // ByteArrayOutputStream bioos = new ByteArrayOutputStream();
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

                }
            } catch (SocketTimeoutException e1) {

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

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

            File dirF = new File(dir + "restoredFiles\\");
            // File dirF = new File(dir + "restoredFiles/");
            dirF.mkdirs();
            FileWriter fileWriter = new FileWriter(dir + "restoredFiles\\" + fileName);
            // FileWriter fileWriter = new FileWriter(dir + "restoredFiles/" + fileName);

            FileClass fileClass = new FileClass(dir + "restoredFiles\\" + fileName, true);
            // FileClass fileClass = new FileClass(dir + "restoredFiles/" + fileName, true);
            fileClass.setId(readId);
            fileClass.getChunk(countLines);
        } else {
            System.out.println("No backed up file found for " + fileName);
            System.exit(1);
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

    public void setProtocol(int protocol) {
        this.protocol = protocol;
    }
}
