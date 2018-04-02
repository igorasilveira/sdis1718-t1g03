
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;

public class Utilities {
    public static String encodeSHA256(String toEncode) throws NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(
                toEncode.getBytes(StandardCharsets.UTF_8));

        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static int randomMiliseconds() {
        Random random = new Random();

        int randomDuration = random.nextInt(400);

        return randomDuration;
    }

    public static String deleteFromString(String toDelete, String deleteMe) {
        String[] elements = toDelete.split(" ");

        String result = "";

        for (String elemen: elements) {
            if (!deleteMe.equals(elemen))
                result += elemen + " ";
        }

        return result;
    }

    public static long findSize(String path) {
        long totalSize = 0;
        ArrayList<String> directory = new ArrayList<String>();
        File file = new File(path);

        if(file.isDirectory()) {
            directory.add(file.getAbsolutePath());
            while (directory.size() > 0) {
                String folderPath = directory.get(0);
                directory.remove(0);
                File folder = new File(folderPath);
                File[] filesInFolder = folder.listFiles();
                int noOfFiles = filesInFolder.length;

                for(int i = 0 ; i < noOfFiles ; i++) {
                    File f = filesInFolder[i];
                    if(f.isDirectory()) {
                        directory.add(f.getAbsolutePath());
                    } else {
                        totalSize+=f.length();
                    }
                }
            }
        } else {
            totalSize = file.length();
        }
        return totalSize;
    }
}
