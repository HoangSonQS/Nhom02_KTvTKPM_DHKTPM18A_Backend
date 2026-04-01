package iuh.fit.se.modules.inventory;

import iuh.fit.se.modules.inventory.application.port.in.InventoryInternalUseCase;
import iuh.fit.se.modules.inventory.application.port.in.StockResult;
import iuh.fit.se.modules.inventory.adapter.outbound.persistence.InventoryJpaRepository;
import iuh.fit.se.modules.inventory.domain.InventoryStock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test") // Đảm bảo sử dụng application-test.properties (H2/Testcontainers)
public class InventoryConcurrencyIT {

    @Autowired
    private InventoryInternalUseCase inventoryService;

    @Autowired
    private InventoryJpaRepository inventoryRepository;

    private Long testBookId;

    @BeforeEach
    void setup() {
        inventoryRepository.deleteAll();
        
        // Khởi tạo 100 sản phẩm trong kho
        InventoryStock stock = InventoryStock.builder()
                .bookId(101L)
                .quantity(100)
                .version(0L)
                .build();
        inventoryRepository.save(stock);
        testBookId = 101L;
    }

    @Test
    void decreaseStock_ConcurrentRequests_ShouldNotOversell() throws InterruptedException {
        // Giả lập 150 request mua hàng đồng thời (mỗi request mua 1 cuốn)
        // Kho chỉ có 100 cuốn => Chắc chắn phải có đúng 100 request SUCCESS và 50 request OUT_OF_STOCK
        
        int numberOfThreads = 150;
        ExecutorService executorService = Executors.newFixedThreadPool(30);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger outOfStockCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    String refId = UUID.randomUUID().toString();
                    StockResult result = inventoryService.decreaseStock(testBookId, 1, refId);
                    
                    if (result.getStatus() == StockResult.Status.SUCCESS) {
                        successCount.incrementAndGet();
                    } else if (result.getStatus() == StockResult.Status.OUT_OF_STOCK) {
                        outOfStockCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }));
        }

        latch.await(30, TimeUnit.SECONDS); // Chờ tối đa 30s
        executorService.shutdown();

        // Kiểm tra kết quả
        System.out.println("✅ SUCCESS: " + successCount.get());
        System.out.println("❌ OUT_OF_STOCK: " + outOfStockCount.get());
        System.out.println("⚠️ OTHER_ERRORS/RETRY_LATER: " + errorCount.get());

        // Lấy tồn kho cuối cùng
        InventoryStock finalStock = inventoryRepository.findByBookId(testBookId).orElseThrow();
        System.out.println("📦 Final Stock: " + finalStock.getQuantity());

        // Assertions
        assertEquals(0, finalStock.getQuantity(), "Tồn kho phải về 0 do số lượng mua > tồn kho ban đầu");
        assertEquals(100, successCount.get(), "Chỉ được phép có đúng 100 giao dịch thành công");
        assertTrue(outOfStockCount.get() + errorCount.get() >= 50, "Các giao dịch còn lại phải thất bại");
    }
}
