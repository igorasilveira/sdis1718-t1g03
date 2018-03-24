import java.io.IOException;

import static java.lang.System.exit;

public class Client {

    public static String version = "1.0";

    public static void main(String args[]) throws IOException {

        if (args.length != 2) {
            System.out.println("usage <peer_id> <is_initiator>");
            exit(1);
        }

        Peer peer = new Peer(Integer.parseInt(args[0]), Boolean.valueOf(args[1]));
		if (Boolean.valueOf(args[1]))
        	peer.backupFile("../assets/TestFile", 3);
    }
}
