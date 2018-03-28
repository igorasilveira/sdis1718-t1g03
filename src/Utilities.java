
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
}
