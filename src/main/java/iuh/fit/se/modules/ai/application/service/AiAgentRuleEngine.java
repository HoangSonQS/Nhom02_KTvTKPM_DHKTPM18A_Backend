package iuh.fit.se.modules.ai.application.service;

import iuh.fit.se.modules.ai.application.port.in.AiAgentUseCase;
import iuh.fit.se.modules.ai.domain.AiAgentIntent;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AiAgentRuleEngine {

    private static final Pattern QUANTITY_PATTERN = Pattern.compile("(?<![\\p{Alnum}])-?\\d{1,5}(?![\\p{Alnum}])");
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("(?iu)(?:\\bORD\\s*0*|#\\s*|\\b\\u0111\\u01a1n\\s*(?:h\\u00e0ng)?\\s*0*|\\bdon\\s*(?:hang)?\\s*0*)?(\\d{1,12})\\b");
    private static final Pattern STOCK_CLEANUP_PATTERN = Pattern.compile(
            "(?iu)\\b(sách|sach|quyển|quyen|cuốn|cuon|còn hàng không|con hang khong|còn bao nhiêu|con bao nhieu|còn hàng|con hang|còn không|con khong|hết hàng chưa|het hang chua|hết hàng|het hang|tồn kho|ton kho|số lượng|so luong|có sẵn|co san|không|khong|ko)\\b|[?!.:,;\"']"
    );
    private static final Pattern CART_CLEANUP_PATTERN = Pattern.compile(
            "(?iu)\\b(tôi|toi|tao|mình|minh|muốn|muon|hãy|hay|giúp|giup|cho|thêm|them|bỏ|bo|xóa|xoa|vào|vao|khỏi|khoi|giỏ|gio|giỏ hàng|gio hang|sách|sach|quyển|quyen|cuốn|cuon|cái|cai)\\b|\\b\\d{1,2}\\b|[?!.:,;\"']"
    );
    private static final Pattern PURCHASE_CLEANUP_PATTERN = Pattern.compile(
            "(?iu)\\b(tôi|toi|tao|mình|minh|muốn|muon|hãy|hay|giúp|giup|cho|đặt hàng|dat hang|đặt|dat|mua|order|sách|sach|quyển|quyen|cuốn|cuon|cái|cai|bằng|bang|cod|vnpay|vn pay|thanh toán|thanh toan)\\b|\\b\\d{1,2}\\b|[?!.:,;\"']"
    );

    private static final Pattern QUANTITY_PHRASE_CLEANUP_PATTERN = Pattern.compile(
            "\\b(hai\\s+muoi|muoi\\s+chin|muoi\\s+tam|muoi\\s+bay|muoi\\s+sau|muoi\\s+(?:lam|nam)|muoi\\s+bon|muoi\\s+ba|muoi\\s+hai|muoi\\s+mot|muoi|chin|tam|bay|sau|nam|lam|bon|tu|ba|hai|mot)\\s+(?:cuon|quyen|sach|cai)\\b"
    );

    public AiAgentAnalysis analyze(String message, AiAgentUseCase.ClientAction clientAction) {
        if (clientAction != null && clientAction.action() != null && !clientAction.action().isBlank()) {
            return analyzeClientAction(clientAction);
        }

        String normalized = normalize(message);
        if (normalized.isBlank()) {
            return AiAgentAnalysis.unknown(AiAgentSource.RULE, "blank message");
        }
        if (looksLikeAdviceOrPreviewQuestion(normalized)) {
            String bookName = extractAdviceBookQuery(message);
            if (hasSpecificBookQuery(bookName)) {
                return analysis(AiAgentIntent.VIEW_BOOK_DETAIL, bookName, null, 1, null, null, null, 0.88, false, "book advice/detail question");
            }
        }
        if (looksLikeHowToOrHypotheticalQuestion(normalized)) {
            return AiAgentAnalysis.unknown(AiAgentSource.RULE, "how-to or hypothetical question");
        }
        if (isAmbiguousWrite(normalized)) {
            return AiAgentAnalysis.unknown(AiAgentSource.RULE, "ambiguous write");
        }
        if (looksLikeChangePaymentMethod(normalized)) {
            return paymentMethodAnalysis(extractOrderId(message), normalizePaymentMethod(message));
        }
        if (looksLikeCancelOrder(normalized)) {
            return orderAnalysis(AiAgentIntent.CANCEL_ORDER, extractOrderId(message), 0.93, true, "cancel order request");
        }
        if (looksLikeCartWrite(normalized)) {
            AiAgentIntent intent = cartWriteIntent(normalized);
            String bookName = extractCartBookQuery(message);
            if (hasSpecificBookQuery(bookName)) {
                return analysis(intent, bookName, null, quantity(message), null, null, null, 0.95, false, "cart write");
            }
            return analysis(intent, null, null, quantity(message), null, null, null, 0.9, false, "cart write missing book");
        }
        if (looksLikeStockQuestion(normalized)) {
            String bookName = extractStockBookQuery(message);
            if (hasSpecificBookQuery(bookName)) {
                return analysis(AiAgentIntent.CHECK_STOCK, bookName, null, quantity(message), null, null, null, 0.92, false, "stock question");
            }
        }
        if (looksLikeLatestOrderQuestion(normalized)) {
            return new AiAgentAnalysis(AiAgentIntent.VIEW_LATEST_ORDER, new AiAgentEntities(), 0.91, AiAgentSource.RULE, false, "latest order", List.of());
        }
        if (looksLikeViewCart(normalized)) {
            return new AiAgentAnalysis(AiAgentIntent.VIEW_CART, new AiAgentEntities(), 0.9, AiAgentSource.RULE, false, "view cart", List.of());
        }
        if (looksLikeBookDetailQuestion(normalized)) {
            String bookName = extractBookDetailQuery(message);
            if (hasSpecificBookQuery(bookName)) {
                return analysis(AiAgentIntent.VIEW_BOOK_DETAIL, bookName, null, 1, null, null, null, 0.9, false, "book detail question");
            }
        }
        if (looksLikeBroadAudienceRecommendation(normalized)) {
            return AiAgentAnalysis.unknown(AiAgentSource.RULE, "broad audience recommendation");
        }
        if (looksLikeSearchOrRecommend(normalized)) {
            String query = extractSearchQuery(normalized);
            if (hasSpecificBookQuery(query)) {
                AiAgentIntent intent = looksLikeRecommendation(normalized) ? AiAgentIntent.RECOMMEND_BOOK : AiAgentIntent.SEARCH_BOOK;
                return analysis(intent, query, null, 1, null, null, null, 0.9, false, "catalog search");
            }
        }
        if (looksLikeCartWrite(normalized)) {
            AiAgentIntent intent = normalized.contains("xoa") || normalized.contains("bo khoi") || normalized.contains("bo ")
                    ? AiAgentIntent.REMOVE_FROM_CART
                    : AiAgentIntent.ADD_TO_CART;
            String bookName = extractCartBookQuery(message);
            if (hasSpecificBookQuery(bookName)) {
                return analysis(intent, bookName, null, quantity(message), null, null, null, 0.95, false, "cart write");
            }
        }
        if (looksLikePurchase(normalized)) {
            String bookName = extractPurchaseBookQuery(message);
            if (hasSpecificBookQuery(bookName) || isCartCheckoutRequest(normalized)) {
                String purchaseText = stripCheckoutDetails(message);
                return analysis(
                        AiAgentIntent.PLACE_ORDER,
                        hasSpecificBookQuery(bookName) ? bookName : null,
                        null,
                        quantity(purchaseText),
                        normalizePaymentMethod(message),
                        extractShippingAddress(message),
                        extractPhoneNumber(message),
                        0.9,
                        true,
                        "clear order request"
                );
            }
            String purchaseText = stripCheckoutDetails(message);
            return analysis(
                    AiAgentIntent.PLACE_ORDER,
                    null,
                    null,
                    quantity(purchaseText),
                    normalizePaymentMethod(message),
                    extractShippingAddress(message),
                    extractPhoneNumber(message),
                    0.88,
                    true,
                    "order request missing book"
            );
        }
        if (looksLikeContextReference(normalized)) {
            return analysis(AiAgentIntent.PLACE_ORDER, null, null, quantity(message), null, null, null, 0.88, true, "context reference missing");
        }
        return AiAgentAnalysis.unknown(AiAgentSource.RULE, "no confident rule");
    }

    private AiAgentAnalysis analyzeClientAction(AiAgentUseCase.ClientAction action) {
        AiAgentIntent intent = switch (action.action()) {
            case "ADD_TO_CART" -> AiAgentIntent.ADD_TO_CART;
            case "VIEW_BOOK_DETAIL" -> AiAgentIntent.VIEW_BOOK_DETAIL;
            case "PAY_ORDER" -> AiAgentIntent.PAY_ORDER;
            case "CHANGE_PAYMENT_METHOD" -> AiAgentIntent.CHANGE_PAYMENT_METHOD;
            case "CANCEL_ORDER" -> AiAgentIntent.CANCEL_ORDER;
            case "PLACE_ORDER" -> AiAgentIntent.PLACE_ORDER;
            default -> AiAgentIntent.UNKNOWN;
        };
        return new AiAgentAnalysis(
                intent,
                new AiAgentEntities(
                        null,
                        action.bookId(),
                        action.quantity(),
                        null,
                        action.paymentMethod(),
                        action.shippingAddress(),
                        action.customerPhone(),
                        action.orderId(),
                        null,
                        null
                ),
                intent == AiAgentIntent.UNKNOWN ? 0.0 : 1.0,
                AiAgentSource.RULE,
                intent.isImportantWrite(),
                "client action",
                List.of()
        );
    }

    private AiAgentAnalysis analysis(
            AiAgentIntent intent,
            String bookName,
            Long bookId,
            Integer quantity,
            String paymentMethod,
            String shippingAddress,
            String customerPhone,
            double confidence,
            boolean needConfirmation,
            String reason
    ) {
        return new AiAgentAnalysis(
                intent,
                new AiAgentEntities(bookName, bookId, quantity, null, paymentMethod, shippingAddress, customerPhone, null, null, null),
                confidence,
                AiAgentSource.RULE,
                needConfirmation,
                reason,
                List.of()
        );
    }

    private AiAgentAnalysis orderAnalysis(
            AiAgentIntent intent,
            Long orderId,
            double confidence,
            boolean needConfirmation,
            String reason
    ) {
        return new AiAgentAnalysis(
                intent,
                new AiAgentEntities(null, null, 1, null, null, null, null, orderId, null, null),
                confidence,
                AiAgentSource.RULE,
                needConfirmation,
                reason,
                List.of()
        );
    }

    private AiAgentAnalysis paymentMethodAnalysis(Long orderId, String paymentMethod) {
        return new AiAgentAnalysis(
                AiAgentIntent.CHANGE_PAYMENT_METHOD,
                new AiAgentEntities(null, null, 1, null, paymentMethod, null, null, orderId, null, null),
                0.95,
                AiAgentSource.RULE,
                true,
                "change payment method request",
                List.of()
        );
    }

    private boolean looksLikeStockQuestion(String normalized) {
        return normalized.contains("con hang")
                || normalized.contains("con bao nhieu")
                || normalized.contains("het hang")
                || normalized.contains("ton kho")
                || normalized.contains("so luong")
                || normalized.contains("con du")
                || normalized.contains("co san")
                || normalized.contains("stock");
    }

    private boolean looksLikeBookDetailQuestion(String normalized) {
        return normalized.contains("chi tiet")
                || normalized.contains("gia bao nhieu")
                || normalized.contains("bao nhieu tien")
                || normalized.contains("gia sach")
                || normalized.contains("tac gia nao")
                || normalized.contains("cua tac gia");
    }

    private boolean looksLikeAdviceOrPreviewQuestion(String normalized) {
        return normalized.contains("co nen mua")
                || normalized.contains("nen mua")
                || normalized.contains("co dang mua")
                || normalized.contains("dang mua")
                || normalized.contains("phan van")
                || normalized.contains("xem thu");
    }

    private boolean looksLikeHowToOrHypotheticalQuestion(String normalized) {
        return normalized.contains("lam sao")
                || normalized.contains("huong dan")
                || normalized.contains("hoi cach")
                || normalized.contains("cach huy")
                || normalized.contains("cach dat")
                || normalized.contains("da mua")
                || (normalized.contains("neu") && normalized.contains("thi"));
    }

    private boolean looksLikeLatestOrderQuestion(String normalized) {
        return (normalized.contains("don") || normalized.contains("order"))
                && (normalized.contains("gan nhat")
                || normalized.contains("toi dau")
                || normalized.contains("den dau")
                || normalized.contains("trang thai")
                || normalized.contains("dang o dau"));
    }

    private boolean looksLikeCancelOrder(String normalized) {
        return (normalized.contains("huy") || normalized.contains("cancel"))
                && (normalized.contains("don") || normalized.contains("order"));
    }

    private boolean looksLikeChangePaymentMethod(String normalized) {
        return (normalized.contains("doi") || normalized.contains("chuyen"))
                && normalized.contains("thanh toan")
                && (normalized.contains("cod") || normalized.contains("tien mat"));
    }

    private boolean looksLikeViewCart(String normalized) {
        return normalized.contains("gio hang") && (normalized.contains("xem") || normalized.contains("co gi") || normalized.contains("hien tai"));
    }

    private boolean looksLikeCartWrite(String normalized) {
        return normalized.contains("gio") && (normalized.contains("them")
                || normalized.contains("bo")
                || normalized.contains("xoa")
                || normalized.contains("doi")
                || normalized.contains("cap nhat")
                || normalized.contains("so luong")
                || normalized.contains("thanh"));
    }

    private AiAgentIntent cartWriteIntent(String normalized) {
        if (normalized.contains("xoa") || normalized.contains("bo khoi") || normalized.contains("bo ") || normalized.contains(" ra khoi")) {
            return AiAgentIntent.REMOVE_FROM_CART;
        }
        if (normalized.contains("doi") || normalized.contains("cap nhat") || normalized.contains("so luong") || normalized.contains("thanh")) {
            return AiAgentIntent.UPDATE_CART_QUANTITY;
        }
        return AiAgentIntent.ADD_TO_CART;
    }

    private boolean looksLikeSearchOrRecommend(String normalized) {
        return normalized.contains("tim sach")
                || normalized.contains("tim giup")
                || normalized.startsWith("tim ")
                || normalized.contains("muon tim")
                || normalized.contains("co sach")
                || normalized.contains("sach cua")
                || normalized.contains("quyen nao")
                || normalized.contains("cuon nao")
                || normalized.contains("sach nao")
                || normalized.contains("goi y")
                || normalized.contains("de xuat");
    }

    private boolean looksLikeRecommendation(String normalized) {
        return normalized.contains("quyen nao")
                || normalized.contains("cuon nao")
                || normalized.contains("sach nao")
                || normalized.contains("giong")
                || normalized.contains("tuong tu")
                || normalized.contains("goi y")
                || normalized.contains("de xuat");
    }

    private boolean looksLikeBroadAudienceRecommendation(String normalized) {
        return normalized.contains("phu hop cho")
                && (normalized.contains("sach nao")
                || normalized.contains("quyen nao")
                || normalized.contains("cuon nao"));
    }

    private boolean looksLikePurchase(String normalized) {
        return normalized.contains("dat hang")
                || normalized.startsWith("dat ")
                || normalized.contains(" dat ")
                || normalized.startsWith("mua ")
                || normalized.contains(" mua ")
                || normalized.contains("order ");
    }

    private boolean isCartCheckoutRequest(String normalized) {
        return normalized.contains("dat hang")
                || normalized.contains("checkout")
                || normalized.contains("thanh toan")
                || normalized.contains("cod")
                || normalized.contains("vnpay");
    }

    private boolean looksLikeContextReference(String normalized) {
        return normalized.contains("cuon do")
                || normalized.contains("quyen do")
                || normalized.contains("sach do")
                || normalized.contains("cuon dau tien")
                || normalized.contains("quyen dau tien")
                || normalized.contains("sach dau tien")
                || normalized.contains("lay cuon")
                || normalized.contains("lay quyen")
                || (normalized.contains("lay") && (normalized.contains("cuon") || normalized.contains("quyen") || normalized.contains("sach")));
    }

    private boolean isAmbiguousWrite(String normalized) {
        return normalized.contains("co nen mua")
                || normalized.contains("nen mua")
                || normalized.contains("dang mua")
                || normalized.contains("da mua")
                || normalized.contains("phan van")
                || normalized.contains("tinh dat sau")
                || normalized.contains("neu con hang")
                || normalized.contains("lam sao")
                || normalized.contains("hoi cach")
                || normalized.contains("cach huy")
                || normalized.contains("cach dat")
                || normalized.contains("xem thu")
                || (normalized.contains("tim") && normalized.contains("dat") && normalized.contains("phu hop"));
    }

    private String extractStockBookQuery(String message) {
        String cleaned = normalize(message)
                .replaceAll("\\b(con du khong|con du|con hang khong|con hang|con bao nhieu|con khong|het hang chua|het hang|ton kho|so luong|co san|stock)\\b", " ")
                .replaceAll("\\b(toi|tao|minh|muon|mua|sach|quyen|cuon|du|khong|ko)\\b", " ")
                .replaceAll("\\b\\d{1,2}\\b", " ");
        cleaned = STOCK_CLEANUP_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned.isBlank() ? message : cleaned;
    }

    private String extractBookDetailQuery(String message) {
        String cleaned = normalize(message)
                .replaceAll("\\b(gia bao nhieu|bao nhieu tien|cua tac gia nao|tac gia nao|gia sach|chi tiet)\\b", " ")
                .replaceAll("\\b(toi|tao|minh|muon|cho|xem|thong tin|sach|quyen|cuon|cua|khong|ko)\\b", " ")
                .replaceAll("[?!.:,;\"']", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isBlank() ? message : cleaned;
    }

    private String extractAdviceBookQuery(String message) {
        String cleaned = normalize(message)
                .replaceAll("\\b(co nen mua|nen mua|co dang mua|dang mua|phan van|xem thu|mua)\\b", " ")
                .replaceAll("\\b(toi|tao|minh|muon|cho|sach|quyen|cuon|cua|khong|ko|neu|con hang|thi|tinh|dat sau|sau|dang)\\b", " ")
                .replaceAll("[?!.:,;\"']", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private String extractCartBookQuery(String message) {
        String cleaned = CART_CLEANUP_PATTERN.matcher(removeQuantityPhrases(normalize(message))).replaceAll(" ");
        cleaned = cleaned
                .replaceAll("\\b(doi|cap nhat|so luong|thanh|trong|ra|hang)\\b", " ");
        cleaned = removeContextReferenceWords(cleaned);
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private String extractPurchaseBookQuery(String message) {
        String source = stripCheckoutDetails(message);
        String cleaned = PURCHASE_CLEANUP_PATTERN.matcher(removeQuantityPhrases(normalize(source))).replaceAll(" ");
        cleaned = cleaned.replaceAll("\\b(va|chon)\\b", " ");
        cleaned = removeContextReferenceWords(cleaned);
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private String removeQuantityPhrases(String normalized) {
        return QUANTITY_PHRASE_CLEANUP_PATTERN.matcher(normalized)
                .replaceAll(" ")
                .replaceAll("(?<![\\p{Alnum}])-?\\d{1,5}(?![\\p{Alnum}])", " ");
    }

    private String removeContextReferenceWords(String cleaned) {
        return cleaned
                .replaceAll("\\b(dau tien|thu nhat|cuon do|quyen do|sach do|cuon nay|quyen nay|sach nay|do|nay)\\b", " ");
    }

    private String extractSearchQuery(String normalized) {
        String cleaned = normalized
                .replaceAll("\\b(toi|tao|minh|muon|can|hay|giup|cho|tim|sach|quyen|cuon|nao|ve|cua|co|khong|giong|tuong|tu|goi y|de xuat|giup toi)\\b", " ")
                .replaceAll("[?!.:,;\"']", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isBlank() ? normalized : cleaned;
    }

    private String stripCheckoutDetails(String message) {
        if (message == null) {
            return "";
        }
        return message
                .replaceFirst("(?iu)(;|,)?\\s*(địa chỉ giao hàng|dia chi giao hang|giao tới|giao toi|địa chỉ|dia chi)\\s*[:：]?.*$", "")
                .replaceFirst("(?iu)(;|,)?\\s*(số điện thoại|so dien thoai|sdt|phone)\\s*[:：]?.*$", "")
                .trim();
    }

    private String extractShippingAddress(String message) {
        if (message == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?iu)(địa chỉ giao hàng|dia chi giao hang|giao tới|giao toi|địa chỉ|dia chi)\\s*[:：]?\\s*(.+?)(?:\\s*(?:;|,)\\s*(?:số điện thoại|so dien thoai|sdt|phone)\\s*[:：]?|$)").matcher(message);
        return matcher.find() ? normalizeBlank(matcher.group(2)) : null;
    }

    private String extractPhoneNumber(String message) {
        if (message == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?iu)(số điện thoại|so dien thoai|sdt|phone)\\s*[:：]?\\s*([0-9+ .-]{8,16})").matcher(message);
        return matcher.find() ? normalizeBlank(matcher.group(2).replaceAll("[ .-]", "")) : null;
    }

    private Integer quantity(String message) {
        Matcher matcher = QUANTITY_PATTERN.matcher(message == null ? "" : message);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return vietnameseWordQuantity(normalize(message));
    }

    private Integer vietnameseWordQuantity(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        if (normalized.contains("hai muoi")) return 20;
        if (normalized.contains("muoi chin")) return 19;
        if (normalized.contains("muoi tam")) return 18;
        if (normalized.contains("muoi bay")) return 17;
        if (normalized.contains("muoi sau")) return 16;
        if (normalized.contains("muoi lam") || normalized.contains("muoi nam")) return 15;
        if (normalized.contains("muoi bon")) return 14;
        if (normalized.contains("muoi ba")) return 13;
        if (normalized.contains("muoi hai")) return 12;
        if (normalized.contains("muoi mot")) return 11;
        if (normalized.contains("muoi")) return 10;
        if (normalized.contains("chin")) return 9;
        if (normalized.contains("tam")) return 8;
        if (normalized.contains("bay")) return 7;
        if (normalized.contains("sau")) return 6;
        if (normalized.contains("nam") || normalized.contains("lam")) return 5;
        if (normalized.contains("bon") || normalized.contains("tu")) return 4;
        if (normalized.contains("ba")) return 3;
        if (normalized.contains("hai")) return 2;
        if (normalized.contains("mot")) return 1;
        return null;
    }

    private Long extractOrderId(String message) {
        Matcher matcher = ORDER_ID_PATTERN.matcher(message == null ? "" : message);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean hasSpecificBookQuery(String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        String normalized = normalize(query);
        return normalized.length() >= 3
                && !normalized.equals("don")
                && !normalized.equals("don nay")
                && !normalized.equals("don hang")
                && !normalized.equals("nay");
    }

    private String normalizePaymentMethod(String value) {
        String normalized = normalize(value).toUpperCase(Locale.ROOT);
        if (normalized.contains("COD") || normalized.contains("TIEN MAT") || normalized.contains("KHI NHAN")) {
            return "COD";
        }
        if (normalized.contains("VNPAY") || normalized.contains("VN PAY") || normalized.contains("ONLINE")) {
            return "VNPAY";
        }
        return null;
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
