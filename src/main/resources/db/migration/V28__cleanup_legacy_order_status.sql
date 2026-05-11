-- V28: Cleanup Legacy OrderStatus — Xóa cột `status` cũ sau khi migration V27 hoàn tất.
-- Prerequisites:
--   • V27 đã chạy thành công (cột fulfillment_status đã có dữ liệu đầy đủ).
--   • Toàn bộ code Java đã switch sang FulfillmentStatus (không còn reference OrderStatus trong production code).
-- Tham chiếu: order_status_refactor_plan_v2.md — Phase 3 (Final Cleanup)

-- ============================================================
-- Step 1: Drop index của cột status cũ (nếu tồn tại)
-- ============================================================
DROP INDEX IF EXISTS idx_ord_order_status;

-- ============================================================
-- Step 2: Drop cột status cũ
-- ============================================================
ALTER TABLE ord_order
    DROP COLUMN IF EXISTS status;

-- ============================================================
-- NOTE: Sau migration này, source of truth duy nhất cho
-- trạng thái xử lý đơn hàng là cột fulfillment_status.
-- OrderStatus.java enum có thể được xóa khỏi codebase.
-- ============================================================
