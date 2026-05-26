package iuh.fit.se.modules.home.adapter.inbound.web;

import iuh.fit.se.modules.home.application.port.in.HomeDiscoveryUseCase;
import iuh.fit.se.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeDiscoveryController {

    private final HomeDiscoveryUseCase homeDiscoveryUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<HomeDiscoveryUseCase.HomeDiscoveryResponse>> getDiscovery(
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(homeDiscoveryUseCase.getDiscovery(limit)));
    }

    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<HomeDiscoveryUseCase.HomeBookResponse>>> getTrending(
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(homeDiscoveryUseCase.getTrendingBooks(limit)));
    }

    @GetMapping("/hot-books")
    public ResponseEntity<ApiResponse<List<HomeDiscoveryUseCase.HomeBookResponse>>> getHotBooks(
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(homeDiscoveryUseCase.getHotBooks(limit)));
    }

    @GetMapping("/shock-sale")
    public ResponseEntity<ApiResponse<List<HomeDiscoveryUseCase.HomeBookResponse>>> getShockSale(
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(homeDiscoveryUseCase.getShockSaleBooks(limit)));
    }

    @GetMapping("/rankings")
    public ResponseEntity<ApiResponse<List<HomeDiscoveryUseCase.HomeBookResponse>>> getRanking(
            @RequestParam(defaultValue = "BEST_SELLER") String type,
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(homeDiscoveryUseCase.getRankingBooks(type, limit)));
    }
}
