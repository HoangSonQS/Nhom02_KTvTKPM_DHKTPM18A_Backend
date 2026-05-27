package iuh.fit.se.modules.returns.application.port.out;

import iuh.fit.se.shared.infrastructure.cloudinary.CloudinaryUploadResult;

public interface ReturnEvidenceImagePort {
    CloudinaryUploadResult uploadEvidenceImage(byte[] file);
}
