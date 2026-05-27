package iuh.fit.se.modules.returns.adapter.outbound.external;

import iuh.fit.se.modules.returns.application.port.out.ReturnEvidenceImagePort;
import iuh.fit.se.shared.infrastructure.cloudinary.CloudinaryClient;
import iuh.fit.se.shared.infrastructure.cloudinary.CloudinaryUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReturnEvidenceImageAdapter implements ReturnEvidenceImagePort {

    private static final String FOLDER = "return-evidence";

    private final CloudinaryClient cloudinaryClient;

    @Override
    public CloudinaryUploadResult uploadEvidenceImage(byte[] file) {
        return cloudinaryClient.upload(file, FOLDER);
    }
}
