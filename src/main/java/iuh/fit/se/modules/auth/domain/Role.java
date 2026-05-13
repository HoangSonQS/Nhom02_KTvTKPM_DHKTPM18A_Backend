package iuh.fit.se.modules.auth.domain;

/**
 * Enum Role của hệ thống.
 * Nằm trong domain của module auth vì auth là nơi định nghĩa Identity.
 * Module khác reference bằng String "ROLE_ADMIN" thay vì import enum này.
 */
import lombok.Getter;
import java.util.EnumSet;
import java.util.Set;
import static iuh.fit.se.modules.auth.domain.Permission.*;

/**
 * Enum Role của hệ thống — RBAC 2.0.
 * Mỗi vai trò nắm giữ một danh sách quyền hạn (Permissions) cứng.
 */
@Getter
public enum Role {
    ADMIN(EnumSet.allOf(Permission.class)),

    STAFF_SELLER(EnumSet.of(
        AUTH_LOGIN, AUTH_REFRESH_TOKEN, AUTH_LOGOUT,
        ACCOUNT_VIEW_SELF, ACCOUNT_UPDATE_SELF,
        CATALOG_READ, CATALOG_SEARCH, CATALOG_BOOK_CREATE, CATALOG_BOOK_UPDATE, CATALOG_CATEGORY_WRITE,
        ORDER_READ_ALL, ORDER_UPDATE_STATUS,
        PAYMENT_READ_ALL,
        PROMOTION_VALIDATE, PROMOTION_READ_ALL, PROMOTION_CREATE, PROMOTION_PAUSE_RESUME,
        RETURN_READ_ALL, RETURN_APPROVE, RETURN_RECEIVE,
        DASHBOARD_REVENUE
    )),

    STAFF_WAREHOUSE(EnumSet.of(
        AUTH_LOGIN, AUTH_REFRESH_TOKEN, AUTH_LOGOUT,
        ACCOUNT_VIEW_SELF, ACCOUNT_UPDATE_SELF,
        CATALOG_READ, CATALOG_SEARCH,
        INVENTORY_READ, INVENTORY_IMPORT_STOCK, INVENTORY_VIEW_HISTORY,
        PURCHASE_ORDER_CREATE, PURCHASE_ORDER_READ_ALL,
        STOCK_REQUEST_CREATE, STOCK_REQUEST_APPROVE, STOCK_REQUEST_PROCESS, STOCK_REQUEST_READ_ALL,
        STOCK_ADJUSTMENT_EXECUTE,
        RETURN_READ_ALL, RETURN_RECEIVE,
        DASHBOARD_INVENTORY
    )),

    CUSTOMER(EnumSet.of(
        AUTH_LOGIN, AUTH_REGISTER, AUTH_REFRESH_TOKEN, AUTH_LOGOUT,
        ACCOUNT_VIEW_SELF, ACCOUNT_UPDATE_SELF,
        CATALOG_READ, CATALOG_SEARCH,
        CART_READ_SELF, CART_WRITE_SELF,
        ORDER_CREATE, ORDER_READ_SELF, ORDER_CANCEL_SELF, ORDER_CONFIRM_RECEIVED,
        PAYMENT_INITIATE, PAYMENT_CALLBACK,
        PROMOTION_VALIDATE,
        RETURN_CREATE, RETURN_READ_SELF
    )),

    GUEST(EnumSet.of(
        AUTH_LOGIN, AUTH_REGISTER,
        CATALOG_READ, CATALOG_SEARCH
    ));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }
}
