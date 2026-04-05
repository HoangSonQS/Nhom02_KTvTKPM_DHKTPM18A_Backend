-- Flyway Migration V11: Cập nhật ràng buộc Role cho RBAC 2.0
-- Bổ sung các Role mới: STAFF_SELLER, STAFF_WAREHOUSE, GUEST

-- 1. Xóa ràng buộc CHECK cũ
ALTER TABLE auth_user DROP CONSTRAINT IF EXISTS chk_auth_user_role;

-- 2. Thêm ràng buộc CHECK mới bao gồm đầy đủ 5 Roles của Phase 5.5
ALTER TABLE auth_user ADD CONSTRAINT chk_auth_user_role 
CHECK (role IN ('ADMIN', 'STAFF_SELLER', 'STAFF_WAREHOUSE', 'CUSTOMER', 'GUEST'));

-- 3. (Tùy chọn) Chuyển đổi dữ liệu cũ nếu cần
-- Giả sử Role 'STAFF' cũ được gán mặc định cho STAFF_SELLER
UPDATE auth_user SET role = 'STAFF_SELLER' WHERE role = 'STAFF';
