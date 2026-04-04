package iuh.fit.se.modules.catalog.adapter.inbound.web;

import iuh.fit.se.modules.catalog.application.port.in.CategoryUseCase;
import iuh.fit.se.modules.catalog.domain.Category;
import iuh.fit.se.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CategoryController — Inbound Adapter cho Category.
 */
@RestController
@RequestMapping("/api/v1/catalog/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryUseCase categoryUseCase;

    @PreAuthorize("hasAuthority('CATALOG_READ')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Category>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(categoryUseCase.getAllCategories()));
    }

    @PreAuthorize("hasAuthority('CATALOG_CATEGORY_WRITE')")
    @PostMapping
    public ResponseEntity<ApiResponse<Category>> create(@RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo danh mục thành công", 
                categoryUseCase.createCategory(request.name())));
    }

    @PreAuthorize("hasAuthority('CATALOG_CATEGORY_WRITE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> update(@PathVariable Long id, @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công", 
                categoryUseCase.updateCategory(id, request.name())));
    }

    @PreAuthorize("hasAuthority('CATALOG_CATEGORY_WRITE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryUseCase.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa danh mục thành công", null));
    }

    record CategoryRequest(String name) {}
}
