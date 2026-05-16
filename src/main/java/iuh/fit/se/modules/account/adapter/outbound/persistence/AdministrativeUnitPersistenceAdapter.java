package iuh.fit.se.modules.account.adapter.outbound.persistence;

import iuh.fit.se.modules.account.application.port.out.AdministrativeUnitLookupPort;
import iuh.fit.se.modules.account.domain.AdministrativeProvince;
import iuh.fit.se.modules.account.domain.AdministrativeWard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AdministrativeUnitPersistenceAdapter implements AdministrativeUnitLookupPort {

    private final AdministrativeProvinceJpaRepository provinceJpaRepository;
    private final AdministrativeWardJpaRepository wardJpaRepository;

    @Override
    public List<AdministrativeProvince> findAllProvincesWithWards() {
        Map<String, List<AdministrativeWard>> wardsByProvinceCode = wardJpaRepository
                .findAllByOrderByProvinceCodeAscNameAsc()
                .stream()
                .map(this::mapToDomainWard)
                .filter(ward -> ward.provinceCode() != null)
                .collect(Collectors.groupingBy(AdministrativeWard::provinceCode));

        return provinceJpaRepository.findAllByOrderByNameAsc().stream()
                .map(province -> mapToDomainProvince(
                        province,
                        wardsByProvinceCode.getOrDefault(province.getCode(), List.of())))
                .toList();
    }

    private AdministrativeProvince mapToDomainProvince(
            AdministrativeProvinceJpaEntity entity,
            List<AdministrativeWard> wards) {
        return new AdministrativeProvince(
                entity.getCode(),
                entity.getName(),
                entity.getNameEn(),
                entity.getFullName(),
                entity.getFullNameEn(),
                entity.getCodeName(),
                entity.getAdministrativeUnitId(),
                wards);
    }

    private AdministrativeWard mapToDomainWard(AdministrativeWardJpaEntity entity) {
        return new AdministrativeWard(
                entity.getCode(),
                entity.getName(),
                entity.getNameEn(),
                entity.getFullName(),
                entity.getFullNameEn(),
                entity.getCodeName(),
                entity.getProvinceCode(),
                entity.getAdministrativeUnitId());
    }
}
