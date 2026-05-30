package iuh.fit.se.modules.ai.domain;

public enum AiAgentIntent {
    SEARCH_BOOK,
    VIEW_BOOK_DETAIL,
    CHECK_STOCK,
    VIEW_CART,
    VIEW_ORDER,
    VIEW_LATEST_ORDER,
    CHECK_ORDER_STATUS,
    RECOMMEND_BOOK,
    ADD_TO_CART,
    REMOVE_FROM_CART,
    UPDATE_CART_QUANTITY,
    PLACE_ORDER,
    CANCEL_ORDER,
    PAY_ORDER,
    CHANGE_SHIPPING_ADDRESS,
    UNKNOWN,

    // Legacy aliases kept for backward compatibility with older payloads/prompts.
    SEARCH_BOOKS,
    BOOK_DETAIL,
    UPDATE_CART,
    REMOVE_CART,
    CHECKOUT,
    ORDER_STATUS,
    GENERAL_CHAT;

    public AiAgentIntent normalized() {
        return switch (this) {
            case SEARCH_BOOKS -> SEARCH_BOOK;
            case BOOK_DETAIL -> VIEW_BOOK_DETAIL;
            case UPDATE_CART -> UPDATE_CART_QUANTITY;
            case REMOVE_CART -> REMOVE_FROM_CART;
            case CHECKOUT -> PLACE_ORDER;
            case ORDER_STATUS -> CHECK_ORDER_STATUS;
            case GENERAL_CHAT -> UNKNOWN;
            default -> this;
        };
    }

    public boolean isImportantWrite() {
        return switch (normalized()) {
            case PLACE_ORDER, CANCEL_ORDER, PAY_ORDER, CHANGE_SHIPPING_ADDRESS -> true;
            default -> false;
        };
    }

    public boolean isLightWrite() {
        return switch (normalized()) {
            case ADD_TO_CART, REMOVE_FROM_CART, UPDATE_CART_QUANTITY -> true;
            default -> false;
        };
    }
}
