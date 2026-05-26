package iuh.fit.se.modules.notification.adapter.inbound.web;

import iuh.fit.se.modules.notification.application.port.in.NewsletterUseCase;
import iuh.fit.se.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterUseCase newsletterUseCase;

    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<NewsletterUseCase.NewsletterSubscriptionResponse>> subscribe(
            @Valid @RequestBody NewsletterSubscribeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Dang ky nhan ban tin thanh cong",
                newsletterUseCase.subscribe(request.email())
        ));
    }

    public record NewsletterSubscribeRequest(
            @NotBlank(message = "Email khong duoc de trong")
            @Email(message = "Email khong hop le")
            String email
    ) {}
}
