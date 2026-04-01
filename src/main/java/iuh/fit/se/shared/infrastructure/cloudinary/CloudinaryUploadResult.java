package iuh.fit.se.shared.infrastructure.cloudinary;

/**
 * Record chứa kết quả upload ảnh từ Cloudinary.
 * Đảm bảo tính Type-safe và không dùng Map bừa bãi.
 */
public record CloudinaryUploadResult(String publicId, String url) {
}
