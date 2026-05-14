package iuh.fit.se.modules.catalog.adapter.inbound.web;

import iuh.fit.se.modules.catalog.application.port.in.CategoryUseCase;
import iuh.fit.se.modules.catalog.domain.Category;
import iuh.fit.se.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * CategoryController — Inbound Adapter cho Category.
 */
@RestController
@RequestMapping("/api/v1/catalog/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryUseCase categoryUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {
        List<CategoryResponse> categories = categoryUseCase.getAllCategories().stream()
                .map(CategoryResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @PreAuthorize("hasAuthority('CATALOG_CATEGORY_WRITE')")
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo danh mục thành công",
                CategoryResponse.from(categoryUseCase.createCategory(request.name()))));
    }

    @PreAuthorize("hasAuthority('CATALOG_CATEGORY_WRITE')")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(@PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công",
                CategoryResponse.from(categoryUseCase.updateCategory(id, request.name()))));
    }

    @PreAuthorize("hasAuthority('CATALOG_CATEGORY_WRITE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryUseCase.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa danh mục thành công", null));
    }

    record CategoryRequest(@NotBlank(message = "Tên danh mục không được để trống") String name) {
    }

    record CategoryResponse(Long id, String name, boolean active) {
        static CategoryResponse from(Category category) {
            return new CategoryResponse(category.getId(), category.getName(), category.isActive());
        }
    }
}
