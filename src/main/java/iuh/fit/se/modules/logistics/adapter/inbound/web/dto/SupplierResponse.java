package iuh.fit.se.modules.logistics.adapter.inbound.web.dto;

import iuh.fit.se.modules.logistics.domain.Supplier;

import java.time.LocalDateTime;

public record SupplierResponse(
        Long id,
        String name,
        String contactPerson,
        String phoneNumber,
        String email,
        String address,
        String taxCode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SupplierResponse from(Supplier supplier) {
        if (supplier == null) return null;
        return new SupplierResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getContactPerson(),
                supplier.getPhoneNumber(),
                supplier.getEmail(),
                supplier.getAddress(),
                supplier.getTaxCode(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt()
        );
    }
}
