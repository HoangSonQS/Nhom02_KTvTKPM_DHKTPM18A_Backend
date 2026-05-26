package iuh.fit.se.modules.account.application.port.out;

import iuh.fit.se.modules.account.domain.AdministrativeProvince;

import java.util.List;

public interface AdministrativeUnitLookupPort {

    List<AdministrativeProvince> findAllProvincesWithWards();
}
