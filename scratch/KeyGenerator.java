import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class KeyGenerator {
    public static void main(String[] args) throws Exception {
        String dir = "config/keys";
        Files.createDirectories(Paths.get(dir));

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();
        
        saveKey(pair.getPrivate(), dir + "/private_key.pem", "RSA PRIVATE KEY");
        saveKey(pair.getPublic(), dir + "/public_key.pem", "PUBLIC KEY");
        
        System.out.println("Keys generated successfully in " + dir);
    }

    private static void saveKey(java.security.Key key, String path, String label) throws Exception {
        String base64 = Base64.getEncoder().encodeToString(key.getEncoded());
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN ").append(label).append("-----\n");
        for (int i = 0; i < base64.length(); i += 64) {
            pem.append(base64, i, Math.min(i + 64, base64.length())).append("\n");
        }
        pem.append("-----END ").append(label).append("-----\n");
        Files.write(Paths.get(path), pem.toString().getBytes());
    }
}
