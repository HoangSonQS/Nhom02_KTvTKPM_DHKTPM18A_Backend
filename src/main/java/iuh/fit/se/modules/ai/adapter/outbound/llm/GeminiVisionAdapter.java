package iuh.fit.se.modules.ai.adapter.outbound.llm;

import iuh.fit.se.modules.ai.application.port.out.VisionModelPort;
import iuh.fit.se.modules.ai.domain.OcrResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiVisionAdapter implements VisionModelPort {

    private final ChatModel chatModel;

    @Override
    @SuppressWarnings("deprecation")
    public OcrResult extractBookData(MultipartFile file) {
        try {
            byte[] compressedImage = compressImage(file.getBytes());

            String systemPrompt = "You are a professional librarian. Extract the book title and author from this image. "
                    + "Return ONLY a JSON object like: {\"title\": \"...\", \"author\": \"...\"}";

            Media media = new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(compressedImage));

            UserMessage userMessage = UserMessage.builder()
                    .text(systemPrompt)
                    .media(media)
                    .build();

            ChatResponse response = chatModel.call(new Prompt(userMessage));

            String content = response.getResult().getOutput().getText();
            log.info("OCR Response: {}", content);

            return parseOcrResult(content);

        } catch (Exception e) {
            log.error("Failed to extract book data from image", e);
            return OcrResult.builder().detected(false).rawText("Error processing image: " + e.getMessage()).build();
        }
    }

    private byte[] compressImage(byte[] originalData) throws IOException {
        BufferedImage originalImage = ImageIO.read(new java.io.ByteArrayInputStream(originalData));
        if (originalImage == null)
            return originalData;

        int maxDim = 1024;
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        if (width <= maxDim && height <= maxDim)
            return originalData;

        double scale = (double) maxDim / Math.max(width, height);
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);

        Image scaledImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        BufferedImage outputImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(outputImage, "jpg", baos);
        return baos.toByteArray();
    }

    private OcrResult parseOcrResult(String content) {
        try {
            String cleanJson = content.substring(content.indexOf("{"), content.lastIndexOf("}") + 1);
            if (cleanJson.contains("title")) {
                return OcrResult.builder()
                        .title(extractField(cleanJson, "title"))
                        .author(extractField(cleanJson, "author"))
                        .detected(true)
                        .rawText(content)
                        .build();
            }
        } catch (Exception e) {
            log.warn("Failed to parse OCR JSON: {}", content);
        }
        return OcrResult.builder().detected(false).rawText(content).build();
    }

    private String extractField(String json, String field) {
        int start = json.indexOf("\"" + field + "\":") + field.length() + 4;
        int end = json.indexOf("\"", start);
        if (start > 5 && end > start) {
            return json.substring(start, end);
        }
        return "Unknown";
    }
}
