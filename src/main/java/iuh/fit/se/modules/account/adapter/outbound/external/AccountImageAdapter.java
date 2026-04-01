package iuh.fit.se.modules.account.adapter.outbound.external;

import iuh.fit.se.modules.account.application.port.out.ProfileImagePort;
import iuh.fit.se.shared.infrastructure.cloudinary.CloudinaryClient;
import iuh.fit.se.shared.infrastructure.cloudinary.CloudinaryUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AccountImageAdapter — Implementation của ProfileImagePort.
 * Wrap CloudinaryClient (Shared Infrastructure).
 */
@Component
@RequiredArgsConstructor
public class AccountImageAdapter implements ProfileImagePort {

    private final CloudinaryClient cloudinaryClient;
    private static final String FOLDER = "avatars";

    @Override
    public CloudinaryUploadResult uploadAvatar(byte[] file) {
        return cloudinaryClient.upload(file, FOLDER);
    }

    @Override
    public void deleteOldAvatar(String publicId) {
        cloudinaryClient.delete(publicId);
    }
}
