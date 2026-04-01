package iuh.fit.se.modules.catalog.application.port.out;

import iuh.fit.se.shared.infrastructure.cloudinary.CloudinaryUploadResult;

/**
 * BookImagePort — Outbound Port cho việc xử lý ảnh sách.
 */
public interface BookImagePort {

    CloudinaryUploadResult uploadBookImage(byte[] file);

    void deleteBookImage(String publicId);
}
