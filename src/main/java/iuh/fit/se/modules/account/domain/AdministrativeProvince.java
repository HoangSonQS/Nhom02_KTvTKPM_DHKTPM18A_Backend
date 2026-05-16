package iuh.fit.se.modules.account.domain;

import java.util.List;

public record AdministrativeProvince(
        String code,
        String name,
        String nameEn,
        String fullName,
        String fullNameEn,
        String codeName,
        Integer administrativeUnitId,
        List<AdministrativeWard> wards) {
}
