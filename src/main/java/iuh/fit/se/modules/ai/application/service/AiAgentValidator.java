package iuh.fit.se.modules.ai.application.service;

import iuh.fit.se.modules.ai.domain.AiAgentIntent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AiAgentValidator {

    public boolean requiresAuth(AiAgentIntent intent) {
        return switch (intent.normalized()) {
            case VIEW_CART, VIEW_ORDER, VIEW_LATEST_ORDER, CHECK_ORDER_STATUS,
                 ADD_TO_CART, REMOVE_FROM_CART, UPDATE_CART_QUANTITY,
                 PLACE_ORDER, CANCEL_ORDER, PAY_ORDER, CHANGE_PAYMENT_METHOD, CHANGE_SHIPPING_ADDRESS -> true;
            default -> false;
        };
    }

    public List<String> missingFields(AiAgentIntent intent, AiAgentService.ActionPayload payload) {
        List<String> missing = new ArrayList<>();
        AiAgentIntent normalized = intent.normalized();
        if ((normalized == AiAgentIntent.ADD_TO_CART
                || normalized == AiAgentIntent.UPDATE_CART_QUANTITY
                || normalized == AiAgentIntent.REMOVE_FROM_CART)
                && payload.bookId() == null
                && (payload.selectedBookIds() == null || payload.selectedBookIds().isEmpty())) {
            missing.add("sách cần thao tác");
        }
        if (normalized == AiAgentIntent.PLACE_ORDER) {
            if (!payload.cartCheckout()
                    && payload.bookId() == null
                    && (payload.selectedBookIds() == null || payload.selectedBookIds().isEmpty())) {
                missing.add("sách cần thao tác");
            }
            if (payload.shippingAddress() == null) missing.add("địa chỉ giao hàng");
            if (payload.customerPhone() == null) missing.add("số điện thoại");
            if (payload.paymentMethod() == null) missing.add("phương thức thanh toán COD hoặc VNPAY");
        }
        if ((normalized == AiAgentIntent.CANCEL_ORDER
                || normalized == AiAgentIntent.PAY_ORDER
                || normalized == AiAgentIntent.CHANGE_PAYMENT_METHOD) && payload.orderId() == null) {
            missing.add("id đơn hàng");
        }
        if (normalized == AiAgentIntent.CHANGE_PAYMENT_METHOD && !"COD".equals(payload.paymentMethod())) {
            missing.add("phương thức thanh toán COD");
        }
        return missing;
    }

    public boolean lowConfidence(AiAgentAnalysis analysis, double threshold) {
        return analysis == null || analysis.intent() == null || analysis.intent().normalized() == AiAgentIntent.UNKNOWN
                || analysis.confidence() < threshold;
    }
}
