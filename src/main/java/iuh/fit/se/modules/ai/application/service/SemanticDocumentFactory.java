package iuh.fit.se.modules.ai.application.service;

import iuh.fit.se.modules.catalog.application.port.in.BookDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SemanticDocumentFactory {

    private static final String VERSION = "1";

    public String createWeightedText(BookDTO book) {
        StringBuilder sb = new StringBuilder();

        sb.append("[MAIN TOPIC / TITLE]\n");
        sb.append(book.title()).append("\n");
        sb.append("Importance: CRITICAL\n\n");

        if (book.keywords() != null && !book.keywords().isEmpty()) {
            sb.append("[KEYWORDS / CATEGORIES]\n");
            String keywords = book.keywords().stream().collect(Collectors.joining(", "));
            sb.append(keywords).append("\n");
            sb.append("Importance: HIGH\n\n");
        }

        sb.append("[AUTHOR]\n");
        sb.append(book.author()).append("\n");
        sb.append("Importance: HIGH\n\n");

        sb.append("[BOOK SUMMARY / DESCRIPTION]\n");
        sb.append(book.description() != null ? book.description() : "N/A").append("\n");
        
        if (book.excerpt() != null) {
            sb.append("\n[EXCERPT / PREVIEW]\n");
            sb.append(book.excerpt()).append("\n");
        }
        sb.append("Importance: MEDIUM\n\n");

        sb.append("[PUBLISHING METADATA]\n");
        sb.append("Publisher: ").append(book.publisher() != null ? book.publisher() : "Unknown").append("\n");
        sb.append("Language: ").append(book.language() != null ? book.language() : "Unknown").append("\n");
        sb.append("Importance: LOW");

        return sb.toString();
    }

    public String calculateHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withLowerCase().formatHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            throw new RuntimeException("Hash calculation failed", e);
        }
    }

    public String getCurrentVersion() {
        return VERSION;
    }
}
