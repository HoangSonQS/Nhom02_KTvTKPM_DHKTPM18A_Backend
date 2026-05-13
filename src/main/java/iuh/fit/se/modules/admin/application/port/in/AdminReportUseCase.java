package iuh.fit.se.modules.admin.application.port.in;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

public interface AdminReportUseCase {
    DashboardDto getDashboardMetrics();

    DashboardV2Dto getDashboardMetricsV2();

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
        }
    }

    @Getter
    @Builder
    @JsonDeserialize(builder = DashboardV2Dto.DashboardV2DtoBuilder.class)
    class DashboardV2Dto {
        private long totalOrders;
        private long paidOrders;
        private double conversionRate;
        private double averageTimeToPaymentSeconds;
        private BigDecimal averageOrderValue;
        private long uniqueBuyers;
        private BigDecimal totalRevenue;
        private BigDecimal netRevenue;
        private BigDecimal refundAmount;
        private List<TopBookDto> topBooks;

        @JsonPOJOBuilder(withPrefix = "")
        public static class DashboardV2DtoBuilder {
        }
    }

    record TopBookDto(
            Long bookId,
            String title,
            long quantitySold,
            BigDecimal revenue
    ) {}
}
