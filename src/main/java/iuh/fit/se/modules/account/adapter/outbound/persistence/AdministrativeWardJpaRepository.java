package iuh.fit.se.modules.account.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdministrativeWardJpaRepository extends JpaRepository<AdministrativeWardJpaEntity, String> {

    List<AdministrativeWardJpaEntity> findAllByOrderByProvinceCodeAscNameAsc();
}
