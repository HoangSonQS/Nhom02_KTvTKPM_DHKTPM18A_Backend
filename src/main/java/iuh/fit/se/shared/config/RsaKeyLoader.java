package iuh.fit.se.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Component
public class RsaKeyLoader {

    public PrivateKey loadPrivateKey(String keySource) throws Exception {
        if (keySource == null || keySource.isEmpty()) {
            throw new IllegalArgumentException("Key source must not be empty");
        }

        // 1. Clean up keySource (handle Spring resource prefixes)
        String source = keySource.trim();
        if (source.startsWith("file:")) {
            source = source.substring(5);
        }

        byte[] keyBytes;
        if (source.startsWith("-----BEGIN")) {
            // PEM format
            String privateKeyPEM = source
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "") // Thêm support cho RSA header
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            keyBytes = Base64.getDecoder().decode(privateKeyPEM);
        } else if (isValidFilePath(source)) {
            // File Path
            String content = new String(Files.readAllBytes(Paths.get(source)), StandardCharsets.UTF_8);
            return loadPrivateKey(content);
        } else {
            // Raw Base64
            keyBytes = Base64.getDecoder().decode(source);
        }

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    public PublicKey loadPublicKey(String keySource) throws Exception {
        if (keySource == null || keySource.isEmpty()) {
            throw new IllegalArgumentException("Key source must not be empty");
        }

        // 1. Clean up keySource
        String source = keySource.trim();
        if (source.startsWith("file:")) {
            source = source.substring(5);
        }

        byte[] keyBytes;
        if (source.startsWith("-----BEGIN")) {
            // PEM format
            String publicKeyPEM = source
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            keyBytes = Base64.getDecoder().decode(publicKeyPEM);
        } else if (isValidFilePath(source)) {
            // File Path
            String content = new String(Files.readAllBytes(Paths.get(source)), StandardCharsets.UTF_8);
            return loadPublicKey(content);
        } else {
            // Raw Base64
            keyBytes = Base64.getDecoder().decode(source);
        }

        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    private boolean isValidFilePath(String source) {
        try {
            return Files.exists(Paths.get(source));
        } catch (Exception e) {
            return false;
        }
    }
}
