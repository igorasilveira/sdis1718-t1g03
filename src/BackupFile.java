import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

import static java.lang.System.exit;

public class BackupFile {

    private String id;
    private byte[] body;
    private int numberChunks = 0;

    public BackupFile(String path) {

        File file = new File(path);

        if (!file.isFile()) {
            System.out.println("[ERROR] No valid file found from path " + path + ".");
        } else {
            System.out.println("Processing file...");

            String fileName = file.getName();
            String owner = "", lastModified = "";

            try {
                owner = String.valueOf(Files.getOwner(Paths.get(file.getPath())));
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("[ERROR] Could not get file owner successfully.");
                exit(1);
            }

            try {
                lastModified = String.valueOf(Files.getLastModifiedTime(Paths.get(file.getPath())));
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("[ERROR] Could not get last modified date successfully.");
                exit(1);
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

    public String getId() {
        return id;
    }

    public byte[] getBody() {
        return body;
    }

    public int getNumberChunks() {
        return numberChunks;
    }
}
