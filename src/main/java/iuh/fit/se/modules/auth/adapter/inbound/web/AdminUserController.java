package iuh.fit.se.modules.auth.adapter.inbound.web;

import iuh.fit.se.modules.auth.application.port.in.AdminUserUseCase;
import iuh.fit.se.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PutMapping("/{id}/lock")
    public ResponseEntity<ApiResponse<AdminUserUseCase.UserSummary>> lockUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminUserUseCase.lockUser(id)));
    }
}
