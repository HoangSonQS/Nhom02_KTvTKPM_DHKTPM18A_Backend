package iuh.fit.se.modules.account.application.port.out;

import iuh.fit.se.shared.infrastructure.cloudinary.CloudinaryUploadResult;

/**
 * ProfileImagePort — Outbound Port cho việc xử lý ảnh.
 * Dùng để bọc CloudinaryClient (Shared).
 */
public interface ProfileImagePort {

    CloudinaryUploadResult uploadAvatar(byte[] file);

    void deleteOldAvatar(String publicId);
}
