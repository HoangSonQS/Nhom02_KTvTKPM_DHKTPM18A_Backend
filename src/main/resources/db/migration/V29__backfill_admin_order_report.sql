-- V29: Backfill adm_order_report từ ord_order cho các đơn hàng lịch sử
-- Lý do: Bảng adm_order_report chỉ được populate qua event-driven pipeline (AdminReportEventListener).
--        Các đơn hàng được tạo trước khi hệ thống sự kiện hoạt động sẽ không có bản ghi tương ứng.
--        Migration này đồng bộ hóa Read Model với Write Model hiện tại.
-- ON CONFLICT DO NOTHING: An toàn khi chạy lại — bỏ qua nếu bản ghi đã tồn tại.

INSERT INTO adm_order_report (
    order_id,
    customer_name,
    total_amount,
    status,
    items_summary,
    payment_method,
    checkout_at,
    paid_at,
    created_at,
    updated_at
)
SELECT
    o.id                                            AS order_id,
    COALESCE(u.full_name, 'Unknown')                AS customer_name,
    o.total_amount                                  AS total_amount,
    -- Map fulfillment_status -> report status (theo FulfillmentStatus domain)
    CASE o.fulfillment_status
        WHEN 'PENDING'    THEN 'PENDING_PAYMENT'
        WHEN 'CONFIRMED'  THEN 'CONFIRMED'
        WHEN 'PROCESSING' THEN 'PROCESSING'
        WHEN 'DELIVERING' THEN 'DELIVERING'
        WHEN 'DELIVERED'  THEN 'DELIVERED'
        WHEN 'CANCELLED'  THEN 'CANCELLED'
        ELSE o.fulfillment_status
    END                                             AS status,
    -- items_summary: tóm tắt tối đa 5 sản phẩm đầu tiên
    (
        SELECT STRING_AGG(i.book_title || ' (x' || i.quantity || ')', ', ')
        FROM ord_order_item i
        WHERE i.order_id = o.id
        LIMIT 5
    )                                               AS items_summary,
    -- payment_method: lấy từ bản ghi thanh toán thành công
    (
        SELECT p.payment_method
        FROM pay_payment p
        WHERE p.order_id = o.id
          AND p.status = 'SUCCESS'
        ORDER BY p.updated_at DESC
        LIMIT 1
    )                                               AS payment_method,
    -- checkout_at: thời điểm tạo đơn hàng (convert sang timestamptz)
    o.created_at AT TIME ZONE 'UTC'                 AS checkout_at,
    -- paid_at: thời điểm thanh toán thành công
    (
        SELECT p.updated_at AT TIME ZONE 'UTC'
        FROM pay_payment p
        WHERE p.order_id = o.id
          AND p.status = 'SUCCESS'
        ORDER BY p.updated_at DESC
        LIMIT 1
    )                                               AS paid_at,
    NOW()                                           AS created_at,
    NOW()                                           AS updated_at
FROM ord_order o
LEFT JOIN auth_user u ON u.id = o.user_id
ON CONFLICT (order_id) DO NOTHING;
