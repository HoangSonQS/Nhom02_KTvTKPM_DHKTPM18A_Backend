package iuh.fit.se.modules.auth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolePermissionTest {

    @Test
    void givenSellerStaff_whenCheckingPermissions_thenOnlySalesOperationsAreAllowed() {
        assertTrue(Role.STAFF_SELLER.getPermissions().contains(Permission.ORDER_READ_ALL));
        assertTrue(Role.STAFF_SELLER.getPermissions().contains(Permission.ORDER_UPDATE_STATUS));
        assertFalse(Role.STAFF_SELLER.getPermissions().contains(Permission.CATALOG_BOOK_CREATE));
        assertFalse(Role.STAFF_SELLER.getPermissions().contains(Permission.CATALOG_BOOK_UPDATE));
        assertFalse(Role.STAFF_SELLER.getPermissions().contains(Permission.CATALOG_CATEGORY_WRITE));
        assertFalse(Role.STAFF_SELLER.getPermissions().contains(Permission.INVENTORY_IMPORT_STOCK));
    }

    @Test
    void givenWarehouseStaff_whenCheckingPermissions_thenCatalogAndInventoryOperationsAreAllowed() {
        assertTrue(Role.STAFF_WAREHOUSE.getPermissions().contains(Permission.CATALOG_BOOK_CREATE));
        assertTrue(Role.STAFF_WAREHOUSE.getPermissions().contains(Permission.CATALOG_BOOK_UPDATE));
        assertTrue(Role.STAFF_WAREHOUSE.getPermissions().contains(Permission.CATALOG_CATEGORY_WRITE));
        assertTrue(Role.STAFF_WAREHOUSE.getPermissions().contains(Permission.INVENTORY_READ));
        assertTrue(Role.STAFF_WAREHOUSE.getPermissions().contains(Permission.INVENTORY_IMPORT_STOCK));
        assertFalse(Role.STAFF_WAREHOUSE.getPermissions().contains(Permission.ORDER_UPDATE_STATUS));
    }
}
