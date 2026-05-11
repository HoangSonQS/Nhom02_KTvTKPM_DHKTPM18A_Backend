package iuh.fit.se.modules.admin.application.port.in;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Port/In cho các báo cáo quản trị.
 */
public interface AdminReportUseCase {
    DashboardDto getDashboardMetrics();

    /**
     * DTO cho Dashboard metrics.
     *
     * Annotation @JsonDeserialize + @JsonPOJOBuilder là bắt buộc để Jackson có thể
     * deserialize object này khi đọc từ Redis cache (GenericJackson2JsonRedisSerializer).
     * Lombok @Builder không tự sinh @JsonCreator, nên cần chỉ định tường minh.
     */
    @Getter
    @Builder
    @JsonDeserialize(builder = DashboardDto.DashboardDtoBuilder.class)
    class DashboardDto {
        private long totalOrders;
        private long paidOrders;
        private double conversionRate;
        private double averageTimeToPaymentSeconds;
        private BigDecimal averageOrderValue;
        private long uniqueBuyers;

        @JsonPOJOBuilder(withPrefix = "")
        public static class DashboardDtoBuilder {
            // Lombok tự sinh phần còn lại — class này chỉ để gắn @JsonPOJOBuilder
        }
    }
}
