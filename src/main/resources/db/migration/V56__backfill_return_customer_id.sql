-- Repair return requests created while the controller used a fixed customer id.
UPDATE ret_return_requests r
SET customer_id = o.user_id
FROM ord_order o
WHERE o.id = r.order_id
  AND r.customer_id IS DISTINCT FROM o.user_id;
