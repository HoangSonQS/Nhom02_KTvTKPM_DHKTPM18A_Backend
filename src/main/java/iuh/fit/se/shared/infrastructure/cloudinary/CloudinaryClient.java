package iuh.fit.se.shared.infrastructure.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * Low-level Cloudinary Client.
 * Chỉ chịu trách nhiệm upload/delete file vật lý.
 * Không chứa bất kỳ business logic nào về Avatar hay Book.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryClient {

    private final Cloudinary cloudinary;

    /**
     * Upload file (byte array) lên Cloudinary vào folder chỉ định.
     */
    public CloudinaryUploadResult upload(byte[] file, String folder) {
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file,
                    ObjectUtils.asMap("folder", folder));

            String publicId = (String) uploadResult.get("public_id");
            String url = (String) uploadResult.get("secure_url");

            log.info("Uploaded file to Cloudinary: folder={}, publicId={}", folder, publicId);
            return new CloudinaryUploadResult(publicId, url);
        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary: {}", e.getMessage());
            throw new RuntimeException("Lỗi upload ảnh lên Cloudinary", e);
        }
    }

    /**
     * Delete file khỏi Cloudinary theo publicId.
     */
    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Deleted file from Cloudinary: publicId={}", publicId);
        } catch (IOException e) {
            log.error("Failed to delete file from Cloudinary: publicId={}, error={}",
                    publicId, e.getMessage());
            // Không nhất thiết phải throw exception để tránh block transaction nếu deletion fail
        }
    }
}
