package iuh.fit.se.modules.admin.adapter.inbound.event;

import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class AdminReportEventListenerTest {

    @Test
    void givenReportMutationEvents_whenInspectCacheEviction_thenDashboardStatsAreEvicted() {
        assertDashboardCacheEvicted("onOrderCreated", "OrderCreatedDomainEvent");
        assertDashboardCacheEvicted("onPaymentSuccess", "PaymentSuccessEvent");
        assertDashboardCacheEvicted("onOrderCancelled", "OrderCancelledEvent");
        assertDashboardCacheEvicted("onReturnRefunded", "ReturnRequestRefundedEvent");
    }

    private static void assertDashboardCacheEvicted(String methodName, String eventSimpleName) {
        Method method = Arrays.stream(AdminReportEventListener.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .filter(candidate -> candidate.getParameterTypes()[0].getSimpleName().equals(eventSimpleName))
                .findFirst()
                .orElseThrow();

        Caching caching = method.getAnnotation(Caching.class);
        assertThat(caching)
                .as("%s must evict dashboardStats after updating adm_order_report", methodName)
                .isNotNull();

        assertThat(Arrays.stream(caching.evict())
                .filter(evict -> Arrays.asList(evict.value()).contains("dashboardStats"))
                .anyMatch(CacheEvict::allEntries))
                .as("%s must evict all dashboardStats entries", methodName)
                .isTrue();
    }
}
