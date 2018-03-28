import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

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
            byte[] buf = new byte[1024 * 60];

            System.out.println("backup");
            try {
                DatagramPacket recvMDB = new DatagramPacket(buf, buf.length);
                System.out.println("antes");
                socket_mdb.receive(recvMDB);
                System.out.println("depois");

                String responseMDB = new String(recvMDB.getData(), recvMDB.getOffset(), recvMDB.getLength());
                System.out.println("Response: " + responseMDB);

                Message messageReceivedMDB = new Message(responseMDB);

                if (messageReceivedMDB.getMessageType() == "PUTCHUNK") {

                    int storedCount = 0;

                    //String dir = "../assets/Peer_" + peer_id + "/" + messageReceivedMDB.getFileId() + "/";
                    String dir = "C:\\Users\\up201505172\\IdeaProjects\\sdis1718-t1g03\\assets\\Peer_" + peer_id + "\\" + messageReceivedMDB.getFileId() + "\\";
                    String path = messageReceivedMDB.getChunkNo();
                    File dirF = new File(dir);
                    File file = new File(dir + path);

                    if (!file.isFile()) {

                        dirF.mkdirs();
                        //                	String fileName = "./assets/id" + response_get[1];//fileName for chunk
                        FileOutputStream fileOutputStream = new FileOutputStream(dir + path);
                        fileOutputStream.write(messageReceivedMDB.getBody().getBytes());
                        System.out.println("/////////////////////////////////////////\n" + messageReceivedMDB.getBody().length());
                        fileOutputStream.close();
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

                            String responseMC = new String(recvMC.getData(), recvMC.getOffset(), recvMC.getLength());
                            //System.out.println("Response: " + responseMC);

                            Message messageReceivedMC = new Message(responseMC);

                            if (messageReceivedMC.getMessageType() == "STORED") {

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
                System.out.println("CATCHED");

            }
        }
        if (protocol == 1) {
            try {
                byte[] buf = new byte[1024 * 60];
                DatagramPacket recvMC = new DatagramPacket(buf, buf.length);
                socket_mc.receive(recvMC);
                String responseMC = new String(recvMC.getData(), recvMC.getOffset(), recvMC.getLength());
                System.out.println("Response: " + responseMC);

                Message messageReceivedMC = new Message(responseMC);

                if (messageReceivedMC.getMessageType() == "GETCHUNK") {
                    File dir = new File("C:\\Users\\up201505172\\IdeaProjects\\sdis1718-t1g03\\assets\\Peer_" + peer_id + "\\" + messageReceivedMC.getFileId());

                    if (!dir.exists()) {
                        System.out.println("Did not store this file");
                    }

                    File file = new File("C:\\Users\\up201505172\\IdeaProjects\\sdis1718-t1g03\\assets\\Peer_" + peer_id + "\\" + messageReceivedMC.getFileId() + "\\" + messageReceivedMC.getChunkNo());

                    if (file.exists()) {

                        int sizeOfFiles = 1024 * 60;// 64KB
                        byte[] buffer = new byte[sizeOfFiles];

                        Message message = new Message();
                        message.setMessageType("CHUNK");
                        message.setFileId(messageReceivedMC.getFileId());
                        message.setChunkNo(messageReceivedMC.getChunkNo());

                        try (FileInputStream fis = new FileInputStream(file);
                             BufferedInputStream bis = new BufferedInputStream(fis)) {
                            bis.read(buffer);

                            String v = new String( buffer, Charset.forName("UTF-8") );
                            message.setBody(v);

                        } catch (FileNotFoundException e) {

                        }

                        String msg = message.toString();

                        DatagramPacket send = new DatagramPacket(msg.getBytes(), msg.length(), Peer.mdr, 4448);

                        Peer.socket_mdr.send(send);
                    }

                }
            } catch (SocketTimeoutException e1) {

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

        File backupUpFiles = new File("C:\\Users\\up201505172\\IdeaProjects\\sdis1718-t1g03\\assets\\Initiator\\backed_up_files.txt");

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
            String dir = "C:\\Users\\up201505172\\IdeaProjects\\sdis1718-t1g03\\assets\\Initiator\\";
            File fileChunks = new File(dir + readId + ".txt");

            try (BufferedReader br = new BufferedReader(new FileReader(fileChunks))) {
                String line;
                while ((line = br.readLine()) != null) {
                    countLines++;
                }
            }

            File dirF = new File(dir + "restoredFiles\\");
            dirF.mkdirs();
            FileWriter fileWriter = new FileWriter(dir + "restoredFiles\\" + fileName);

            FileClass fileClass = new FileClass(dir + "restoredFiles\\" + fileName, true);
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
