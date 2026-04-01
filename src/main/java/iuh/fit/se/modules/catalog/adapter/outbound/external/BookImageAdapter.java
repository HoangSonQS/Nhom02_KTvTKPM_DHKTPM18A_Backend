package iuh.fit.se.modules.catalog.adapter.outbound.external;

import iuh.fit.se.modules.catalog.application.port.out.BookImagePort;
import iuh.fit.se.shared.infrastructure.cloudinary.CloudinaryClient;
import iuh.fit.se.shared.infrastructure.cloudinary.CloudinaryUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * BookImageAdapter — Implementation cho BookImagePort.
 */
@Component
@RequiredArgsConstructor
public class BookImageAdapter implements BookImagePort {

    private final CloudinaryClient cloudinaryClient;
    private static final String FOLDER = "books";

    @Override
    public CloudinaryUploadResult uploadBookImage(byte[] file) {
        return cloudinaryClient.upload(file, FOLDER);
    }

    @Override
    public void deleteBookImage(String publicId) {
        cloudinaryClient.delete(publicId);
    }
}
