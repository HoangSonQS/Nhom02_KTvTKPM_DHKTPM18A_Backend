package iuh.fit.se.modules.inventory.application.service;

import iuh.fit.se.modules.inventory.application.port.out.InventoryPersistencePort;
import iuh.fit.se.shared.event.inventory.InventoryStockChangedIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryAdminServiceTest {

    @Test
    void givenNewStock_whenInitialize_thenPublishRealtimeInventoryEvent() {
        InventoryPersistencePort persistencePort = mock(InventoryPersistencePort.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        when(persistencePort.findStockByBookId(7L)).thenReturn(Optional.empty());
        InventoryAdminService service = new InventoryAdminService(persistencePort, eventPublisher);

        service.initializeStock(7L, 12);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue()).isInstanceOfSatisfying(InventoryStockChangedIntegrationEvent.class, payload -> {
            assertThat(payload.bookId()).isEqualTo(7L);
            assertThat(payload.remainingQuantity()).isEqualTo(12);
            assertThat(payload.changeType()).isEqualTo("INITIALIZE");
        });
    }

    @Test
    void givenInventoryMutationMethods_whenInspectCacheEviction_thenCatalogBookCachesAreEvicted() throws Exception {
        assertCatalogBookCachesEvicted("initializeStock", Long.class, int.class);
        assertCatalogBookCachesEvicted("increaseStock", Long.class, int.class);
        assertCatalogBookCachesEvicted("decreaseStock", Long.class, int.class);
    }

    private static void assertCatalogBookCachesEvicted(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = InventoryAdminService.class.getMethod(methodName, parameterTypes);
        Caching caching = method.getAnnotation(Caching.class);

        assertThat(caching)
                .as("%s must evict catalog caches so /catalog/books returns fresh stock", methodName)
                .isNotNull();

        assertThat(Arrays.stream(caching.evict()).map(CacheEvict::value).flatMap(Arrays::stream))
                .contains("books", "bookDetails");

        assertThat(Arrays.stream(caching.evict())
                .filter(evict -> Arrays.asList(evict.value()).contains("books"))
                .anyMatch(CacheEvict::allEntries))
                .as("%s must evict all entries of the books search cache", methodName)
                .isTrue();
    }
}
