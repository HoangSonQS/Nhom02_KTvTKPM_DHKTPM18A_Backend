package iuh.fit.se.modules.inventory.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryAdminServiceTest {

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
