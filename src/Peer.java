import java.io.*;
import java.net.*;

public class Peer{
    private static Boolean running = true;
    static int peer_id = 0;
    private String version = "";

    public Peer(int id, boolean isInitiatior) throws IOException {

        peer_id = id;
        this.version = version;

        System.out.println("Initializing Peer with ID " + id + ".");

        if (!isInitiatior) {
            MulticastSocket socket_mc = new MulticastSocket(4446);//mcast_port
            InetAddress mc = InetAddress.getByName("224.0.0.1");//mcast_addr
            socket_mc.joinGroup(mc);
            
            MulticastSocket socket_mdb = new MulticastSocket(4447);
            InetAddress mdb = InetAddress.getByName("224.0.0.2");//mcast_addr
            socket_mdb.joinGroup(mdb);
            
            while (running) {
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
                	byte data[] = recv.getData();
//                	String fileName = "./assets/id" + response_get[1];//fileName for chunk
                	String fileName = "D:\\Data\\GitHub\\sdis1718-t1g03\\assets\\" + peer_id + "\\id" + messageReceived.getChunkNo();//fileName for chunk
					FileOutputStream out = new FileOutputStream(fileName);//create file
					out.write(data);
					out.close();
                	FileClass receivedChunk = new FileClass(fileName, 1);

                    Message message = new Message();
                    message.setMessageType("STORED");
                    message.setFileId(messageReceived.getFileId());
                    message.setChunkNo(messageReceived.getChunkNo());

					receivedChunk.storeChunk(message);
                }

				

            }
        }
    }

    public void backupFile(String filePath, int replicationDeg) throws IOException, InterruptedException {
        FileClass fileClass = new FileClass(filePath, replicationDeg);

        if (fileClass.isValid()) {
            fileClass.putChunk();
        }
    }
}

