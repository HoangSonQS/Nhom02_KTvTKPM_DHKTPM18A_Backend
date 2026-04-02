-- V7: Khởi tạo bảng Lưu trữ Reservation tạm thời cho mã giảm giá (Coupon)
-- Ngăn chặn triệt để tình trạng Race Condition và hỗ trợ Saga Compensation

CREATE SEQUENCE prm_coupon_reservation_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE prm_coupon_reservation (
    id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    reference_id VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL, -- RESERVED, RELEASED, CONFIRMED
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_prm_reservation_coupon FOREIGN KEY (coupon_id) REFERENCES prm_coupon(id)
);

-- Bảo đảm tính Idempotent bằng cách không cho phép tạo 2 reservation trùng ReferenceId cho cùng 1 chiến dịch
CREATE UNIQUE INDEX idx_prm_coupon_res_ref ON prm_coupon_reservation(reference_id);
-- Đánh Index để Job quét dọn hoạt động nhanh cục bộ
CREATE INDEX idx_prm_coupon_res_status_exp ON prm_coupon_reservation(status, expires_at);
