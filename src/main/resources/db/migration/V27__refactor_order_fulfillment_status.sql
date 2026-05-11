-- V27: Refactor Order Status — Tách FulfillmentStatus khỏi legacy OrderStatus
-- Backward compat: Giữ lại cột `status` cũ cho đến khi V28 migration hoàn tất.
-- Source of truth sau migration này: cột `fulfillment_status` mới.
-- Tham chiếu: order_status_refactor_plan_v2.md — Phase 2

-- ============================================================
-- Step 1: Thêm cột fulfillment_status (nullable tạm thời)
-- ============================================================
ALTER TABLE ord_order
    ADD COLUMN IF NOT EXISTS fulfillment_status VARCHAR(30);

-- ============================================================
-- Step 2: Migrate dữ liệu từ status cũ → fulfillment_status
-- Logic mapping (backward compatible):
--   PENDING_PAYMENT → PENDING      (chờ thanh toán = chưa fulfill)
--   PAID            → CONFIRMED    (đã thanh toán = order được xác nhận)
--   PROCESSING      → PROCESSING   (đang đóng gói)
--   DELIVERING      → DELIVERING   (đang giao hàng)
--   COMPLETED       → DELIVERED    (đã giao = hoàn tất)
--   CANCELLED       → CANCELLED    (đã hủy)
--   RETURNED        → DELIVERED    (đã trả hàng — fulfillment vẫn là DELIVERED, Return track ở ReturnRequest)
--   PARTIAL_RETURNED → DELIVERED   (tương tự RETURNED)
-- ============================================================
UPDATE ord_order
SET fulfillment_status = CASE status
    WHEN 'PENDING_PAYMENT'   THEN 'PENDING'
    WHEN 'PAID'              THEN 'CONFIRMED'
    WHEN 'PROCESSING'        THEN 'PROCESSING'
    WHEN 'DELIVERING'        THEN 'DELIVERING'
    WHEN 'COMPLETED'         THEN 'DELIVERED'
    WHEN 'CANCELLED'         THEN 'CANCELLED'
    WHEN 'RETURNED'          THEN 'DELIVERED'
    WHEN 'PARTIAL_RETURNED'  THEN 'DELIVERED'
    ELSE 'PENDING'
END
WHERE fulfillment_status IS NULL;

-- ============================================================
-- Step 3: Set DEFAULT và NOT NULL constraint
-- ============================================================
ALTER TABLE ord_order
    ALTER COLUMN fulfillment_status SET DEFAULT 'PENDING';

ALTER TABLE ord_order
    ALTER COLUMN fulfillment_status SET NOT NULL;

-- ============================================================
-- Step 4: Tạo index cho fulfillment_status
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_ord_order_fulfillment_status
    ON ord_order(fulfillment_status);

-- ============================================================
-- NOTE: Cột `status` cũ ĐƯỢC GIỮ LẠI ở migration này.
-- Sẽ được DROP tại V28__cleanup_legacy_order_status.sql
-- sau khi toàn bộ code đã được migrate sang fulfillment_status.
-- ============================================================
