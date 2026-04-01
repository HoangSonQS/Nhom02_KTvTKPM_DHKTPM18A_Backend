package iuh.fit.se.modules.auth.domain;

/**
 * Enum Role của hệ thống.
 * Nằm trong domain của module auth vì auth là nơi định nghĩa Identity.
 * Module khác reference bằng String "ROLE_ADMIN" thay vì import enum này.
 */
public enum Role {
    ADMIN,
    STAFF,
    CUSTOMER
}
