package iuh.fit.se.modules.logistics.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.modules.logistics.application.port.out.LogisticsOutboxPersistencePort;
import iuh.fit.se.modules.logistics.application.port.out.PurchaseOrderPersistencePort;
import iuh.fit.se.modules.logistics.application.port.out.SupplierPersistencePort;
import iuh.fit.se.modules.logistics.domain.Supplier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierSoftDeleteTest {

    @Test
    void givenSupplier_whenDeleteSupplier_thenMarksDeletedAndPersists() {
        SupplierPersistencePort supplierPort = mock(SupplierPersistencePort.class);
        Supplier supplier = Supplier.create("Demo", "A", "090", "demo@sebook.local", "HCM", "TAX");
        when(supplierPort.findById(1L)).thenReturn(Optional.of(supplier));

        LogisticsService service = new LogisticsService(
                supplierPort,
                mock(PurchaseOrderPersistencePort.class),
                mock(LogisticsOutboxPersistencePort.class),
                new ObjectMapper()
        );

        service.deleteSupplier(1L);

        assertThat(supplier.isDeleted()).isTrue();
        verify(supplierPort).save(supplier);
    }

    @Test
    void whenGetAllSuppliers_thenOnlyActiveSuppliersAreReturned() {
        SupplierPersistencePort supplierPort = mock(SupplierPersistencePort.class);
        Supplier active = Supplier.create("Active", null, null, null, null, null);
        when(supplierPort.findAllActive()).thenReturn(List.of(active));

        LogisticsService service = new LogisticsService(
                supplierPort,
                mock(PurchaseOrderPersistencePort.class),
                mock(LogisticsOutboxPersistencePort.class),
                new ObjectMapper()
        );

        assertThat(service.getAllSuppliers()).containsExactly(active);
    }
}
