package iuh.fit.se.modules.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PaymentModuleSmokeTest {

    @Test
    void contextLoads() {
        // Kiểm tra xem ApplicationContext có load thành công hay không
        // Nếu có lỗi Schema Validation, test này sẽ FAIL ngay lập tức
    }
}
