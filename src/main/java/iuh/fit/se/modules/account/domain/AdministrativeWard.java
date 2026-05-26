package iuh.fit.se.modules.account.domain;

public record AdministrativeWard(
        String code,
        String name,
        String nameEn,
        String fullName,
        String fullNameEn,
        String codeName,
        String provinceCode,
        Integer administrativeUnitId) {
}
