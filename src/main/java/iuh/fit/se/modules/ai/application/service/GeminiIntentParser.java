package iuh.fit.se.modules.ai.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.modules.ai.application.port.out.LlmPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiIntentParser {

    private final LlmPort llmPort;
    private final ObjectMapper objectMapper;

    public AiAgentAnalysis parse(String message) {
        String prompt = """
                Bạn là intent parser cho web bán sách. Nhiệm vụ duy nhất là phân tích câu chat của user và trả về JSON hợp lệ theo schema.
                Không thực hiện hành động thật, không nói đã đặt hàng, không nói đã thêm giỏ. Backend mới là nơi thực hiện nghiệp vụ.

                Intent hợp lệ:
                SEARCH_BOOK, VIEW_BOOK_DETAIL, CHECK_STOCK, ADD_TO_CART, REMOVE_FROM_CART, UPDATE_CART_QUANTITY,
                VIEW_CART, PLACE_ORDER, VIEW_ORDER, VIEW_LATEST_ORDER, CHECK_ORDER_STATUS, CANCEL_ORDER, PAY_ORDER,
                CHANGE_PAYMENT_METHOD, CHANGE_SHIPPING_ADDRESS, RECOMMEND_BOOK, UNKNOWN.

                Schema bắt buộc:
                {"intent":"string","entities":{"bookName":"string or null","bookId":null,"quantity":null,"orderId":null,"category":null,"author":null,"couponCode":null,"paymentMethod":null,"shippingAddress":null,"customerPhone":null},"confidence":0.0,"needConfirmation":false,"reason":"short string"}

                Quy tắc:
                - Nếu user muốn đặt hàng, hủy đơn hoặc thanh toán, needConfirmation = true.
                - Nếu user chỉ hỏi thông tin, needConfirmation = false.
                - Nếu câu mơ hồ, intent = UNKNOWN hoặc confidence thấp.
                - Không tự bịa tên sách, số lượng, giá, tồn kho.
                - Nếu không có số lượng, mặc định quantity = 1 chỉ khi intent là ADD_TO_CART hoặc PLACE_ORDER và có bookName rõ ràng.
                - Không trả markdown. Chỉ trả JSON.

                Câu chat: "%s"
                """.formatted(message == null ? "" : message.replace("\"", "\\\""));
        try {
            String raw = llmPort.chat(prompt, List.of());
            AiAgentAnalysis analysis = objectMapper.readValue(extractJson(raw), AiAgentAnalysis.class)
                    .withDefaults(AiAgentSource.GEMINI);
            log.debug("AI agent Gemini intent parsed: intent={}, confidence={}", analysis.intent(), analysis.confidence());
            return analysis;
        } catch (Exception e) {
            log.warn("Unable to parse AI agent Gemini intent safely: {}", e.getMessage());
            return AiAgentAnalysis.unknown(AiAgentSource.GEMINI, "gemini parse failed");
        }
    }

    private String extractJson(String raw) {
        if (raw == null) {
            return "{}";
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }
}
