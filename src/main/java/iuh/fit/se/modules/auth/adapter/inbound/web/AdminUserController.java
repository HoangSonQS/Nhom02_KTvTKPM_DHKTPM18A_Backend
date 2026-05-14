package iuh.fit.se.modules.auth.adapter.inbound.web;

import iuh.fit.se.modules.auth.application.port.in.AdminUserUseCase;
import iuh.fit.se.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserUseCase adminUserUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminUserUseCase.UserSummary>>> listUsers() {
        return ResponseEntity.ok(ApiResponse.success(adminUserUseCase.listUsers()));
    }

    @PostMapping("/staff")
    public ResponseEntity<ApiResponse<AdminUserUseCase.UserSummary>> createStaff(
            @Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo tài khoản nhân viên thành công",
                adminUserUseCase.createStaff(new AdminUserUseCase.CreateStaffCommand(
                        request.email(),
                        request.fullName(),
                        request.password(),
                        request.role()))));
    }

    @PutMapping("/{id}/lock")
    public ResponseEntity<ApiResponse<AdminUserUseCase.UserSummary>> lockUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminUserUseCase.lockUser(id)));
    }

    record CreateStaffRequest(
            @Email(message = "Email không hợp lệ") @NotBlank(message = "Email không được để trống") String email,
            @NotBlank(message = "Họ tên không được để trống") @Size(max = 120) String fullName,
            @NotBlank(message = "Mật khẩu không được để trống") @Size(min = 8, max = 72) String password,
            @Pattern(regexp = "STAFF_SELLER|STAFF_WAREHOUSE",
                    message = "Vai trò nhân viên chỉ gồm STAFF_SELLER hoặc STAFF_WAREHOUSE") String role
    ) {}
}
