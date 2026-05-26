package iuh.fit.se.modules.account.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdministrativeProvinceJpaRepository extends JpaRepository<AdministrativeProvinceJpaEntity, String> {

    List<AdministrativeProvinceJpaEntity> findAllByOrderByNameAsc();
}
